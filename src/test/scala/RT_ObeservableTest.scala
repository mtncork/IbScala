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

@RunWith(classOf[JUnitRunner])
class RT_ObeservableTest extends FunSuite {

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
    
    //test("**** These tests assume TWS is not running ! ****"){
    //    assert(true)
    // }
    
    /*
    test("Create RtbObservable - should fail on connect") {
      println("START: Create RtbObservable - should fail on connect")  
      intercept [Exception] {
	  val rtbObsrv = RtbObservable.create( new FuturesContract("1","2","3"))
      }	  
    }
    */
    
    
    
}