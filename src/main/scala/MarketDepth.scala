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

// class MultiSubs

/**  Implements exported functions and TWS callback functions to access market depth requests
  * 
  */
trait MarketDepth extends Errors {

    // abstracts filled in by the "mother" class
    val client : EClientSocket
    def getNextReqId: Int
 
    // classes for Market Depth
    sealed abstract class MarketDepth
    case class MktDepth( position: Int, operation: Int, side: Int, price: Double, size: Int ) extends MarketDepth {
        lazy override val toString = s"pos: $position op: $operation side: $side price: ${price}%.2f size: %size" 
    }
    // data for Level II market depth
    case class MktDepthL2( position: Int, marketMaker: String, operation: Int, side: Int, price: Double, size: Int )  extends MarketDepth  {
        lazy override val toString = s"pos: $position mktMaker: $marketMaker op: $operation side: $side price: ${price}%.2f size: %size"
    }
  
    type MktDepthHandlerType = ( MarketDepth ) => Unit
    val defaultMktDepthHandler: MktDepthHandlerType = (_) => Unit
  
    // map of request IDs to tickHandler objects   
    val MktDepthHandlers = HashMap[Int,MktDepthHandlerType]()
  
    def addMktDepthHandler( reqId: Int, handler: MktDepthHandlerType ) = MktDepthHandlers += ( (reqId, handler) )
    def delMktDepthHandler( reqId: Int ) = if ( MktDepthHandlers.isDefinedAt( reqId ) ) MktDepthHandlers -= reqId
    def getMktDepthHandler( reqId: Int ): MktDepthHandlerType = 
        if ( MktDepthHandlers.isDefinedAt( reqId ) ) MktDepthHandlers( reqId )
        else defaultMktDepthHandler

  
    /** TWS callback */
    def updateMktDepth( tickerId: Int, position: Int, operation: Int, side: Int, price: Double, size: Int) {
        getMktDepthHandler( tickerId ) ( MktDepth( position, operation, side, price, size ) )
        traceln("updateMktDepth");
    }

    /** TWS callback */
    def updateMktDepthL2( tickerId: Int, position: Int, marketMaker: String, operation: Int,
                                side: Int, price: Double, size: Int) {
        getMktDepthHandler( tickerId ) ( MktDepthL2( position, marketMaker, operation, side, price, size ) )                            
        traceln("updateMktDepthL2");
    }
    
    //
    // ======================= Connect object Exports - Market Depth Functions ====================
    // 
    
    def getMarketDepth( contract: Contract, numRows: Int ): Observable[MarketDepth] = {
        val reqId  = getNextReqId
        // setup our subscription manager
        val subs = new MultiSubs[MarketDepth]( {
                            client.cancelMktDepth(reqId)
                            delMktDepthHandler(reqId)
                            traceln(s"Unsubscribe Market Depth Data for $reqId: ${contract.m_symbol}") } )
       // create an observable ( which we'll return )	       		
       val obsMktDpth: Observable[MarketDepth] = Observable {
	    observer =>
        
	        def mktDepthHandler( md: MarketDepth ): Unit = subs.onNext( md )
            
            if ( subs.isEmpty ) {
                // create the handler function(s), and add to tickHandler list
                // TODO: setup the error func for this
                addMktDepthHandler( reqId, mktDepthHandler )
                // start getting market depth data
                client.reqMktDepth( reqId, contract, numRows ) // "TODO: 233 ?? - this needs to be a param
            }    
            subs.add( observer ) // adds this observer and returns a subscription
        }
        // return our observable
        obsMktDpth   
    }

} // trait MarketDepth