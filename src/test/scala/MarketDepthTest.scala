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
class MarketDepthTest extends FunSuite {

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
    
    test("Market Depth Data Test 1 - request mkt depth data for ticker symbol and succeed") {
        println("Market Depth Data Test 1 - request mkt depth data for ticker symbol and succeed")
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        // request the data
       	val f = conn.getMarketDepth( StockContract("GM","ISLAND"), 24 )
       	val sub = f.subscribe(
       	    md => { println( md.toString )},
       	    e => { println(s"Failure: $e"); assert(false) },
       	    () => { println("Completed") }
       	)
        Thread.sleep( 15000 )
        sub.unsubscribe()
        Thread.sleep( 1000 )
        conn.disconnect()
    }
        
}