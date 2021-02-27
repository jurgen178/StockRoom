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

package com.thecloudsite.stockroom.news

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.simplexml.SimpleXmlConverterFactory

// https://www.nasdaq.com/feed/rssoutbound?symbol=msft

// nasdaq rss feed is returning 403 even with added http header
open class NewsApiFactoryHttpHeader {
  var url = ""

  // https://futurestud.io/tutorials/retrofit-how-to-integrate-xml-converter
  fun retrofit(): Retrofit = Retrofit.Builder()
      .client(
          OkHttpClient().newBuilder()
//              .followRedirects(true)
//              .followSslRedirects(true)
              .addInterceptor { chain ->
                val original = chain.request()
                val newRequest = original
                    .newBuilder()

//GET /tutorials/other/top-20-mysql-best-practices/ HTTP/1.1
//Host: net.tutsplus.com
//User-Agent: Mozilla/5.0 (Windows; U; Windows NT 6.1; en-US; rv:1.9.1.5) Gecko/20091102 Firefox/3.5.5 (.NET CLR 3.5.30729)
//Accept: text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8
//Accept-Language: en-us,en;q=0.5
//Accept-Encoding: gzip,deflate
//Accept-Charset: ISO-8859-1,utf-8;q=0.7,*;q=0.7
//Keep-Alive: 300
//Connection: keep-alive
//Cookie: PHPSESSID=r2t5uvjq435r4q7ib3vtdjq120
//Pragma: no-cache
//Cache-Control: no-cache

                    .addHeader("Host", "www.nasdaq.com")
                    .addHeader("User-Agent", "Android/10")
                    .addHeader("Accept", "*/*")
                    .addHeader("Accept-Language", "en")
//                    .addHeader("Accept-Language", "en-us,en;q=0.5")
//                    .addHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7")
//                    .addHeader("Keep-Alive", "300")
//                    .addHeader("Connection", "keep-alive")
//                    .addHeader("Pragma", "no-cache")
//                    .addHeader("Cache-Control", "no-cache")
                    //.method(original.method, original.body)
                    .build()
                chain.proceed(newRequest)

// original
// Request{method=GET, url=https://www.nasdaq.com/feed/rssoutbound?symbol=GECC,
// tags={class retrofit2.Invocation=com.thecloudsite.stockroom.news.NasdaqNewsApi.getNewsDataAsync() [GECC]}}

// newRequest
// Request{method=GET, url=https://www.nasdaq.com/feed/rssoutbound?symbol=GECC,
// headers=[Host:www.nasdaq.com, User-Agent:Android/10, Accept:*/*],
// tags={class retrofit2.Invocation=com.thecloudsite.stockroom.news.NasdaqNewsApi.getNewsDataAsync() [GECC]}}

                  }
              .build()
      )
      .baseUrl(url)
      .addConverterFactory(SimpleXmlConverterFactory.create())
      .addCallAdapterFactory(CoroutineCallAdapterFactory())
      .build()
}
