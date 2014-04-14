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

object IbPromise {

    sealed abstract class PromiseData
  
    class IbData( val data: PromiseData )
  
    // add case classes here for each type of promise that we use
    case class ContractDetailsDataList( contractDetails: Vector[ContractDetails] ) extends PromiseData
    case class ContractDetailsData( contractDetails: ContractDetails) extends PromiseData
    case class HistoricalDataData( vb: Vector[Bar2] ) extends PromiseData
    
    // case class OptionChain( options: Vector[
}

trait IbPromise {
    
  /*  
  sealed abstract class PromiseData
  
  class IbData( val data: PromiseData )
  
  // add case classes here for each type of promise that we use
  case class ContractDetailsDataList( contractDetails: Vector[ContractDetails] ) extends PromiseData
  case class ContractDetailsData( contractDetails: ContractDetails) extends PromiseData
  case class HistoricalDataData( vb: Vector[Bar2] ) extends PromiseData
  // case class ScannerParameters( xml: String ) extends PromiseData
  */
  
  import IbPromise._
  
  // the promise is always created with the IbData wrapper since the trait Promise[T] 
  // is invariant in type T
  
  // maps our request IDs to the promises created for them
  private var reqIdToPromise: HashMap[Int,Promise[IbData]] = HashMap[Int,Promise[IbData]]()
  
  /** Adds a new promise to our map.
    *  
    * @param reqId A request ID that's used as a key
    * @param promise The promise we need to add to the map. 
    */
  def addPromise( reqId: Int, promise: Promise[IbData] ): Unit = {
      reqIdToPromise += ((reqId,promise))
  }
  
  /** Returns a promise for a given request ID and if found removes it from the map.
    * 
    * @param reqId A key for the promise we seek.
    * @return Option[Promise[IbData]] An option on a promise, Some(Promise(...)) if available, None otherwise. 
    */
  def getAndRemovePromise( reqId: Int ): Option[Promise[IbData]] = {
      if ( ! reqIdToPromise.isDefinedAt( reqId ) ) None
      else {
	  	val promise = reqIdToPromise( reqId )
		reqIdToPromise -= reqId
		Some(promise)
      }
  }    

}