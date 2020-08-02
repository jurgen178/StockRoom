package com.example.android.stockroom

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class NewsRepository(private val api: NewsApi) : BaseRepository() {

  private val _data = MutableLiveData<List<NewsData>>()
  val data: LiveData<List<NewsData>>
    get() = _data

  suspend fun getNewsData(
    symbol: String
  ) {
    _data.value = getOnlineNewsData(symbol)
  }

  suspend fun getOnlineNewsData(
    stock: String
  ): List<NewsData>? {

    val newsResponse = safeApiCall(
        call = {
          api.getNewsDataAsync(stock)
              .await()
        },
        errorMessage = "Error getting news data."
    )

    // Convert the response to a news data list.
    var newsData: List<NewsData>? = null
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
                link = newsItem.link
            )
          }
          ?.sortedByDescending { data ->
            data.date
          }
    }

    return newsData ?: emptyList()
  }
}
