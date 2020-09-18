/*
 * Copyright (C) 2020
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
import com.thecloudsite.stockroom.utils.checkBaseUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

// https://android.jlelse.eu/android-networking-in-2019-retrofit-with-kotlins-coroutines-aefe82c4d777

//StockApiFactory to create the Yahoo Api
object StockMarketDataApiFactory {
  // https://query2.finance.yahoo.com/v6/finance/quote?symbols=msft
  // https://query1.finance.yahoo.com/v7/finance/quote?format=json&symbols=msft,aapl

  private var defaultBaseUrl = "https://query1.finance.yahoo.com/v7/finance/"
  private var baseUrl = ""

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

  //OkhttpClient for building http request url
  private val yahooClient = OkHttpClient().newBuilder()
//      .addInterceptor(authInterceptor)
      .build()

  private fun retrofit(): Retrofit = Retrofit.Builder()
      .client(yahooClient)
      .baseUrl(baseUrl)
      .addConverterFactory(MoshiConverterFactory.create())
      .addCallAdapterFactory(CoroutineCallAdapterFactory())
      .build()

  fun update(_baseUrl: String) {
    if (baseUrl != _baseUrl) {
      if (_baseUrl.isBlank()) {
        baseUrl = ""
        yahooApi = null
      } else {
        baseUrl = checkBaseUrl(_baseUrl)
        yahooApi = try {
          retrofit().create(YahooApiMarketData::class.java)
        } catch (e: Exception) {
          null
        }
      }
    }
  }

  init {
    update(defaultBaseUrl)
  }

  var yahooApi: YahooApiMarketData? = null
}

object StockChartDataApiFactory {

  // https://query1.finance.yahoo.com/v7/finance/chart/?symbol=aapl&interval=1d&range=3mo
  // https://query1.finance.yahoo.com/v8/finance/chart/?symbol=aapl&interval=1d&range=3mo

  private var defaultBaseUrl = "https://query1.finance.yahoo.com/v8/finance/"
  private var baseUrl = ""

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

  //OkhttpClient for building http request url
  private val yahooClient = OkHttpClient().newBuilder()
//      .addInterceptor(authInterceptor)
      .build()

  private fun retrofit(): Retrofit = Retrofit.Builder()
      .client(yahooClient)
      .baseUrl(baseUrl)
      .addConverterFactory(MoshiConverterFactory.create())
      .addCallAdapterFactory(CoroutineCallAdapterFactory())
      .build()

  fun update(_baseUrl: String) {
    if (baseUrl != _baseUrl) {
      if (_baseUrl.isBlank()) {
        baseUrl = ""
        yahooApi = null
      } else {
        baseUrl = checkBaseUrl(_baseUrl)
        yahooApi = try {
          retrofit().create(YahooApiChartData::class.java)
        } catch (e: Exception) {
          null
        }
      }
    }
  }

  init {
    update(defaultBaseUrl)
  }

  var yahooApi: YahooApiChartData? = null
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