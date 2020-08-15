package com.thecloudsite.stockroom

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

// https://news.google.com/news/rss/headlines/section/topic/BUSINESS
// https://news.google.com/rss/search?q=msft&hl=en-US&gl=US&ceid=US:en
// https://news.google.com/rss/search?q=msft&hl=de&gl=DE&ceid=DE:de

object GoogleNewsApiFactory {

  private var defaultBaseUrl = "https://news.google.com/"
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
      if (_baseUrl.isBlank()) {
        baseUrl = ""
        newsApi = null
      } else {
        baseUrl = checkBaseUrl(_baseUrl)
        newsApi = try {
          retrofit().create(GoogleNewsApi::class.java)
        } catch (e: Exception) {
          null
        }
      }
    }
  }

  init {
    update(defaultBaseUrl)
  }

  var newsApi: GoogleNewsApi? = null
}
