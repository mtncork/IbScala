package test

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.ib.scalaib._
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.Future._
import scala.util.{Try,Success,Failure}
import com.ib.client.ContractDetails
import ExecutionContext.Implicits.global
import IbDataTypes._
import Tickers._
import IbPromise._


@RunWith(classOf[JUnitRunner])
class HistoricalDataTest extends FunSuite {

    test("Starting tests ..."){
        println("===============================================================================")
        println("****                                                                       ****")
        println("****               These tests assume TWS is running.                      ****") 
        println("****                                                                       ****")
        println("****               If not, please start then press ENTER.                  ****")
        println("****                                                                       ****")
        println("===============================================================================")
        readLine()
        assert(true)
    }
   
    test("Test 1a - request too much data and fail") {
        println("\n==== Data Test 1 - request too much data and fail\n")
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        // request the data
       	val f = conn.getHistoricalData( StockContract("NFLX"), "20140219 16:30:00", "8 W", "1 Min" )
       	f onComplete {
            case Success( ibd ) => println( s"Naughty naughty - got something back !! size: ${conn.extractHistoricalData(ibd.data).size}") ; assert(false )
            case Failure( e: Throwable ) => println(s"Failure (as expected): $e"); assert(true)    
        }
        Thread.sleep( 2000 )
        conn.disconnect()
    }
    
    test("Test 1b - request data for bogus ticker symbol and fail") {
        println("\n==== Test 1b - request data for bogus ticker symbol and fail\n")
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        // request the data
       	val f = conn.getHistoricalData( StockContract("ZQBTP"), "20140219 16:30:00", "8 W", "1 Min" )
       	f onComplete {
            case Success( ibd ) => println( s"Naughty naughty - got something back !! size: ${conn.extractHistoricalData(ibd.data).size}") ; assert(false )
            case Failure( e: Throwable ) => println(s"Failure (as expected): $e"); assert(true)    
        }
        Thread.sleep( 2000 )
        conn.disconnect()
    }
    
    test("Test 2a - request data and succeed") {
        println("\n==== Data Test 2a - request data and succeed\n")
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        // request the data
        val maxSizeOfData = 5 /* days */ * 7 /* hrs */ * 60 /* mins */
        val minSizeOfData = 5 * 60 * 6.4
        def dataOk( s: Int ): Boolean = ( s <= maxSizeOfData && s >= minSizeOfData )
       	val f = conn.getHistoricalData( StockContract("NFLX"), 
                                                    "20140214 12:59:00" /* end date */, 
                                                    "5 D" /* duration */, 
                                                    "1 Min" /* bar size */ )
       	f onComplete {
            case Success( ibd ) => val vb = conn.extractHistoricalData(ibd.data) ; println( s"Data size: ${vb.size}") ; assert( dataOk(vb.size) )
            case Failure( e: Throwable ) => println(s"Failure (as expected): $e"); assert(false)    
        }
        Thread.sleep( 10000 )
        conn.disconnect()
    }
  
    test("Test 2b - request data for multiple tickers and succeed") {
        println("\n==== Test 2b - request data for multiple tickers and succeed\n")
        val l1: List[String] = Tickers.getRandTickerList( 5 )
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        // request the data
        // Future.sequence transforms a collection of futures to a Future of a collection 
        val l2 = Future.sequence( l1.map( s1 => conn.getHistoricalData( StockContract( s1 ), "20140214 12:59:00", "5 D", "1 Min" )))
        // when we're all done ..
        l2 onComplete {
            case Success( l3 ) => {
                // get the individual vectors
                val l4 = l3 map ( ibd => conn.extractHistoricalData(ibd.data) )
                assert( l4.size == 5 )
                val maxSizeOfData = 5 /* days */ * 7 /* hrs */ * 60 /* mins */ 
                val minSizeOfData = 5 * 6.4 * 60 
                def dataOk( s: Int ): Boolean = ( s <= maxSizeOfData && s >= minSizeOfData )
                // check the size of each vector
                val l5 = l4 filter ( d => ! dataOk( d.size) )
                // all should fit
                assert( l5.isEmpty )
                println("Historical Data Test 2b - Success")
            }
            case Failure( e ) => println( s"Historical Data Test 2b Failure: $e" ); assert(false)
        }
        //val l3 = Await.result( l2, 20 seconds )
        //
        Thread.sleep( 20000 )
        conn.disconnect()
    }
    
    
}