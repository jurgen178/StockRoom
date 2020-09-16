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

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.thecloudsite.stockroom.news.NewsData
import com.thecloudsite.stockroom.news.YahooNewsRepository
import kotlinx.coroutines.launch

class YahooNewsViewModel(application: Application) : AndroidViewModel(application) {

  private val newsRepository: YahooNewsRepository =
    YahooNewsRepository()

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