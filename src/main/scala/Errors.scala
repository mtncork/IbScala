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

trait Errors /* extends EWrapper */ {

    /* @note all of the 'public' methods of thios trait are marked 'protected'
    * @note this prevents them from being public in the classes that inherit from this
    */

    // TODO: mechanism for notifying the setter of this ???
    //
    protected def setGenericErrorHandler( reqId: Int, prefix: String, handleError: ( Int ) => Unit ) = {
    
        def genericHandler( reqId: Int, errCode: Int, errMsg: String ) = {
            traceln( s"$prefix: Error for: reqId: $reqId - Error code: $errCode - $errMsg") ; 
            handleError( errCode ) 
            // delete self once called
            delErrorFunc( reqId )
        }
        addErrorFunc( reqId, genericHandler ) 
    }
    
      
  // Error Funcs - these are all accessed by a request ID - different handlers may be set for 
  //               different functions that are invoked ( i.e. reqContractDetails vs reqHistoricalData )
  
  type ErrorFuncType = ( Int, Int, String ) => Unit
   
  private var errorFuncs = HashMap[Int,ErrorFuncType]() ; 
   
  protected def addErrorFunc( reqId: Int, f: ErrorFuncType ): Unit = errorFuncs += ( (reqId, f))
  
  protected def delErrorFunc( reqId: Int ): Unit = if ( errorFuncs. isDefinedAt( reqId )) errorFuncs -= reqId
  
  protected def getErrorFunc( reqId: Int ): ErrorFuncType = if ( errorFuncs. isDefinedAt( reqId )) errorFuncs( reqId ) 
  						  else nullErrorFunc
  
  // a global prefix string set by each of our user call-able methods
  protected var errorsFor: String = ""

  /** TWS callback for an error (not seen) */    
  def error(e: Exception) { e.printStackTrace() }

  /** TWS callback for an error (not seen) */
  def error(str: String) { traceln(str) }

  
  /** Empty error handler */
  protected def nullErrorFunc( id: Int, errorCode: Int, errorMsg: String ) = {}
  // var errorFunc = nullErrorFunc _
  

  /** Primary error function called by TWS. Defers to a caller set error function.
    *
    * @param id Request ID
    * @param errorCode TWS error code
    * @param errorMsg Text description
    * 
    * @note The action taken as the result of an error detected by TWS is deferred to
    * a user settable error function. The current implementation ignores id == -1. 
    * These seem to be merely informative. 
    */
  def error( reqId: Int, errorCode: Int, errorMsg: String) {
    // id == -1 are simply informative ( and usually OK )
    //  may need to change this later to pay attention to the errorCode as well  
    /* if ( reqId != -1 ) */ traceln(errorsFor + "Error id=" + reqId + " code=" + errorCode + " msg=" + errorMsg)
    getErrorFunc( reqId )( reqId, errorCode, errorMsg )
  }
    
    
}