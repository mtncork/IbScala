package com.ib.scalaib

import org.joda.time._

object IbDataTypes {

    import java.util.Date
    
    case class MktDataS( tickerId: Int, sData: String)
    case class MktData( tickerId: Int, price: Double, size: Int , curTime: Date, 
                    totalVol: Int, vwap: Double, singleTrade: Boolean )
    case class MktBar( tickerId: Int, time: Long, open: Double, high: Double, 
        	   low: Double, close: Double, volume: Long, wap: Double, count: Int )                    

    case class Bar ( time:DateTime, open: Double, high: Double, low: Double, close: Double, volume: Int )
    case class Bar2 ( time:String, open: Double, high: Double, low: Double, close: Double, volume: Int )
    case class Bar3( time:DateTime, open: Double, high: Double, low: Double, close: Double, volume: Int, tradeCount: Int )
    
}