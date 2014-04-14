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

trait MarketTick extends Errors {

  import Utils._
  import IbTickTypes._
  
  val client : EClientSocket
  def getNextReqId: Int
  
  
  /** TWS callback */
  def tickPrice(tickerId: Int, field: Int, price: Double, canAutoExecute: Int) {
    getTickHandler( tickerId )( TickPrice( field, price, canAutoExecute  == 1))
    // traceln(s"tickPrice: Id: $tickerId " + TickType.getField(field) + s" price: $price");
  }

  /** Displays a tickSize field and its value. */
  private def dispTickSize(tickerId: Int, field: Int, size: Int): Unit =
  {
      val BID_SIZE = 0
      val ASK_SIZE = 3
      val LAST_SIZE = 5
      val VOLUME_SIZE = 8
      field match {
        case BID_SIZE => traceln(s"$tickerId: tickSize: BID_SIZE: $size")
        case ASK_SIZE => traceln(s"$tickerId: tickSize: ASK_SIZE: $size")
        case LAST_SIZE => traceln(s"$tickerId: tickSize: LAST_SIZE: $size")
        case VOLUME_SIZE => traceln(s"$tickerId: tickSize: VOLUME_SIZE: $size")
        case _ => traceln(s"$tickerId: tickSize: field: $field size: $size")
      }
  }

  /** TWS callback */
  def tickSize(tickerId: Int, field: Int, size: Int) {
      // hesed sizes come through WITHOUT the size multiplier !!
    getTickHandler( tickerId )( TickSize( TickSizeType(field), size /* *100 */ ) )
  }

  /** TWS callback */
  def tickGeneric(tickerId: Int, tickType: Int, value: Double) {
    // get the tick handler for this ticker (request) id
    getTickHandler( tickerId )( TickGeneric( tickType, value ) )
    // traceln(s"tickGeneric: Id: $tickerId tickType: $tickType value: $value");
  }
  
  private def chkInt( s: String): Int = { if ( s == "" | s.size == 0 | s.isEmpty ) 0 else s.toInt }
  private def chkDouble( s: String): Double = { if ( s == "" | s.size == 0 | s.isEmpty ) 0d else s.toDouble }
  private def chkLong( s: String): Long = { if ( s == "" | s.size == 0 | s.isEmpty ) 0L else s.toLong }
  
  /** TWS callback */
  def tickString(tickerId: Int, tickType: Int, value: String): Unit = {
    tickType match {
        case TickType.RT_VOLUME => {
            // trace(s"RTVolume: $value");
            val part = value.split(';')
            part.toList match  {
                case price :: size :: curtime :: totalvol :: vwap :: singletrade :: Nil => {
                    // TODO: conversions protected (but better way ??)
                    //
                    // added volume/size multiplier
                    //       if we multiply by 1000 ( millisFromEpoch ) we get wrong time
                    //       looking at the resolution, the timestamp here has valid millisecs precision !
                    // 4/2/2014 - volume here needs multiplier ( * 100 ), size I don't think so !
                    //            ( where in docs is this ?? )
                    //            (again) looking at single trades that come through and how they affect the total volume
                    //             the  size MUST BE * 100, or the numbers don't work out, i.e:
                    // 1000: 1396463374083 Price: 16.41 Size: 1   Total Volume: 264556 VWap: 16.3253171 Single Trade: true
                    // 1000: 1396463376841 Price: 16.41 Size: 265 Total Volume: 264821 VWap: 16.32540184 Single Trade: false
                    // 1000: 1396463377128 Price: 16.41 Size: 27  Total Volume: 264848 VWap: 16.32541046 Single Trade: false
                    // 1000: 1396463377417 Price: 16.40 Size: 1   Total Volume: 264849 VWap: 16.32541074 Single Trade: true
                    // 1000: 1396463377697 Price: 16.41 Size: 9   Total Volume: 264858 VWap: 16.32541362 Single Trade: true
                    // 1000: 1396463378846 Price: 16.41 Size: 9   Total Volume: 264867 VWap: 16.32541648 Single Trade: false
                    //
                    // the above example was produced with the following traceln (notice without the multipliers):
                    // this was for Ford (F) whose volume that day (at that time) was ~ 27M shares
                    ///  traceln(s"$tickerId: $curtime Price: $price Size: $size Total Volume: $totalvol VWap: $vwap Single Trade: $singletrade")
                    //
                    getTickHandler( tickerId )( TickRtVolume( chkDouble(price), chkInt(size) * 100, 
                            			new DateTime(chkLong(curtime)), chkInt(totalvol)*100, chkDouble(vwap), singletrade == "true" ) )
                    // traceln(s"$tickerId: $curtime Price: $price Size: $size Total Volume: $totalvol VWap: $vwap Single Trade: $singletrade")
                }
                case _ => getTickHandler( tickerId )( TickString( tickType, s"Problem parsing: $value" ))
            }
        }
        case TickType.LAST_TIMESTAMP => {
            // this has a time value based on seconds since epoch, we need milliseconds
            // traceln(s"reqId: $tickerId ${Field(tickType)}: Time: ($value) ${dateFormat.format(new Date(value.toLong*1000))}")
            getTickHandler( tickerId )( TickLastTimestamp( new DateTime(millisFromEpoch(value.toLong)) ))
        }
            
        case _ => {
            // traceln(s"${Field(tickType)}: tickString: $value")
            getTickHandler( tickerId )( TickString( tickType, s"?? $value" ))
        }    
      }
  }
  
  // type TickHandlerType = ( Tick ) => Unit
  private val defaultTickHandler: TickHandlerType = (t:Tick) => traceln(s"defTickHandler: ${t.toString}")
  
  // map of request IDs to tickHandler objects   
  private val tickHandlers = HashMap[Int,TickHandlerType]()
  
  def addTickHandler( reqId: Int, handler: TickHandlerType, e: ErrorFuncType ): Unit = {
      if ( !tickHandlers.isDefinedAt( reqId) )
          tickHandlers += ( (reqId, handler) )
      addErrorFunc( reqId, e)
  }
  
  def delTickHandler( reqId: Int ): Unit = {
      if ( tickHandlers.isDefinedAt( reqId ) ) tickHandlers -= reqId
      delErrorFunc( reqId )
  }
  
  protected def getTickHandler( reqId: Int ): TickHandlerType = 
        if ( tickHandlers.isDefinedAt( reqId ) ) tickHandlers( reqId )
        else defaultTickHandler
  
          		   
  val dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS") // the SSS part indicates milliseconds
  
  
  /** TWS callback */
  def tickSnapshotEnd(tickerId: Int) {
    // traceln(s"tickSnapshotEnd for $tickerId");
    getTickHandler(tickerId)(TickComplete())        
  }

  /** TWS callback */
  def tickOptionComputation( tickerId: Int, field: Int, impliedVol: Double,
	  		     delta: Double, optPrice: Double, pvDividend: Double,
	  		     gamma: Double, vega: Double, theta: Double, undPrice: Double) {
    val toc = TickOptionCalc( OptionCalcType(field), impliedVol, delta, optPrice, pvDividend, gamma, vega, theta, undPrice )
   
    // traceln(s"tickOptionComputation($tickerId): ${toc.toString}")
    
    getTickHandler(tickerId)( TickOptionCalc( OptionCalcType(field), impliedVol,
                                        delta, optPrice, pvDividend, gamma, vega, theta, undPrice ))
                                       
                                        
  }

  /** TWS callback */
  def tickEFP(tickerId: Int, tickType: Int, basisPoints: Double,
    formattedBasisPoints: String, impliedFuture: Double, holdDays: Int,
    futureExpiry: String, dividendImpact: Double, dividendsToExpiry: Double) {
    traceln("tickEFP");
  }
  
  private def setTickErrorHandler( subs: MultiSubs[Tick] ): ErrorFuncType = {
     def handleErrorForTickData ( errId: Int, errCode: Int, desc: String ) = {
         subs.onError( new Throwable( s"errCode: $errCode - $desc") )
     }    
     // return this function ( that has captured the observer in its closure )
     handleErrorForTickData 
    }
  
  // =======================================================================
  
  /** @note These functions are available through the connect object
    *
    *
    */
    
  /** Request market data ( reported through tick functions ) from TWS */
  private def getMarketData(reqId: Int, contract: Contract) = {
    errorsFor = "getMarketData(): "
    client.reqMktData(reqId, contract, "233", false)
  }

  /** Returns an Observable of Tick data, the primary, user-callable function
    *
    * @param contract securities contract of the requested data
    * @param tickTypes list generic tick types ( default = 233, returns RTVolume )
    */    
  def getMarketData( contract: Contract, tickTypes: List[Int] = List(233) ): Observable[Tick] = {
       val reqId  = getNextReqId
       // setup our subscription manager
       val subs = new MultiSubs[Tick]( {
       			    cancelMarketData(reqId)
       	       		delTickHandler(reqId)
       	       		traceln(s"Unsubscribe Tick Data for $reqId: ${contract.m_symbol}") } )
       // create an observable ( which we'll return )	       		
       val obsTicks: Observable[Tick] = Observable {
	    observer =>
	        def tickHandler( tick: Tick ): Unit = subs.onNext( tick )
	        if ( subs.isEmpty ) {
	            // create the handler function(s), and add to tickHandler list
	            // TODO: setup the error func for this
	            addTickHandler( reqId, tickHandler, setTickErrorHandler( subs ) )
	            // create string for ticktypes
	            val sTickTypes = tickTypes.mkString(",") 
	            // start getting Ticks
	            client.reqMktData(reqId, contract, sTickTypes, false) // "TODO: 233 ?? - this needs to be a param
	        }    
	        subs.add( observer ) // adds this observer and returns a subscription
	}
	// return our observable
	obsTicks
  }

  /** Cancel a market data request from TWS
    *  
    */
  def cancelMarketData(reqId: Int) = {
    errorsFor = "cancelMarketData(): "
    client.cancelMktData(reqId)
  }
  
}   // trait - MarketTick