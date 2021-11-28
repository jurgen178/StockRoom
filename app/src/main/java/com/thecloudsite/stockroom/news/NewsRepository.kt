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

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.thecloudsite.stockroom.Result
import com.thecloudsite.stockroom.Result.Error
import com.thecloudsite.stockroom.Result.Success
import com.thecloudsite.stockroom.updateCounter
import retrofit2.Response
import java.io.IOException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

open class NewsRepository(
    private val api: () -> NewsApi?,
    private val newsType: Int
) {

    private suspend fun <T : Any> apiCall(
        call: suspend () -> Response<T>,
        errorMessage: String
    ): T? {
        val result: Result<T> = apiResult(call, errorMessage)
        var data: T? = null

        when (result) {
            is Success ->
                data = result.data
            is Error -> {
                Log.d(
                    "NewsRepository apiCall failed",
                    "$errorMessage , Exception: ${result.exception}"
                )
            }
        }

        return data
    }

    private suspend fun <T : Any> apiResult(
        call: suspend () -> Response<T>,
        errorMessage: String
    ): Result<T> {
        val response = call.invoke()
        if (response.isSuccessful)
            return Success(response.body()!!)

        return Error(
            IOException("Error in NewsRepository::apiResult: $errorMessage")
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
    ): List<NewsData> {

        var newsData: List<NewsData>? = null

        val api: NewsApi = api() ?: return emptyList()

        try {
            val newsResponse = apiCall(
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
                                ZonedDateTime.parse(
                                    newsItem.pubDate,
                                    DateTimeFormatter.RFC_1123_DATE_TIME
                                )
                            date = localDateTime.toEpochSecond() // in GMT
                        } catch (e: java.lang.Exception) {
                        }

                        if (date == 0L) {
                            date = ZonedDateTime.now()
                                .toEpochSecond() // in GMT
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
            Log.d("getOnlineNewsData failed", "Exception: $e")
        }

        return newsData ?: emptyList()
    }
}
