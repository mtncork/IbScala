package test

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.ib.scalaib._
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.Future._
import scala.util.{Try,Success,Failure}
import com.ib.client.Contract
import com.ib.client.ContractDetails
import ExecutionContext.Implicits.global
import com.ib.scalaib.IbDataTypes._
import test.Tickers._

import Utils._


@RunWith(classOf[JUnitRunner])
class MarketDataTickTest extends FunSuite {
    
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
    
 /*   
    test("Test 1a - request data for bogus ticker symbol and fail") {
        println("Market Data Tick Test 1a - request data for bogus ticker symbol and fail")
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        var failed = true
        // request the data
       	val f = conn.getMarketData( StockContract("ZQBTP") )
       	f.subscribe(
       	    bar => { /* bogus ticker, there should be no data */ assert(false)},
       	    e => { println(s"Expected failure: $e"); failed = false },
       	    () => { println("Completed") }
       	)
        Thread.sleep( 2000 )
        conn.disconnect()
        assert( !failed )
    }
    

   
    test("Test 1b - request tick data for ticker symbol and succeed") {
        println("Market Data Tick Test 1b - request tick data for ticker symbol and succeed")
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        // request the data
        var failed = false
       	val f = conn.getMarketData( StockContract("GM") )
       	val sub = f.subscribe(
       	    tick => { println( tick )},
       	    e => { println(s"!!! Failure: $e"); failed = true },
       	    () => { println("Completed") }
       	)
        Thread.sleep( 30 * 1000 )
        sub.unsubscribe()
        Thread.sleep( 5000 )
        conn.disconnect()
        assert( !failed )
    }
 
 */   
 /*   
    test("Test 1c - request tick data for option contract and  succeed") {
        println("Market Data Tick Test 1c - request tick data for for option contract and succeed")
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        var failed = false ;
        // request the data
        val f = conn.getMarketData( OptionsContract("GM","201406",35.00,"CALL") )
        val sub = f.subscribe(
            tick => { println( tick )},
            e => { println(s" !!! Failure: $e"); failed = true },
            () => { println("Completed") }
        )
        Thread.sleep( 30 * 1000 )
        sub.unsubscribe()
        Thread.sleep( 5000 )
        conn.disconnect()
        assert(!failed)
    }
*/   
    test("Test 1d - request tick data for option contract (using conId) and  succeed") {
        println("Test 1d - request tick data for for option contract (using conId) and succeed")
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        assert(ok)
        TraceOn
        var failed = false ;
        // request the data
        val optCont = new Contract() 
        optCont.m_conId = 142388354
        optCont.m_exchange = "CBOE"
        val f = conn.getMarketData( optCont )
        val sub = f.subscribe(
            tick => { println( tick )},
            e => { println(s" !!! Failure: $e"); failed = true },
            () => { println("Completed") }
        )
        Thread.sleep( 30 * 1000 )
        sub.unsubscribe()
        Thread.sleep( 5000 )
        conn.disconnect()
        assert(!failed)
    }

}