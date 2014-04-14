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
import Utils._

object IbOpenOrders {

    case class OpenOrderDetails( orderId: Int, contract: Contract, order: Order, orderState: OrderState )
    case class OpenOrder( promise: Promise[OrderStatusData], orderDetails: OpenOrderDetails )
}    

object OrderStatus extends Enumeration {
    type OrderStatus = Value
    val Inactive,Filled, Cancelled, Submitted, PreSubmitted, PendingCancel, PendingSubmit = Value
}


import OrderStatus._
class OrderStatusData( val OrderId: Int, val status: OrderStatus, val filled: Int = 0 , val remaining: Int = 0 , val avgFillPrice: Double = 0.0, 
           		  val permId: Int = 0, val parentId: Int = 0, val lastFillPrice: Double = 0, val clientId: Int = 0, val whyHeld: String = "" ) 
 
object OrderStatusData {
       def apply( orderId: Int, status: OrderStatus, filled: Int, remaining: Int, avgFillPrice: Double, 
           	  permId: Int, parentId: Int, lastFillPrice: Double, clientId: Int, whyHeld: String ) = new
           OrderStatusData( orderId, status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld )		  
} 

trait OpenOrders extends EWrapper {

   import IbOpenOrders._
   
  // TODO: for now, open orders reside here
  //       to consider, how do we handle this with a reconnect
  //       more thought ...
  //
  
  // old stuff
  private var openOrderDetails: HashMap[Int,OpenOrderDetails] = new HashMap[Int,OpenOrderDetails]()
  
  
  private var signalComplete = {}
  
  def setSignalComplete( f: => Unit  ) = signalComplete = f
  
  private var openOrders: HashMap[Int,OpenOrder] = new HashMap[Int,OpenOrder]()
  
  def isOpenOrder( openOrderId: Int ): Boolean = openOrders.isDefinedAt( openOrderId )
  
  // ?? TODO - needs more work ???
  /** Handler function used when open orders are enumerated during a connection (???)
    * 
    */
  def openOrderHandler( orderId: Int, contract: Contract, order: Order, orderState: OrderState ): Unit = {
    // for now this simply adds to the map
    // TODO: should we check first ??, maybe save previous contents if different ??
    if ( orderId == -1 ) // openOrderEnd called, we are finished
        signalComplete
    else {
        // recreate the promise for this order ( so that we can return its future )
        val promise = Promise[OrderStatusData]()
        addOpenOrder( orderId, OpenOrder( promise, OpenOrderDetails(orderId, contract, order, orderState )))
    }	
  }
  
  def addOpenOrder( orderId: Int, openOrder: OpenOrder ): Unit = 
      openOrders += ( (orderId, openOrder ))
 
  // old stuff
  private def addOpenOrder( openOrder: OpenOrderDetails ): Unit = 
      openOrderDetails += ( (openOrder.orderId, openOrder ))
 
  
  private def delOpenOrder( orderId: Int ): Unit = 
      if ( openOrders.isDefinedAt( orderId )) openOrders -= orderId
      
  type SaveOpenOrderDetails = OpenOrderDetails => Unit
  implicit def extOpenOrders( ood: OpenOrderDetails ) = new OrderDetailsExt( ood ) 

  def defSaveOpenOrders( ood: OpenOrderDetails): Unit = {
      // default is to write to a log
      traceln( ood.toDetailString )
  }    
  
  var saveOpenOrderDetails: SaveOpenOrderDetails = defSaveOpenOrders _  
   
  // def addPendingOrder( orderId: Int, promise: Promise[OrderStatusData] ): Unit = pendingOrders += ((orderId, promise ))
  def getAndRemoveOpenOrder( orderId: Int ): Option[Promise[OrderStatusData]] = {
       if ( !openOrders.isDefinedAt( orderId ) ) { traceln(s"OpenOrder: Id: $orderId Not Found in OpenOrder map") ; None }
       else {
           val promise = openOrders(orderId).promise
           saveOpenOrderDetails( openOrders(orderId).orderDetails )
           openOrders -= orderId
           Some(promise)
       }
  }
  
  //
  
  // TWS handlers and defs
  // open order handler defs
  type OpenOrderFunc = (Int,Contract,Order,OrderState) => Unit
  private def emptyOpenOrderFunc = (_:Int,_:Contract,_:Order,_:OrderState) => {}
  var openOrderFunc: OpenOrderFunc = emptyOpenOrderFunc 
  
  /** TWS callback */
  def openOrder(orderId: Int, contract: Contract, order: Order, orderState: OrderState) {
      openOrderFunc( orderId, contract, order, orderState )
      traceln(s"openOrder called: $orderId status: ${orderState.m_status}")
  }
  // set/reset-ers for the handler
  def setOpenOrderHandler( f: OpenOrderFunc ): Unit = openOrderFunc = f 
  def resetOpenOrderHandler = openOrderFunc = emptyOpenOrderFunc

  /** TWS callback */
  def openOrderEnd() {
    // 
    openOrderFunc( -1, null, null, null )  
    traceln("openOrderEnd");
  }
  
  // status handler, for a single order changing status
  // TODO: this needs the other parameters, plus what additional do we need to do at this level .. ??
  import OrderStatus._
   
  def orderStatusHandlerForFutures( orderId: Int, status: String, filledCount: Int, remainingCount: Int,
                                             avgFillPrice: Double, permId: Int, parentId: Int, lastFillPrice: Double,
                                             clientId: Int, whyHeld: String ): Unit = {
    status match {
        case "PendingSubmit" 	=> traceln(s"ordrStatHandlr: PendingSubmit: $orderId")// ignore ??
        case "PendingCancel" 	=> traceln(s"ordrStatHandlr: PendingCancel: $orderId")// ignore
        case "PreSubmitted"  	=> traceln(s"ordrStatHandlr: PreSubmitted: $orderId")// ignore
        case "Submitted" 	    => traceln(s"Submitted: $orderId")// ignore 
        case "Cancelled" 	    => getAndRemoveOpenOrder( orderId ) match { 
                                            case None => traceln(s"No Future for: $orderId")
                                            case Some(p) => p.success(new OrderStatusData(orderId,Cancelled)) 
                                        } 
        case "Filled" 	        => getAndRemoveOpenOrder( orderId ) match { 
                                            case None => traceln(s"No Future for: $orderId")  // TODO  ?? better match something ?? need to issue a failure of some sort; 
                                            case Some(p) => p.success( OrderStatusData( orderId, Filled, filledCount, remainingCount, avgFillPrice, 
              				   				       	    permId, parentId, lastFillPrice, clientId, whyHeld )) 
                                        } 
        case "Inactive" 	    => getAndRemoveOpenOrder( orderId ) match { 
                                            case None => traceln(s"No Future for: $orderId") ; 
                                            case Some(p) => p.success(new OrderStatusData(orderId, Inactive)) 
                                        }
    	}       
    }
    
    // order status handler defs
  type OrderStatusFunc = (Int,String,Int,Int,Double,Int,Int,Double,Int,String) => Unit
  private def emptyOrderStatusFunc = (_:Int,_:String,_:Int,_:Int,_:Double,_:Int,_:Int,_:Double,_:Int,_:String) => { traceln("Empty OrderStatusFuc called")}
  private var orderStatusFunc: OrderStatusFunc = emptyOrderStatusFunc
  // set/reset-ers for the handler
  def setOrderStatusHandler( f: OrderStatusFunc ): Unit = orderStatusFunc = f 
  def resetOrderStatusHandler = orderStatusFunc = emptyOrderStatusFunc
  
  def setStdOrderStatusHandler: Unit = setOrderStatusHandler( orderStatusHandlerForFutures )
  
  /** TWS callback */
  def orderStatus( orderId: Int, status: String, filled: Int, remaining: Int,
                        avgFillPrice: Double, permId: Int, parentId: Int, lastFillPrice: Double,
                        clientId: Int, whyHeld: String) {
      // traceln(s"orderStatus called: orderId: $orderId permId: $permId status: $status")
      orderStatusFunc( orderId, status, filled, remaining, avgFillPrice, permId, parentId, lastFillPrice, clientId, whyHeld )  
  }
  
  // public, exported library functions
  
    /** Return a Map keyed by OrderId of the futures for open orders
    *  
    * @note this only returns the contents of the internal map, it
    *       does not request from TWS 
    */
    
    def getOpenOrders(): Map[Int,Future[OrderStatusData]] = {
      type MapVal = (Int,OpenOrder)
      val v = Map[Int,Future[OrderStatusData]]()
      openOrders.foldLeft( v )( (acc:Map[Int,Future[OrderStatusData]],tup2: MapVal) => acc + ((tup2._1,tup2._2.promise.future)) ) 
    }
    
  /** Return an Option for an OpenOrderDetails record
    *
    * @param orderId the ID number o\f the open order requested
    *
    */    
    def getOpenOrderDetails( orderId: Int ): Option[OpenOrderDetails] = {
      if ( !openOrders.isDefinedAt( orderId ) ) None 
      else Some( openOrders( orderId ).orderDetails )
  }
  
}