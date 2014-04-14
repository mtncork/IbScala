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
import com.ib.scalaib._
import ExecutionContext.Implicits.global
import MktAction._
import OrderType._
import OrderStatus._
import scala.collection.mutable.HashMap
import Tickers._



@RunWith(classOf[JUnitRunner])
class OrderTests extends FunSuite {

    test("Starting tests ..."){
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        println("*** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ***")
        println("****                                                                       ****")
        println("**** !>!>!>             These tests place ACTIVE ORDERS!            <!<!<! ****")
        println("**** !>!>!>            Run ONLY in a Paper Trading Account         <!<!<!  ****")
        println("****                       ( You have been WARNED !!)                      ****")
        println("****                                                                       ****")
        println("*** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ** ***")
        println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        println
        println
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
    
    import ExecutionContext.Implicits.global
    /*
    test("Order Test 1: Get NextValidOrderId and advance") {
        println("Order Test 1: Get NextValidOrderId and advance")
        val conn = new IbConnection()
        val nextId = conn.getNextOrderId
        assert(nextId == -1)
        conn.connect(123)
        // give it some time to fully connect
        Thread.sleep( 1000 )
        val nextOrdId = conn.getNextOrderId
        println(s"NextOrderID (incremented from TWS ): $nextOrdId")
        assert( nextId != nextOrdId )
        conn.disconnect()
    }
    
    test("Order Test 2: Place and cancel an order - Success"){
        import OrderStatus._
        println("Order Test 2: Place and cancel an order - Success")
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        // give it some time to fully connect
        Thread.sleep( 1000 )
        assert(ok)
        val stkContract = StockContract("IBM")
        val orderId = conn.getNextOrderId
        println(s"$testHdr OrderId: $orderId" )
        val order = new IbOrder( 100, 		// clientId: Int, 
                		 orderId, 	// orderId: Int, 
                		 orderId, 	// permId: Int, 
                		 BUY,		// action: MktAction,
                		 LMT,		// orderType: OrderType, 
                		 10,		// quantity: Int,
                		 0.0,		// auxPrice: Double = 0.0, 
                		 100.00		//limitPrice: Double = 0.0 
                	       ) 
        val orderFut = conn.placeOrder(orderId, stkContract, order)
        // set callback for result
        orderFut.onComplete {
            case Success(os) => println(s"$testHdr TestStatus: ${os.status.toString}") ; assert( os.status == Cancelled )
            case Failure(x) => println(s"$testHdr Failure: ${x.getMessage}") ; assert(false)
        }
        Thread.sleep( 3000 )
        conn.cancelOrder( orderId )
        Thread.sleep( 3000 )
        conn.disconnect()
        assert(true)
    }
    */
    
    test("Order Test 2: Connect, get open orders (there should be none)") {
        println("Order Test 2: Connect, get open orders(there should be none)")
        // this assumes that test3 has just been run (and in a fashion that the orders are there and open)
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        // give it some time to fully connect
        // Thread.sleep( 1000 )
        assert(ok)
        val openOrders = conn.getOpenOrders()
        assert( openOrders.size == 0 )
        conn.disconnect
    }    
    
    
    test("Order Test 3a: Place several orders and disconnect") {
        println("Order Test 3: Place several orders and disconnect")
        val stkList = List("IBM","GE","F","AA")
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        // give it some time to fully connect
        Thread.sleep( 1000 )
        assert(ok)
        // val orderId = conn.getNextOrderId
        var ordIdToStk: HashMap[Int,String] = HashMap[Int,String]() 
        // place an order for each stock in the list
        stkList map { s => 
            		val stkContract = new StockContract(s)
            		val ordId = conn.getNextOrderId
            		ordIdToStk += ((ordId,s))
            		val order = new IbOrder( 100, 		// clientId: Int, 
                		 ordId, 	// orderId: Int, 
                		 ordId, 	// permId: Int, 
                		 BUY,		// action: MktAction,
                		 LMT,		// orderType: OrderType, 
                		 10,		// quantity: Int,
                		 0.0,		// auxPrice: Double = 0.0, 
                		 10.00		//limitPrice: Double = 0.0 
                	       )	
            		val orderFut = conn.placeOrder(ordId, stkContract, order)
            		orderFut.onFailure{ case e => { println(s"Order Test 3: Failed: ${e.getMessage}");assert(false)} }
        	}
        // give it some time then disconnect
        Thread.sleep( 10000 )
        conn.disconnect()
        // give it some time then reconnect
        Thread.sleep( 3000 )
        //conn.connect(123)
        //Thread.sleep(10000)
        //conn.disconnect()
        assert(true)
    }
    
    
    
    test("Order Test 3b: Connect, get open orders and affirm all") {
        println("Order Test 3b: Connect, get open orders and affirm all")
        // this assumes that test3 has just been run (and in a fashion that the orders are there and open)
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        // give it some time to fully connect
        // Thread.sleep( 1000 )
        assert(ok)
        val openOrders = conn.getOpenOrders()
        assert( openOrders.size == 4)
        val opens = openOrders.keys.toList.map( k => conn.getOpenOrderDetails(k).get.contract.m_symbol ).sorted
        assert( opens == List("IBM","GE","F","AA").sorted )
        conn.disconnect()
    }
    
    test("Order Test 3c: Connect, get open orders and cancel all") {
        println("Order Test 3c: Connect, get open orders and cancel all")
        // this assumes that test3 has just been run (and in a fashion that the orders are there and open)
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        // give it some time to fully connect
        // Thread.sleep( 1000 )
        assert(ok)
        val openOrders = conn.getOpenOrders()
        var ids: Set[Int] = openOrders.keys.toSet  
        assert( openOrders.size == 4 && ids.size == 4 )
        openOrders.values.toList.foreach( (f) => f onComplete { 
            	case Success(o) => println(s"Order ID: ${o.OrderId} Status: ${o.status}") ; ids -= o.OrderId  
            	case Failure(x) => println(s"Failure: ${x.getMessage}")
        } )
        openOrders.keys.foreach( (k) => conn.cancelOrder(k))
        Thread.sleep(10000)
        // if we've gotten all the callbacks then the ID set should be empty !
        assert( ids.isEmpty )
        conn.disconnect()
        assert(true)
    }
     
    
}