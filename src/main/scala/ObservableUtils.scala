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

//
// ===========================================================================
//
// Utility Classes and Objects for use with Observables
//



// Code for multiple subscriptions
class MultiSubs[T]( cleanup: => Unit ) {
     import Utils._
     // type Collection = Set[Observer[T]]
     private var observers: Set[Observer[T]] = Set[Observer[T]]() 
     
     def add( o: Observer[T] ): Subscription = { 
         observers = observers + o
         println(s"add obsv: $o, new size: ${observers.size}")
         Subscription {
             // the unsubscribe code
             // affect the delete functionality
             observers = observers - o
             traceln(s"del obsv: $o, new size: ${observers.size}")
             if ( observers.size == 0) cleanup
         }
     }
     
     // pass the data to each of the observers
     def onNext( v: T ) = observers.foreach( _.onNext( v ) )
     
     // ?? need onError and onCompleted
     // maybe this is NOT what we want to do - we have the option - possibly handle the error here
     // and simply complete all the streams
     def onError( e: Throwable ) = observers.foreach( _.onError( e ) )
     
     def onCompleted = observers.foreach( _.onCompleted )
     
     def isEmpty: Boolean = observers.size == 0 

}

