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
import IbPromise._


trait HistoricalData extends IbPromise  with Errors{

  import IbDataTypes._
  
  val client : EClientSocket
  def getNextReqId: Int
  
  //
  // -------------------- TWS Callbacks and Handlers -------------------------------
  //
 
  // TODO: re-look @ this in light of other callback handlers
  //
 
  // handler function types for historical data
  type HistoricalDataFunc = ( /* reqId: */ Int, /* date: */ String, /* open: */ Double, 
                                     /* high: */ Double, /* low: */ Double, /* close: */ Double, 
                                     /* volume: */ Int, /* count: */ Int, /* WAP: */ Double, 
                                     /* hasGaps: */ Boolean  ) => Unit
  def setNoHistoricalData( a: Int, date: String, open: Double, high: Double, low: Double,
                                  close: Double, volume: Int, count: Int, WAP: Double, hasGaps: Boolean  ): Unit = { Unit } 
  var setHistoricalData: HistoricalDataFunc = setNoHistoricalData _

  // this allows us to set a function from an external class 
  // that will select the appropriate historical data handler based on the request ID (whew!)
  type SelectHistDatHandler = ( Int ) => HistoricalDataFunc
  var selectHistDataHandler: SelectHistDatHandler = ( n ) => setHistoricalData
  
  /** TWS callback */
  def historicalData(reqId: Int, date: String, open: Double, high: Double, low: Double,
	  	     close: Double, volume: Int, count: Int, WAP: Double, hasGaps: Boolean) {
    // traceln(s"Thread(${Thread.currentThread().getId()}) HD($reqId): $date $open $high $low $close $volume $count $WAP $hasGaps");
    selectHistDataHandler(reqId)( reqId, date, open, high, low, close, volume, count, WAP, hasGaps )
    // getHistoricalDataHandeler( reqId )( reqId, date, open, high, low, close, volume, count, WAP, hasGaps )   
  } 
  
  // storage, keyed by request ID, for each data handler
  private var reqIdToHistDataHandler: HashMap[Int,HistoricalDataFunc] = HashMap[Int,HistoricalDataFunc]()
  
  /** add historical data handler functions ( data and error ) for a reqId */
  private def addHistoricalDataHandeler( reqId: Int, f: HistoricalDataFunc, e: ErrorFuncType ): Unit = {
      if ( !reqIdToHistDataHandler.isDefinedAt(reqId) )
          reqIdToHistDataHandler += ( (reqId, f ))
      addErrorFunc( reqId, e )    
  } 
  
  /** Return a hanfdler function for the request ID
    * if none then return an empty handler
    *
    */
  private def getHistoricalDataHandeler( reqId: Int ): HistoricalDataFunc = {
      if ( reqIdToHistDataHandler.isDefinedAt(reqId) )
        reqIdToHistDataHandler(reqId )
      else
        setNoHistoricalData 
  }
  
  /** Remove the handler for a request ID, if one exists
    *
    */
  private def removeHistoricalDataHandeler( reqId: Int ) = {
      if ( reqIdToHistDataHandler.isDefinedAt( reqId ))  reqIdToHistDataHandler -= reqId
      delErrorFunc( reqId )
  }
  
  selectHistDataHandler = getHistoricalDataHandeler _ 
  
  
  //
  // -------------------------- Connect object Exports ---------------------
  // 
  
  
  /** Sets the error data if one is encountered.
    *
    * @param errId Corresponds to the request ID
    * @param errCode A TWS error code detailing what went wrong.
    * @param desc A text description.
    * 
    * @note this function sets a failure in the relevant Promise.   
    */
  private def setErrorHandlerForHistData( observer: Observer[Bar2]/*, setComplete: => Unit */ ): ErrorFuncType = {
      
     def handleErrorForHistData ( errId: Int, errCode: Int, desc: String ) = {
         // ??? set complete flag so we don't try to cancel the data
         observer.onError( new Throwable( s"errCode: $errCode - $desc") )
     }	 
     // return this error ( that has captured the observer in its closure )
     handleErrorForHistData

  }
  
  // TODO: consider: do we want an function that returns the observable, or is this simpler
  //       way to retrieve historical data adequate
  
  /** Returns a Future for a Vector[Bar2] of Historical data for the specified security and timeframes
    * 
    * @param contract Security for the data request
    * @param endDateTime yyyymmdd hh:mm:ss tmz, for the final datetime, tmz is a timezone after a space at the end
    * @param duration datetime duration covered by the request (see IB documentation)
    * @param barSize string from set for legal bar sizes
    * 
    * TODO add the whatToShow param, useRTH param, and dateFormat param, all now hard-coded
    */
  def getHistoricalData( contract: Contract, endDateTime: String, 
                               duration: String, barSize: String ): Future[IbData] = {
    traceln(s"getHistoricalData for: ${contract.m_symbol}")  
    val reqId = getNextReqId
    val promise = Promise[IbData]()
    addPromise( reqId, promise )
    val barBuf = new ArrayBuffer[Bar2]()
    
    // create our observable
    val obsHistData: Observable[Bar2] = Observable {
        observer =>
        	
	        /**  Handler function called in the TWS historicalData callback */
	        def histDataFunc( reqId: Int, date: String, open: Double, high: Double, low: Double,
                                  close: Double, volume: Int, count: Int, WAP: Double, hasGaps: Boolean ) = 
	        {
                // end of data is signaled by 'finished' in the date field (?? where in IB docs )
  	            if ( date.startsWith("finished")){
  	                // traceln(s"histDataFunc complete: Thread(${Thread.currentThread().getId()})")
       	   	        observer.onCompleted()  
       	   	        traceln(s"Hist Data for ${contract.m_symbol} completed")
       	   	    }
       	   	    else {
                    // traceln( s"$reqId $date O: $open, H: $high L: $low C: $close: V: $volume VW: $WAP $count")
                    // post to the observer
                    observer.onNext( Bar2( date, open, high, low, close, volume.toInt ) )
       	   	    }	
          	}
            
  	        // add our handler function (to be called in the TWS callback)
	        val f = histDataFunc _
       	   	addHistoricalDataHandeler( reqId, f, setErrorHandlerForHistData ( observer )  )
	      
       	   	// request the data
       	   	getHistoricalData( reqId, contract, endDateTime, duration, barSize )
       	   	// return our subscription
       	   	Subscription{
	            // on unsubscribe, remove the historical data func ( also removes the error func for the reqID )
	            removeHistoricalDataHandeler( reqId )
       	   	    // we don't want to cancel if we have completed - TWS generates an (innocuous) error
       	   	    // that says we don't have a Historical Data Query for the request ID we used
	            
	            traceln(s"Subscription completed for reqId: $reqId" )
       	   	}
        }
    
    // subscribe to the observable
    val sub = obsHistData.subscribe(
        // add a bar
        bar => { barBuf += bar },  
        // failure, set into our future
        e => getAndRemovePromise( reqId ) match {
                case Some( p ) => p.failure(e)  ; traceln("Failure for hist data promise")
                case None => traceln("getHistoricalData: No promise for reqId: $reqId")
            },
        // completed, set the future's success value
        () => {
            traceln(s"obHistData subscription ($reqId) completed")
            val pOpt = getAndRemovePromise( reqId )
            pOpt match {
                case Some( p ) => p.success( new IbData( HistoricalDataData(barBuf.toVector) ) )
                case None => traceln("getHistoricalData (completed): No promise for reqId: $reqId")
            }
        }
    )
    // return our Future
    promise.future
  }      
        

  // the setup for this call to TWS has been done in the getHistoricalData function
  private def getHistoricalData( reqId: Int, contract: Contract, endDateTime: String, 
                              duration: String, barSize: String): Unit = {
      client.reqHistoricalData( reqId, contract, endDateTime, duration, barSize,
	    		      "TRADES", 1 /* use regular hours */ , 1)
  }
  
  
  private def cancelHistoricalData( reqId: Int ): Unit = {
      
      client.cancelHistoricalData(reqId)
      // the function reset the function that handles the data
      removeHistoricalDataHandeler( reqId )
  }
  
}