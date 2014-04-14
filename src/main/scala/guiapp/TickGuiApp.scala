package guiapp

import com.ib.scalaib._
import rx.lang.scala.Observable
import rx.lang.scala.Observer
import rx.lang.scala.Subscription

import IbDataTypes._

import scala.swing._
import scala.swing.event._

import java.awt.{Dimension, Color}

import com.ib.scalaib.IbTickTypes._




object TickDataGuiApp extends SimpleSwingApplication {

    import Tickers._

    val tickers = List("F","GM") ++ Tickers.getRandTickerList(38)
    
    // columns              0       1       2        3       4          5        6              7         8       9      10  
    val indicators = List("Stock","Bid", "Bid-Vol", "Ask", "Ask-Vol", "Last", "Last-Vol", "Tot-Volume", "High", "Low", "Close")
    
    val data = new Table( tickers.length+1, indicators.length )
    // set column heads ( row = 0 )
    indicators.foldLeft(0)((acc,t) => { data.update( 0, acc, t); acc+1 } )
    // set labels for tickers ( col = 0 ), start at row=1, row=0 has the header
    tickers.foldLeft(1)((acc,t) => { data.update( acc, 0, t); acc+1 } )
    
    def top = new MainFrame {
        title = "IbScala Tick Data Test App"
        
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
            data.selection.cells.clear()
            data.selection.elementMode_=( scala.swing.Table.ElementMode.Cell )
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
        var ticks: List[Observable[Tick]] = null
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
            ticks = tickers.map( t => conn.getMarketData( StockContract(t)))
            progLabel.text = "Requested data from TWS"
            progLabel.repaint
            // set our observers (subscribers)
            subs = ticks.zipWithIndex.map( rb => { rb._1.subscribe( tickObserver( rb._2+1 ))} )
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
        def tickObserver( row: Int ): Observer[Tick] = Observer( new rx.Observer[Tick]{
            def onNext(t:Tick): Unit = {
                updateTickData( row, t )
            }    
            def onError(e: Throwable): Unit = { println(s"Failure in Row: $row: $e"); pLabel.text = s"$e"; pLabel.repaint  }
            def onCompleted(): Unit = println("Completed")
        } )

        //     columns              0       1       2        3       4          5        6              7         8       9      10  
        // val indicators = List("Stock","Bid", "Bid-Vol", "Ask", "Ask-Vol", "Last", "Last-Vol", "Tot-Volume", "High", "Low", "Close")
        def priceTypeToCol( f: Int ): Int = {
            f match {
                case 1 => 1     // bid  
                case 2 => 3     // ask  
                case 4 => 5     // last  
                case 6 => 8     // high 
                case 7 => 9     // low  
                case 9 => 10    // close 
                case _ => -1    // otherwise
            }
        }
        
        import IbTickTypes.TickSizeType._
        def sizeTypeToCol( f: TickSizeType ): Int = {
            f match {
                case BID_SIZE => 2     // bid 
                case ASK_SIZE => 4     // ask 
                case LAST_SIZE => 6     // last 
                case VOLUME_SIZE => 7     // volume 
                case _ => -1    // otherwise
            }
        }
        
        import scala.swing.Color
        def updateValDouble( row: Int, col: Int, v: Double) = {
            /*
            val down = data(row,col).asInstanceOf[Double] > v
            data.selection.cells.clear()
            data.selection.cells += (( row,col))
            data.selectionBackground_= ( Color.DARK_GRAY )
            data.selectionForeground_= (if (down) Color.RED else Color.GREEN )
            * 
            */
            data.update( row, col, v )
            data.updateCell( row, col )
        }
        
        def updateValInt( row: Int, col: Int, v: Int ) = {
            /*
            data.selection.cells.clear()
            data.selection.cells += (( row,col))
            data.selectionBackground_= ( Color.DARK_GRAY ) 
            data.selectionForeground_= ( Color.GREEN )
            * 
            */
            data.update( row, col, v )
            data.updateCell( row, col )
        }
        
        
        def updateTickData( row: Int, tick: Tick ): Unit = {
            tick match {
                case TickPrice( f, p, canAE ) => { 
                    // println( s"TickPrice: $tick" )
                    val col = priceTypeToCol( f )
                    if ( col != -1 ) updateValDouble( row, col, p ) 
                } 
                case TickSize( sT, size ) => { 
                    // println( s"TickSize: $tick" )
                    val col = sizeTypeToCol( sT )
                    // NOTE: the size reported in last and volume needs the (*100) multiplier
                    // TODO: incorporate these 'facts' into the API
                    if ( col != -1 ) updateValInt( row, col, if ( col == 6 || col == 7 ) size*100 else size ) 
                }
                case TickGeneric( f, v )      => println( s"TickGeneric: $tick" )
                case TickString( f, s )       => println( s"TickSstring: $tick" )
                case TickRtVolume( p, s, t, v, vwap, st ) => { 
                    // println( s"TickRtVolume: $tick" )
                    // val col = sizeTypeToCol( sT )
                    if ( p != 0.0 ) updateValDouble( row, 5, p )
                    if ( s != 0 ) updateValInt( row, 6, s )
                    updateValInt( row, 7, v )
                } 
                case _ => {} // println( s"Unexpected tick: $t" )
            }    
        }
    }    
}