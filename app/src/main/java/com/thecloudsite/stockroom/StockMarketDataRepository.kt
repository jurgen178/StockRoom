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
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.database.Dividend
import com.thecloudsite.stockroom.database.Event
import com.thecloudsite.stockroom.database.StockDBdata
import java.util.Locale

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
@Synchronized fun updateCounter() {
  responseCounter++
}

class StockMarketDataRepository(
  private val yahooApi: () -> YahooApiMarketData?,
  private val coingeckoApi: () -> CoingeckoApiMarketData?
) : BaseRepository() {

  private val yahooMarketData = MutableLiveData<List<OnlineMarketData>>()
  private val yahooMarketDataList: LiveData<List<OnlineMarketData>>
    get() = yahooMarketData

  private val coingeckoMarketData = MutableLiveData<List<OnlineMarketData>>()
  private val coingeckoMarketDataList: LiveData<List<OnlineMarketData>>
    get() = coingeckoMarketData

  private var yahooMarketSourceDataList: List<OnlineMarketData> = emptyList()
  private var coingeckoMarketSourceDataList: List<OnlineMarketData> = emptyList()
  val onlineMarketDataList = MediatorLiveData<List<OnlineMarketData>>()

  init {

    onlineMarketDataList.addSource(yahooMarketDataList) { marketLiveData ->
      if (marketLiveData != null) {
        yahooMarketSourceDataList = marketLiveData
        onlineMarketDataList.postValue(
          combineData(
            yahooMarketSourceDataList,
            coingeckoMarketSourceDataList
          )
        )
      }
    }

    onlineMarketDataList.addSource(coingeckoMarketDataList) { marketLiveData ->
      if (marketLiveData != null) {
        coingeckoMarketSourceDataList = marketLiveData
        onlineMarketDataList.postValue(
          combineData(
            yahooMarketSourceDataList,
            coingeckoMarketSourceDataList
          )
        )
      }
    }
  }

  private fun combineData(
    list1: List<OnlineMarketData>,
    list2: List<OnlineMarketData>
  ): List<OnlineMarketData> {

    val combinedList: MutableList<OnlineMarketData> = mutableListOf()
    combinedList.addAll(list1)
    combinedList.addAll(list2)

    return combinedList
  }

  suspend fun getStockData(symbols: List<StockSymbol>): Pair<MarketState, String> {

    // When the app is in the background.
    if (symbols.isEmpty()) {
      // no _data.value because this is a background thread
      yahooMarketData.postValue(emptyList())
      coingeckoMarketData.postValue(emptyList())
      return Pair(MarketState.NO_SYMBOL, "")
    }

    val yahooResult = getYahooStockData(symbols)

    val coingeckoResult = getCoingeckoStockData(symbols)

    return yahooResult
  }

  private suspend fun getYahooStockData(symbols: List<StockSymbol>): Pair<MarketState, String> {

    val api: YahooApiMarketData? = yahooApi()

    if (api != null) {
      // for reference, all data in one go
//      val quoteResponse: YahooResponse? = try {
//        safeApiCall(
//            call = {
//              updateCounter()
//              api.getStockDataAsync(symbols.joinToString(","))
//                  .await()
//            }, errorMessage = "Error getting finance data."
//        )
//      } catch (e: Exception) {
//        errorMsg = "StockMarketDataRepository.getStockData(symbols) failed, Exception=$e"
//        null
//      }
//
//      val onlineMarketDataResultList: List<OnlineMarketData> = quoteResponse?.quoteResponse?.result
//          ?: emptyList()

      val result = queryYahooStockData(api, symbols)

      // Get all results.
      val onlineMarketDataResultList = result.first
      val errorMsg = result.second

      if (onlineMarketDataResultList.isEmpty()) {
        // no _data.value because this is a background thread
        yahooMarketData.postValue(onlineMarketDataResultList)
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

        yahooMarketData.postValue(onlineMarketDataResultList2)
      } else {
        yahooMarketData.postValue(onlineMarketDataResultList)
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

    yahooMarketData.postValue(emptyList())
    return Pair(MarketState.UNKNOWN, "")
  }

  private suspend fun getCoingeckoStockData(symbols: List<StockSymbol>): Pair<MarketState, String> {

    val api: CoingeckoApiMarketData? = coingeckoApi()

    if (api != null) {

      val result = queryCoingeckoStockData(api, symbols)

      // Get all results.
      val onlineMarketDataResultList = result.first
      val errorMsg = result.second

      if (onlineMarketDataResultList.isEmpty()) {
        // no _data.value because this is a background thread
        coingeckoMarketData.postValue(onlineMarketDataResultList)
        return Pair(MarketState.NO_NETWORK, errorMsg)
      }


      coingeckoMarketData.postValue(onlineMarketDataResultList)

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

    coingeckoMarketData.postValue(emptyList())
    return Pair(MarketState.UNKNOWN, "")
  }

  suspend fun getStockData2(symbols: List<StockSymbol>): List<OnlineMarketData> {

    val api: YahooApiMarketData = yahooApi() ?: return emptyList()

//    val quoteResponse: YahooResponse? = try {
//      safeApiCall(
//        call = {
//          updateCounter()
//          api.getStockDataAsync(symbols.joinToString(","))
//            .await()
//        }, errorMessage = "Error getting finance data."
//      )
//    } catch (e: Exception) {
//      Log.d("StockMarketDataRepository.getStockData2 failed", "Exception=$e")
//      null
//    }
//
//    return quoteResponse?.quoteResponse?.result ?: emptyList()

    val result = queryYahooStockData(api, symbols)
    return result.first
  }

  suspend fun getStockData(symbol: StockSymbol): OnlineMarketData? {

    val api: YahooApiMarketData? = yahooApi()

    if (symbol.symbol.isNotEmpty() && api != null) {
      val result = queryYahooStockData(api, listOf(symbol))
      return result.first.firstOrNull()
    }

    return OnlineMarketData(symbol = symbol.symbol)
  }

  // Query stock data by splitting symbols to blocks.
  private suspend fun queryYahooStockData(
    api: YahooApiMarketData,
    symbols: List<StockSymbol>
  ): Pair<List<OnlineMarketData>, String> {

    // Get blockSize symbol data at a time.
    val blockSize = 32
    var errorMsg = ""

    // Get online data in blocks.
    val onlineMarketDataResultList: MutableList<OnlineMarketData> = mutableListOf()
    var remainingSymbolsToQuery = symbols

    do {
      // Get the first number of blockSize symbols.
      val symbolsToQuery: List<String> = remainingSymbolsToQuery
        .take(blockSize)
        .map { symbol ->
          symbol.symbol
        }

      val quoteResponse: YahooResponse? = try {
        safeApiCall(
          call = {
            updateCounter()
            api.getStockDataAsync(symbolsToQuery.joinToString(","))
              .await()
          }, errorMessage = "Error getting finance data."
        )
      } catch (e: Exception) {
        errorMsg += "StockMarketDataRepository.queryStockData failed, Exception=$e\n"
        Log.d("StockMarketDataRepository.queryStockData failed", "Exception=$e")
        null
      }

      // Add the result.
      onlineMarketDataResultList.addAll(
        quoteResponse?.quoteResponse?.result
          ?: emptyList()
      )

      // Remove the queried symbols.
      remainingSymbolsToQuery = remainingSymbolsToQuery.drop(blockSize)

    } while (remainingSymbolsToQuery.isNotEmpty())

    return Pair(onlineMarketDataResultList, errorMsg)
  }

  private suspend fun queryCoingeckoStockData(
    api: CoingeckoApiMarketData,
    symbols: List<StockSymbol>
  ): Pair<List<OnlineMarketData>, String> {

    // Get blockSize symbol data at a time.
    val blockSize = 1
    var errorMsg = ""

    // Get online data in blocks.
    val onlineMarketDataResultList: MutableList<OnlineMarketData> = mutableListOf()
    var remainingSymbolsToQuery = symbols

    do {
      // Get the first number of blockSize symbols.
      val symbolsToQuery: List<String> = remainingSymbolsToQuery
        .take(blockSize)
        .map { symbol ->
          symbol.symbol
        }

      val response: CoingeckoResponse? = try {
        safeApiCall(
          call = {
            updateCounter()
            api.getStockDataAsync(symbolsToQuery.joinToString(",").toLowerCase(Locale.ROOT))
              .await()
          }, errorMessage = "Error getting finance data."
        )
      } catch (e: Exception) {
        errorMsg += "StockMarketDataRepository.queryStockData failed, Exception=$e\n"
        Log.d("StockMarketDataRepository.queryStockData failed", "Exception=$e")
        null
      }

      // Add the result.
      if (response != null) {
        onlineMarketDataResultList.add(
          OnlineMarketData(
            symbol = response.name,
            marketPrice = 1.23,
            name1 = "name1",
            name2 = "name2"
          )
        )
      }

      // Remove the queried symbols.
      remainingSymbolsToQuery = remainingSymbolsToQuery.drop(blockSize)

    } while (remainingSymbolsToQuery.isNotEmpty())

    return Pair(onlineMarketDataResultList, errorMsg)
  }

  class StockRawMarketDataRepository(private val api: () -> YahooApiRawMarketData?) :
    BaseRepository() {

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
}
