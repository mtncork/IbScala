/**
 * @author cork
 *
 */
package com.ib.scalaib

import java.io.PrintWriter
import java.text.SimpleDateFormat
// import java.util.Date
import org.joda.time._

import scala.collection.mutable.{ HashMap, ArrayBuffer }
import scala.concurrent._
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.Await
import scala.concurrent.duration._

import com.ib.client.CommissionReport
import com.ib.client.Contract
import com.ib.client.ContractDetails
import com.ib.client.EClientSocket
import com.ib.client.EWrapper
import com.ib.client.Execution
import com.ib.client.Order
import com.ib.client.OrderState
import com.ib.client.TickType
import com.ib.client.UnderComp

import rx.lang.scala.Observable
import rx.lang.scala.Observer
import rx.lang.scala.Subscription

import IbDataTypes._
import IbOpenOrders._


// TODO: only the basics here for now, add more detail later ??
class OrderDetailsExt( val ood: OpenOrderDetails ) {
    def toDetailString: String = {
        val s: StringBuilder = new StringBuilder()
        s ++= s"Order ID: ${ood.orderId}\n"
        s ++= s"Ticker: ${ood.contract.m_symbol}\n"
        s ++= s"Client ID: ${ood.order.m_clientId }\n"
        s ++= s"Perm ID: ${ood.order.m_permId }\n"
        s ++= s"Action: ${ood.order.m_action}\n"
        s ++= s"Quantity: ${ood.order.m_totalQuantity}\n"
        s ++= s"Order Type: ${ood.order.m_orderType}\n"
        s ++= s"Limit Price: ${ood.order.m_lmtPrice}\n"
        s ++= s"Aux Price: ${ood.order.m_auxPrice}\n"
        // OrderState fields
        s ++= s"Order Status: ${ood.orderState.m_status}\n" // Displays the order status.
        s ++= s"Warning Text: ${ood.orderState.m_warningText}\n"
        s ++= s"Commission: ${ood.orderState.m_commission}\n" // Shows the commission amount on the order.
        s ++= s"Commission Currency: ${ood.orderState.m_commissionCurrency}\n" // Shows the currency of the commission value.
        s ++= s"Equity w/Loan: ${ood.orderState.m_equityWithLoan}\n"  // Shows the impact the order would have on your equity with loan value.
        s ++= s"Init Margin: ${ood.orderState.m_initMargin}\n" // Shows the impact the order would have on your initial margin.
        s ++= s"Maint Margin: ${ood.orderState.m_maintMargin}\n" // Shows the impact the order would have on your maintenance margin.
        s ++= s"Max Commission: ${ood.orderState.m_maxCommission}\n" // Used in conjunction with the minCommission field, this defines the highest end of the possible range into which the actual order commission will fall.
        s ++= s"Min Commission: ${ood.orderState.m_minCommission}\n" // Used in conjunction with the maxCommission field, this defines the lowest end of the possible range into which the actual order commission will fall.
        s.toString
    }
}

//** TWS/IB Market Actions */
object MktAction extends Enumeration {
    type MktAction = Value
    val BUY,SELL,SSHORT = Value
}

// TWS/IB Order types with abbreviations (start with these - this list from available types for ActiveX )
object OrderType extends Enumeration {
    type OrderType = Value
    val LMT,	    // Limit 
	MKT,	        // Market 
	LIT,	        // Limit if Touched
	MIT,	        // Market if Touched
	MOC,	        // Market on Close 
	LOC,	        // Limit on Close
	PEGMKT,	    // Pegged to Market
	REL,	        // Pegged to Market
	STP,	        // Stop
	STPLMT,	    // Stop Limit
	TRAIL,	    // Trailing Stop
	TRAILLIMIT,	// Trailing Stop Limit
	VWAP,	        // Volume Weighted Average Price
	VOL		    // Volatility orders
	= Value
}

import MktAction._
import OrderType._


class IbOrder( clientId: Int, orderId: Int, permId: Int, action: MktAction,
                   orderType: OrderType, quantity: Int,
                   auxPrice: Double = 0.0, limitPrice: Double = 0.0 ) extends Order {
	// if its a limit order, we need a limit price			   
	assert( if (Set(LMT,LIT,LOC,STPLMT,TRAILLIMIT).contains(orderType)) limitPrice > 0.0
		      else true )
	// if its a stop order we need a stop price ( for relative orders a relative price )
	assert( if (Set(REL,STP,STPLMT,TRAIL,TRAILLIMIT).contains(orderType)) auxPrice > 0.0
		      else true )
	m_clientId = clientId
	m_orderId = orderId
	m_permId = permId  // permanent ID that TWS assigns, for an initial order set to orderId
	m_action = action.toString
	m_orderType = orderType.toString
	m_totalQuantity = quantity
	m_auxPrice = auxPrice
	m_lmtPrice = limitPrice
	// there are other extended fields but these are the principal ones
	//
}

trait Orders extends EWrapper {
   
    val client : EClientSocket
    def addOpenOrder( orderId: Int, openOrder: OpenOrder )
    def isOpenOrder( orderId: Int ): Boolean
    def setStdOrderStatusHandler: Unit
    
    //
  // ======================= Order Functions ====================
  //
  
  // enum for orderStatus return strings
   import OrderStatus._
   
   
   // @note PendingOrders is keyed by an order ID and returns that order's Promise 
   
   // this has all been changed to openOrders
   /*
   private var pendingOrders = HashMap[Int,Promise[OrderStatusData]]()
   
   def addPendingOrder( orderId: Int, promise: Promise[OrderStatusData] ): Unit = pendingOrders += ((orderId, promise ))
   def getAndRemovePendingOrder( orderId: Int ): Option[Promise[OrderStatusData]] = {
       if ( !pendingOrders.isDefinedAt( orderId ) ) None
       else {
           val promise = pendingOrders(orderId)
           pendingOrders -= orderId
           Some(promise)
       }
   }
   * 
   */
      	
   /*
    *@note  an order may go through multiple status changes before resolving
    *       a future implies that we'll only report one final status
    *       using an observable would allow the transparency of the changes in status ???
    *       current thought 2/10/'14 is to possibly provide both and allow the user
    *       to choose how complicated they want it to be
    * 
    */
   
   /** Places an order with TWS and returns a future of its status 
     * 
     * @param orderId A valid TWS order ID 
     * @param contract A security's contract ( currently stock, option, or future )
     * @param order Details for the order
    */
   def placeOrder( orderId: Int, contract: Contract, order: Order ): Future[OrderStatusData] = {
        val promise = Promise[OrderStatusData]()
        addOpenOrder( orderId, OpenOrder( promise, OpenOrderDetails( orderId, contract, order, null)))
        // addOpenOrder( orderId, OpenOrder( promise, OpenOrderDetails( orderId, contract, order, new OrderState() ))
        // set the order status handler
        setStdOrderStatusHandler
        client.placeOrder( orderId, contract, order )
        promise.future    
   }
   
   def cancelOrder( orderId: Int ): Boolean = {
       if ( ! isOpenOrder( orderId ) ) false
       else {
           // we have a future outstanding for this order
           // just use that to report the status
           client.cancelOrder( orderId )
           true
       }
   }
}