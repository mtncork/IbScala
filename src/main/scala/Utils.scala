package com.ib.scalaib

/*
 *  define some util functions
 */
 
object Utils
{
    
    def traceln( s: String = "" ): Unit = if ( _traceOn ) println(s)
    def trace( s: String ): Unit = if ( _traceOn ) print(s)
    
    def millisFromEpoch( s: Long ): Long = s * 1000
    
    private var _traceOn: Boolean = false
    
    def TraceOn: Unit = _traceOn = true
    def TraceOff: Unit = _traceOn = false
    
} 