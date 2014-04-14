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
import com.ib.client.Contract

import scala.collection.mutable._

import org.joda.time._
import Tickers._

/** Tests to enumerate option chains
 *  
 * @author cork
 *
 */
@RunWith(classOf[JUnitRunner])
class EnumSP500Options extends FunSuite {
    
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
    
    val Infinite = scala.concurrent.duration.Duration.Inf
    
    var connId = 600
    
    def pause( secs: Int = 30 ) = { println(s"Pause ... $secs seconds") ; Thread.sleep( secs * 1000 ) }
    
    def toString( c: Contract ): String = s"${c.m_symbol} ${c.m_expiry} ${c.m_strike} ${c.m_right}" 

    test("Test 1: Get options chain for the SP 500 ( Calls only )"){
        println("\n==== Test 1: Get options chain for the SP 500 ( Calls only )\n")
        val conn = new IbConnection()
        connId += 1
        val ok = conn.connect( connId )
        assert(ok)
        var errors = 0
        var batchTimes = Set[Long]() 
        var result: Option[Bool] = None
        println(s"+++++++ Start: ${DateTime.now()}")
        // val stkList = Tickers.getRandTickerList( 20 )
        val options = new  HashMap[String,Vector[ContractDetails]]()   
        result = Some(true) 
        val batchSize = 50
        val batchCount = 500 / batchSize
        for ( i <- 0 to (batchCount-1) ) {
            val batchStart = DateTime.now()
            result = None
            // do 'batchSize' per batch at a time
            val stkList = Tickers.tickers.drop(i*batchSize).take(batchSize)
            // prinltn( 
            // val stkList = List( "IBM", "GM", "F", "NFLX" )
            // stkList foreach ( println )
            // readLine()
            // fix so that if we hit an error we simply get an empty list for that one
            val listOfFuts = stkList map ( s => conn.getOptionsContract( s, "201406", 0.00, "") recover { 
                                                        case e: Throwable => errors += 1 ;
                                                            new IbData( ContractDetailsDataList( List[ContractDetails]().toVector )) 
                                                        } 
                                                )
            val futList = Future.sequence( listOfFuts )
            futList onComplete {
                case Success( ibd ) => {
                    println( s"Size of return List: ${ibd.length}" )
                    println( s"Error Count: $errors" ) 
                    ibd foreach { lcd =>
                        val lCDs = lcd.data.asInstanceOf[ContractDetailsDataList].contractDetails  //foreach { d: ContractDetails => println( toString(d.m_summary)) }
                        // println( if ( l > 0 ) s"${lcd.data.asInstanceOf[ContractDetailsDataList].contractDetails(0).m_summary.m_symbol}: Option Count: $l" else "" )
                        if ( lCDs.length > 0 ) {
                            // add it
                            options += ((lCDs(0).m_summary.m_symbol, lCDs ))
                        }    
                    }
                    result = Some(true)
                }
                case Failure( e  ) => { 
                    println(s"------- End: ${DateTime.now()} FAILED !!")
                    result =  Some(false ) 
                }
            }
            // block here, for each batch 
            // println(result)
            while( result == None ){ Thread.sleep( 3000 ) }
            val batchEnd = DateTime.now()
            val lDiff: Long = batchEnd.getMillis() - batchStart.getMillis() 
            batchTimes = batchTimes + lDiff 
            // println(result)
        }        
        // val s = conn.extractContractDetailsToString( cd.data )
        // println( s )
        // Thread.sleep( 300000 )
        conn.disconnect()
        // list results
        options foreach ( (kv) => println(s"${kv._1}: ${kv._2.length}") )
        println( s"\n*** Stocks in SP500 with Options: ${500-errors} ***\n" )
        println( s"*** Batch Size: $batchSize Batch Count: ${batchTimes.size}")
        println( s"*** Total Time: ${batchTimes.sum / 1000}")
        println( s"*** Avg Time Per Batch: ${batchTimes.sum / batchTimes.size }" )
        
        result match {
            case Some(true) => assert(true)
            case Some(false) => assert(false)
            case None => assert(false)
        }
        pause()
    }
}    
    