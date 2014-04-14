package test

import org.scalatest.FunSuite
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.ib.scalaib._
import org.joda.time.DateTime
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.Future._
import scala.util.{Try,Success,Failure}
import com.ib.client.ContractDetails
import ExecutionContext.Implicits.global
import IbDataTypes._
import Tickers._

       

@RunWith(classOf[JUnitRunner])
class FutureTests extends FunSuite {
    
    type Bool = Boolean
    type TestFuture = Future[String]
    
    def now = DateTime.now()
    
    def chkResult( res: Option[Bool] ) = {
        res match {
            case Some(true)  => println(s"${now}: Success"); assert(true)
            case Some(false) => println(s"${now}: Failed"); assert(false)
            case None => println(s"${now}: No value returned!"); assert(false)
        }
    }
    
    // TODO: need to add failure cases for each of these
    
    test("Future Test 1 - test timeout and non-execution of completeion"){
        println("\n==== Future Test 1 - test timeout and non-execution of completion\n")
        
        var result: Option[Bool] = None 
        println(s"1 ${now}")
        val f : TestFuture = Future{ Thread.sleep( 5000 ) ; "Result String!" }
        val t : TestFuture = Future{ Thread.sleep( 3000 ) ; "Timeout"  }
        println(s"2 ${now}")
        val first = Await.ready( Future.firstCompletedOf( List( f, t )), 10 seconds )
        val fut = first.asInstanceOf[TestFuture]
        println(s"3 ${now}")
        fut.onComplete {
            case Success(s) => { println(s"${now}: String Result: $s") ; chkResult( if ( s == "Timeout" ) Some(false) else Some(true) ) }
            case Failure(e) => { println(s"${now}:  * * * Failure: $e"); chkResult( Some(false) ) }
        }
    }
}
