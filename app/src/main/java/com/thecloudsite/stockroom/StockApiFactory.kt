package com.thecloudsite.stockroom

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.thecloudsite.stockroom.StockRoomViewModel.AlertData
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
