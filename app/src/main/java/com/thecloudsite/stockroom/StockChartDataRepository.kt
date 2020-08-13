package com.thecloudsite.stockroom

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class StockChartDataRepository(private val api: YahooApi) : BaseRepository() {

  private val _data = MutableLiveData<List<StockDataEntry>>()
  val data: LiveData<List<StockDataEntry>>
    get() = _data

  suspend fun getChartData(
    symbol: String,
    interval: String,
    range: String
  ) {
    _data.value = getYahooChartData(symbol, interval, range)
  }

  private suspend fun getYahooChartData(
    stock: String,
    interval: String,
    range: String
  ): List<StockDataEntry>? {

    val quoteResponse: YahooChartData? = try {
      safeApiCall(
          call = {
            api.getYahooChartDataAsync(stock, interval, range)
                .await()
          },
          errorMessage = "Error getting finance data."
      )
    } catch (e: Exception) {
      Log.d("StockChartDataRepository.getYahooChartDataAsync() failed", "Exception=$e")
      null
    }

    val stockDataEntries: MutableList<StockDataEntry> = mutableListOf()
    if (quoteResponse != null) {
      val yahooChartData = quoteResponse

      if (yahooChartData.chart != null) {
        val yahooChartDataEntry = yahooChartData.chart!!.result[0]
        val timestamps = yahooChartDataEntry.timestamp
        val yahooChartQuoteEntries = yahooChartDataEntry.indicators?.quote?.first()
        if (timestamps.size == yahooChartQuoteEntries?.close?.size && yahooChartQuoteEntries.close.size > 0) {
          val gmtoffset: Long = yahooChartDataEntry.meta?.gmtoffset?.toLong() ?: 0
          // Interpolate values in case value is missing to avoid zero points.
          interpolateData(yahooChartQuoteEntries.high)
          interpolateData(yahooChartQuoteEntries.low)
          interpolateData(yahooChartQuoteEntries.open)
          interpolateData(yahooChartQuoteEntries.close)

          for (i in timestamps.indices) {
            val dateTimePoint = (timestamps[i] + gmtoffset)
            stockDataEntries.add(
                StockDataEntry(
                    dateTimePoint, i.toFloat(),
                    yahooChartQuoteEntries.high[i],
                    yahooChartQuoteEntries.low[i],
                    yahooChartQuoteEntries.open[i],
                    yahooChartQuoteEntries.close[i]
                )
            )
          }
        }
      }
    }

    return stockDataEntries.toList()
  }

  // Interpolate values in case value is missing to avoid zero points.
  // For example: 3.2948999404907227,null,3.299999952316284,3.309799909591675,3.309999942779541,3.3299999237060547,...
  private fun interpolateData(
    values: MutableList<Float>
  ) {
    val size: Int = values.size
    // The first value must not be null because it is used as the first left side value.
    if (values[0] == null || values[0] == 0f) {
      var j: Int = 1
      while (j < size && (values[j] == null || values[j] == 0f)) {
        j++
      }
      if (j < size && values[j] != null && values[j] != 0f) {
        values[0] = values[j]
      }
    }

    // The last value must not be null because it is used as the last right side value.
    if (size > 1 && (values[size - 1] == null || values[size - 1] == 0f)) {
      var j: Int = size - 1
      while (j > 0 && (values[j] == null || values[j] == 0f)) {
        j--
      }
      if (j > 0 && values[j] != null && values[j] != 0f) {
        values[size - 1] = values[j]
      }
    }

    for (i in 1 until size) {
      if (values[i] == null || values[i] == 0f) {
        // Index is missing. Search for the next available index.
        var j: Int = i + 1
        while (j < size && (values[j] == null || values[j] == 0f)) {
          j++
        }
        if (j < size && values[j] != null && values[j] != 0f) {
          // Calculate the weighted interpolated values.
          val prevValue = values[i - 1]
          // No interpolation for more than three missing values.
          // This is often the case when a stock is halted or not traded for that day.
          val segment = if (j > i + 3) {
            0f
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
