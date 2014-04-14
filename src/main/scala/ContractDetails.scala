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

trait IbContractDetails extends IbPromise with Errors {

    val client : EClientSocket
    
    def getNextReqId: Int
    // def addErrorFunc( reqId: Int, f: ErrorFuncType )
    
    //
    // ==================== TWS Callbacks - Contract Detail Functions ====================
    //
    
    // TODO: bring into alignment with other handler functions
    //       there is no add/del here !!
    type ContractDetailsFunc = ( Int, ContractDetails ) => Unit
    private def emptyContDetailsHandler = ( _: Int, _: ContractDetails ) => {} 
 //   var setContractDetails = emptyContractDetails
  
 //   def setContractDetailsHandler( f: SetContractDetailsFunc ) = setContractDetails = f
 //   def resetContractDetailsHandler = setContractDetails = emptyContractDetails
  
  /** TWS callback that receives contract data for a particular request.
    *
    * @param reqId The ID of the initiating request.
    * @param contractDetails The contract details data received from TWS
    * 
    * @note This function will set the data of a Promise created by the request,
    * and saved in a map with reqId as the key.      
    */
    def contractDetails(reqId: Int, contractDetails: ContractDetails) {
        getContDetailsHandler( reqId )( reqId, contractDetails )   
        // traceln(s"contractDetails for reqId: $reqId $contractDetails");
    }

    /** TWS callback */
    def contractDetailsEnd(reqId: Int) {
        getContDetailsHandler( reqId ) ( -1,  null )   
        traceln(s"contractDetailsEnd for reqId: $reqId");
    }

    /** TWS callback */
    def bondContractDetails(reqId: Int, contractDetails: ContractDetails) {
        traceln("bondContractDetails");
    }
    
    // storage, keyed by request ID, for each data handler
    private var reqIdToContDetailsHandler: HashMap[Int,ContractDetailsFunc] = HashMap[Int,ContractDetailsFunc]()
  
    /** add historical data handler functions ( data and error ) for a reqId */
    private def addContDetailsHandler( reqId: Int, f: ContractDetailsFunc, e: ErrorFuncType ): Unit = {
        if ( !reqIdToContDetailsHandler.isDefinedAt(reqId) )
            reqIdToContDetailsHandler += ( (reqId, f ))
        addErrorFunc( reqId, e )    
    } 
  
    /** Return a hanfdler function for the request ID
     * if none then return an empty handler
     *
     */
    private def getContDetailsHandler( reqId: Int ): ContractDetailsFunc = {
      if ( reqIdToContDetailsHandler.isDefinedAt(reqId) )
            reqIdToContDetailsHandler(reqId )
      else
            emptyContDetailsHandler
    }
  
    /** Remove the handler for a request ID, if one exists
     *
     */
    private def removeContDetailsHandler( reqId: Int ) = {
      if ( reqIdToContDetailsHandler.isDefinedAt( reqId ))  reqIdToContDetailsHandler -= reqId
      delErrorFunc( reqId )
    }
    
    //
    // ===================  Connect object Exports -Contract Details Functions ====================
    //
  
  /** Sets the ContractDetails data into the Promise for the passed request ID
    *  
    * @param reqId The request for which we have received the cContractDetails
    * @param contractDetails The data poassed to us by TWS.
    *  
    * @note This function is called within the ContractDetails callback (by TWS)
    */ 
    private def setContractDetailsFunc( reqId: Int, contDetails: ContractDetails ): Unit = {
        val promiseOpt = getAndRemovePromise( reqId ) 
        promiseOpt match {
            case Some(promise) => promise.success( new IbData(ContractDetailsData(contDetails)) )
            case None =>	throw new Exception(s"No Promise for RequestID: $reqId" )    
        }
    }
  
  /** Sets the error data if one is encountered.
    *
    * @param errId Corresponds to the request ID
    * @param errCode A TWS error code detailing what went wrong.
    * @param desc A text description.
    * 
    * @note this function sets a failure in the relevant Promise.   
    */
    private def setError ( errId: Int, errCode: Int, desc: String ) = {
        val prom = getAndRemovePromise( errId )
        prom match {
            case Some(promise) => promise.failure( new Throwable( s"errCode: $errCode - $desc") )
            // see the TODO note about error ids and codes
            case None => if ( errId != -1 ) traceln( s"errId: $errId errCode: $errCode - $desc") 
        }
    }
    
    /*  this is n ow handled through theobservable/observer
    private def constructErrorFunc( prefix: String ): ErrorFuncType = {
    
        def setError ( errId: Int, errCode: Int, desc: String ) = {
            val prom = getAndRemovePromise( errId )
            prom match {
                case Some(promise) => promise.failure( new Throwable( s"$prefix: errCode: $errCode - $desc") )
                // see the TODO note about error ids and codes
                case None => if ( errId != -1 ) traceln( s"$prefix: errId: $errId errCode: $errCode - $desc") 
            }
        }
        // return the error handler with its closure
        setError
    }
    */
    
    private def setErrorHandlerForContDetails( observer: Observer[ContractDetails]/*, setComplete: => Unit */ ): ErrorFuncType = {
      
        def handleErrorForContractDetails ( errId: Int, errCode: Int, desc: String ) = {
            // ??? set complete flag so we don't try to cancel the data
            observer.onError( new Throwable( s"errCode: $errCode - $desc") )
        }	 
        // return this error ( that has captured the observer in its closure )
        handleErrorForContractDetails
    }
  
  /** Returns the details for a futures contract in a Future.
    *
    * @param symbol Ticker symbol
    * @param expiry Expiration date of contract <YYY><MM> format
    * @param exchange Trading exchange for the contract
    * 
    * @return Future[IbData] A Future for the contract details, completed through TWS callback      
    */
    def getFuturesContract(symbol: String, expiry: String, exchange: String ): Future[IbData] = {
        // errorsFor = "getFuturesContract(): "
        // year must be after 2000 - expiry is <year><month> - checking for the '2' is quick   
        assert( expiry(0) == '2')    
        val futContract = new FuturesContract( symbol, expiry, exchange )
        getContractDetails( futContract, "getFuturesContract(): " )
    }
  
  /** Returns the details for an options contract in a Future.
    *
    * @param symbol Ticker symbol
    * @param expiry Expiration date of contract <YYY><MM> format
    * @param strike Strike price for the contract.
    * @param right The type of the option, either "CALL" or "PUT"
    * 
    * @return Future[IbData] A Future for the contract details, completed through TWS callback      
    */
    def getOptionsContract(symbol: String, expiry: String, strike: Double, right: String ): Future[IbData] = {
        // assert ( right =="PUT" || right == "CALL" )
        // errorsFor = "getOptionsContract(): "
        // year must be after 2000 - expiry is <year><month> - checking for the '2' is quick   
        assert( expiry(0) == '2')    
        val futContract = OptionsContract( symbol, expiry, strike, right )
        getContractDetails( futContract, "getOptionsContract(): " )
    }
  
  /** Returns the details for a stock contract in a Future.
    *
    * @param symbol Ticker symbol
    * 
    * @return Future[IbData] A Future for the contract details, completed through TWS callback      
    */
    def getStockContract(symbol: String): Future[IbData] = {
        // errorsFor = "getStockContract(): "
        val stkCont = new StockContract(symbol)
        getContractDetails( stkCont, s"getStockContract('$symbol'): " )
    }
  
  /** Get the details for any security's contract (workhorse function)
    * 
    * @param contract ecirity's contract (currently either stock, future, or option )
    * 
    * @return Future[IbData] A Future for the contract details    
    */
    
  /*  Old version - simple
    def getContractDetails( contract: Contract, label: String ): Future[IbData] = {
        val promise = Promise[IbData]()
        val reqid = getNextReqId
        addPromise( reqid, promise)
        setContractDetails = setContractDetailsFunc
        addErrorFunc( reqid, constructErrorFunc( label ) )
        client.reqContractDetails( reqid, contract)
        promise.future
    }
 */

    // new version, use an Obervable as in Historical Data
    def getContractDetails( contract: Contract, label: String ): Future[IbData] = {
        val reqId = getNextReqId
        traceln(s"getContractDetails for: ${contract.m_symbol} reqId: $reqId")
        val promise = Promise[IbData]()
        addPromise( reqId, promise )
        val barBuf = new ArrayBuffer[ContractDetails]()
    
        // create our observable
        val obsContDetails: Observable[ContractDetails] = Observable {
            observer =>
        	
	        /**  Handler function called in the TWS historicalData callback */
	        def contractDetailsFunc( reqId: Int, contractDetails: ContractDetails ) = {
                // end of data is signaled by 'finished' in the date field (?? where in IB docs )
  	            if ( reqId == -1 ){
  	                // traceln(s"histDataFunc complete: Thread(${Thread.currentThread().getId()})")
       	   	        observer.onCompleted()  
       	   	        traceln(s"getContractDetails Data for ${contract.m_symbol} completed")
       	   	    }
       	   	    else {
                    // traceln( s"$reqId $date O: $open, H: $high L: $low C: $close: V: $volume VW: $WAP $count")
                    // post to the observer
                    observer.onNext( contractDetails )
       	   	    }	
          	}
            
  	        // add our handler function (to be called in the TWS callback)
	        val f = contractDetailsFunc _
       	   	addContDetailsHandler( reqId, f, setErrorHandlerForContDetails( observer )  )
       	   	// request the data
       	   	client.reqContractDetails( reqId, contract)
       	   	// return our subscription
       	   	Subscription{
	            // on unsubscribe, remove the historical data func ( also removes the error func for the reqID )
	            removeContDetailsHandler( reqId )
       	   	    // we don't want to cancel if we have completed - TWS generates an (innocuous) error
       	   	    // that says we don't have a Historical Data Query for the request ID we used
	            
	            traceln(s"Subscription completed for reqId: $reqId" )
       	   	}
        }
    
    // subscribe to the observable
    val sub = obsContDetails.subscribe(
        // add a bar
        bar => { barBuf += bar },  
        // failure, set into our future
        e => getAndRemovePromise( reqId ) match {
                case Some( p ) => p.failure(e)  ; traceln(s"Failure for contract details promise ($reqId)")
                case None => traceln(s"getContractDetails: No promise for reqId: $reqId")
            },
        // completed, set the future's success value
        () => {
            traceln(s"obsContDetails subscription ($reqId) completed")
            val pOpt = getAndRemovePromise( reqId )
            pOpt match {
                case Some( p ) => p.success( new IbData( ContractDetailsDataList( barBuf.toVector ) ) )
                case None => traceln(s"getContractDetails (completed): No promise for reqId: $reqId")
            }
        }
    )
    // return our Future
    promise.future
  } 
 
    
    
   // Extend the ContractDetails class
  implicit def ContDetToContractDetailsExt( c: ContractDetails ) = new ContractDetailsExt( c )
  
  /** Return a ContractDetails object extracted from a PromiseData bundle
    * 
    * @param data A PromiseData bundle
    * @return ContractDetails A ContractDetails object
    * @throws An exception if the data in the bundle is NOT of type ContractDetailsData  
    */
  def extractContractDetails( data: PromiseData ): ContractDetails = { 
      // TODO: make this more user friendly ?!?
      assert( data.getClass() == classOf[ContractDetailsData] )	
      data.asInstanceOf[ContractDetailsData].contractDetails
  }
  
  /** Return a string representation of ContractDetails */
  def extractContractDetailsToString( data: PromiseData ): String = extractContractDetails(data).toDetailString
  
  def extractHistoricalData( data: PromiseData ): Vector[Bar2] = { 
      // TODO: make this more user friendly ?!?
      assert( data.getClass() == classOf[HistoricalDataData] )	
      data.asInstanceOf[HistoricalDataData].vb
  }
  
} // trait Contract Details

/** Extend the ContractDetails class with additional string functions
  *
  * @constructor Single param for the CopntractDetails object.   
  */
class ContractDetailsExt( val contractDetails: ContractDetails ) {
    
    def toStringWithReq( reqId: Int ): String = s"ContractDetails (for ReqID: $reqId):\n" + toString() 
    
    def toDetailString: String = {
        val s: StringBuilder = new StringBuilder()
        s ++= s"Category: ${contractDetails.m_category}"
        s ++= s"\nSub-Category: ${contractDetails.m_subcategory}"
        s ++= s"\nIndustry: ${contractDetails.m_industry}"
        s ++= s"\nLong Name: ${contractDetails.m_longName}"
        s ++= s"\nMkt Name: ${contractDetails.m_marketName}"
        s ++= s"\nTrading Class: ${contractDetails.m_tradingClass}"
        s ++= s"\nMin Tick: ${contractDetails.m_minTick}"
        s ++= s"\nPrice Magnifier: ${contractDetails.m_priceMagnifier}"
        s ++= s"\nOrder Types: ${contractDetails.m_orderTypes}"
        s ++= s"\nValid Exchanges: ${contractDetails.m_validExchanges}"
        s ++= s"\nContract Month: ${contractDetails.m_contractMonth}"
        s ++= s"\nTimezone Id: ${contractDetails.m_timeZoneId}"
        s ++= s"\nTrading Hours: ${contractDetails.m_tradingHours}"
        s ++= s"\nLiquid Hours: ${contractDetails.m_liquidHours}"
        s ++= s"\nEcon Value Rule: ${contractDetails.m_evRule}"
        s ++= s"\nEcon Value Multiplier: ${contractDetails.m_evMultiplier}"
        
        s.toString
    }
    
}