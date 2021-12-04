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
import java.util.*
import kotlin.math.max
import kotlin.math.min

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

@Synchronized
fun updateCounter() {
    responseCounter++
}

data class MarketDataResult
    (
    var marketState: MarketState,
    var delayInMs: Long,
    var msg: String,
)

class StockMarketDataRepository(
    private val yahooApi: () -> YahooApiMarketData?,
    private val coingeckoApi: () -> CoingeckoApiMarketData?,
    private val coinpaprikaApi: () -> CoinpaprikaApiMarketData?,
    private val geminiApi: () -> GeminiApiMarketData?,
) : BaseRepository() {

    private val yahooMarketData = MutableLiveData<List<OnlineMarketData>>()
    private val yahooMarketDataList: LiveData<List<OnlineMarketData>>
        get() = yahooMarketData

    private val coingeckoMarketData = MutableLiveData<List<OnlineMarketData>>()
    private val coingeckoMarketDataList: LiveData<List<OnlineMarketData>>
        get() = coingeckoMarketData

    private val coinpaprikaMarketData = MutableLiveData<List<OnlineMarketData>>()
    private val coinpaprikaMarketDataList: LiveData<List<OnlineMarketData>>
        get() = coinpaprikaMarketData

    private val geminiMarketData = MutableLiveData<List<OnlineMarketData>>()
    private val geminiMarketDataList: LiveData<List<OnlineMarketData>>
        get() = geminiMarketData

    private var yahooMarketSourceDataList: List<OnlineMarketData> = emptyList()
    private var coingeckoMarketSourceDataList: List<OnlineMarketData> = emptyList()
    private var coinpaprikaMarketSourceDataList: List<OnlineMarketData> = emptyList()
    private var geminiMarketSourceDataList: List<OnlineMarketData> = emptyList()
    val onlineMarketDataList = MediatorLiveData<List<OnlineMarketData>>()

    init {

        onlineMarketDataList.addSource(yahooMarketDataList) { marketLiveData ->
            if (marketLiveData != null) {
                yahooMarketSourceDataList = marketLiveData
                onlineMarketDataList.postValue(
                    combineData(
                        yahooMarketSourceDataList,
                        coingeckoMarketSourceDataList,
                        coinpaprikaMarketSourceDataList,
                        geminiMarketSourceDataList
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
                        coingeckoMarketSourceDataList,
                        coinpaprikaMarketSourceDataList,
                        geminiMarketSourceDataList
                    )
                )
            }
        }

        onlineMarketDataList.addSource(coinpaprikaMarketDataList) { marketLiveData ->
            if (marketLiveData != null) {
                coinpaprikaMarketSourceDataList = marketLiveData
                onlineMarketDataList.postValue(
                    combineData(
                        yahooMarketSourceDataList,
                        coingeckoMarketSourceDataList,
                        coinpaprikaMarketSourceDataList,
                        geminiMarketSourceDataList
                    )
                )
            }
        }

        onlineMarketDataList.addSource(geminiMarketDataList) { marketLiveData ->
            if (marketLiveData != null) {
                geminiMarketSourceDataList = marketLiveData
                onlineMarketDataList.postValue(
                    combineData(
                        yahooMarketSourceDataList,
                        coingeckoMarketSourceDataList,
                        coinpaprikaMarketSourceDataList,
                        geminiMarketSourceDataList
                    )
                )
            }
        }
    }

    private fun combineData(
        list1: List<OnlineMarketData>,
        list2: List<OnlineMarketData>,
        list3: List<OnlineMarketData>,
        list4: List<OnlineMarketData>,
    ): List<OnlineMarketData> {

        val combinedList: MutableList<OnlineMarketData> = mutableListOf()
        combinedList.addAll(list1)
        combinedList.addAll(list2)
        combinedList.addAll(list3)
        combinedList.addAll(list4)

        return combinedList
    }

    suspend fun getStockData(symbols: List<StockSymbol>): MarketDataResult {

        // When the app is in the background.
        if (symbols.isEmpty()) {
            // no _data.value because this is a background thread
            yahooMarketData.postValue(emptyList())
            coingeckoMarketData.postValue(emptyList())
            coinpaprikaMarketData.postValue(emptyList())
            return MarketDataResult(marketState = MarketState.NO_SYMBOL, delayInMs = -1, msg = "")
        }

        val marketDataResult =
            MarketDataResult(marketState = MarketState.NO_SYMBOL, delayInMs = -1, msg = "")

        val standardList = symbols.filter { stock ->
            stock.type == DataProvider.Standard
        }
        if (standardList.isNotEmpty()) {
            val yahooResult = getYahooStockData(standardList)
            marketDataResult.marketState = yahooResult.marketState
            marketDataResult.delayInMs = yahooResult.delayInMs
            marketDataResult.msg += yahooResult.msg
        }

        val cryptoListCoingecko = symbols.filter { stock ->
            stock.type == DataProvider.Coingecko
        }
        if (cryptoListCoingecko.isNotEmpty()) {
            val coingeckoResult = getCoingeckoStockData(cryptoListCoingecko)
            marketDataResult.marketState = coingeckoResult.marketState
            marketDataResult.delayInMs = coingeckoResult.delayInMs
            marketDataResult.msg += coingeckoResult.msg
        }

        val cryptoListCoinpaprika = symbols.filter { stock ->
            stock.type == DataProvider.Coinpaprika
        }
        if (cryptoListCoinpaprika.isNotEmpty()) {
            val coinpaprikaResult = getCoinpaprikaStockData(cryptoListCoinpaprika)
            marketDataResult.marketState = coinpaprikaResult.marketState
            marketDataResult.delayInMs = coinpaprikaResult.delayInMs
            marketDataResult.msg += coinpaprikaResult.msg
        }

        val cryptoListGemini = symbols.filter { stock ->
            stock.type == DataProvider.Gemini
        }
        if (cryptoListGemini.isNotEmpty()) {
            val geminiResult = getGeminiStockData(cryptoListGemini)
            marketDataResult.marketState = geminiResult.marketState
            marketDataResult.delayInMs = geminiResult.delayInMs
            marketDataResult.msg += geminiResult.msg
        }

        return marketDataResult
    }

    private suspend fun getYahooStockData(symbols: List<StockSymbol>): MarketDataResult {

        val api: YahooApiMarketData? = yahooApi()

        if (api != null) {
            // for reference, all data in one go
//      val quoteResponse: YahooResponse? = try {
//        apiCall(
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
                return MarketDataResult(
                    marketState = MarketState.NO_NETWORK,
                    delayInMs = -1,
                    msg = errorMsg
                )
            }

            val postMarket: Boolean = SharedRepository.postMarket

            if (postMarket) {
                // Transform onlinedata
                // replace market value with postmarket value
                val onlineMarketDataResultList2 =
                    onlineMarketDataResultList.map { onlineMarketData ->
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

            return MarketDataResult(marketState = marketState, delayInMs = -1, msg = "")
        }

        yahooMarketData.postValue(emptyList())
        return MarketDataResult(marketState = MarketState.UNKNOWN, delayInMs = -1, msg = "")
    }

    private suspend fun getCoingeckoStockData(symbols: List<StockSymbol>): MarketDataResult {

        val api: CoingeckoApiMarketData? = coingeckoApi()

        if (api != null) {

            val result = queryCoingeckoStockData(api, symbols)

            // Get all results.
            val onlineMarketDataResultList = result.first
            val errorMsg = result.second

            if (onlineMarketDataResultList.isEmpty()) {
                // no _data.value because this is a background thread
                coingeckoMarketData.postValue(onlineMarketDataResultList)
                return MarketDataResult(
                    marketState = MarketState.QUOTA_EXCEEDED,
                    delayInMs = -1,
                    msg = errorMsg
                )
            }

            coingeckoMarketData.postValue(onlineMarketDataResultList)

            // Crypto is 24h.
            // Coingecko free API has a rate limit of 50 calls/minute
            // Query not faster than every 10s
            // max delay = 60s as every minute quota is reset
            val delayInSeconds = min(60, max(10, 60 * onlineMarketDataResultList.size / 50))

            return MarketDataResult(
                marketState = MarketState.REGULAR,
                delayInMs = delayInSeconds * 1000L,
                msg = ""
            )
        }

        coingeckoMarketData.postValue(emptyList())
        return MarketDataResult(marketState = MarketState.UNKNOWN, delayInMs = -1, msg = "")
    }

    private suspend fun getCoinpaprikaStockData(symbols: List<StockSymbol>): MarketDataResult {

        val api: CoinpaprikaApiMarketData? = coinpaprikaApi()

        if (api != null) {

            val result = queryCoinpaprikaStockData(api, symbols)

            // Get all results.
            val onlineMarketDataResultList = result.first
            val errorMsg = result.second

            if (onlineMarketDataResultList.isEmpty()) {
                // no _data.value because this is a background thread
                coinpaprikaMarketData.postValue(onlineMarketDataResultList)
                return MarketDataResult(
                    marketState = MarketState.QUOTA_EXCEEDED,
                    delayInMs = -1,
                    msg = errorMsg
                )
            }

            coinpaprikaMarketData.postValue(onlineMarketDataResultList)

            // Crypto is 24h.
            // Single IP address can send less than 10 requests per second
            val delayInSeconds = 1

            // add 100% to not exceed quota too often
            return MarketDataResult(
                marketState = MarketState.REGULAR,
                delayInMs = 2 * delayInSeconds * 1000L,
                msg = ""
            )
        }

        coingeckoMarketData.postValue(emptyList())
        return MarketDataResult(marketState = MarketState.UNKNOWN, delayInMs = -1, msg = "")
    }

    private suspend fun getGeminiStockData(symbols: List<StockSymbol>): MarketDataResult {

        val api: GeminiApiMarketData? = geminiApi()

        if (api != null) {

            val result = queryGeminiStockData(api, symbols)

            // Get all results.
            val onlineMarketDataResultList = result.first
            val errorMsg = result.second

            if (onlineMarketDataResultList.isEmpty()) {
                // no _data.value because this is a background thread
                geminiMarketData.postValue(onlineMarketDataResultList)
                return MarketDataResult(
                    marketState = MarketState.QUOTA_EXCEEDED,
                    delayInMs = -1,
                    msg = errorMsg
                )
            }

            geminiMarketData.postValue(onlineMarketDataResultList)

            // Crypto is 24h.
            // Single IP address can send 120 requests per minute.
            val delayInSeconds = 1

            return MarketDataResult(
                marketState = MarketState.REGULAR,
                delayInMs = delayInSeconds * 1000L,
                msg = ""
            )
        }

        geminiMarketData.postValue(emptyList())
        return MarketDataResult(marketState = MarketState.UNKNOWN, delayInMs = -1, msg = "")
    }

    // used by populateDatabase()
    suspend fun getStockData2(symbols: List<StockSymbol>): List<OnlineMarketData> {

        val api: YahooApiMarketData = yahooApi() ?: return emptyList()

//    val quoteResponse: YahooResponse? = try {
//      apiCall(
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

        if (symbol.symbol.isNotEmpty()) {
            if (symbol.type == DataProvider.Standard) {
                val api: YahooApiMarketData? = yahooApi()
                if (api != null) {
                    return queryYahooStockData(api, listOf(symbol)).first.firstOrNull()
                }
            } else
                if (symbol.type == DataProvider.Coingecko) {
                    val api: CoingeckoApiMarketData? = coingeckoApi()
                    if (api != null) {
                        return queryCoingeckoStockData(api, listOf(symbol)).first.firstOrNull()
                    }
                }
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
                apiCall(
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

        var errorMsg = ""

        // Get online data.
        val onlineMarketDataResultList: MutableList<OnlineMarketData> = mutableListOf()

        symbols.forEach { stockSymbol ->

            val response: CoingeckoResponse? = try {
                apiCall(
                    call = {
                        updateCounter()
                        api.getStockDataAsync(stockSymbol.symbol.lowercase(Locale.ROOT))
                            .await()
                    }, errorMessage = "Error getting finance data."
                )
            } catch (e: Exception) {
                errorMsg += "StockMarketDataRepository.queryCoingeckoStockData failed, Exception=$e\n"
                Log.d("StockMarketDataRepository.queryCoingeckoStockData failed", "Exception=$e")
                null
            }

            // Add the result.
            if (response != null) {
                onlineMarketDataResultList.add(
                    OnlineMarketData(
                        symbol = stockSymbol.symbol,
                        name1 = response.name,
                        marketPrice = response.market_data.current_price.usd,
                        marketChange = response.market_data.price_change_24h,
                        marketChangePercent = response.market_data.price_change_percentage_24h,
                        coinImageUrl = response.image.small,
                        quoteType = "CRYPTOCURRENCY",
                    )
                )
            }
        }

        return Pair(onlineMarketDataResultList, errorMsg)
    }

    private suspend fun queryCoinpaprikaStockData(
        api: CoinpaprikaApiMarketData,
        symbols: List<StockSymbol>
    ): Pair<List<OnlineMarketData>, String> {

        // Get blockSize symbol data at a time.
        // 10 requests per second.
        val blockSize = 10
        var errorMsg = ""

        // Get online data in blocks.
        val onlineMarketDataResultList: MutableList<OnlineMarketData> = mutableListOf()
        var remainingSymbolsToQuery = symbols

        do {
            // Get the first number of blockSize symbols.
            remainingSymbolsToQuery
                .take(blockSize)
                .map { symbol ->
                    symbol.symbol
                }.forEach { stockSymbol ->

                    val response: CoinpaprikaResponse? = try {
                        apiCall(
                            call = {
                                updateCounter()
                                api.getStockDataAsync(stockSymbol.lowercase(Locale.ROOT))
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
                                symbol = stockSymbol,
                                name1 = response.name,
                                marketPrice = response.quotes.USD.price,
                                marketChange = response.quotes.USD.price * response.quotes.USD.percent_change_24h / 100,
                                marketChangePercent = response.quotes.USD.percent_change_24h,
                                //coinImageUrl = ,
                                quoteType = "CRYPTOCURRENCY",
                            )
                        )
                    }
                }

            // Remove the queried symbols.
            remainingSymbolsToQuery = remainingSymbolsToQuery.drop(blockSize)

            // TODO
            // 1 second delay for the next query.
//            if (remainingSymbolsToQuery.isNotEmpty()) {
//            }

        } while (remainingSymbolsToQuery.isNotEmpty())

        return Pair(onlineMarketDataResultList, errorMsg)
    }

    private suspend fun queryGeminiStockData(
        api: GeminiApiMarketData,
        symbols: List<StockSymbol>
    ): Pair<List<OnlineMarketData>, String> {

        var errorMsg = ""

        // Get online data.
        val onlineMarketDataResultList: MutableList<OnlineMarketData> = mutableListOf()

        val response: GeminiResponse? = try {
            apiCall(
                call = {
                    updateCounter()
                    // Get all symbols in one call using https://api.gemini.com/v1/pricefeed.
                    api.getStockDataAsync("")
                        .await()
                }, errorMessage = "Error getting finance data."
            )
        } catch (e: Exception) {
            errorMsg += "StockMarketDataRepository.queryCoingeckoStockData failed, Exception=$e\n"
            Log.d("StockMarketDataRepository.queryCoingeckoStockData failed", "Exception=$e")
            null
        }

        // Add the result.
        response?.result?.forEach { result ->
            onlineMarketDataResultList.add(
                OnlineMarketData(
                    symbol = result.pair,
                    name1 = result.pair,
                    marketPrice = result.price,
                    marketChange = result.percentChange24h,
                    marketChangePercent = result.price * result.percentChange24h / 100,
                    quoteType = "CRYPTOCURRENCY",
                )
            )
        }

        return Pair(onlineMarketDataResultList, errorMsg)
    }

    class StockRawMarketDataRepository(private val api: () -> YahooApiRawMarketData?) :
        BaseRepository() {

        suspend fun getStockRawData(symbol: String): String {

            val api: YahooApiRawMarketData? = api()

            if (symbol.isNotEmpty() && api != null) {

                val quoteResponse: String? = try {
                    apiCall(
                        call = {
                            updateCounter()
                            api.getStockDataAsync(symbol)
                                .await()
                        },
                        errorMessage = "Error getting finance data."
                    )
                } catch (e: Exception) {
                    Log.d(
                        "StockRawMarketDataRepository.getStockRawData($symbol) failed",
                        "Exception=$e"
                    )
                    null
                }

                return quoteResponse ?: ""
            }

            return ""
        }
    }
}
