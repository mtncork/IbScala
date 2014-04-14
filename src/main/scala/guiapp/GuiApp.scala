package guiapp

import com.ib.scalaib._
import rx.lang.scala.Observable
import rx.lang.scala.Observer
import rx.lang.scala.Subscription

import IbDataTypes._

import scala.swing._
import scala.swing.event._


object RtbGuiApp extends SimpleSwingApplication {

    import Tickers._
    
    val tickers = Tickers.getRandTickerList(20)
    val indicators = List("Stock","Open", "High", "Low", "Close","Volume")
    
    val data = new Table( tickers.length+1, indicators.length )
    // set column heads
    indicators.foldLeft(0)((acc,t) => { data.update( 0, acc, t); acc+1 } )
    // set labels for tickers
    tickers.foldLeft(1)((acc,t) => { data.update( acc, 0, t); acc+1 } )
    
    def top = new MainFrame {
        title = "IbScala Test App"
        
        val button = new Button {
            text = "Start"
        }
        
        val label = new Label {
            text = "Starting Position"
        } 
        
        contents = new BoxPanel(Orientation.Vertical) {
            contents += data
            contents += button
            contents += label
            // border = Swing.EmptyBorder(10, 10, 10, 100)
        }
        
        listenTo(button)
        var bStarted = false
        reactions += {
            case ButtonClicked(b) =>
                if ( bStarted ) {
                    button.text = "Start"
                    bStarted = false    
                    DoData.stopTickerData( label)
                }
                else {
                    button.text = "Stop"
                    bStarted = true
                    DoData.startTickerData( label)
                }
        }
    }
    
    
    object DoData {
        var rtbBars: List[Observable[Bar3]] = null
        var conn: IbConnection = null
        var subs: List[Subscription] = null 
        var pLabel: Label = null
        
        // start receiving realtime data from TWS
        def startTickerData( progLabel: Label ) {
            pLabel = progLabel
            progLabel.text = "Connecting to TWS"
            progLabel.repaint
            conn = new IbConnection()
            val ok = conn.connect( connctId = 345, serverLogLevel = 5 ) 
            if ( !ok ) {
                progLabel.text = "Unable to connect to TWS !!"
                progLabel.repaint    
                return        
            }
            progLabel.text = "Connected to TWS"
            progLabel.repaint    
            // set our data streams ( at least the bar variety - OHLCV )
            rtbBars = tickers.map( t => conn.getRealTimeBars( StockContract(t)))
            progLabel.text = "Requested data from TWS"
            progLabel.repaint
            // set our observers (subscribers)
            subs = rtbBars.zipWithIndex.map( rb => { rb._1.subscribe( barObserver( rb._2+1 ))} )
            progLabel.text = "Subscribed to data from TWS"
            progLabel.repaint
        }
    
        // stop receiving realtime data from TWS
        def stopTickerData( progLabel: Label ) {
            if ( subs == null ) return
            progLabel.text = "Unsubscribing from data"
            progLabel.repaint
            subs foreach ( _.unsubscribe )
            progLabel.text = "Disconnecting from TWS"
            progLabel.repaint
            conn.disconnect() 
            progLabel.text = "TWS Disconnected"
            progLabel.repaint
        }
        
        // @note to create an observer ( rather than just provide the functions to subscribe )
        //       it needs to be done this way: 1) create an instance of rx.Observer[T], 
        //       providing the 3 main funcs of the interface, then 2) wrap this with an
        //       Observer object. The second part (the wrap) fills in the asJavaObserver func that's needed    
        def barObserver( row: Int ): Observer[Bar3] = Observer( new rx.Observer[Bar3]{
            def onNext(v: Bar3): Unit = { 
                // update open
                data.update( row, 1, v.open )
                // update high
                data.update( row, 2, v.high )
                // update low
                data.update( row, 3, v.low )
                // update close
                data.update( row, 4, v.close )
                // update volume
                // accumulate it
                val curVol = data( row, 5).asInstanceOf[Int]
                data.update( row, 5, if ( curVol > 0 ) v.volume + curVol else v.volume )
            }    
            def onError(e: Throwable): Unit = { println(s"Failure in Row: $row: $e"); pLabel.text = s"$e"; pLabel.repaint  }
            def onCompleted(): Unit = println("Completed")
        } )
    }    
}