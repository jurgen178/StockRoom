package com.example.android.stockroom

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class NewsViewModel(application: Application) : AndroidViewModel(application) {

  private val newsRepository: NewsRepository =
    NewsRepository(NewsApiFactory.newsApi)

  val data: LiveData<List<NewsData>>

  init {
    data = newsRepository.data
  }

  fun getNewsData(
    symbol: String
  ) {
    viewModelScope.launch {
      newsRepository.getNewsData(symbol)
    }
  }
}