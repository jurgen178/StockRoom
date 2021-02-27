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

package com.thecloudsite.stockroom

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class StockChartDataViewModel(application: Application) : AndroidViewModel(application) {

  private val stockChartDataRepository: StockChartDataRepository =
    StockChartDataRepository { StockChartDataApiFactory.yahooApi }

  val chartData: LiveData<StockChartData> = stockChartDataRepository.chartData

  private fun getChartData(
    symbol: String,
    interval: String,
    range: String
  ) {
    viewModelScope.launch {
      stockChartDataRepository.getChartData(symbol, interval, range)
    }
  }

  fun getChartData(
    symbol: String,
    stockViewRange: StockViewRange
  ) {
    // Valid intervals: [1m, 2m, 5m, 15m, 30m, 60m, 90m, 1h, 1d, 5d, 1wk, 1mo, 3mo]
    // Valid ranges: ["1d","5d","1mo","3mo","6mo","1y","2y","5y","ytd","max"]
    when (stockViewRange) {
      StockViewRange.OneDay -> {
        getChartData(symbol, "5m", "1d")
      }
      StockViewRange.FiveDays -> {
        getChartData(symbol, "15m", "5d")
      }
      StockViewRange.OneMonth -> {
        getChartData(symbol, "90m", "1mo")
      }
      StockViewRange.ThreeMonth -> {
        getChartData(symbol, "1d", "3mo")
      }
      StockViewRange.YTD -> {
        getChartData(symbol, "1d", "ytd")
      }
      StockViewRange.OneYear -> {
        getChartData(symbol, "1d", "1y")
      }
      StockViewRange.FiveYears -> {
        getChartData(symbol, "1d", "5y")
      }
      StockViewRange.Max -> {
        getChartData(symbol, "1d", "max")
      }
    }
  }
}
