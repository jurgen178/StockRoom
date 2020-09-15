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

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import retrofit2.Response
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

open class NewsRepository(
  private val api: () -> NewsApi?,
  private val newsType: Int
) {

  private suspend fun <T : Any> safeApiCall(
    call: suspend () -> Response<T>,
    errorMessage: String
  ): T? {

    val result: Result<T> = safeApiResult(call, errorMessage)
    var data: T? = null

    when (result) {
      is Result.Success ->
        data = result.data
      is Result.Error -> {
        Log.d(
            "BaseRepository safeApiCall failed", "$errorMessage & Exception - ${result.exception}"
        )
      }
    }

    return data
  }

  private suspend fun <T : Any> safeApiResult(
    call: suspend () -> Response<T>,
    errorMessage: String
  ): Result<T> {
    val response = call.invoke()
    if (response.isSuccessful) return Result.Success(response.body()!!)

    return Result.Error(
        IOException("Error Occurred during getting safe Api result, Custom ERROR - $errorMessage")
    )
  }

  private val _data = MutableLiveData<List<NewsData>>()
  val data: LiveData<List<NewsData>>
    get() = _data

  suspend fun getNewsData(
    newsQuery: String
  ) {
    _data.value = getOnlineNewsData(newsQuery)
  }

  private suspend fun getOnlineNewsData(
    newsQuery: String
  ): List<NewsData>? {

    var newsData: List<NewsData>? = null

    val api: NewsApi = api() ?: return emptyList()

    try {
      val newsResponse = safeApiCall(
          call = {
            updateCounter()
            api.getNewsDataAsync(newsQuery)
                .await()
          },
          errorMessage = "Error getting news data."
      )

      // Convert the response to a news data list.
      if (newsResponse != null) {
        newsData = newsResponse.newsItems?.filter { newsItem ->
          newsItem.title.isNotEmpty()
        }
            ?.map { newsItem ->

              var date: Long = 0
              // Convert pubDate field "Fri, 31 Jul 2020 14:54:54 GMT" to unix time
              try {
                val localDateTime =
                  LocalDateTime.parse(newsItem.pubDate, DateTimeFormatter.RFC_1123_DATE_TIME)
                date = localDateTime.toEpochSecond(ZoneOffset.UTC)
              } catch (e: java.lang.Exception) {
              }

              if (date == 0L) {
                date = LocalDateTime.now()
                    .toEpochSecond(ZoneOffset.UTC)
              }

              NewsData(
                  title = newsItem.title,
                  text = newsItem.description,
                  date = date,
                  link = newsItem.link,
                  type = newsType
              )
            }
            ?.sortedByDescending { data ->
              data.date
            }
      }
    } catch (e: Exception) {
      Log.d("getOnlineNewsData failed", "Exception - $e")
    }

    return newsData ?: emptyList()
  }
}
