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
import com.thecloudsite.stockroom.utils.checkBaseUrl
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
      if (_baseUrl.isBlank()) {
        baseUrl = ""
        newsApi = null
      } else {
        baseUrl = checkBaseUrl(_baseUrl)
        newsApi = try {
          retrofit().create(YahooNewsApi::class.java)
        } catch (e: Exception) {
          null
        }
      }
    }
  }

  init {
    update(defaultBaseUrl)
  }

  var newsApi: YahooNewsApi? = null
}
