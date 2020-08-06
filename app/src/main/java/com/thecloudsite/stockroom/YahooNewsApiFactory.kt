package com.thecloudsite.stockroom

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

// https://feeds.finance.yahoo.com/rss/2.0/headline?s=msft

object YahooNewsApiFactory {

  // https://futurestud.io/tutorials/retrofit-how-to-integrate-xml-converter
  private fun retrofit(): Retrofit = Retrofit.Builder()
      .client(
          OkHttpClient().newBuilder()
              .build()
      )
      .baseUrl("https://feeds.finance.yahoo.com/")
      .addConverterFactory(SimpleXmlConverterFactory.create())
      .addCallAdapterFactory(CoroutineCallAdapterFactory())
      .build()

  val newsApi: YahooNewsApi = retrofit().create(YahooNewsApi::class.java)
}
