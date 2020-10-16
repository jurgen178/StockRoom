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

// https://news.google.com/news/rss/headlines/section/topic/BUSINESS
// https://news.google.com/rss/search?q=msft&hl=en-US&gl=US&ceid=US:en
// https://news.google.com/rss/search?q=msft&hl=de&gl=DE&ceid=DE:de

open class GoogleNewsBaseApiFactory {

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

object GoogleNewsApiFactory : GoogleNewsBaseApiFactory() {

  var newsApi: GoogleNewsApi? = null

  private var defaultUrl = "https://news.google.com/"

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
          retrofit().create(GoogleNewsApi::class.java)
        } catch (e: Exception) {
          null
        }
      }
    }
  }
}

// https://news.google.com/news/rss/headlines/section/topic/BUSINESS

object GoogleAllNewsApiFactory : GoogleNewsBaseApiFactory() {

  var newsApi: GoogleAllNewsApi? = null

  private var defaultUrl = "https://news.google.com/"

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
          retrofit().create(GoogleAllNewsApi::class.java)
        } catch (e: Exception) {
          null
        }
      }
    }
  }
}
