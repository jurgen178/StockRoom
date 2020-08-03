package com.android.stockroom

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

// https://android.jlelse.eu/android-networking-in-2019-retrofit-with-kotlins-coroutines-aefe82c4d777

//StockApiFactory to create the Yahoo Api
object StockApiFactory {

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

  fun retrofit(): Retrofit = Retrofit.Builder()
      .client(yahooClient)
      .baseUrl("https://query1.finance.yahoo.com/v7/finance/")
      .addConverterFactory(MoshiConverterFactory.create())
      .addCallAdapterFactory(CoroutineCallAdapterFactory())
      .build()

  val yahooApi: YahooApi = retrofit().create(YahooApi::class.java)
}
