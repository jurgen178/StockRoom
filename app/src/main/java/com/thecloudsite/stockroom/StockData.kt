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

import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.Entry
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// https://www.google.com/finance?q=msft
// http://www.google.com/finance?q=INDEXNASDAQ:.IXIC
// http://www.google.com/finance/getprices?q=.IXIC&x=INDEXNASDAQ&i=120&p=10m&f=d,c,v,o,h,l&df=cpct&auto=1&ts=1307994768643

// https://finance.yahoo.com/quote/MSFT?p=MSFT&.tsrc=fin-srch HTTP/1.1

// https://finance.yahoo.com/quotes/GOOG,MAPP,API,v3,Json,not

// https://api.nyse.com/docs/

/*
{
    "quoteResponse": {
        "result": [{
            "language": "en-US",
            "region": "US",
            "quoteType": "EQUITY",
            "quoteSourceName": "Nasdaq Real Time Price",
            "triggerable": true,
            "currency": "USD",
            "fiftyDayAverageChange": 20.281479,
            "fiftyDayAverageChangePercent": 0.10554281,
            "twoHundredDayAverage": 173.40393,
            "twoHundredDayAverageChange": 39.041077,
            "twoHundredDayAverageChangePercent": 0.22514528,
            "marketCap": 1611063885824,
            "forwardPE": 34.100323,
            "priceToBook": 14.082262,
            "sourceInterval": 15,
            "exchangeDataDelayedBy": 0,
            "tradeable": false,
            "firstTradeDateMilliseconds": 511108200000,
            "priceHint": 2,
            "regularMarketChange": 4.1950073,
            "regularMarketChangePercent": 2.0144093,
            "regularMarketTime": 1594235058,
            "regularMarketPrice": 212.445,
            "regularMarketDayHigh": 212.67,
            "regularMarketDayRange": "208.69 - 212.67",
            "regularMarketDayLow": 208.69,
            "regularMarketVolume": 23759221,
            "regularMarketPreviousClose": 208.25,
            "bid": 211.42,
            "ask": 211.39,
            "bidSize": 12,
            "askSize": 9,
            "fullExchangeName": "NasdaqGS",
            "financialCurrency": "USD",
            "regularMarketOpen": 210.07,
            "averageDailyVolume3Month": 36879369,
            "averageDailyVolume10Day": 31302900,
            "fiftyTwoWeekLowChange": 81.66501,
            "fiftyTwoWeekLowChangePercent": 0.6244457,
            "fiftyTwoWeekRange": "130.78 - 214.67",
            "fiftyTwoWeekHighChange": -2.2249908,
            "fiftyTwoWeekHighChangePercent": -0.010364703,
            "fiftyTwoWeekLow": 130.78,
            "fiftyTwoWeekHigh": 214.67,
            "dividendDate": 1599696000,
            "earningsTimestamp": 1588190786,
            "earningsTimestampStart": 1594911540,
            "earningsTimestampEnd": 1595260800,
            "trailingAnnualDividendRate": 1.99,
            "trailingPE": 35.395702,
            "trailingAnnualDividendYield": 0.009555822,
            "epsTrailingTwelveMonths": 6.002,
            "epsForward": 6.23,
            "sharesOutstanding": 7583439872,
            "bookValue": 15.086,
            "fiftyDayAverage": 192.16353,
            "exchange": "NMS",
            "shortName": "Microsoft Corporation",
            "longName": "Microsoft Corporation",
            "messageBoardId": "finmb_21835",
            "exchangeTimezoneName": "America/New_York",
            "exchangeTimezoneShortName": "EDT",
            "gmtOffSetMilliseconds": -14400000,
            "market": "us_market",
            "esgPopulated": false,
            "marketState": "REGULAR",
            "displayName": "Microsoft",
            "symbol": "MSFT"
        }],
        "error": null
    }
}
*/

/*
https://query2.finance.yahoo.com/v7/finance/quote?formatted=false&symbols=azn

{
    "quoteResponse": {
        "result": [{
            "language": "en-US",
            "region": "US",
            "quoteType": "EQUITY",
            "quoteSourceName": "Delayed Quote",
            "triggerable": true,
            "currency": "USD",
            "firstTradeDateMilliseconds": 737213400000,
            "priceHint": 2,
            "sharesOutstanding": 2624539904,
            "bookValue": 4.1075,
            "fiftyDayAverage": 53.38912,
            "fiftyDayAverageChange": 4.610882,
            "fiftyDayAverageChangePercent": 0.0863637,
            "twoHundredDayAverage": 49.63933,
            "twoHundredDayAverageChange": 8.360668,
            "twoHundredDayAverageChangePercent": 0.1684283,
            "marketCap": 152194891776,
            "forwardPE": 23.293173,
            "priceToBook": 14.120511,
            "sourceInterval": 15,
            "exchangeDataDelayedBy": 0,
            "tradeable": false,
            "marketState": "POSTPOST",
            "exchange": "NYQ",
            "shortName": "Astrazeneca PLC",
            "longName": "AstraZeneca PLC",
            "messageBoardId": "finmb_336774",
            "exchangeTimezoneName": "America/New_York",
            "exchangeTimezoneShortName": "EDT",
            "gmtOffSetMilliseconds": -14400000,
            "market": "us_market",
            "esgPopulated": false,
            "postMarketChangePercent": 3.4310374,
            "postMarketTime": 1594851571,
            "postMarketPrice": 59.99,
            "postMarketChange": 1.9900017,
            "regularMarketChange": 4.0200005,
            "regularMarketChangePercent": 7.447203,
            "regularMarketTime": 1594843201,
            "regularMarketPrice": 58.0,
            "regularMarketDayHigh": 58.84,
            "regularMarketDayRange": "55.8699 - 58.84",
            "regularMarketDayLow": 55.8699,
            "regularMarketVolume": 19106298,
            "regularMarketPreviousClose": 53.98,
            "bid": 59.5,
            "ask": 59.75,
            "bidSize": 12,
            "askSize": 10,
            "fullExchangeName": "NYSE",
            "financialCurrency": "USD",
            "regularMarketOpen": 56.32,
            "averageDailyVolume3Month": 4960208,
            "averageDailyVolume10Day": 5071916,
            "fiftyTwoWeekLowChange": 21.849998,
            "fiftyTwoWeekLowChangePercent": 0.6044259,
            "fiftyTwoWeekRange": "36.15 - 58.84",
            "fiftyTwoWeekHighChange": -0.84000015,
            "fiftyTwoWeekHighChangePercent": -0.014276005,
            "fiftyTwoWeekLow": 36.15,
            "fiftyTwoWeekHigh": 58.84,
            "dividendDate": 1585526400,
            "trailingAnnualDividendRate": 2.8,
            "trailingPE": 100.25929,
            "trailingAnnualDividendYield": 0.05187106,
            "epsTrailingTwelveMonths": 0.5785,
            "epsForward": 2.49,
            "displayName": "AstraZeneca",
            "symbol": "AZN"
        }],
        "error": null
    }
}



https://query2.finance.yahoo.com/v10/finance/quoteSummary/msft?modules=financialData
https://query2.finance.yahoo.com/v10/finance/quoteSummary/msft?modules=calendarEvents

https://query2.finance.yahoo.com/v10/finance/quoteSummary/YOUR_COMPANY_SYMBOL?modules=ANY_PERMITTED_MODULE_SEPPARATED_BY_COMMAS

assetProfile
financialData
defaultKeyStatistics
calendarEvents
incomeStatementHistory
cashflowStatementHistory
balanceSheetHistory

{
    "quoteSummary": {
        "result": [{
            "financialData": {
                "maxAge": 86400,
                "currentPrice": {
                    "raw": 208.9,
                    "fmt": "208.90"
                },
                "targetHighPrice": {
                    "raw": 260.0,
                    "fmt": "260.00"
                },
                "targetLowPrice": {
                    "raw": 169.49,
                    "fmt": "169.49"
                },
                "targetMeanPrice": {
                    "raw": 226.93,
                    "fmt": "226.93"
                },
                "targetMedianPrice": {
                    "raw": 230.0,
                    "fmt": "230.00"
                },
                "recommendationMean": {
                    "raw": 1.7,
                    "fmt": "1.70"
                },
                "recommendationKey": "buy",
                "numberOfAnalystOpinions": {
                    "raw": 31,
                    "fmt": "31",
                    "longFmt": "31"
                },
                "totalCash": {
                    "raw": 136491999232,
                    "fmt": "136.49B",
                    "longFmt": "136,491,999,232"
                },
                "totalCashPerShare": {
                    "raw": 18.036,
                    "fmt": "18.04"
                },
                "ebitda": {
                    "raw": 65258999808,
                    "fmt": "65.26B",
                    "longFmt": "65,258,999,808"
                },
                "totalDebt": {
                    "raw": 82109997056,
                    "fmt": "82.11B",
                    "longFmt": "82,109,997,056"
                },
                "quickRatio": {
                    "raw": 2.33,
                    "fmt": "2.33"
                },
                "currentRatio": {
                    "raw": 2.516,
                    "fmt": "2.52"
                },
                "totalRevenue": {
                    "raw": 143015002112,
                    "fmt": "143.02B",
                    "longFmt": "143,015,002,112"
                },
                "debtToEquity": {
                    "raw": 69.406,
                    "fmt": "69.41"
                },
                "revenuePerShare": {
                    "raw": 18.793,
                    "fmt": "18.79"
                },
                "returnOnAssets": {
                    "raw": 0.11261,
                    "fmt": "11.26%"
                },
                "returnOnEquity": {
                    "raw": 0.4014,
                    "fmt": "40.14%"
                },
                "grossProfits": {
                    "raw": 96937000000,
                    "fmt": "96.94B",
                    "longFmt": "96,937,000,000"
                },
                "freeCashflow": {
                    "raw": 34257999872,
                    "fmt": "34.26B",
                    "longFmt": "34,257,999,872"
                },
                "operatingCashflow": {
                    "raw": 60674998272,
                    "fmt": "60.67B",
                    "longFmt": "60,674,998,272"
                },
                "earningsGrowth": {
                    "raw": -0.142,
                    "fmt": "-14.20%"
                },
                "revenueGrowth": {
                    "raw": 0.128,
                    "fmt": "12.80%"
                },
                "grossMargins": {
                    "raw": 0.67780995,
                    "fmt": "67.78%"
                },
                "ebitdaMargins": {
                    "raw": 0.45631,
                    "fmt": "45.63%"
                },
                "operatingMargins": {
                    "raw": 0.3703,
                    "fmt": "37.03%"
                },
                "profitMargins": {
                    "raw": 0.30962,
                    "fmt": "30.96%"
                },
                "financialCurrency": "USD"
            }
        }],
        "error": null
    }
}

params = {"formatted": "true",
        "crumb": "AKV/cl0TOgz", # works without so not sure of significance
        "lang": "en-US",
        "region": "US",
        "modules": "defaultKeyStatistics,financialData,calendarEvents",
        "corsDomain": "finance.yahoo.com"}

r = requests.get("https://query1.finance.yahoo.com/v10/finance/quoteSummary/GSB", params=params)
data = r.json()[u'quoteSummary']["result"][0]

r = requests.get('https://query2.finance.yahoo.com/v10/finance/quoteSummary/GLW?formatted=true&crumb=8ldhetOu7RJ&lang=en-US&region=US&modules=summaryDetail&corsDomain=finance.yahoo.com')
data = r.json()
financial_data=data['quoteSummary']['result'][0]['summaryDetail']
twoHundredMA_dict = financial_data['twoHundredDayAverage']
print(twoHundredMA_dict['fmt'])

https://query2.finance.yahoo.com/v11/finance/quoteSummary/KO?modules=summaryProfile,financialData,defaultKeyStatistics

https://query2.finance.yahoo.com/v10/finance/quoteSummary/AAPL?formatted=true&crumb=8ldhetOu7RJ&lang=en-US&region=US&modules=defaultKeyStatistics%2CfinancialData%2CcalendarEvents&corsDomain=finance.yahoo.com

http://d.yimg.com/autoc.finance.yahoo.com/autoc?query=amd&region=1&lang=e

https://query2.finance.yahoo.com/v7/finance/options/amd?date=1487289600

Here's the base call. The {} is for either the number 1 or 2 as both work: query{}.finance.yahoo.com/v7/finance/
Now, as @Yago notes, you can append chart/ and you can also append options/

 */

fun getName(onlineMarketData: OnlineMarketData): String {
  return if (onlineMarketData.name1.isNotEmpty()) {
    onlineMarketData.name1
  } else {
    onlineMarketData.name2
  }
}

// Data Model
@JsonClass(generateAdapter = true)
data class OnlineMarketData(
  val symbol: String,
  @field:Json(
      name = "longName"
  ) val name1: String = "",  // displayName is not available for "GlaxoSmithKline plc", but longName

  @field:Json(
      name = "shortName"
  ) val name2: String = "",  // ^GSPC only has shortName

    // Market values will be set with the post market values when the aftermarket option is set.
  @field:Json(
      name = "regularMarketPrice"
  ) var marketPrice: Double = 0.0,
  @field:Json(
      name = "regularMarketChange"
  ) var marketChange: Double = 0.0,
  @field:Json(
      name = "regularMarketChangePercent"
  ) var marketChangePercent: Double = 0.0,

    // Ignore
  @Transient
  var postMarketData: Boolean = false,

  var dividendDate: Long = 0,
  @field:Json(
      name = "trailingAnnualDividendRate"
  ) var annualDividendRate: Double = 0.0,
  @field:Json(
      name = "trailingAnnualDividendYield"
  ) var annualDividendYield: Double = 0.0,

  var marketState: String = "", // "marketState":"PREPRE" or "marketState":"PRE" or "marketState": "REGULAR" or "marketState":"POST" or "marketState":"POSTPOST" or "marketState":"CLOSED"
  var preMarketChangePercent: Double = 0.0,
  var preMarketPrice: Double = 0.0,
  var preMarketChange: Double = 0.0,

  var postMarketChangePercent: Double = 0.0,
  var postMarketPrice: Double = 0.0,
  var postMarketChange: Double = 0.0,

  var region: String = "",
  var language: String = "",
  var fullExchangeName: String = "",
  var messageBoardId: String = "",
  var financialCurrency: String = "",

  var quoteSourceName: String = "",
  var sharesOutstanding: Long = Long.MIN_VALUE,
  var fiftyDayAverage: Double = Double.NaN,
  var twoHundredDayAverage: Double = Double.NaN,
  var fiftyTwoWeekRange: String = "",
  var marketCap: Long = Long.MIN_VALUE,
  var regularMarketDayRange: String = "",
  var regularMarketVolume: Long = Long.MIN_VALUE,
  var regularMarketPreviousClose: Double = Double.NaN,
  var regularMarketOpen: Double = Double.NaN,

  var epsTrailingTwelveMonths: Double = Double.NaN,
  var epsCurrentYear: Double = Double.NaN,
  var epsForward: Double = Double.NaN,

  var trailingPE: Double = Double.NaN,
  var priceEpsCurrentYear: Double = Double.NaN,
  var forwardPE: Double = Double.NaN,

  var bookValue: Double = Double.NaN,
  var priceToBook: Double = Double.NaN
)

enum class MarketState(val value: String) {
  PRE("PRE"),
  PREPRE("PREPRE"),
  REGULAR("REGULAR"),
  POST("POST"),
  POSTPOST("POSTPOST"),
  CLOSED("CLOSED"),
  NO_NETWORK("NO_NETWORK"),
  NO_SYMBOL("NO_SYMBOL"),
  UNKNOWN("")
}

fun <K> Enum.Companion.toString(marketState: K): String {
  return when (marketState) {
    MarketState.REGULAR -> {
      "regular market"
    }
    MarketState.PRE, MarketState.PREPRE -> {
      "pre market"
    }
    MarketState.POST, MarketState.POSTPOST -> {
      "post market"
    }
    MarketState.CLOSED -> {
      "market closed"
    }
    MarketState.NO_NETWORK, MarketState.UNKNOWN -> {
      "network not available"
    }
    MarketState.NO_SYMBOL -> {
      "no symbol"
    }
    else -> ""
  }
}

//Enum.toString(DEF_TYPE("default"))

/*
"preMarketChange": 0.23999023,
"preMarketChangePercent": 0.11589812,
"preMarketTime": 1594730861,
"preMarketPrice": 207.31
 */


// Data Model for the Response returned
@JsonClass(generateAdapter = true)
data class QuoteResponse(
  val result: List<OnlineMarketData>
)

@JsonClass(generateAdapter = true)
data class YahooResponse(
  var quoteResponse: QuoteResponse? = null
)

// https://api.iextrading.com/1.0/ref-data/symbols

//A retrofit Network Interface for the Api
interface YahooApiMarketData {
  // https://query2.finance.yahoo.com/v6/finance/quote?symbols=msft
  // https://query1.finance.yahoo.com/v7/finance/quote?format=json&symbols=msft,aapl
  @GET("quote?format=json")
  fun getStockDataAsync(
    @Query(
        value = "symbols"
    ) symbols: String
  ): Deferred<Response<YahooResponse>>
}

interface YahooApiRawMarketData {
  @GET("quote?format=json")
  fun getStockDataAsync(
    @Query(
        value = "symbols"
    ) symbols: String
  ): Deferred<Response<String>>
}

interface YahooApiChartData {
  // https://query1.finance.yahoo.com/v7/finance/chart/?symbol=aapl&interval=1d&range=3mo
  // https://query1.finance.yahoo.com/v8/finance/chart/?symbol=aapl&interval=1d&range=3mo
  // Valid intervals: [1m, 2m, 5m, 15m, 30m, 60m, 90m, 1h, 1d, 5d, 1wk, 1mo, 3mo]
  // Valid ranges: ["1d","5d","1mo","3mo","6mo","1y","2y","5y","ytd","max"]
  @GET("chart/")
  fun getYahooChartDataAsync(
    @Query(
        value = "symbol"
    ) symbol: String,
    @Query(
        value = "interval"
    ) interval: String,
    @Query(
        value = "range"
    ) range: String
  ): Deferred<Response<YahooChartData>>
}

@JsonClass(generateAdapter = true)
data class YahooChartData(
  var chart: YahooChartResult? = null
)

@JsonClass(generateAdapter = true)
data class YahooChartResult(
  var result: List<YahooChartDataEntry> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YahooChartDataEntry(
  var meta: YahooChartMeta? = null,
  var timestamp: List<Int> = listOf(),
  var indicators: YahooChartIndicators? = null
)

@JsonClass(generateAdapter = true)
data class YahooChartMeta(
  var gmtoffset: Int = 0
)

@JsonClass(generateAdapter = true)
data class YahooChartIndicators(
  var quote: List<YahooChartQuoteEntries> = emptyList()
)

@JsonClass(generateAdapter = true)
data class YahooChartQuoteEntries(
  var high: MutableList<Double> = mutableListOf(),
  var low: MutableList<Double> = mutableListOf(),
  var open: MutableList<Double> = mutableListOf(),
  var close: MutableList<Double> = mutableListOf()
//  var volume: MutableList<Double> = mutableListOf()
)

data class StockChartData(
  var symbol: String,
  var stockDataEntries: List<StockDataEntry>?
)

class StockDataEntry(
  var dateTimePoint: Long,
  x: Double,
  high: Double,
  low: Double,
  open: Double,
  close: Double
) {
  var candleEntry: CandleEntry =
    CandleEntry(x.toFloat(), high.toFloat(), low.toFloat(), open.toFloat(), close.toFloat())
}

class DataPoint(
  x: Float,
  y: Float
) : Entry(x, y), Comparable<DataPoint> {

  override fun compareTo(other: DataPoint): Int = x.compareTo(other.x)
}