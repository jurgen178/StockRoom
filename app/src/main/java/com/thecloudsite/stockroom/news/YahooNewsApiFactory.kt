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

package com.thecloudsite.stockroom.news

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.thecloudsite.stockroom.utils.checkUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

// https://feeds.finance.yahoo.com/rss/2.0/headline?s=msft
// https://finance.yahoo.com/news/rssindex
// https://finance.yahoo.com/rss/topfinstories

// https://observablehq.com/@stroked/yahoofinance
// https://github.com/topics/yahoo-finance-api

open class YahooNewsBaseApiFactory
{
  var url = ""

  // https://futurestud.io/tutorials/retrofit-how-to-integrate-xml-converter
  fun retrofit(): Retrofit = Retrofit.Builder()
      .client(
          OkHttpClient().newBuilder()
              .build()
      )
      .baseUrl(url)
      .addConverterFactory(SimpleXmlConverterFactory.create())
      .addCallAdapterFactory(CoroutineCallAdapterFactory())
      .build()
}

object YahooNewsApiFactory : YahooNewsBaseApiFactory() {

  var newsApi: YahooNewsApi? = null

  private var defaultUrl = "https://feeds.finance.yahoo.com/"

  init {
    update(defaultUrl)
  }

  fun update(_url: String) {
    if (url != _url) {
      if (_url.isBlank()) {
        url = ""
        newsApi = null
      } else {
        url = checkUrl(_url)
        newsApi = try {
          retrofit().create(YahooNewsApi::class.java)
        } catch (e: Exception) {
          null
        }
      }
    }
  }
}

// https://finance.yahoo.com/rss/topstories
// https://finance.yahoo.com/news/rssindex

object YahooAllNewsApiFactory : YahooNewsBaseApiFactory() {

  var newsApi: YahooAllNewsApi? = null

  private var defaultUrl = "https://finance.yahoo.com/"

  init {
    update(defaultUrl)
  }

  fun update(_url: String) {
    if (url != _url) {
      if (_url.isBlank()) {
        url = ""
        newsApi = null
      } else {
        url = checkUrl(_url)
        newsApi = try {
          retrofit().create(YahooAllNewsApi::class.java)
        } catch (e: Exception) {
          null
        }
      }
    }
  }
}
