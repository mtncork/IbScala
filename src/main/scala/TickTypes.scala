package com.ib.scalaib
  
import com.ib.client.TickType
import org.joda.time._
  
object IbTickTypes  {

  sealed abstract class Tick
  
  case class TickPrice( field: Int, price: Double, canAutoExecute: Boolean ) extends Tick {
        override lazy val toString: String = s"${Field(field)}: Price: $price CanAutoExecute: $canAutoExecute"
  }
  
  case class TickSize( sizeType: TickSizeType.Value, size: Int ) extends Tick {
        override lazy val toString: String = s"${sizeType.toString}: Size: $size"
  }
  
  case class TickRtVolume( price: Double, size: Int, curtime: DateTime, totalvol: Int, vwap: Double, singletrade: Boolean ) extends Tick {
        override lazy val toString: String =  f"$curtime Price: ${price}%.2f, Size: $size, TotalVol: $totalvol Vwap: ${vwap}%.3f $singletrade" 
  }
  
  case class TickLastTimestamp( timestamp: DateTime ) extends Tick {
        override lazy val toString: String =  s"$timestamp" 
  }
  
  case class TickGeneric( field: Int, value: Double ) extends Tick {
        override lazy val toString: String =  s"${Field(field)}: Value: $value"
  }
  
  case class TickString( field: Int, value: String ) extends Tick {
        override lazy val toString: String =  s"${Field(field)}: Value: $value"
  }
  
  // this is used with market snapshot
  case class TickComplete extends Tick  {
        override lazy val toString: String = "TickComplete"
  } 
  
  case class TickOptionCalc( calcType: OptionCalcType.Value, impliedVol: Double,
                                     delta: Double, optPrice: Double, pvDividend: Double,
                                     gamma: Double, vega: Double, theta: Double, undPrice: Double ) extends Tick {
        override lazy val toString: String = s"${calcType.toString}: impliedVol: $impliedVol, delta: $delta, optPrice: $optPrice, pvDividend: $pvDividend, gamma: $gamma, vega: $vega, theta: $theta, undPrice: $undPrice" 
                
  }                                      

  // Returns a name for the field number 
  def Field( nField: Int ): String = TickType.getField( nField )
  
   /** This only applies to data received through a TickZize callback */
  object TickSizeType extends Enumeration {
      type TickSizeType = Value
      val BID_SIZE = Value(0,"bidSize")
      val ASK_SIZE = Value(3,"askSize") 
      val LAST_SIZE = Value(5,"lastSize")
      val VOLUME_SIZE= Value(8,"volumeSize")
  }
  
  object OptionCalcType extends Enumeration {
      type OptionCalcType = Value
      val BID_CALC   = Value(10,"bidCalc")
      val ASK_CALC   = Value(11,"askCalc") 
      val LAST_CALC  = Value(12,"lastCalc")
      val MODEL_CALC = Value(13,"modelCalc")
  }
  
  type TickHandlerType = ( Tick ) => Unit
  
}  