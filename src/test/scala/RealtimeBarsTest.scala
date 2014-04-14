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


@RunWith(classOf[JUnitRunner])
class RealtimeBarsTest extends FunSuite {
    
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
 
    // These tests assert only minimally.
    // TODO: look further @ ScalaTest to see how tests like these (using realtime data ) might be improved
    /*
    test("Realtime Bars Data Test 1a - request data for bogus stock ticker symbol and fail") {
        println("Realtime Bars Data Test 1a - request data for bogus stock ticker symbol and fail")
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        // request the data
       	val f = conn.getRealTimeBars( StockContract("ZQBTP"), "MIDPOINT" )
       	f.subscribe(
       	    bar => { /* bogus ticker, there should be no data */ assert(false)},
       	    e => { println(s"Failure (as expected): $e"); assert(true) },
       	    () => { println("Completed") }
       	)
        Thread.sleep( 2000 )
        conn.disconnect()
    }
    */
    
    /*
    test("Realtime Bars Data Test 1b - request data for ticker stock symbol and succeed") {
        println("Realtime Bars Data Test 1b - request data for stock ticker symbol and succeed")
        val conn = new IbConnection()
        val ok = conn.connect( connctId = 123, serverLogLevel = 5 )
        // request the data
       	val f = conn.getRealTimeBars( StockContract("GM") )
       	val sub = f.subscribe(
       	    bar => { /*  ticker OK, there should be data */ 
       	        println(s"GM: ${bar.time} O:${bar.open} H:${bar.high} L:${bar.low} C:${bar.close} V:${bar.volume} Count:${bar.tradeCount}")
       	    },
       	    e => { println(s"Failure (as expected): $e"); assert(false) },
       	    () => { println("Completed") }
       	)
        Thread.sleep( 2*60*1000 )
        sub.unsubscribe()
        conn.disconnect()
    }
    */
    
    /*
    test("Realtime Bars Data Test 1c - request all data types for stock ticker symbol and succeed") {
        println("Realtime Bars Data Test 1c - request all data types for stock ticker symbol and succeed")
        val conn = new IbConnection()
        val ok = conn.connect( connctId = 123, serverLogLevel = 5 )
        // request the data
       	val f1 = conn.getRealTimeBars( StockContract("GM"), "TRADES" )
       	val sub1 = f1.subscribe(
       	    bar => { /*  ticker OK, there should be data */ 
       	        println(s"TRADES: ${bar.time} O:${bar.open} H:${bar.high} L:${bar.low} C:${bar.close} V:${bar.volume} Trades:${bar.tradeCount}")
       	    },
       	    e => { println(s"Failure (as expected): $e"); assert(false) },
       	    () => { println("Completed") }
       	)
       	val f2 = conn.getRealTimeBars( StockContract("GM"), "MIDPOINT" )
       	val sub2 = f2.subscribe(
       	    bar => { /*  ticker OK, there should be data */ 
       	        println(s"MIDPOINT: ${bar.time} O:${bar.open} H:${bar.high} L:${bar.low} C:${bar.close} V:${bar.volume}")
       	    },
       	    e => { println(s"Failure (as expected): $e"); assert(false) },
       	    () => { println("Completed") }
       	)
       	val f3 = conn.getRealTimeBars( StockContract("GM"), "BID" )
       	val sub3 = f3.subscribe(
       	    bar => { /*  ticker OK, there should be data */ 
       	        println(s"BID: ${bar.time} O:${bar.open} H:${bar.high} L:${bar.low} C:${bar.close} V:${bar.volume}")
       	    },
       	    e => { println(s"Failure (as expected): $e"); assert(false) },
       	    () => { println("Completed") }
       	)
       	val f4 = conn.getRealTimeBars( StockContract("GM"), "ASK" )
       	val sub4 = f4.subscribe(
       	    bar => { /*  ticker OK, there should be data */ 
       	        println(s"ASK: ${bar.time} O:${bar.open} H:${bar.high} L:${bar.low} C:${bar.close} V:${bar.volume}")
       	    },
       	    e => { println(s"Failure (as expected): $e"); assert(false) },
       	    () => { println("Completed") }
       	)
        Thread.sleep( 2*60*1000 )
        sub1.unsubscribe()
        sub2.unsubscribe()
        sub3.unsubscribe()
        sub4.unsubscribe()
        Thread.sleep( 2000 )
        conn.disconnect()
    }
    */
    
    /*
    test("Realtime Bars Data Test 1d - request data for multiple (random) stock ticker symbols and unsubscribe & cancel") {
        import Tickers._
        println("Realtime Bars Data Test 1d - request data for multiple (random) stock ticker symbols and unsubscribe & cancel")
        val l1: List[String] = Tickers.getRandTickerList( 5 )
        l1 foreach ( s => print(s"$s,")) ; println()
        val conn = new IbConnection()
        val ok = conn.connect( connctId = 123, serverLogLevel = 5 )
        // request the data
        val obs = l1.map( s => conn.getRealTimeBars( StockContract(s), "TRADES" ) )
        // zip each observable wit its ID (ticker symbol) and then subscribe
        val subs = l1.zip( obs ).map( tup2 => tup2._2.subscribe(
                bar => { /*  ticker OK, there should be data */ 
       	           println(s"${tup2._1}: ${bar.time} O:${bar.open} H:${bar.high} L:${bar.low} C:${bar.close} V:${bar.volume} Trades:${bar.tradeCount}")
       	    	},
       	    	e => { println(s"Failure: $e"); assert(false) },
       	    	() => { println("${tup2._1}: Completed") }
            ))
       	Thread.sleep( 2000 )  // 2 secs
        subs.foreach( _.unsubscribe() )
        Thread.sleep( 10000 )
        conn.disconnect()
    }
    */
    
    /*    
    test("Realtime Bars Data Test 1d - request data for multiple (random) stock ticker symbols and succeed") {
        import Tickers._
        println("Realtime Bars Data Test 1d - request data for multiple (random) stock ticker symbols and succeed")
        val l1: List[String] = Tickers.getRandTickerList( 30 )
        l1 foreach ( s => print(s"$s,")) ; println()
        val conn = new IbConnection()
        val ok = conn.connect( connctId = 123, serverLogLevel = 5 )
        // request the data
        val obs = l1.map( s => conn.getRealTimeBars( StockContract(s), "TRADES" ) )
        // zip each observable wit its ID (ticker symbol) and then subscribe
        val subs = l1.zip( obs ).map( tup2 => tup2._2.subscribe(
                bar => { /*  ticker OK, there should be data */ 
       	           println(s"${tup2._1}: ${bar.time} O:${bar.open} H:${bar.high} L:${bar.low} C:${bar.close} V:${bar.volume} Trades:${bar.tradeCount}")
       	    	},
       	    	e => { println(s"Failure (as expected): $e"); assert(false) },
       	    	() => { println("${tup2._1}: Completed") }
            ))
       	
       	Thread.sleep( 1*60*1000 )  // 2 mins
        subs.foreach( _.unsubscribe() )
        Thread.sleep( 10000 )
        conn.disconnect()
    }
    */

    /*
    test("Realtime Bars Data Test 1e - request data for futures contract and succeed") {
        println("Realtime Bars Data Test 1e - request data for futures contract and succeed")
        val conn = new IbConnection()
        val ok = conn.connect( connctId = 123, serverLogLevel = 5 )
        // request the data
       	val f = conn.getRealTimeBars( FuturesContract("ES","201403") )
       	val sub = f.subscribe(
       	    bar => { /*  ticker OK, there should be data */ 
       	        println(s"ES: ${bar.time} O:${bar.open} H:${bar.high} L:${bar.low} C:${bar.close} V:${bar.volume} Count:${bar.tradeCount}")
       	    },
       	    e => { println(s"Failure: $e"); assert(false) },
       	    () => { println("Completed") }
       	)
        Thread.sleep( 1*60*1000 )
        sub.unsubscribe()
        conn.disconnect()
    }
    */
    
    test("Realtime Bars Data Test 1f - request data for options contract and succeed") {
        println("Realtime Bars Data Test 1f - request data for options contract and succeed")
        val conn = new IbConnection()
        val ok = conn.connect( connctId = 123, serverLogLevel = 5 )
        // request the data
       	val f = conn.getRealTimeBars( OptionsContract("IBM 140322C00185000") )
       	var count = 0
       	val sub = f.subscribe(
       	    bar => { /*  ticker OK, there should be data */ 
       	        println(s"IBM OPT: ${bar.time} O:${bar.open} H:${bar.high} L:${bar.low} C:${bar.close} V:${bar.volume} Count:${bar.tradeCount}"); 
       	        count += 1
       	    },
       	    e => { println(s"Failure: $e"); assert(false)},
       	    () => { println("Completed") }
       	)
        Thread.sleep( 1*60*1000 )
        sub.unsubscribe()
        assert( count >= 11)
        conn.disconnect()
    }
}