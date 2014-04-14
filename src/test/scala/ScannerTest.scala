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
import scala.xml._
import IbDataTypes._
import Tickers._


@RunWith(classOf[JUnitRunner])
class ScannerTest extends FunSuite {

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
    
    test("Test 1 - get scanner parameters"){
        println("\n==== Test 1 - get scanner parameters\n")
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        assert(ok)
        val futContDetails = conn.getScannerParameters
        val s = Await.result( futContDetails, 2 seconds )
        assert( (s contains "ScanParameterResponse") && (s contains "InstrumentList") && (s contains "LocationTree") )
        
        // once we have this, extract all the scan codes that can then be used in further tests
        val xml = XML.loadString( s )
        val scanTypeNodes = xml \\ "ScanType"
        println(s"ScanTypeNodes size: ${scanTypeNodes.size}")
        /**
          *  save the scan codes 
        val scanCodeNodes = xml \\ "scanCode"  
        val l = scanCodeNodes.toList map ( n => n.text )
        // val scanCodes = scanCodeNodes map ( _.text )
        println( l.size )
        import java.io.PrintWriter
        val f = new PrintWriter( "E:\\Temp\\IbScanCodes.txt")
        l.foreach( f.println(_) )
        f.close()
        * 
        */
        // get the 'accessible' nodes
        val an = scanTypeNodes.filter( n => (n \ "access").text == "allowed" )
        println(s"Accessible scan types count: ${an.size}")
        // get the scans that concern options
        val optScans = an.filter( n => (n \ "scanCode").text.contains("OPT"))
        println(s"Accessible OPTION scan types count: ${optScans.size}")
        optScans.foreach( n => println(s"${(n \ "displayName").text} ${(n \ "scanCode").text}") )
        conn.disconnect()
    }
    
    
    test("Test 2 - get scanner results"){
        println("\n==== Test 2 - get scanner results\n")
        val conn = new IbConnection()
        val ok = conn.connect( 123 )
        assert(ok)
        // TODO: possibly encapsulate this in a scala class
        val scanner = new com.ib.client.ScannerSubscription()
        /*
         from Java test client
         
         private JTextField m_Id = new JTextField( "0");
         private JTextField m_numberOfRows = new JTextField("10");
         private JTextField m_instrument = new JTextField("STK");
         private JTextField m_locationCode = new JTextField("STK.US.MAJOR");
         private JTextField m_scanCode = new JTextField("HIGH_OPT_VOLUME_PUT_CALL_RATIO");
         private JTextField m_abovePrice = new JTextField("3");
         private JTextField m_belowPrice = new JTextField();
         private JTextField m_aboveVolume = new JTextField("0");
         private JTextField m_averageOptionVolumeAbove = new JTextField("0");
         private JTextField m_marketCapAbove = new JTextField("100000000");
         private JTextField m_marketCapBelow = new JTextField();
         private JTextField m_moodyRatingAbove = new JTextField();
         private JTextField m_moodyRatingBelow = new JTextField();
         private JTextField m_spRatingAbove = new JTextField();
         private JTextField m_spRatingBelow = new JTextField();
         private JTextField m_maturityDateAbove = new JTextField();
         private JTextField m_maturityDateBelow = new JTextField();
         private JTextField m_couponRateAbove = new JTextField();
         private JTextField m_couponRateBelow = new JTextField();
         private JTextField m_excludeConvertible = new JTextField("0");
         private JTextField m_scannerSettingPairs = new JTextField("Annual,true");
         private JTextField m_stockTypeFilter = new JTextField("ALL");
      */
        scanner.numberOfRows(50)
        scanner.instrument("STK")
        scanner.locationCode("STK.US.MAJOR")
        scanner.scanCode("HIGH_OPT_VOLUME_PUT_CALL_RATIO")
        scanner.abovePrice(3.0)
        scanner.belowPrice(10.00)
        scanner.aboveVolume(500000)
        scanner.averageOptionVolumeAbove(1000)
        scanner.marketCapAbove(100000000)
        //scanner.scanCode("US Movers")
        //scanner.m_marketCapBelow = 
        //scanner.m_moodyRatingAbove = 
        //scanner.m_moodyRatingBelow = 
        //scanner.m_spRatingAbove = 
        //scanner.m_spRatingBelow = 
        //scanner.m_maturityDateAbove = 
        //scanner.m_maturityDateBelow = 
        //scanner.m_couponRateAbove = 
        //scanner.m_couponRateBelow = 
        scanner.excludeConvertible("0")
        scanner.scannerSettingPairs("Annual,true")
        scanner.stockTypeFilter("ALL")
        var count = 0
        /**
        val obs1 = conn.getScannerSubscription( scanner )
        obs1 subscribe (
            d => {println(s"[3-10] ${d.rank} ${d.contractDetails.m_marketName} distance: ${d.distance} benchmark: ${d.benchmark} projection: ${d.projection}, legsStr: ${d.legsStr}" ); count += 1},
            e => println(s"Failure: ${e.getMessage()}" ),
            () => println("Complete")
        )
      **/
      
        //
        // do 3 scans, grouped by stock price (10-50,50-100,100-200)
        // for stocks with high option implied volatility greater than their historical volatility
        //
        scanner.scanCode("HIGH_OPT_IMP_VOLAT_OVER_HIST")
        scanner.abovePrice(10.00)
        scanner.belowPrice(50.00)
        val obs2 = conn.getScannerSubscription( scanner )
        obs2 subscribe (
            d => {
                println(s"[10-50] ${d.rank} ${d.contractDetails.m_summary.m_symbol}") 
                // distance: ${d.distance} benchmark: ${d.benchmark} projection: ${d.projection}, legsStr: ${d.legsStr}" )
                count += 1
            },
            e => println(s"Failure: ${e.getMessage()}" ),
            () => println("Complete")
        )
        scanner.scanCode("HIGH_OPT_IMP_VOLAT_OVER_HIST")
        scanner.abovePrice(50.00)
        scanner.belowPrice(100.00)
        val obs3 = conn.getScannerSubscription( scanner )
        obs3 subscribe (
            d => {
                println(s"[50-100] ${d.rank} ${d.contractDetails.m_summary.m_symbol}")
                // distance: ${d.distance} benchmark: ${d.benchmark} projection: ${d.projection}, legsStr: ${d.legsStr}" )
                count += 1
            },
            e => println(s"Failure: ${e.getMessage()}" ),
            () => println("Complete")
        )
        scanner.scanCode("HIGH_OPT_IMP_VOLAT_OVER_HIST")
        scanner.abovePrice(100.00)
        scanner.belowPrice(200.00)
        val obs4 = conn.getScannerSubscription( scanner )
        obs4.subscribe (
            d => {
                println(s"[100-200] ${d.rank} ${d.contractDetails.m_summary.m_symbol}")
                // distance: ${d.distance} benchmark: ${d.benchmark} projection: ${d.projection}, legsStr: ${d.legsStr}" )
                count += 1
            },
            e => println(s"Failure: ${e.getMessage()}" ),
            () => println("Complete")
        )
        Thread.sleep( 10 * 1000 )
        assert( count >= 10 )
        // println( s )
        conn.disconnect()
    }
    
    
}