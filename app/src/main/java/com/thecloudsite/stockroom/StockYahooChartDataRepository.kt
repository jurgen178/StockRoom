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

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.Locale

class StockChartDataRepository(
  private val yahooApi: () -> YahooApiChartData?,
  private val coingeckoApi: () -> CoingeckoApiChartData?
) : BaseRepository() {

  private val _data = MutableLiveData<StockChartData>()
  val chartData: LiveData<StockChartData>
    get() = _data

  suspend fun getYahooChartData(
    stockSymbol: StockSymbol,
    interval: String,
    range: String
  ) {
    _data.value =
      StockChartData(
        symbol = stockSymbol.symbol,
        stockDataEntries = getYahooChartDataEntries(stockSymbol, interval, range)
      )
  }

  private suspend fun getYahooChartDataEntries(
    stockSymbol: StockSymbol,
    interval: String,
    range: String
  ): List<StockDataEntry> {

    val stockDataEntries: MutableList<StockDataEntry> = mutableListOf()
    val api: YahooApiChartData = yahooApi() ?: return stockDataEntries.toList()

    val quoteResponse: YahooChartData? = try {
      safeApiCall(
        call = {
          updateCounter()
          api.getYahooChartDataAsync(stockSymbol.symbol, interval, range)
            .await()
        },
        errorMessage = "Error getting finance data."
      )
    } catch (e: Exception) {
      Log.d("StockChartDataRepository.getYahooChartDataAsync() failed", "Exception=$e")
      null
    }

    if (quoteResponse != null) {
      val yahooChartData = quoteResponse

      if (yahooChartData.chart != null) {
        val yahooChartDataEntry = yahooChartData.chart!!.result[0]
        val timestamps = yahooChartDataEntry.timestamp
        val yahooChartQuoteEntries = yahooChartDataEntry.indicators?.quote?.first()
        if (timestamps.size == yahooChartQuoteEntries?.close?.size && yahooChartQuoteEntries.close.size > 0) {

          // Do not use gmtoffset but display the data in local time using the GMT time data.
          // default is -18000: NYSE and NASDAQ are -5hour (-18000=-5*60*60) from London GMT
          val gmtoffset: Long = 0 //yahooChartDataEntry.meta?.gmtoffset?.toLong() ?: -18000

          // Interpolate values in case value is missing to avoid zero points.
          interpolateData(yahooChartQuoteEntries.high)
          interpolateData(yahooChartQuoteEntries.low)
          interpolateData(yahooChartQuoteEntries.open)
          interpolateData(yahooChartQuoteEntries.close)

          for (i in timestamps.indices) {
            val dateTimePoint = timestamps[i] + gmtoffset
            stockDataEntries.add(
              StockDataEntry(
                dateTimePoint = dateTimePoint,
                x = i.toDouble(),
                high = yahooChartQuoteEntries.high[i],
                low = yahooChartQuoteEntries.low[i],
                open = yahooChartQuoteEntries.open[i],
                close = yahooChartQuoteEntries.close[i]
              )
            )
          }
        }
      }
    }

    return stockDataEntries.toList()
  }

  suspend fun getCoingeckoChartData(
    stockSymbol: StockSymbol,
    currency: String,
    days: Int
  ) {
    _data.value =
      StockChartData(
        symbol = stockSymbol.symbol,
        stockDataEntries = getCoingeckoChartDataEntries(stockSymbol, currency, days)
      )
  }

  private suspend fun getCoingeckoChartDataEntries(
    stockSymbol: StockSymbol,
    currency: String,
    days: Int
  ): List<StockDataEntry> {

    val api: CoingeckoApiChartData = coingeckoApi() ?: return emptyList()

    val response: CoingeckoChartData? = try {
      safeApiCall(
        call = {
          updateCounter()
          val daysStr: String = if (days == 0) {
            "max"
          } else {
            "$days"
          }
          api.getCoingeckoChartDataAsync(
            stockSymbol.symbol.toLowerCase(Locale.ROOT),
            currency,
            daysStr
          )
            .await()
        },
        errorMessage = "Error getting finance data."
      )
    } catch (e: Exception) {
      Log.d("StockChartDataRepository.getCoingeckoChartDataAsync() failed", "Exception=$e")
      null
    }

    if (response?.prices != null) {
      val prices: List<MutableList<Double>> = response.prices!!
      var index = 0.0
      return prices.map { price ->
        val y = price[1]
        StockDataEntry(
          dateTimePoint = (price[0] / 1000.0).toLong(),
          x = index++,
          high = y,
          low = y,
          open = y,
          close = y
        )
      }
    }

    return emptyList()
  }

  // Interpolate values in case value is missing to avoid zero points.
  // For example: 3.2948999404907227,null,3.299999952316284,3.309799909591675,3.309999942779541,3.3299999237060547,...
  private fun interpolateData(
    values: MutableList<Double>
  ) {
    val size: Int = values.size
    // The first value must not be null because it is used as the first left side value.
    if (values[0] == null || values[0] == 0.0) {
      var j: Int = 1
      while (j < size && (values[j] == null || values[j] == 0.0)) {
        j++
      }

      values[0] = if (j < size && values[j] != null && values[j] != 0.0) {
        values[j]
      } else {
        0.0
      }
    }

    // The last value must not be null because it is used as the last right side value.
    if (size > 1 && (values[size - 1] == null || values[size - 1] == 0.0)) {
      var j: Int = size - 1
      while (j > 0 && (values[j] == null || values[j] == 0.0)) {
        j--
      }

      values[size - 1] = if (j > 0 && values[j] != null && values[j] != 0.0) {
        values[j]
      } else {
        values[0]
      }
    }

    for (i in 1 until size) {
      if (values[i] == null || values[i] == 0.0) {
        // Index is missing. Search for the next available index.
        var j: Int = i + 1
        while (j < size && (values[j] == null || values[j] == 0.0)) {
          j++
        }
        if (j < size && values[j] != null && values[j] != 0.0) {
          // Calculate the weighted interpolated values.
          val prevValue = values[i - 1]
          // No interpolation for more than three missing values.
          // This is often the case when a stock is halted or not traded for that day.
          val segment = if (j > i + 3) {
            0.0
          } else {
            (values[j] - prevValue) / (j - i + 1)
          }
          for (k in i until j) {
            values[k] = prevValue + (k - i + 1) * segment
          }
        }
      }
    }
  }
}
