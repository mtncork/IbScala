package apps

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
import Utils._

import java.io.PrintWriter

object EnumSP500Options extends App {

    type Bool = Boolean
    
    // !!!
    // set to a folder that makes sense - location where we'll write the data
    // !!!
    val dataDir = ".\\src\\main\\scala\\test\\data\\"
    
    import ExecutionContext.Implicits.global 
    
    val Infinite = scala.concurrent.duration.Duration.Inf
    
    var connId = 600
    
    def pause( secs: Int = 30 ) = { println(s"Pause ... $secs seconds") ; Thread.sleep( secs * 1000 ) }
    
    def toString( c: Contract ): String = s"${c.m_symbol} ${c.m_expiry} ${c.m_strike} ${c.m_right}" 

    val year = 2015
    val startMonth = 1
    val endMonth = 2
    // NOTE: this only works over a single year boundary
    def mod12( n: Int ): Int =  { val mn = n % 12 ; if ( mn == 0 ) 12 else mn } 
    def genExpiry( month: Int, offset: Int ): String = f"${year}${mod12(month+offset)}%02d${optsExpirDates(month)}%02d"
    
    val conn = new IbConnection()
    connId += 1
    val ok = conn.connect( connId )
    if ( !ok ) {
        println( "Unable to connect to TWS !!")
        // exit
    }
    var errors = 0
    var batchTimes = Set[Long]() 
    var result: Option[Bool] = None
    val tStart = DateTime.now()
    val batchSize = 10
    val batchCount = 500 / batchSize
    println(s"+++++++ Start: ${tStart}")
    
    // val stkList = Tickers.getRandTickerList( 20 )
    for ( month <- startMonth to endMonth ) {
        val expiry = genExpiry( month, 0 )
        println("Starting Expiry: " + expiry)
        val options = new  HashMap[String,Vector[ContractDetails]]()   
        val srtdTickers = Tickers.tickers
        result = Some(true) 
        val filename = dataDir + s"SP500Options-$expiry.txt"
        println( s"Creating $filename")
        val p = new PrintWriter( filename )
        for ( i <- 0 to (batchCount-1) ) {
            println(s"Batch: ${i+1}")
            val batchStart = DateTime.now()
            result = None
            // tracing in the library - on or off
            TraceOff   // TraceOn
            // do 'batchSize' per batch at a time
            val stkList = srtdTickers.drop(i*batchSize).take(batchSize)
            // stkList foreach ( println )
            // readLine()
            val listOfFuts = stkList map ( s => conn.getOptionsContract( s, expiry, 0.00, "") 
                                                // recoverWith - use where the recovery is another future, i.e. check a subsequent month ??
                                                // recover     - use with the data structure for the future, i.e. to return with an ampty one, as here
                                                //
                                                // TODO: each of these recoverable cases should check the throwable
                                                // to confirm that its a "security not found" exception 
                                                // { case e: Throwable =>
                                                //    println(s"Checkng options for: $s in ${genExpiry(1)}")
                                                //    conn.getOptionsContract( s, genExpiry(1), 0.00, "") } recoverWith
                                                //{ case e: Throwable =>
                                                //    println(s"Checkng options for: $s in ${genExpiry(2)}")
                                                //    conn.getOptionsContract( s, genExpiry(2), 0.00, "") } 
                                                recover { 
                                                    case e: Throwable => errors += 1 ;
                                                        println(s"---> No options for: $s !!! <---")
                                                        // return an empty list
                                                        new IbData( ContractDetailsDataList( List[ContractDetails]().toVector )) 
                                                } 
                                            )
            val futList = Future.sequence( listOfFuts )
            futList onComplete {
                case Success( ibd ) => {
                    // println( s"Size of return List: ${ibd.length}" )
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
        }     
        // println(result)
        val srtdOptions = options.toList.sortBy( x => x._1 ) 
        srtdOptions foreach ( (kv) => print(s"${kv._1}: ${kv._2.length}\t") ) ; println()
        // write results to file
        for ( s <- srtdOptions ;
                sCD <- s._2  ) {
                    p.println(s"${s._1},${sCD.m_summary.m_conId},${sCD.m_summary.m_expiry},${sCD.m_summary.m_right},${sCD.m_summary.m_strike}" )
                }    
        p.close()
    }    
    // val s = conn.extractContractDetailsToString( cd.data )
    // println( s )
    // Thread.sleep( 300000 )
    conn.disconnect()
    val tEnd = DateTime.now()
    // list results
    
    
    println( s"\n*** Error count: $errors} ***" )
    println( s"*** Batch Size: $batchSize  Batch Count: $batchCount")
    println( s"*** Total Time: ${(tEnd.getMillis() - tStart.getMillis())/1000}")
    println( s"*** Sum of Batch Time: ${batchTimes.sum / 1000}")
    println( s"*** Avg Time Per Batch: ${batchTimes.sum / batchTimes.size }" )
    
 
}


