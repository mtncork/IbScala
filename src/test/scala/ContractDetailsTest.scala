/**
 *
 */
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
import IbPromise._

/** Tests for various contract types and their collections.
 *  
 * @author cork
 *
 */




@RunWith(classOf[JUnitRunner])
class ContractDetailsTest extends FunSuite {
    
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
    
    type Bool = Boolean
    
    import ExecutionContext.Implicits.global
    
    test("Test 1a - Get stock contract details"){
        println("\n==== Test 1a - Get stock contract details\n")
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        assert(ok)
        val futContDetails = conn.getStockContract("NFLX")
        val cd = Await.result( futContDetails, 2 seconds )
        val s = conn.extractContractDetailsToString( cd.data )
        println( s )
        conn.disconnect()
        assert(true)
        Thread.sleep( 1000 )
    }
   
    test("Test 1b - Get futures contract details"){
        println("\n==== Test 1b - Get futures contract details\n")
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        assert(ok)
        val futContDetails = conn.getFuturesContract("ES", "201406", "GLOBEX")
        val cd = Await.result( futContDetails, 2 seconds )
        val s = conn.extractContractDetailsToString( cd.data )
        println( s )
        conn.disconnect()
        assert(true)
        Thread.sleep( 1000 )
    }
    
    test("Test 1c - Get options contract details"){
        println("\n==== Test 1c - Get options contract details\n")
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        assert(ok)
        val futContDetails = conn.getOptionsContract("IBM", "201406", 175.00, "CALL")
        val cd = Await.result( futContDetails, 2 seconds )
        val s = conn.extractContractDetailsToString( cd.data )
        println( s )
        conn.disconnect()
        assert(true)
        Thread.sleep( 1000 )
    }
    
    test("Test 1d - Get stock contract details --- Failure"){
        println("\n==== Test 1d - Get stock contract details --- Failure\n")
        // this test to pass, must fail - in onComplete matches Failure
        val conn = new IbConnection()
        val ok = conn.connect( 234 )
        assert(ok)
        var result: Option[Bool] = None 
        // pass in a "bogus" ticker symbol
        val futContDetails = conn.getStockContract("ZQDY")
        futContDetails onComplete {
            case Success(cd) => result = Some(false)  
            case Failure(x)  => { println(s"Failed: $x"); result = Some(true) }
        }
        Thread.sleep( 2000 )
        conn.disconnect()
        result match {
            case Some(true)  => println("Success"); assert(true)
            case Some(false) => println("Failed"); assert(false)
            case None => println("No value returned!"); assert(false)
        }
    }
    
    
    test("Test 1e - Get a list of stock contracts and filter their contents"){
        println("\n==== Test 1e - Get a list of stock contracts and filter their contents\n")
        val l1: List[String] = List( "IBM","F" ) ++ Tickers.getRandTickerList( 10)
        // connect to acct
        val conn = new IbConnection()
        val ok = conn.connect( 234 )
        assert(ok)
        var result: Option[Bool] = None 
        // 1. create a list of futures (for the contract details )
        // 2. transform to a single future of a list 
        // 3. filter that list to give us a new list
        val l2 = Future.sequence(l1.map( s1 => conn.getStockContract( s1 )))
        val l3 = Await.result( l2, 4 seconds )
        val l4 = l3 filter( s2 => conn.extractContractDetails(s2.data).m_validExchanges.contains("NYSE") )
        val l5 = l4 map ( s3 => conn.extractContractDetails(s3.data).m_marketName )
        conn.disconnect()
        result = Some(l5.size > 0 )
        l5 map ( println )
        /*
        l5 match{
            case List("IBM","GE","F") => assert(true) ; 
            case l => println("stock list for failure") ; l map ( println ) ; assert(false)
        }
       * 
       */
        result match {
            case Some(true)  => println("Success"); assert(true)
            case Some(false) => println("Failed"); assert(false)
            case None => println("No value returned!"); assert(false)
        }
    }
    
    test("Test 1f - Find the exchanges a list of stocks has in common"){
        println("\n==== Test 1f - Find the exchanges a list of stocks has in common\n")
        // val l1: List[String] = List( "IBM", "NFLX", "GE", "F", "VZ", "AAPL" )
        val l1 = Tickers.getRandTickerList( 20 )
        // connect to acct
        val conn = new IbConnection()
        val ok = conn.connect( 234 )
        // 1. create a list of futures (for the contract details )
        // 2. transform to a single future of a list 
        // 3. filter that list to give us a new list
        val l2 = Future.sequence(l1.map( s1 => conn.getStockContract( s1 )))
        val l3 = Await.result( l2, 10 seconds )
        val acc1 = conn.extractContractDetails(l3(0).data).m_validExchanges.split(',').toSet
        val common = l3.foldLeft (acc1)((acc: Set[String],s2: IbData) => conn.extractContractDetails(s2.data).m_validExchanges.split(',').toSet & acc )
        val all = l3.foldLeft (acc1)((acc: Set[String],s2: IbData) => conn.extractContractDetails(s2.data).m_validExchanges.split(',').toSet  | acc )
        println(s"Common Exchanges: ${common.toString}")
        println(s"   All Exchanges: ${all.toString}")
        assert( (common & Set("BATS","ISLAND","ARCA")).nonEmpty)
        conn.disconnect()
        /*        
        val l5 = l4 map ( s3 => conn.extractContractDetails(s3.data).m_marketName )
        conn.disconnect
        l5 match{
            case List("IBM","GE","F") => assert(true) ; 
            case l => println("stock list for failure") ; l map ( println ) ; assert(false)
        }
        * 
        */
        Thread.sleep( 1000 )
    }
    

}