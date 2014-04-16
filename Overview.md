# IbScala Overview #

The majority of the functionality in **IbScala** is implemented in a set of <a href="http://docs.scala-lang.org/tutorials/tour/traits.html">Scala traits</a> which are mixed-into a single class **IbConnection**. An instance of this class is created for each desired connection to TWS. A reference to that instance is passed to TWS so that it can access the Java callbacks that it expects.

Each trait implements a combination of user-callable functions along with the portions of the TWS callback interface supporting that specific area of functionality. The source files are found in a standard SBT location **src/main/scala**. The sample applications can be found in subdirectories **apps** and **guiapps**. ScalaTest files are found in **src/test/scala**.

The library is split into the following modules (files):
- **Connect** - this module implements the IbConnection class previously described. Besides creating the physical connection to TWS it also provides unique request IDs when queried through user facing functions in the other traits. The file contains a section of 'stub' functions for some of the those aspects of the TWS interface not fully implemented. 
- **Account** - contains 'stub' functions for portfolio / account update functions
- **ContractDetails** - implements functionality for the retrieval of contract details
- **ContractTypes** - implements several classes representing common contract types
- **Errors** - implements the error callbacks required by TWS along with the ability to dynamically add specific handlers which are dispatched based on a request ID.
- **Historical Data** - implements functionality that allows the retrieval of historical data for a given security. The results are returned as an Observable of Bar data.
- **IbDataTypes** - defines several data types exported through Observables.
- **MarketDepth** - implements functionality to retrieve the market depth for a given security
- **MarketTick** - implements the retrieval and monitoring of market tick data for a given security. The results are returned as an Observable of Tick data.
- **MktScanner** - implements access to IB's market scanners. The results are returned as an Observable of ScannerData. Included is a utility function that retrieves the current scanner parameters as a Future of XML.
- **ObservableUtils** - (currently) implements a single class that aids the management of multiple subscriptions ( to an Observable ).
- **OpenOrders** - implements functions to access the existence and status of open orders and their corresponding Futures
- **Orders** - implements the ability to place (and cancel) an order through TWS. It returns a Future for a value of type OrderStatusData. When that open order's status changes to a terminal condition then the Future is fulfilled.
- **Promise** - implements the ability to set, track, and retrieve(remove) Promises/Futures. These are the return results for various data requests, as indicated, and accessed by their unique request IDs.
- **Quickies** - implements a handful of simplified functions for accessing price (currently)
- **RealTimeBars** - implements functionality to access and monitor IB's real-time, 5 second, bar data (OHLCV) for a given security. The data is returned as an Observable of Bar data.
- **TickTypes** - definitions ( case classes ) for the various forms of Tick data that TWS will provide.
- **Utils** - functions (currently) related to tracing

Sample applications include:
- **GuiApp** - a swing application that monitors and displays real-time bar data for a sampling of S&P 500 tickers
- **TickGuiApp** - a swing application that monitors and displays market tick data for a sampling of S&P 500 tickers
   