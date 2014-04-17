# IbScala 

**IbScala** is a Scala encapsulation of the **Interactive Brokers** Java API. Its design features the use of <a href="http://www.scala-lang.org/api/2.10.4/#scala.concurrent.Future">Scala/Akka Futures</a> and <a href="https://github.com/Netflix/RxJava">RxScala/RxJava Observables.</a>

This initial version was built using **IB API version 9.68**.

The project is principally maintained with <a href="http://www.scala-sbt.org/">SBT</a>, for this, initial commit, version 0.13.0.

There is an overview of the structure and files in the project in **Overview.md**

### Using Eclipse ###
You can generate an Eclipse project from the SBT settings by:
<ol>
<li>at a command line/shell prompt change (cd) to the main directory for this project</li>
<li>type 'sbt' ( sbt will attempt to resolve the library dependencies for the project )</li>
<li>once its finished, at the sbt prompt type 'eclipse'</li>
<li>sbt will generate the appropriate project files</li>
<li>open eclipse and then import the main folder (project) as a new project</li>
</ol>

The above procedure assumes that the file 'plugins.sbt' exists in the project directory. If that file is missing, then add it with the following content:

	// Comment to get more information during initialization
	logLevel := Level.Warn

	resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

	// add SBTeclipse
	addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.4.0")

Empty lines are required between each of the above entries.

### TWS Connection ###
The test code as well as the sample applications require that an instance of the IB Trader Workstation application is running. The current connection code assumes that **TWS is running on the same machine**, and tries to connect through localhost, IP address 127.0.0.1. The standard port for TWS of **7496** is used. You will need to set TWS to allow local connections. The setting is found through the TWS application menu at: 
   **File -> Global Configuration -> API -> Settings**
1. Enable ActiveX and Socket Clients. 
1. Set the port to 7496.
 
The dialog in question looks like this:<br>![TWS Settings](http://i.imgur.com/6Ij6kZc.jpg)

Likewise add the localhost address, 127.0.0.1. It will need to be in the following list, which may have other entries as well. 

![Trusted IP Addresses](http://i.imgur.com/z4W4nMt.jpg)




### Testing ###

Most of the <a href="http://www.scalatest.org/">ScalaTest</a> tests in src/test/scala require a connection to TWS. The first test in a suite displays a message and then pauses until the user "presses enter/return" indicating that TWS is active. If TWS is not active the test suite will fail. In general most tests will perform an individual connect and then disconnect so that each can be self-contained.

For tests that place orders additional warning messages have been included.

#### <center>Do  not run the tests in the OrderTests module in a standard, active IB account !!!</center> ####
#### <center>Use a paper trading, or demo account only !!!</center>####
#### <center>$$$ Otherwise, **MONETARY LOSSES MAY RESULT $$$** !!!</center>####



All of the testing for this release was done within a paper trading account.

### Caveats ###

The current codebase is lacking particular areas of functionality:
<ol>
<li>There is no Financial Advisor support. The code to manage accounts has not been implemented.</li>
<li>There is no specific support for algo-based orders.</li>
<li>There is no support for News bulletins.</li>
<li>There is no support for the retrieval of execution reports.</li>
<li>There is no explicit support for bond contracts, unlike stock, futures, and option ( on stocks and futures ) contracts.</li>
</ol> 

### In the Pipeline ###


- an Options module
- abstracted classes for realtime Bars and Indicators ( code in the MA_Exp_Test.scala file hints at where these are headed, especially the code implementing moving averages ). These will be designed with an eye to incorporation into a trading DSL. 