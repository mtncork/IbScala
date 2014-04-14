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

trait Quickies extends Errors {

    import IbTickTypes._
    
    val client : EClientSocket
    def getNextReqId: Int
    def addTickHandler( reqId: Int, f: TickHandlerType, e: ErrorFuncType )
    def delTickHandler( reqId: Int )
    
    /** returns a Future[Double] for the current price of the selectd security
     *
     * @param contract the security of interest
     *
     */
    def quickPrice( contract: Contract ): Future[Double] = {
        val promise = Promise[Double]()
        val reqId  = getNextReqId
                  
        def tickHandler( tick: Tick ): Unit = {
            // objects  we're interested in ignore others
            tick match {
                case TickPrice( f, price, _ ) => { 
                    if ( !promise.isCompleted && ( f == TickType.LAST || f == TickType.CLOSE )) {
                        promise.success( price )
                        // cancel mkt data
                        // no need to do this - we get TickComplete
                        // client.cancelMktData( reqId )
                    }    
                    // traceln(s"${Field(f)}: $price")
                }
                case TickComplete() =>  delTickHandler( reqId ) // remove this tick handler
                case _ => // ignore
            }
        }
        
        // setup the error func for this
        def errorFunc( e1: Int, e2: Int, desc: String ): Unit = {
            // traceln(s"quickPrice Error: $e1 - e2 - $desc")
            promise.failure(new Throwable(desc + ": " + contract.m_symbol ))
        }
        // create the handler object, and add to tickHandler list
        addTickHandler( reqId, tickHandler _, errorFunc )
            
        // start getting Ticks
        client.reqMktData(reqId, contract, "", true)
        promise.future
    }
  
    /** returns a Future[Map[Double]] for all the current prices of the selected security
     *
     * @param contract the security of interest
     *
     */
    def completePrice( contract: Contract ): Future[Map[String,Double]] = {
        val promise = Promise[Map[String,Double]]()
        val reqId  = getNextReqId
        val map = HashMap[String,Double]()
                  
        def tickHandler( tick: Tick ): Unit = {
            // objects  we're interested in ignore others
            tick match {
                case TickPrice( f, price, _ ) => { 
                    // if ( !promise.isCompleted && f == TickType.LAST ) promise.success( price )
                    map += ( (Field(f),price))
                    // traceln(s"${Field(f)}: $price")
                }
                case TickComplete() => {
                    promise.success( map.toMap )
                    // remove this tick handler
                    delTickHandler( reqId )
                }    
                case _ => // ignore
            }
        }
        // setup the error func for this
        def errorFunc( e1: Int, e2: Int, desc: String ): Unit = {
            // traceln(s"quickPrice Error: $e1 - e2 - $desc")
            promise.failure(new Throwable(desc + ": " + contract.m_symbol ))
        }
        // create the handler object, and add to tickHandler list
        addTickHandler( reqId, tickHandler _, errorFunc)
            
        // start getting Ticks
        client.reqMktData(reqId, contract, "", true)
        promise.future
    }
   
    sealed abstract class SnapshotData
    case class SnapshotTimestamp( data: DateTime ) extends SnapshotData 	{ override lazy val toString: String = data.toString }
    case class SnapshotInt( data: Int ) extends SnapshotData		{ override lazy val toString: String = data.toString }
    case class SnapshotDouble( data: Double ) extends SnapshotData	{ override lazy val toString: String = data.toString }
    case class SnapshotString( data: String ) extends SnapshotData	{ override lazy val toString: String = data }
  
    type SnapshotMap = Map[String,SnapshotData]
  
    /** returns a Future[SnapshotMap] for the current price
     *
     * @param contract the security of interest
     *
     * @note This returns the associated SnapshotData case classes:
     * @note         SnapshotTimestamp( data: DateTime )
     * @note         SnapshotInt( data: Int )
     * @note         SnapshotDouble( data: Double )
     * @note         SnapshotString( data: String )    
     *
     */
    def snapshot( contract: Contract ): Future[SnapshotMap] = {
        val promise = Promise[SnapshotMap]()
        val reqId  = getNextReqId
        val map = HashMap[String,SnapshotData]()
                  
        def tickHandler( tick: Tick ): Unit = {
            // objects  we're interested in ignore others
            tick match {
                case TickPrice( f, price, _ ) => { 
                    // if ( !promise.isCompleted && f == TickType.LAST ) promise.success( price )
                    map += ( (Field(f),SnapshotDouble(price)))
                    // traceln(s"${Field(f)}: $price")
                }
                case TickSize( f, size ) => map += ( (f.toString,SnapshotInt(size)) )
                case TickLastTimestamp( t ) => map += ( ("timestamp",SnapshotTimestamp(t)) )
                case TickComplete() => {
                    promise.success( map.toMap )
                    delTickHandler( reqId )
                }    
                case t => //traceln(s"Tick(?): ${t.toString}") // ignore
            }
        }
        // setup the error func for this
        def errorFunc( e1: Int, e2: Int, desc: String ): Unit = {
            // traceln(s"quickPrice Error: $e1 - e2 - $desc")
            promise.failure(new Throwable(desc + ": " + contract.m_symbol ))
        }
        // create the handler object, and add to tickHandler list
        addTickHandler( reqId, tickHandler _, errorFunc )
            
        // start getting Ticks
        client.reqMktData(reqId, contract, "", true)
        promise.future
    }

}  // trait Quickies