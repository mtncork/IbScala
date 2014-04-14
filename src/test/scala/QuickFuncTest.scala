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
class QuickFuncTest extends FunSuite {
    
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
    
    // TODO: need to add failure cases for each of these
    
    test("QuickPrice Test 1 - request good ticker and succeed"){
        println("\n==== QuickPrice Test 1 - request good ticker and succeed\n")
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        if ( ! ok ) { println (" !!!--- Did not connect !! ---!!!") ; assert ( false ) }
        // setup test result
        var result: Option[Bool] = None
        // request the data
        val f = conn.quickPrice(StockContract("IBM"))
       	f.onComplete {
       	    case Success(price) => { println(s"    IBM: $price") ; result = Some(true) }
       	    case Failure(e) => { println(s" * * * Failure: $e"); result = Some(false) }
        }
        Thread.sleep( 2000 )    
        conn.disconnect()
        result match {
            case Some(true)  => println("Success"); assert(true)
            case Some(false) => println("Failed"); assert(false)
            case None => println("No value returned!"); assert(false)
        }
    }
    
    
    test("QuickPrice Test 2 - request good ticker(s) and succeed"){
        println("\n==== QuickPrice Test 2 - request good ticker(s) and succeed\n")
        val conn = new IbConnection()
        var ok = conn.connect( 123 )
        if ( ! ok ) { println (" !!!--- Did not connect !! ---!!!") ; assert ( false ) }
        // setup test result
        var result: Option[Bool] = None
        // the ticker list
        val l = List("IBM","AAPL","GM","NFLX","MS")
        // request the data
        val p = l map ( s => conn.quickPrice(StockContract(s)))
       	val f = Future.sequence( p )
       	f.onComplete{
       	    case Success(prices) => { 
       	        l.zip(prices).foreach( tup => println(s"    ${tup._1}: ${tup._2}") ) ; result = Some((prices.size == l.size)) 
       	    }
       	    case Failure(e) => { println(s" * * * Failure: $e"); result = Some(false) }
        }
        Thread.sleep( 4000 )
        conn.disconnect()
        result match {
            case Some(true)  => println("Success"); assert(true)
            case Some(false) => println("Failed"); assert(false)
            case None => println("No value(s) returned!"); assert(false)
        }
    }

    //
    // The complete price tests seem to be very timing dependent
    //  
    test("CompletePrice Test 1 - request good ticker(s) and succeed") {
        println("\n==== CompletePrice Test 1 - request good ticker and succeed\n")
        val conn = new IbConnection()
        var ok = conn.connect( 123 )
        if ( ! ok ) { println (" !!!--- Did not connect ---!!!") ; assert ( false ) }
        // setup test result
        var result: Option[Bool] = None
        // request the data
       	val f = conn.completePrice(StockContract("IBM"))
       	f.onComplete {
       	    case Success(prices) => { prices.foreach( kv => println(s"    IBM: ${kv._1}: ${kv._2}") ) ; result = Some(true) }
       	    case Failure(e) => { println(s" * * * Failure: $e"); result = Some(false) }
        }
        Thread.sleep( 20000 )
        conn.disconnect()
        result match {
            case Some(true)  => println("Success"); assert(true)
            case Some(false) => println("Failed"); assert(false)
            case None => println("No value returned!"); assert(false)
        }
    }
    
    test("CompletePrice Test 2 - request good ticker(s) and succeed"){
        println("\n==== CompletePrice Test 2 - request good ticker(s) and succeed\n")
        val conn = new IbConnection()
        var ok = conn.connect( 123 )
        if ( ! ok ) { println (" !!!--- Did not connect !! ---!!!") ; assert ( false ) }
        val l = List("IBM","AAPL","GM","NFLX","MS")
        // setup test result
        var result: Option[Bool] = None
        // request the data
        val p = l map ( s => conn.completePrice(StockContract(s)))
       	val f = Future.sequence( p )
       	f.onComplete {
       	    case Success(prices) => { 
       	        l.zip(prices).foreach( tup => {
       	            println(s"=== Complete Prices for: ${tup._1} ===")
       	            tup._2.foreach( kv => println(s"    ${kv._1}: ${kv._2}") ) 
       	            result = Some(prices.size == l.size) 
       	        } )
       	    }
       	    case Failure(e) => { println(s" * * * Failure: $e"); result = Some(false) }
        }
        Thread.sleep( 20000 )
        conn.disconnect()
        result match {
            case Some(true)  => println("Success"); assert(true)
            case Some(false) => println("Failed"); assert(false)
            case None => println("No value returned!"); assert(false)
        }
    }
    
    test("Snapshot Test 1 - request good ticker(s) and succeed") {
        println("\n==== Snapshot Test 1 - request good ticker and succeed\n")
        val conn = new IbConnection()
        var ok = conn.connect( 123 )
        if ( ! ok ) { println (" !!!--- Did not connect !! ---!!!") ; assert ( false ) }
        // setup test result
        var result: Option[Bool] = None
        // request the data
       	val f = conn.snapshot(StockContract("IBM"))
       	f.onComplete {
       	    case Success(prices) => { prices.foreach( kv => println(s"    IBM: ${kv._1}: ${kv._2}") ) ; result = Some(true) }
       	    case Failure(e) => { println(s"* >> Failure: $e"); result = Some(false) }
        }
        Thread.sleep( 20000 )
        conn.disconnect()
        result match {
            case Some(true)  => println("Success"); assert(true)
            case Some(false) => println("Failed"); assert(false)
            case None => println("No value returned!"); assert(false)
        }
    }
    
    test("Snapshot Test 2 - request good ticker(s) and succeed"){
        println("\n==== Snapshot Test 2 - request good ticker(s) and succeed\n")
        val conn = new IbConnection()
        var ok = conn.connect( 123 )
        if ( ! ok ) { println (" !!!--- Did not connect !! ---!!!") ; assert ( false ) }
        val l = Tickers.getRandTickerList(6) 
        // setup test result
        var result: Option[Bool] = None
        // request the data
        val p = l map ( s => conn.snapshot(StockContract(s)))
       	val f = Future.sequence( p )
       	f.onComplete{
       	    case Success(prices) => { 
       	        l.zip(prices).foreach( tup => {
       	            println(s"=== Snapshot for: ${tup._1} ===")
       	            tup._2.foreach( kv => println(s"    ${kv._1}: ${kv._2}") ) 
       	            result = Some(prices.size == l.size) 
       	        } )
       	    }
       	    case Failure(e) => { println(s"* >> Failure: $e"); result = Some(false) }
        }
        Thread.sleep( 20000 )
        conn.disconnect()
        result match {
            case Some(true)  => println("Success"); assert(true)
            case Some(false) => println("Failed"); assert(false)
            case None => println("No value returned!"); assert(false)
        }
    }

    test("Snapshot Test 3 - request bad ticker(s) and fail") {
        println("\n==== Snapshot Test 3 - request bad ticker and fail\n")
        val conn = new IbConnection()
        var ok = conn.connect( 123 )
        if ( ! ok ) { println (" !!!--- Did not connect !! ---!!!") ; assert ( false ) }
        // setup test result
        var result: Option[Bool] = None
        // request the data
       	val f = conn.snapshot(StockContract("TSGWZ"))
       	f.onComplete {
       	    case Success(prices) => { prices.foreach( kv => println(s"    TSGWZ: ${kv._1}: ${kv._2}") ) ; result = Some(false) }
       	    case Failure(e) => { println(s"* >> EXPECTED Failure: $e"); result = Some(true) }
        }
        Thread.sleep( 20000 )
        conn.disconnect()
        result match {
            case Some(true)  => println("Success"); assert(true)
            case Some(false) => println("Failed"); assert(false)
            case None => println("No value returned!"); assert(false)
        }
    }
   
}