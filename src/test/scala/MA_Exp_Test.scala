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
import rx.lang.scala.Observable
import rx.lang.scala.Observer
import rx.lang.scala.Subscription
import IbDataTypes._
import Tickers._
import org.joda.time._
import scala.collection.mutable

// test code
object Bars {
    
    def accumBar( accB: Bar3, b1: Bar3 ): Bar3 = {
        new Bar3( 
                  b1.time, 
                  if (accB.open == 0.0 ) b1.open else accB.open,
                  accB.high.max(b1.high),
                  if ( accB.low <= 0 ) b1.low else accB.low.min(b1.low),
                  b1.close,
                  accB.volume + b1.volume,
                  accB.tradeCount + b1.tradeCount
               )
    }
    /*
    def newOrNot( timeEnd: DateTime, b1: Bar ): Unit = {
        if ( b1.time > timeEnd) {
            emit( accumBar)
            accumdBar = new Bar()
        }
        accumdBar = accumBar( accumdBar, b1 )
    }
    */
    
    
    def ThirtySecBars( bars: Observable[Bar3] ): Observable[Bar3] = {
        var accumdBar: Bar3 = new Bar3(new DateTime(),0,0,0,0,0,0)
        // get the very first bar
        // val startB = bars.first.toBlockingObservable.single
        // set the end for this duration ( we want these on even boundaries by the passed timestamp )
        // TODO: this code needs to be generalized !!
        // this is AWFULLY messy, not the least bit functional !!
        val startB = DateTime.now()
        var timeStart = new DateTime( startB.getYear(),startB.monthOfYear.get(), startB.dayOfMonth.get(),
                		    startB.hourOfDay.get(), startB.minuteOfHour.get(), 0)
        var timeEnd = timeStart
        if ( startB.secondOfMinute.get() > 30 ) { timeStart = timeStart.plusSeconds(30) ; timeEnd = timeEnd.plusMinutes(1) }
        else timeEnd = timeEnd.plusSeconds(30)
        
        type AccumBarsFunc = ( Bar3 ) => Bar3
        type TIME_BAR = ( DateTime, Bar3 ) 
        
        def accumBars( b: Bar3 ): TIME_BAR = {
            var barToEmit: Option[Bar3] = None
            var timeToEmit:Option[DateTime] = None  
            if ( new Instant(b.time).getMillis() > new Instant(timeEnd).getMillis() ) {
                timeToEmit = Some(timeEnd)
                timeStart = timeEnd
                timeEnd = timeEnd.plusSeconds(30)
                // emit the one we just finished
                barToEmit = Some(accumdBar)
                // start a new one
                accumdBar = new Bar3(new DateTime(),0,0,0,0,0,0)
            }
            accumdBar = accumBar( accumdBar, b )
            ( timeToEmit.getOrElse( timeStart ), barToEmit.getOrElse( accumdBar ) )
        }
        
        // scan looks like the way to go, coupled with distinctUntilChanged
        // maybe map ...
        bars.map( accumBars ).distinctUntilChanged[DateTime]( _._1 ).map( x => x._2)
    }
    
    
    /*
    def OneMinBars( bars: Observable[Bar2] ): Observable[Bar2] = {
        
    }
    * 
    */
    class BarFieldType extends Enumeration {
        type BarFieldType = Value
        val Open = Value
        val High = Value
        val Low  = Value
        val Close = Value
        val Volume = Value
        val TradeCount = Value
    }
    
    type DATETIME = DateTime
    type TIMESERIES_DOUBLE = Tuple2[DATETIME,Double]
    type TSD = TIMESERIES_DOUBLE
    type TIMESERIES_INT = Tuple2[DATETIME,Int]
    type TSI = TIMESERIES_INT
    type TIMESERIES_BOOL = Tuple2[DATETIME, Boolean]
    type TSB = TIMESERIES_BOOL
    
    // defined with tuples here,
    // possibly use case classes later ...
    def Open( bars: Observable[Bar3] ): Observable[TIMESERIES_DOUBLE] = bars.map( b => Tuple2(b.time,b.open))
    def High( bars: Observable[Bar3] ): Observable[TIMESERIES_DOUBLE] = bars.map( b => Tuple2(b.time,b.high))
    def Low( bars: Observable[Bar3] ): Observable[TIMESERIES_DOUBLE] = bars.map( b => Tuple2(b.time,b.low))
    def Close( bars: Observable[Bar3] ): Observable[TIMESERIES_DOUBLE] = bars.map( b => Tuple2(b.time,b.close))
    def Volume( bars: Observable[Bar3] ): Observable[TIMESERIES_INT] = bars.map( b => Tuple2(b.time,b.volume))
    def TradeCount( bars: Observable[Bar3] ): Observable[TIMESERIES_INT] = bars.map( b => Tuple2(b.time,b.tradeCount))
    
    
    
    // type Arith[T] = {def +(T)=>T; def -(T)=>T ; def /(T)=>T ; def *(T)=>T }
    
    /** Returns a simple moving average, defined by its period(length) ) as an Observable
     *  
     *  @param period, number of positions in the buffer to average
     *  @param ts the input Observable
     */
    def MA( period: Int, ts: Observable[TIMESERIES_DOUBLE] ): Observable[TIMESERIES_DOUBLE] = {
        // by setting the "skip count" = 1 we are sequentially moving through the Observable with each position
        ts.buffer( period, 1 ).map( (s: Seq[TIMESERIES_DOUBLE]) => { Tuple2( s(period-1)._1,s.map( _._2 ).sum.toDouble / s.size.toDouble ) } )
    }
    
    /** Returns a weighted moving average as an Observable
     *    the period of the average is set by the size of the list of weights
     * 
     *  
     *  @param weights the list of the set of weights to be applied ( should sum to 1 )
     *  @param ts the input Observable
     *  
     *  @note the current implementation does not "normalize" the weights 
     *        it expects them to sum to 1.0 ( if not "weird scenes inside the goldmine" ... )    
     */
    def WMA( weights: List[Double], ts: Observable[TIMESERIES_DOUBLE] ): Observable[TIMESERIES_DOUBLE] = {
        // TODO: ?? are we accessing the positions in he buffer as we expect and are we 
        //       multiplying by the correct weight
        // the first position in WeightedAccum is the accumulated sum, the second is the weight list
        // which diminishes as we proceed through the buffer
        type WeightedAccum = Tuple2[Double,List[Double]]
        // this func weights each item in the buffer and sums (accumulates) them
        def weightAndAccum( wa: WeightedAccum, v: TIMESERIES_DOUBLE ): WeightedAccum = ( wa._1 + wa._2.head * v._2, wa._2.tail )
        ts.buffer( weights.size, 1 ).map( (s: Seq[TIMESERIES_DOUBLE]) => { Tuple2( s(weights.size-1)._1,s.foldLeft(Tuple2(0.0,weights))(weightAndAccum)._1 / s.size.toDouble ) } )
    }
   
    def EMA( period: Int, ts: Observable[TIMESERIES_DOUBLE] )(implicit num:Numeric[Double]): Observable[TIMESERIES_DOUBLE] = {
        // we need an MA of the first 'period' values
        val startF = Future{ ts.map( _._2 ).take(period).toSeq.toBlockingObservable.single.sum / period }
        var startMA = 0.0 
        startF onComplete {
            case Failure( e ) => { println(s"Failed: $e.getMessage") ; return Observable[TIMESERIES_DOUBLE]( e ) } 
            case Success( v ) => startMA = v
        }
        val K: Double = 2.0 / (period.toDouble + 1.0)
        var ema = startMA
        ts.map( (tsv: TSD) =>{ ema = ((tsv._2 - ema) * K) + ema ; ( tsv._1, ema ) })
    }
    
    val tolerance: Double = 0.003
    
    def cross( ts1: Observable[TIMESERIES_DOUBLE], ts2: Observable[TIMESERIES_DOUBLE] ): Observable[TIMESERIES_BOOL] = {
        def approxEq( d1: Double, d2: Double ): Boolean = (d1 > (d2-tolerance)) && (d1 < (d2+tolerance)) 
        ts1.zip(ts2).map( (tsv_pair: Tuple2[TSD,TSD]) => ( tsv_pair._1._1, approxEq( tsv_pair._1._2, tsv_pair._2._2)) )  
    } 
    
    def above( ts1: Observable[TIMESERIES_DOUBLE], ts2: Observable[TIMESERIES_DOUBLE] ): Observable[TIMESERIES_BOOL] = 
        ts1.zip(ts2).map( (tsv_pair: Tuple2[TSD,TSD]) => ( tsv_pair._1._1, tsv_pair._1._2 > tsv_pair._2._2 ))
   
    // @note to create an observer ( rather than just provide the functions to subscribe )
    //       it needs to be done this way: 1) create an instance of rx.Observer[T], 
    //       providing the 3 main funcs of the interface, then 2) wrap this with an
    //       Observer object. The second part (the wrap) fills in the asJavaObserver func that's needed    
    def tsdObserver( label:String ): Observer[TSD] = Observer( new rx.Observer[TSD]{
        def onNext(v: TSD): Unit = { println(f"$label:\t${v._1.getHourOfDay()}%02d:${v._1.getMinuteOfHour()}%02d:${v._1.getSecondOfMinute()}%02d\t${v._2}%.3f") }
        def onError(e: Throwable): Unit = { println(s"Failure $label: $e"); assert(false) }
        def onCompleted(): Unit = println("Completed")
    } )

    
        
}

@RunWith(classOf[JUnitRunner])
class MA_Exp_Test extends FunSuite {
    
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
    

    test("Test 1 - show multiple moving averages for the Close price from RTBs") {
        println("\n==== Test 1 - show multiple moving averages for the Close price from RTBs\n")
        import Numeric._
        val conn = new IbConnection()
        val ok = conn.connect( connctId = 345, serverLogLevel = 5 )
        if ( ! ok ) { println(s"!!! >>>> !!! Not Connected !!! <<<< !!!"); assert(false) }
        // request the data
        val ticker = "GM"
       	val rtbBars = conn.getRealTimeBars( StockContract(ticker) )
       	// get the close price(s)
       	val Close = Bars.Close(rtbBars)
       	
       	val low = 2
       	val high = 10
       	println(s"\n---> Creating MAs and EMAs for periods: $low to $high\n")
       	var subs = mutable.ArrayBuffer[Subscription]()  
       	for ( i <- low to high ) {
       	    val maC = Bars.MA(i,Close)
       	    val sub1 = maC.subscribe( Bars.tsdObserver(s"$ticker: MA($i)") )
       	    subs += sub1
       	    val emaC = Bars.EMA(i,Close)
       	    val sub2 = maC.subscribe( Bars.tsdObserver(s"$ticker: EMA($i)") )
       	    subs += sub2
       	}
        
       	//val maC = Bars.MA(5,C) 
       	//val sub1 = maC.subscribe( Bars.tsdObserver("AAPL: MA(5)") )
        //       	        
       	//     v => { /*  ticker OK, there should be data */
       	//            println(s"AAPL: MA(5): ${v._1} ${v._2} ") 
       	//          },
       	//     e => { println(s"Failure 1: $e"); assert(false) },
       	//     () => { println("Completed") }
       	// )

       	//val emaC = Bars.EMA(7,C)
       	//val sub2 = emaC.subscribe( Bars.tsdObserver("AAPL: EMA(7)") )
        //
       	//    v => { /*  ticker OK, there should be data */ 
       	//        println(s"AAPL: EMA(7): ${v._1} ${v._2} ") 
       	//    },
       	//    e => { println(s"Failure 2: $e"); assert(false) },
       	//    () => { println("Completed") }
       	// )

       	Thread.sleep( 2*60*1000 )   // 2  minutes
       	
       	// sub1.unsubscribe()
       	println("%%% -- Unsubscribe ALL --- %%%")
        subs.foreach( _.unsubscribe() )
        
        Thread.sleep( 12000 )
        
        conn.disconnect()
    }
}