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

trait RealTimeBars extends Errors {

    val client : EClientSocket
    def getNextReqId: Int
    
    //
    // ====================== TWS Callbacks - Realtime Bar Functions =======================
    //
  
    /** Empty RTB handler */
    type RtbDataHandlerType = ( Int, Long, Double, Double, Double, Double, Long, Double, Int) => Unit
    private def emptyRtbDataHandler( reqId: Int, time: Long, open: Double, high: Double, low: Double, close: Double,
                                    volume: Long, wap: Double, count: Int): Unit = { /* empty */ }
  
    // this allows us to set a function from an external class 
    // that will select the appropriate realtime bar data handler based on the request ID (whew!)
    type SelectRtbDatHandler = ( Int ) => RtbDataHandlerType
    var selectRtbDataHandler: SelectRtbDatHandler = ( n ) => emptyRtbDataHandler  

    /** TWS callback received after requesting realtime bars.
     *
     * @param reqId ID for the request.
     * @param time Timestamp
     * @param open Open price for the bar.
     * @param high High pricce for the bar.
     * @param low Low price for the bar.
     * @param close Close price for the bar.
     * @param volume Volume for the bar.
     * @param wap Weighted average price for the bar
     * @param count When TRADES is returned, the number of trades during this bar.    	 	   
     */
    def realtimeBar( reqId: Int, time: Long, open: Double, high: Double, low: Double, close: Double,
                          volume: Long, wap: Double, count: Int) 
    {
        selectRtbDataHandler( reqId )( reqId, time, open, high, low, close, volume, wap, count )
        // traceln(s"RTB($reqId): $time $open $high $low $close $volume $count $wap")
    }

    //
    // ====================== Connect object Exports - Realtime Bar Functions =======================
    //
    
    // TODO: bring this into alignment with other handler sections
  
    private var reqIdToRtbDataHandler: HashMap[Int,RtbDataHandlerType] = HashMap[Int,RtbDataHandlerType]()
  
    /** add a handler function for a reqId */
    private def addRtbDataHandler( reqId: Int, f: RtbDataHandlerType, e: ErrorFuncType ): Unit = {
        if ( !reqIdToRtbDataHandler.isDefinedAt(reqId) )
            reqIdToRtbDataHandler += ( (reqId, f ))
        addErrorFunc( reqId, e )    
    } 
  
    private def getRtbDataHandler( reqId: Int ): RtbDataHandlerType = {
        if ( reqIdToRtbDataHandler.isDefinedAt(reqId) )
            reqIdToRtbDataHandler(reqId )
        else
            emptyRtbDataHandler 
    }
 
    private def removeRtbDataHandler( reqId: Int ) = {
        reqIdToRtbDataHandler -= reqId
        delErrorFunc( reqId )
    }
  
    selectRtbDataHandler = getRtbDataHandler _
  
    /** error handler for realtime bars - old version that used an observer*/
    private def setRtbErrorHandler( observer: Observer[Bar3] ): ErrorFuncType = {
      
        def handleErrorForRtbData ( errId: Int, errCode: Int, desc: String ) = {
            observer.onError( new Throwable( s"errCode: $errCode - $desc") )
        }	 
        // return this function ( that has captured the observer in its closure )
        handleErrorForRtbData 
    }
  
    /** Setup and return an error handler function for realtime bars
     *
     */
    private def setRtbErrorHandler( subs: MultiSubs[Bar3] ): ErrorFuncType = {
      
        def handleErrorForRtbData ( errId: Int, errCode: Int, desc: String ) = {
            subs.onError( new Throwable( s"errCode: $errCode - $desc") )
        }	 
        // return this function ( that has captured the observer in its closure )
        handleErrorForRtbData 
    }
  
    /** Request realtime bars from TWS
     * 
     * @param reqId Request ID
     * @param contract securities contract of the requested data
     * @param whatToShow Type of the data, either "MIDPOINT", "TRADES", etc  
     */
    /*
   def getRealTimeBars(reqId: Int, contract: Contract, whatToShow: String = "TRADES") = {
        errorsFor = "getRealTimeBars(): "
        client.reqRealTimeBars(reqId, contract, 5, whatToShow, false)
    }
    */
  
  /** Returns an Observable of realtime bars
    * 
    * @param contract securities contract of the requested data
    * @param whatToShow Type of the data, either "MIDPOINT", "TRADES", etc
    * TODO : other parameters here ...  
    */
    def getRealTimeBars( contract: Contract, whatToShow: String = "TRADES" ): Observable[Bar3] = {
        val reqId  = getNextReqId
        // set for multiple subscriptions
        val subs = new MultiSubs[Bar3]( { 
                traceln(s"Canceling realtime bars for: $reqId and removing data handler")
       	   		cancelRealTimeBars( reqId )
       	   		removeRtbDataHandler( reqId ) } )
        val obsRtb: Observable[Bar3] = Observable {
            observer =>
	        // this function is called in the realtime bar handler
       	   	def relayRtbFunc( reqId: Int, time: Long, open: Double, 
               		     	  high: Double, low: Double, close: Double,
               		     	  volume: Long, wap: Double, count: Int ) = {
        	    // traceln( s"$reqId $time O: $open, H: $high L: $low C: $close: V: $volume VW: $wap $count")
        	    // post to the observable
       	   	    // @note time here is number of seconds since epoch (1/1/1970), java Date needs number of millisecs since epoch
       	   	    // @note the volume returned needs a factor of 100
        	    //observers.foreach( obs => obs.onNext( Bar3( new DateTime(millisFromEpoch(time)), 
        	    //        				  open, high, low, close, volume.toInt*100, count )))
       	   	    subs.onNext( Bar3( new DateTime(millisFromEpoch(time)), 
       	   		    	       open, high, low, close, volume.toInt*100, count ))
          	}
       	   	// the following code should only be executed once
       	   	if ( subs.isEmpty ) {
       	   	    // add our data and error handler functions
       	   	    addRtbDataHandler( reqId, relayRtbFunc, setRtbErrorHandler( subs ) )
       	   	    // start getting RTBs
       	   	    traceln(s"reqRealtimeBars from client: reqId: $reqId")
       	   	    client.reqRealTimeBars(reqId, contract, 5 /* sec bars, must be this */, whatToShow, false)
       	   	}    
       	   	// return the subscription to the (new) observer
       	   	// track this observer
       	   	subs.add(observer)  // returns a Subscription
       	   	
        }
        // return our observable
        obsRtb
    }

  /** Cancel the realtime bars request from TWS for the passed request ID
    *
    * @param reqId Request ID   
    */
    def cancelRealTimeBars(reqId: Int) = {
        client.cancelRealTimeBars(reqId)
    }
  
  
}  // trait - RealTimeBars