package com.ib.scalaib

import java.io.PrintWriter
import java.text.SimpleDateFormat
// import java.util.Date
import org.joda.time._

import scala.collection.mutable.{ HashMap, ArrayBuffer }
import scala.concurrent._
import scala.concurrent.Future
import scala.concurrent.Promise
import scala.concurrent.Await
import scala.concurrent.duration._

import com.ib.client.CommissionReport
import com.ib.client.Contract
import com.ib.client.ContractDetails
import com.ib.client.EClientSocket
import com.ib.client.EWrapper
import com.ib.client.Execution
import com.ib.client.Order
import com.ib.client.OrderState
import com.ib.client.TickType
import com.ib.client.UnderComp

import rx.lang.scala.Observable
import rx.lang.scala.Observer
import rx.lang.scala.Subscription

import IbDataTypes._

import Utils._

trait Account extends EWrapper {

  sealed class AccountUpdateData
  case class PortfolioData(contract: Contract, position: Int, marketPrice: Double, marketValue: Double,
	  		     averageCost: Double, unrealizedPNL: Double, realizedPNL: Double, accountName: String ) extends AccountUpdateData
  case class AccountValue( key: String, value: String, currency: String, accountName: String ) extends AccountUpdateData
  case class AccountTime( timeStamp: String ) extends AccountUpdateData
  case class CompleteUpdate extends AccountUpdateData 

  type AccountUpdateHandlerFunc = ( AccountUpdateData ) => Unit
  var acctUpdateHandler: AccountUpdateHandlerFunc = emptyUpdateHandlerFunc _
  private def emptyUpdateHandlerFunc( a: AccountUpdateData ) = {}
  
  /** TWS callback */
  def updateAccountValue(key: String, value: String, currency: String, accountName: String) {
      acctUpdateHandler( AccountValue(key, value, currency, accountName )) 
      traceln("updateAccountValue");
  }

  /** TWS callback */
  def updatePortfolio(contract: Contract, position: Int, marketPrice: Double, marketValue: Double,
	  	      averageCost: Double, unrealizedPNL: Double, realizedPNL: Double, accountName: String) {
      acctUpdateHandler( new PortfolioData( contract, position, marketPrice, marketValue,
	  		     		    averageCost, unrealizedPNL, realizedPNL, accountName ) )
       traceln("updatePortfolio");
  }

  /** TWS callback */
  def updateAccountTime(timeStamp: String) {
      acctUpdateHandler( new AccountTime( timeStamp) )
      traceln("updateAccountTime");
  }

  /** TWS callback */
  def accountDownloadEnd(accountName: String) {
      acctUpdateHandler( CompleteUpdate() )
      traceln("accountDownloadEnd");
  }
  
}  