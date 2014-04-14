package com.ib.scalaib

import com.ib.client.Contract  
  
/* -------------------------------------------------------------------------------------------
 * 
 *  Enhanced contract types
 * 
 *  ------------------------------------------------------------------------------------------ 
 */

/** An option contract. Typically an option on a stock, though other options, i.e. an option on a future are possible.
  *
  * @param symbol The ticker symbol of the underlying security. Typically a stock.
  * @param expiry The expiration year and month for the contract. <YYYY><MM>
  * @param strike The strike price for the contract.
  * @param right  The type of the option, either "CALL" or "PUT"   
  */
class OptionsContract extends Contract { 
    
    def this( symbol: String, expiry: String, strike: Double, right: String, 
                exchange: String = "SMART", currency: String = "USD" )  {
        this()    
        m_symbol = symbol
        m_secType = "OPT"
        m_exchange = exchange
        m_currency = currency
        m_expiry = expiry
        m_strike = strike
        m_right = right
        m_multiplier = "100"
    }
 
    // ????
    def this( localSymbol: String, exchange: String = "CBOE", currency: String = "USD" ){
        this()
        m_localSymbol = localSymbol
        m_secType = "OPT"
        m_exchange = exchange
        m_currency = currency
        m_multiplier = "100"
    }    
}

object OptionsContract {
    def apply( symbol: String, expiry: String, strike: Double, 
               right: String, exchange: String = "SMART", currency: String = "USD" ) = 
        new OptionsContract( symbol, expiry, strike, right, exchange, currency )
    def apply( localSymbol: String ) /* exchange: String = "SMART", currency: String = "USD" ) */ =
        new OptionsContract( localSymbol )
        
}

/** A stock contract.
  *
  * @param symbol The ticker symbol for the stock. 
  * @param exchange (Default='SMART') The trading exchange for the contract.
  * @param currency (Default='USD') Currency to use for pricing 
  */
class StockContract(symbol: String, exchange: String = "SMART", currency: String = "USD") extends Contract {
  m_symbol = symbol
  m_secType = "STK"
  m_exchange = exchange
  m_currency = currency
}

object StockContract {
    def apply(symbol: String, exchange: String = "SMART", currency: String = "USD") = new StockContract( symbol, exchange, currency ) 
}

/** A futures contract.
  *
  * @param symbol The ticker symbol for the future.
  * @param expiry The expiration year and month for the contract. <YYYY><MM>
  * @param exchange The trading exchange for the contract.
  * @param currency The currency used to price contract. (Default=USD, United States dollars)    
  */
class FuturesContract(symbol: String, expiry: String, exchange: String = "GLOBEX", currency: String = "USD") extends Contract {
  m_symbol = symbol
  m_secType = "FUT"
  m_exchange = exchange
  m_currency = currency
  m_expiry = expiry
}

object FuturesContract {
    def apply(symbol: String, expiry: String, exchange: String = "GLOBEX", currency: String = "USD") = 
        new FuturesContract( symbol, expiry, exchange, currency ) 
}

