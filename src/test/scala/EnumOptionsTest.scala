/**
 *
 */
package test

import org.scalatest.FunSuite


import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import com.ib.scalaib._
import IbPromise._
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.Future._
import scala.util.{Try,Success,Failure}
import com.ib.client.ContractDetails

import org.joda.time._

/** Tests to enumerate option chains
 *  
 * @author cork
 *
 */
@RunWith(classOf[JUnitRunner])
class EnumOptionsTest extends FunSuite {
    
    test("Starting tests ..."){
        println()
        println("*** These tests, for ContractDetails, seem to have some problems if you run ***")
        println("*** them sequentially in a short time span (? exact parameter ?). Each now  ***")
        println("*** has an explicit pause at the end.                                        ***")
        println()
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
    
    var connId = 567
    
    def pause( secs: Int = 30 ) = { println(s"Pause ... $secs seconds") ; Thread.sleep( secs * 1000 ) }

    test("Test 5: Get options chain for year and both rights ( Call+Put )"){
        println("\n==== Test 5: Get options chain for year and both rights ( Call+Put )\n")
        val conn = new IbConnection()
        connId += 1
        val ok = conn.connect( connId )
        assert(ok)
        var result: Option[Bool] = None
        println(s"+++++++ Start: ${DateTime.now()}")
        val futContDetails = conn.getOptionsContract("IBM", "2014", 0.00, "")
        // val cd = Await.result( futContDetails, 5 seconds )
        futContDetails onComplete {
            case Success( cd ) => cd.data match {
                case ContractDetailsDataList(v) => { println( s"------- End: ${DateTime.now()} Size of data: ${v.length}" ) ; result = Some(true ) }
                case _ => { println("Misunderstood !!") ; result = Some( false ) }
            }
            case Failure( e  ) => { println(s"------- End: ${DateTime.now()} FAILED !!") ;  result =  Some(false ) }
        }        
        // val s = conn.extractContractDetailsToString( cd.data )
        // println( s )
        Thread.sleep( 6000 )
        conn.disconnect()
        result match {
            case Some(true) => assert(true)
            case Some(false) => assert(false)
            case None => assert(false)
        }
        pause()
    }
    
    test("Test 1: Get options chain for particular month ( June ) and right ( Call )"){
        println("\n==== Test 1: Get options chain for particular month ( June ) and right ( Call )")
        val conn = new IbConnection()
        val ok = conn.connect( connId )
        assert(ok)
        var result: Option[Bool] = None
        println(s"+++++++ Start: ${DateTime.now()}")
        val futContDetails = conn.getOptionsContract("IBM", "201406", 0.00, "CALL")
        // val cd = Await.result( futContDetails, 5 seconds )
        futContDetails onComplete {
            case Success( cd ) => cd.data match {
                case ContractDetailsDataList(v) => { println( s"------- End: ${DateTime.now()} Size of data: ${v.length}" ) ; result = Some(true ) }
                case _ => { println(s"------- End: ${DateTime.now()} Misunderstood !!") ; result = Some( false ) }
            }
            case Failure( e  ) => { println(s"------- End: ${DateTime.now()} FAILED !!") ;  result =  Some(false ) }
        }        
        // val s = conn.extractContractDetailsToString( cd.data )
        // println( s )
        Thread.sleep( 6000 )
        conn.disconnect()
        result match {
            case Some(true) => assert(true)
            case Some(false) => assert(false)
            case None => assert(false)
        }
        pause()
    }
    
    test("Test 2: Get options chain for month and both rights ( Call+Put )"){
        println("\n==== Test 2: Get options chain for month and both rights ( Call+Put )\n")
        val conn = new IbConnection()
        connId += 1
        val ok = conn.connect( connId )
        assert(ok)
        var result: Option[Bool] = None
        println(s"+++++++ Start: ${DateTime.now()}")
        val futContDetails = conn.getOptionsContract("IBM", "201406", 0.00, "")
        // val cd = Await.result( futContDetails, 5 seconds )
        futContDetails onComplete {
            case Success( cd ) => cd.data match {
                case ContractDetailsDataList(v) => { println( s"------- End: ${DateTime.now()} Size of data: ${v.length}" ) ; result = Some(true ) }
                case _ => { println(s"------- End: ${DateTime.now()} Misunderstood !!") ; result = Some( false ) }
            }
            case Failure( e  ) => { println(s"------- End: ${DateTime.now()} FAILED !!") ;  result =  Some(false ) }
        }        
        // val s = conn.extractContractDetailsToString( cd.data )
        // println( s )
        Thread.sleep( 6000 )
        conn.disconnect()
        result match {
            case Some(true) => assert(true)
            case Some(false) => assert(false)
            case None => assert(false)
        }
        pause()
    }
    
    test("Test 3: Get options chain for particular year and right ( Call )"){
        println("\n==== Test 3: Get options chain for particular year and right ( Call )")
        val conn = new IbConnection()
        connId += 1
        val ok = conn.connect( connId )
        assert(ok)
        var result: Option[Bool] = None
        println(s"+++++++ Start: ${DateTime.now()}")
        val futContDetails = conn.getOptionsContract("IBM", "2014", 0.00, "CALL")
        // val cd = Await.result( futContDetails, 5 seconds )
        futContDetails onComplete {
            case Success( cd ) => cd.data match {
                case ContractDetailsDataList(v) => { println( s"------- End: ${DateTime.now()} Size of data: ${v.length}" ) ; result = Some(true ) }
                case _ => { println("------- End: ${DateTime.now()} Misunderstood !!") ; result = Some( false ) }
            }
            case Failure( e  ) => { println(s"------- End: ${DateTime.now()} FAILED !!") ;  result =  Some(false ) }
        }        
        // val s = conn.extractContractDetailsToString( cd.data )
        // println( s )
        Thread.sleep( 6000 )
        conn.disconnect()
        result match {
            case Some(true) => assert(true)
            case Some(false) => assert(false)
            case None => assert(false)
        }
        pause( 60 )
    }
    
    test("Test 4: Get options chain for particular year and right ( Put )"){
        println("\n==== Test 4: Get options chain for particular year and right ( Put )")
        val conn = new IbConnection()
        connId += 1
        val ok = conn.connect( connId )
        assert(ok)
        var result: Option[Bool] = None
        println(s"+++++++ Start: ${DateTime.now()}")
        val futContDetails = conn.getOptionsContract("IBM", "2014", 0.00, "PUT")
        // val cd = Await.result( futContDetails, 5 seconds )
        futContDetails onComplete {
            case Success( cd ) => cd.data match {
                case ContractDetailsDataList(v) => { println( s"------- End: ${DateTime.now()} Size of data: ${v.length}" ) ; result = Some(true ) }
                case _ => { println("------- End: ${DateTime.now()} Misunderstood !!") ; result = Some( false ) }
            }
            case Failure( e  ) => { println(s"------- End: ${DateTime.now()} FAILED !!") ;  result =  Some(false ) }
        }        
        // val s = conn.extractContractDetailsToString( cd.data )
        // println( s )
        Thread.sleep( 6000 )
        conn.disconnect()
        result match {
            case Some(true) => assert(true)
            case Some(false) => assert(false)
            case None => assert(false)
        }
        pause()
    }

 


}    