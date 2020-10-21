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
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.database.Dividend
import com.thecloudsite.stockroom.database.Event
import com.thecloudsite.stockroom.database.StockDBdata

data class StockItemSet
(
  var allDataReady: Boolean = false,
  var stockItems: MutableList<StockItem> = mutableListOf()
)

// Data from the DB and online data fields.
data class StockItem
(
  var onlineMarketData: OnlineMarketData,
  var stockDBdata: StockDBdata,
  var assets: List<Asset>,
  var events: List<Event>,
  var dividends: List<Dividend>
)

var responseCounter = 0
@Synchronized fun updateCounter(){
  responseCounter++
}

class StockMarketDataRepository(private val api: () -> YahooApiMarketData?) : BaseRepository() {

  private val _data = MutableLiveData<List<OnlineMarketData>>()
  val onlineMarketDataList: LiveData<List<OnlineMarketData>>
    get() = _data

  suspend fun getStockData(symbols: List<String>): Pair<MarketState, String> {

    // no _data.value because this is a background thread
    if (symbols.isEmpty()) {
      _data.postValue(emptyList())
      return Pair(MarketState.NO_SYMBOL, "")
    }

    var errorMsg = ""

    val api: YahooApiMarketData? = api()

    if (api != null) {
      val quoteResponse: YahooResponse? = try {
        safeApiCall(
            call = {
              updateCounter()
              api.getStockDataAsync(symbols.joinToString(","))
                  .await()
            }, errorMessage = "Error getting finance data."
        )
      } catch (e: Exception) {
        errorMsg = "StockMarketDataRepository.getStockData(symbols) failed, Exception=$e"
        null
      }

      val onlineMarketDataResultList: List<OnlineMarketData> = quoteResponse?.quoteResponse?.result
          ?: emptyList()

      // no _data.value because this is a background thread
      if (onlineMarketDataResultList.isEmpty()) {
        _data.postValue(onlineMarketDataResultList)
        return Pair(MarketState.NO_NETWORK, errorMsg)
      }

      val postMarket: Boolean = SharedRepository.postMarket

      if (postMarket) {
        // Transform onlinedata
        // replace market value with postmarket value
        val onlineMarketDataResultList2 = onlineMarketDataResultList.map { onlineMarketData ->
          val onlineMarketData2: OnlineMarketData = onlineMarketData

          onlineMarketData2.postMarketData = false

          if ((onlineMarketData.marketState == MarketState.POST.value
                  || onlineMarketData.marketState == MarketState.POSTPOST.value
                  || onlineMarketData.marketState == MarketState.PREPRE.value
                  || onlineMarketData.marketState == MarketState.CLOSED.value)
              && onlineMarketData.postMarketPrice > 0.0
          ) {
            onlineMarketData2.postMarketData = true
            onlineMarketData2.marketPrice =
              onlineMarketData.postMarketPrice
            onlineMarketData2.marketChange =
              onlineMarketData.postMarketChange
            onlineMarketData2.marketChangePercent =
              onlineMarketData.postMarketChangePercent
          } else
            if ((onlineMarketData.marketState == MarketState.PRE.value)
                && onlineMarketData.preMarketPrice > 0.0
            ) {
              onlineMarketData2.postMarketData = true
              onlineMarketData2.marketPrice =
                onlineMarketData.preMarketPrice
              onlineMarketData2.marketChange =
                onlineMarketData.preMarketChange
              onlineMarketData2.marketChangePercent =
                onlineMarketData.preMarketChangePercent
            }

          onlineMarketData2
        }

        _data.postValue(onlineMarketDataResultList2)
      } else {
        _data.postValue(onlineMarketDataResultList)
      }

      // Stocks could be from different markets.
      // Check if stocks are from markets that have regular hours first, then post market,
      // and then pre market hours.
      val marketState: MarketState = when {
        onlineMarketDataResultList.find { data ->
          data.marketState == MarketState.REGULAR.value
        } != null -> {
          MarketState.REGULAR
        }
        onlineMarketDataResultList.find { data ->
          data.marketState == MarketState.POST.value
        } != null -> {
          MarketState.POST
        }
        onlineMarketDataResultList.find { data ->
          data.marketState == MarketState.POSTPOST.value
        } != null -> {
          MarketState.POSTPOST
        }
        onlineMarketDataResultList.find { data ->
          data.marketState == MarketState.CLOSED.value
        } != null -> {
          MarketState.CLOSED
        }
        onlineMarketDataResultList.find { data ->
          data.marketState == MarketState.PRE.value
        } != null -> {
          MarketState.PRE
        }
        onlineMarketDataResultList.find { data ->
          data.marketState == MarketState.PREPRE.value
        } != null -> {
          MarketState.PREPRE
        }
        else -> {
          MarketState.UNKNOWN
        }
      }

      return Pair(marketState, "")
    }

    _data.postValue(emptyList())
    return Pair(MarketState.UNKNOWN, "")
  }

  suspend fun getStockData2(symbols: List<String>): List<OnlineMarketData> {
    val api: YahooApiMarketData = api() ?: return emptyList()

    val quoteResponse: YahooResponse? = try {
      safeApiCall(
          call = {
            updateCounter()
            api.getStockDataAsync(symbols.joinToString(","))
                .await()
          }, errorMessage = "Error getting finance data."
      )
    } catch (e: Exception) {
      Log.d("StockMarketDataRepository.getStockData2 failed", "Exception=$e")
      null
    }

    return quoteResponse?.quoteResponse?.result ?: emptyList()
  }

  suspend fun getStockData(symbol: String): OnlineMarketData? {
    val api: YahooApiMarketData? = api()

    if (symbol.isNotEmpty() && api != null) {

      val quoteResponse: YahooResponse? = try {
        safeApiCall(
            call = {
              updateCounter()
              api.getStockDataAsync(symbol)
                  .await()
            },
            errorMessage = "Error getting finance data."
        )
      } catch (e: Exception) {
        Log.d("StockMarketDataRepository.getStockData($symbol) failed", "Exception=$e")
        null
      }

      return quoteResponse?.quoteResponse?.result?.firstOrNull()
    }

    return OnlineMarketData(symbol = symbol)
  }
}

class StockRawMarketDataRepository(private val api: () -> YahooApiRawMarketData?) : BaseRepository() {

//  private val _data = MutableLiveData<List<OnlineMarketData>>()
//  val onlineMarketDataList: LiveData<List<OnlineMarketData>>
//    get() = _data

  suspend fun getStockRawData(symbol: String): String {
    val api: YahooApiRawMarketData? = api()

    if (symbol.isNotEmpty() && api != null) {

      val quoteResponse: String? = try {
        safeApiCall(
            call = {
              updateCounter()
              api.getStockDataAsync(symbol)
                  .await()
            },
            errorMessage = "Error getting finance data."
        )
      } catch (e: Exception) {
        Log.d("StockRawMarketDataRepository.getStockRawData($symbol) failed", "Exception=$e")
        null
      }

      return quoteResponse ?: ""
    }

    return ""
  }
}
