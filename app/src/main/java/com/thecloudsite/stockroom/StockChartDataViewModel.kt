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
import java.time.ZoneOffset
import java.time.ZonedDateTime

class StockChartDataViewModel(application: Application) : AndroidViewModel(application) {

    private val stockChartDataRepository: StockChartDataRepository =
        StockChartDataRepository(
            { StockYahooChartDataApiFactory.chartDataApi },
            { StockCoingeckoChartDataApiFactory.chartDataApi },
            { StockCoinpaprikaChartDataApiFactory.chartDataApi },
            { StockGeminiChartDataApiFactory.chartDataApi }
        )

    val chartData: LiveData<StockChartData> = stockChartDataRepository.chartData

    private fun getYahooChartData(
        stockSymbol: StockSymbol,
        interval: String,
        range: String
    ) {
        viewModelScope.launch {
            stockChartDataRepository.getYahooChartData(stockSymbol, interval, range)
        }
    }

    private fun getCoingeckoChartData(
        stockSymbol: StockSymbol,
        days: Int
    ) {
        viewModelScope.launch {
            stockChartDataRepository.getCoingeckoChartData(stockSymbol, "usd", days)
        }
    }

    private fun getCoinpaprikaChartData(
        stockSymbol: StockSymbol,
        start: Long,
        end: Long
    ) {
        viewModelScope.launch {
            stockChartDataRepository.getCoinpaprikaChartData(stockSymbol, start, end)
        }
    }

    private fun getGeminiChartData(
        stockSymbol: StockSymbol,
        timeframe: String
    ) {
        viewModelScope.launch {
            stockChartDataRepository.getGeminiChartData(stockSymbol, timeframe)
        }
    }

    fun getChartData(
        stockSymbol: StockSymbol,
        stockViewRange: StockViewRange
    ) {
        when (stockSymbol.type) {
            DataProvider.Coingecko -> {
                when (stockViewRange) {
                    StockViewRange.OneDay -> {
                        getCoingeckoChartData(stockSymbol, 1)
                    }
                    StockViewRange.FiveDays -> {
                        getCoingeckoChartData(stockSymbol, 5)
                    }
                    StockViewRange.OneMonth -> {
                        getCoingeckoChartData(stockSymbol, 30)
                    }
                    StockViewRange.ThreeMonth -> {
                        getCoingeckoChartData(stockSymbol, 90)
                    }
                    StockViewRange.YTD -> {
                        val datetimeYTD =
                            ZonedDateTime.of(
                                ZonedDateTime.now().year,
                                1,
                                1,
                                0,
                                0,
                                0,
                                0,
                                ZoneOffset.systemDefault()
                            )
                        val datetime = ZonedDateTime.now()
                        val secondsYTD = datetimeYTD.toEpochSecond() // in GMT
                        val seconds = datetime.toEpochSecond() // in GMT
                        val daysYTD = (seconds - secondsYTD) / 60 / 60 / 24 + 1
                        getCoingeckoChartData(stockSymbol, daysYTD.toInt())
                    }
                    StockViewRange.OneYear -> {
                        getCoingeckoChartData(stockSymbol, 365)
                    }
                    StockViewRange.FiveYears -> {
                        getCoingeckoChartData(stockSymbol, 5 * 365)
                    }
                    StockViewRange.Max -> {
                        getCoingeckoChartData(stockSymbol, 0)
                    }
                }
            }
            DataProvider.Coinpaprika -> {
                val datetimeNow = ZonedDateTime.now().toEpochSecond()
                val dayInSeconds = 24 * 60 * 60

                when (stockViewRange) {
                    StockViewRange.OneDay -> {
                        getCoinpaprikaChartData(
                            stockSymbol,
                            datetimeNow - dayInSeconds,
                            datetimeNow
                        )
                    }
                    StockViewRange.FiveDays -> {
                        getCoinpaprikaChartData(
                            stockSymbol,
                            datetimeNow - 5 * dayInSeconds,
                            datetimeNow
                        )
                    }
                    StockViewRange.OneMonth -> {
                        getCoinpaprikaChartData(
                            stockSymbol,
                            datetimeNow - 30 * dayInSeconds,
                            datetimeNow
                        )
                    }
                    StockViewRange.ThreeMonth -> {
                        getCoinpaprikaChartData(
                            stockSymbol,
                            datetimeNow - 90 * dayInSeconds,
                            datetimeNow
                        )
                    }
                    StockViewRange.YTD -> {
                        val datetimeYTD =
                            ZonedDateTime.of(
                                ZonedDateTime.now().year,
                                1,
                                1,
                                0,
                                0,
                                0,
                                0,
                                ZoneOffset.systemDefault()
                            )
                        val secondsYTD = datetimeYTD.toEpochSecond() // in GMT
                        getCoinpaprikaChartData(stockSymbol, secondsYTD, datetimeNow)
                    }
                    StockViewRange.OneYear -> {
                        getCoinpaprikaChartData(
                            stockSymbol,
                            datetimeNow - 365 * dayInSeconds,
                            datetimeNow
                        )
                    }
                    StockViewRange.FiveYears -> {
                        getCoinpaprikaChartData(
                            stockSymbol,
                            datetimeNow - 5 * 365 * dayInSeconds,
                            datetimeNow
                        )
                    }
                    StockViewRange.Max -> {
                        getCoinpaprikaChartData(stockSymbol, 0, datetimeNow)
                    }
                }
            }
            DataProvider.Gemini -> {
                when (stockViewRange) {
                    StockViewRange.OneDay -> {
                        getGeminiChartData(stockSymbol, "1m")
                    }
                    StockViewRange.FiveDays -> {
                        getGeminiChartData(stockSymbol, "5m")
                    }
                    StockViewRange.OneMonth -> {
                        getGeminiChartData(stockSymbol, "30m")
                    }
                    StockViewRange.ThreeMonth -> {
                        getGeminiChartData(stockSymbol, "1hr")
                    }
                    StockViewRange.YTD -> {
                        // TODO set the correct timeframe
                        val datetimeYTD =
                            ZonedDateTime.of(
                                ZonedDateTime.now().year,
                                1,
                                1,
                                0,
                                0,
                                0,
                                0,
                                ZoneOffset.systemDefault()
                            )
                        val datetime = ZonedDateTime.now()
                        val secondsYTD = datetimeYTD.toEpochSecond() // in GMT
                        val seconds = datetime.toEpochSecond() // in GMT
                        val daysYTD = (seconds - secondsYTD) / 60 / 60 / 24 + 1
                        getGeminiChartData(stockSymbol, "6hr")
                    }
                    StockViewRange.OneYear -> {
                        getGeminiChartData(stockSymbol, "1day")
                    }
                    StockViewRange.FiveYears -> {
                        getGeminiChartData(stockSymbol, "1day")
                    }
                    StockViewRange.Max -> {
                        getGeminiChartData(stockSymbol, "1day")
                    }
                }
            }
            else -> {
                // Valid intervals: [1m, 2m, 5m, 15m, 30m, 60m, 90m, 1h, 1d, 5d, 1wk, 1mo, 3mo]
                // Valid ranges: ["1d","5d","1mo","3mo","6mo","1y","2y","5y","ytd","max"]
                when (stockViewRange) {
                    StockViewRange.OneDay -> {
                        getYahooChartData(stockSymbol, "5m", "1d")
                    }
                    StockViewRange.FiveDays -> {
                        getYahooChartData(stockSymbol, "15m", "5d")
                    }
                    StockViewRange.OneMonth -> {
                        getYahooChartData(stockSymbol, "90m", "1mo")
                    }
                    StockViewRange.ThreeMonth -> {
                        getYahooChartData(stockSymbol, "1d", "3mo")
                    }
                    StockViewRange.YTD -> {
                        getYahooChartData(stockSymbol, "1d", "ytd")
                    }
                    StockViewRange.OneYear -> {
                        getYahooChartData(stockSymbol, "1d", "1y")
                    }
                    StockViewRange.FiveYears -> {
                        getYahooChartData(stockSymbol, "1d", "5y")
                    }
                    StockViewRange.Max -> {
                        getYahooChartData(stockSymbol, "1d", "max")
                    }
                }
            }
        }
    }
}
