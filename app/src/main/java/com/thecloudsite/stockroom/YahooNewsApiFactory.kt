package com.thecloudsite.stockroom

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

// https://feeds.finance.yahoo.com/rss/2.0/headline?s=msft

object YahooNewsApiFactory {

  private var defaultBaseUrl = "https://feeds.finance.yahoo.com/"
  private var baseUrl = ""

  // https://futurestud.io/tutorials/retrofit-how-to-integrate-xml-converter
  private fun retrofit(): Retrofit = Retrofit.Builder()
      .client(
          OkHttpClient().newBuilder()
              .build()
      )
      .baseUrl(baseUrl)
      .addConverterFactory(SimpleXmlConverterFactory.create())
      .addCallAdapterFactory(CoroutineCallAdapterFactory())
      .build()

  fun update(_baseUrl: String) {
    if (baseUrl != _baseUrl) {
      baseUrl = checkBaseUrl(_baseUrl)

      newsApi = try {
        retrofit().create(YahooNewsApi::class.java)
      } catch (e: Exception) {
        null
      }
    }
  }

  init {
    update(defaultBaseUrl)
  }

  var newsApi: YahooNewsApi? = null
}
