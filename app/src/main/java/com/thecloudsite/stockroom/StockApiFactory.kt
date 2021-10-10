/*
 * Copyright (C) 2021
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thecloudsite.stockroom

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.thecloudsite.stockroom.utils.checkUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

// https://android.jlelse.eu/android-networking-in-2019-retrofit-with-kotlins-coroutines-aefe82c4d777

// https://api.coingecko.com/api/v3/coins/list
// https://api.coingecko.com/api/v3/simple/supported_vs_currencies

// https://api.coingecko.com/api/v3/simple/price?ids=cartesi&vs_currencies=usd
// {"cartesi":{"usd":1.24}}

// https://api.coingecko.com/api/v3/coins/cartesi

// https://api.coingecko.com/api/v3/coins/cartesi/market_chart?vs_currency=usd&days=10
// {"prices":[[1619625666845,0.6476699905697568],[1619629346430,0.6281418599606636],[1619632894402,0.6059910967268739],[1619636660847,0.6104612592139378],[1619640036142,0.614744809646869],[1619643737476,0.5964704609349185],[1619647401790,0.6035420239511216],[1619650863357,0.5966409608755056],[1619654439638,0.6023990725132919],[1619658440051,0.5994810706866857],[1619661847749,0.6002388209808079],[1619665320398,0.6024006879798155],[1619668987030,0.6004665467341181],[1619672639041,0.5862480715711115],[1619676206939,0.5951259328829168],[1619679826463,0.5940200145799716],[1619683304361,0.603884694731556],[1619686999431,0.6014750931964677],

// https://api.coingecko.com/api/v3/coins/cartesi/market_chart/range?vs_currency=usd&from=1609625666&to=1619625666

// https://api.coingecko.com/api/v3/coins/cartesi?localization=false&tickers=false&market_data=true&community_data=false&developer_data=false&sparkline=false


// https://api.coinpaprika.com/

// https://api.coinpaprika.com/v1/coins
// {"id":"ada-cardano","name":"Cardano","symbol":"ADA","rank":5,"is_new":false,"is_active":true,"type":"coin"},{"id":"doge-dogecoin","name":"Dogecoin","symbol":"DOGE","rank":6,"is_new":false,"is_active":true,"type":"coin"},{"id":"usdt-tether","name":"Tether","symbol":"USDT","rank":7,"is_new":false,"is_active":true,"type":"token"},{"id":"dot-polkadot","name":"Polkadot","symbol":"DOT","rank":8,"is_new":false,"is_active":true,"type":"coin"},

// https://api.coinpaprika.com/v1/tickers
// ,{"id":"ada-cardano","name":"Cardano","symbol":"ADA","rank":4,"circulating_supply":31112484646,"total_supply":45000000000,"max_supply":45000000000,"beta_value":0.994793,"first_data_at":"2017-10-01T00:00:00Z","last_updated":"2021-05-17T04:53:47Z","quotes":{"USD":{"price":2.13071104,"volume_24h":8561645413.6285,"volume_24h_change_24h":-17.2,"market_cap":66291714517,"market_cap_change_24h":-8.91,"percent_change_15m":-0.23,"percent_change_30m":0.61,"percent_change_1h":1.08,"percent_change_6h":-4.41,"percent_change_12h":-4.83,"percent_change_24h":-8.91,"percent_change_7d":20.51,"percent_change_30d":43.97,"percent_change_1y":4064.5,"ath_price":2.46475647,"ath_date":"2021-05-16T07:31:20Z","percent_from_price_ath":-13.55}}},

// https://api.coinpaprika.com/v1/tickers/ada-cardano
// {"id":"ada-cardano","name":"Cardano","symbol":"ADA","rank":4,"circulating_supply":31112484646,"total_supply":45000000000,"max_supply":45000000000,"beta_value":0.994793,"first_data_at":"2017-10-01T00:00:00Z","last_updated":"2021-05-17T04:48:39Z","quotes":{"USD":{"price":2.12002421,"volume_24h":8503083622.9605,"volume_24h_change_24h":-16.85,"market_cap":65959220682,"market_cap_change_24h":-8.4,"percent_change_15m":-0.27,"percent_change_30m":0.45,"percent_change_1h":0.39,"percent_change_6h":-4.21,"percent_change_12h":-5.03,"percent_change_24h":-8.4,"percent_change_7d":20.16,"percent_change_30d":43.97,"percent_change_1y":4064.5,"ath_price":2.46475647,"ath_date":"2021-05-16T07:31:20Z","percent_from_price_ath":-13.99}}}


// https://api.pro.coinbase.com/products
// https://api.pro.coinbase.com/products/ADA-USD/ticker
// https://api.pro.coinbase.com/products/ADA-USD/candles?start=2021-07-10T12:00:00&granularity=300


// https://api.cryptowat.ch/markets/prices


//StockApiFactory to create the Yahoo Api
object StockMarketDataApiFactory {
  // https://query2.finance.yahoo.com/v6/finance/quote?symbols=msft
  // https://query1.finance.yahoo.com/v7/finance/quote?format=json&symbols=msft,aapl

  private var defaultUrl = "https://query2.finance.yahoo.com/v7/finance/"
  private var url = ""

  //Creating Auth Interceptor to add api_key query in front of all the requests.
/*
  private val authInterceptor = Interceptor { chain ->
    val newUrl = chain.request()
        .url()
        .newBuilder()
        .build()

    val newRequest = chain.request()
        .newBuilder()
        .url(newUrl)
        .build()

    chain.proceed(newRequest)
  }
*/

  // building http request url
  private fun retrofit(): Retrofit = Retrofit.Builder()
    .client(
      OkHttpClient().newBuilder()
//      .addInterceptor(authInterceptor)
        .build()
    )
    .baseUrl(url)
    .addConverterFactory(MoshiConverterFactory.create())
    .addCallAdapterFactory(CoroutineCallAdapterFactory())
    .build()

  fun update(_url: String) {
    if (url != _url) {
      if (_url.isBlank()) {
        url = ""
        yahooApi = null
      } else {
        url = checkUrl(_url)
        yahooApi = try {
          retrofit().create(YahooApiMarketData::class.java)
        } catch (e: Exception) {
          null
        }
      }
    }
  }

  init {
    update(defaultUrl)
  }

  var yahooApi: YahooApiMarketData? = null
}

//StockApiFactory to create the Coingecko Api
object StockMarketDataCoingeckoApiFactory {
  // https://api.coingecko.com/api/v3/coins/cartesi/tickers
  private var defaultUrl = "https://api.coingecko.com/api/v3/coins/"
  private var url = ""

  // building http request url
  private fun retrofit(): Retrofit = Retrofit.Builder()
    .client(
      OkHttpClient().newBuilder()
        .build()
    )
    .baseUrl(url)
    .addConverterFactory(MoshiConverterFactory.create())
    .addCallAdapterFactory(CoroutineCallAdapterFactory())
    .build()

  fun update(_url: String) {
    if (url != _url) {
      if (_url.isBlank()) {
        url = ""
        coingeckoApi = null
      } else {
        url = checkUrl(_url)
        coingeckoApi = try {
          retrofit().create(CoingeckoApiMarketData::class.java)
        } catch (e: Exception) {
          null
        }
      }
    }
  }

  init {
    update(defaultUrl)
  }

  var coingeckoApi: CoingeckoApiMarketData? = null
}

object StockRawMarketDataApiFactory {
  // https://query2.finance.yahoo.com/v6/finance/quote?symbols=msft
  // https://query1.finance.yahoo.com/v7/finance/quote?format=json&symbols=msft,aapl

  private var defaultUrl = "https://query2.finance.yahoo.com/v7/finance/"
  private var url = ""

  // building http request url
  private fun retrofit(): Retrofit = Retrofit.Builder()
      .client(
          OkHttpClient().newBuilder()
//      .addInterceptor(authInterceptor)
              .build()
      )
      .baseUrl(url)
      .addConverterFactory(ScalarsConverterFactory.create())
      .addCallAdapterFactory(CoroutineCallAdapterFactory())
      .build()

  fun update(_url: String) {
    if (url != _url) {
      if (_url.isBlank()) {
        url = ""
        yahooApi = null
      } else {
        url = checkUrl(_url)
        yahooApi = try {
          retrofit().create(YahooApiRawMarketData::class.java)
        } catch (e: Exception) {
          null
        }
      }
    }
  }

  init {
    update(defaultUrl)
  }

  var yahooApi: YahooApiRawMarketData? = null
}

object CoingeckoSymbolsApiFactory {

  // https://api.coingecko.com/api/v3/coins/

  private var defaultUrl = "https://api.coingecko.com/api/v3/coins/"
  private var url = ""

  // building http request url
  private fun retrofit(): Retrofit = Retrofit.Builder()
    .client(
      OkHttpClient().newBuilder()
        .build()
    )
    .baseUrl(url)
    .addConverterFactory(MoshiConverterFactory.create())
    .addCallAdapterFactory(CoroutineCallAdapterFactory())
    .build()

  fun update(_url: String) {
    if (url != _url) {
      if (_url.isBlank()) {
        url = ""
        coingeckoApi = null
      } else {
        url = checkUrl(_url)
        coingeckoApi = try {
          retrofit().create(DataProviderSymbolsData::class.java)
        } catch (e: Exception) {
          null
        }
      }
    }
  }

  init {
    update(defaultUrl)
  }

  var coingeckoApi: DataProviderSymbolsData? = null
}

object StockYahooChartDataApiFactory {

  // https://query1.finance.yahoo.com/v7/finance/chart/?symbol=aapl&interval=1d&range=3mo
  // https://query1.finance.yahoo.com/v8/finance/chart/?symbol=aapl&interval=1d&range=3mo

  private var defaultUrl = "https://query2.finance.yahoo.com/v8/finance/"
  private var url = ""

  //Creating Auth Interceptor to add api_key query in front of all the requests.
/*
  private val authInterceptor = Interceptor { chain ->
    val newUrl = chain.request()
        .url()
        .newBuilder()
        .build()

    val newRequest = chain.request()
        .newBuilder()
        .url(newUrl)
        .build()

    chain.proceed(newRequest)
  }
*/

  // building http request url
  private fun retrofit(): Retrofit = Retrofit.Builder()
    .client(
      OkHttpClient().newBuilder()
//      .addInterceptor(authInterceptor)
        .build()
    )
    .baseUrl(url)
    .addConverterFactory(MoshiConverterFactory.create())
    .addCallAdapterFactory(CoroutineCallAdapterFactory())
    .build()

  fun update(_url: String) {
    if (url != _url) {
      if (_url.isBlank()) {
        url = ""
        yahooApi = null
      } else {
        url = checkUrl(_url)
        yahooApi = try {
          retrofit().create(YahooApiChartData::class.java)
        } catch (e: Exception) {
          null
        }
      }
    }
  }

  init {
    update(defaultUrl)
  }

  var yahooApi: YahooApiChartData? = null
}

object StockCoingeckoChartDataApiFactory {

  // https://api.coingecko.com/api/v3/coins/cartesi/market_chart?vs_currency=usd&days=1

  private var defaultUrl = "https://api.coingecko.com/api/v3/coins/"
  private var url = ""

  // building http request url
  private fun retrofit(): Retrofit = Retrofit.Builder()
    .client(
      OkHttpClient().newBuilder()
        .build()
    )
    .baseUrl(url)
    .addConverterFactory(MoshiConverterFactory.create())
    .addCallAdapterFactory(CoroutineCallAdapterFactory())
    .build()

  fun update(_url: String) {
    if (url != _url) {
      if (_url.isBlank()) {
        url = ""
        coingeckoApi = null
      } else {
        url = checkUrl(_url)
        coingeckoApi = try {
          retrofit().create(CoingeckoApiChartData::class.java)
        } catch (e: Exception) {
          null
        }
      }
    }
  }

  init {
    update(defaultUrl)
  }

  var coingeckoApi: CoingeckoApiChartData? = null
}

/*
https://query1.finance.yahoo.com/v7/finance/download/TSLA?events=split&interval=1d&period1=1598572800&period2=1600453099

This returns TSLA.csv containing:

Date,Stock Splits
2020-08-31,5:1
You can also set the start date to 0 to get a full history of splits.

https://query1.finance.yahoo.com/v7/finance/download/AAPL?events=split&interval=1d&period1=0&period2=1600453099

This returns AAPL.csv containing:

Date,Stock Splits
2000-06-21,2:1
2005-02-28,2:1
2020-08-31,4:1
1987-06-16,2:1
2014-06-09,7:1
 */