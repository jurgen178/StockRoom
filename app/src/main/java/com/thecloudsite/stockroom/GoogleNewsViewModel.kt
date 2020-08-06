package com.thecloudsite.stockroom

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class GoogleNewsViewModel(application: Application) : AndroidViewModel(application) {

  private val newsRepository: GoogleNewsRepository =
    GoogleNewsRepository()

  val data: LiveData<List<NewsData>>

  init {
    data = newsRepository.data
  }

  fun getNewsData(
    newsQuery: String
  ) {
    viewModelScope.launch {
      newsRepository.getNewsData(newsQuery)
    }
  }
}