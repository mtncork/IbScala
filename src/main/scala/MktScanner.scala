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

trait MktScanner extends Errors {

    val client : EClientSocket
    def getNextReqId: Int
    
    //
    // ====================== TWS Callbacks - Scanner Functions =======================
    // 
    
    type ScannerParametersFuncType = ( String ) => Unit
    val defaultScannerParametersFunc: ScannerParametersFuncType = (_:String) => Unit
    var scannerParametersFunc: ScannerParametersFuncType = defaultScannerParametersFunc  
  
    /** TWS callback */
    def scannerParameters(xml: String) {
        // traceln(s"scannerParameters: $xml");
        scannerParametersFunc( xml )
        scannerParametersFunc = defaultScannerParametersFunc
    }

    case class ScannerData( rank: Int, contractDetails: ContractDetails, distance: String,
                                    benchmark: String, projection: String, legsStr: String ) 
	  		  
    type ScannerDataHandlerFunc = ( ScannerData ) => Unit
    def defaultScannerHandler: ScannerDataHandlerFunc = (_) => {}
  
    val scannerDataHandlers = HashMap[Int,ScannerDataHandlerFunc]() 
  
    def addScannerHandler( reqId: Int, handler: ScannerDataHandlerFunc ) =
        scannerDataHandlers += ( ( reqId, handler))
    def delScannerHandler( reqId: Int ) =
        if ( scannerDataHandlers.isDefinedAt( reqId ) ) scannerDataHandlers -= reqId
    def getScannerHandler( reqId: Int ) = 
        if ( scannerDataHandlers.isDefinedAt( reqId ) ) scannerDataHandlers( reqId)
        else defaultScannerHandler
      
    /** TWS callback */
    def scannerData( reqId: Int, rank: Int, contractDetails: ContractDetails, 
                         distance: String, benchmark: String, projection: String, legsStr: String) {
        // traceln("scannerData");
        getScannerHandler(reqId)( ScannerData(rank, contractDetails, distance, benchmark, projection, legsStr)) 
        // traceln("scannerData");
    }

    /** TWS callback */
    def scannerDataEnd(reqId: Int) {
        getScannerHandler(reqId)( ScannerData( -1, null, "DONE", "DONE", "DONE", "DONE" ) )
        delScannerHandler( reqId )
        // traceln("scannerDataEnd");
    }
    

    //
    // ====================== Connect object Exports - Scanner Functions =======================
    //  
    
    
    def getScannerParameters: Future[String] = {
        val promise = Promise[String]()
        scannerParametersFunc = setScannerParameters( promise ) _
        client.reqScannerParameters()
        promise.future	      
    }
  
  private def setScannerParameters( promise: Promise[String] )( xml: String ): Unit = {
      promise.success( xml ) 
  }
  
  type ScannerSubscription = com.ib.client.ScannerSubscription
  
  def getScannerSubscription( scanner: ScannerSubscription ): Observable[ScannerData] = {
      traceln(s"getScannerSubsription")  
      val reqId = getNextReqId
      val obsScanner: Observable[ScannerData] = Observable {
	    observer =>
	        // this function is called in the scannerData handler
       	   	def scannerHandler( data: ScannerData ) = 
          	{
       	   	    if ( data.rank == -1 && data.distance == "DONE" ) observer.onCompleted
       	   	    else observer.onNext( data )
          	}
       	   	// add our data and error handler functions
       	   	addScannerHandler( reqId, scannerHandler )
       	   	// start getting scanner data
       	   	client.reqScannerSubscription(reqId, scanner)
       	   	// TODO: should the Subcription returned here be a CompositeSubscription 
       	   	//       to allow multiple subscriptions and multiple cancellations ???
       	   	Subscription{ 
       	   	    // for this iteration there is one connection per contract and a single subscription
       	   	    // TODO: could there be problems with this cancel call ??? do we need a future to assure of the result ?? 
       	   	    // client.cancelScannerSubscription(reqId)
       	   	    // cancelScannerData(reqId)
       	   	    delScannerHandler( reqId )
       	   	    traceln(s"Unsubscribe ScannerData for $reqId")
       	   	}
	}
	// return our observable
	obsScanner
  }
  
}  