package test

import scala.io.Source
import scala.sys.SystemProperties
import scala.util.Random

object Tickers {
    import scala.util.Random
    import scala.io._
    import scala.sys._
    
    val props = new SystemProperties()
    val curDir = props.get("user.dir").get
    val fileSep = props.get("file.separator").get  
    val f = Source.fromFile( curDir+fileSep+"src"+fileSep+"test"+fileSep+"SP500.txt") 
    val tickers = f.getLines.drop(1).toList
    
    def getRandTickerList( count: Int ): List[String] = {
        Random.shuffle( tickers ).take(count)
    }
    
    // dates for 2014
    val optsExpirDates: Map[Int,Int] = Map (
        (1,17),(2,21),(3,21),(4,17),(5,16),(6,20),
        (7,18),(8,15),(9,19),(10,17),(11,21),(12,19) 
    )
}

