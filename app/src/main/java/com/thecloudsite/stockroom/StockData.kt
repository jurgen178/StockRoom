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
 */

// Data Model
@JsonClass(generateAdapter = true)
data class OnlineMarketData(
  val symbol: String,
  @field:Json(
      name = "longName"
  ) val name: String = "",  // displayName is not available for "GlaxoSmithKline plc", but longName

    // Market values will be set with the post market values when the aftermarket option is set.
  @field:Json(
      name = "regularMarketPrice"
  ) var marketPrice: Float = 0f,
  @field:Json(
      name = "regularMarketChange"
  ) var marketChange: Float = 0f,
  @field:Json(
      name = "regularMarketChangePercent"
  ) var marketChangePercent: Float = 0f,

  var dividendDate: Long = 0,
  @field:Json(
      name = "trailingAnnualDividendRate"
  ) var annualDividendRate: Float = 0f,
  @field:Json(
      name = "trailingAnnualDividendYield"
  ) var annualDividendYield: Float = 0f,

  var marketState: String = "", // "marketState":"PREPRE" or "marketState":"PRE" or "marketState": "REGULAR" or "marketState":"POST" or "marketState":"POSTPOST" or "marketState":"CLOSED"
  var preMarketChangePercent: Float = 0f,
  var preMarketPrice: Float = 0f,
  var preMarketChange: Float = 0f,

  var postMarketChangePercent: Float = 0f,
  var postMarketPrice: Float = 0f,
  var postMarketChange: Float = 0f,

  var region: String = "",
  var language: String = "",
  var fullExchangeName: String = "",
  var messageBoardId: String = "",
  var financialCurrency: String = "",
  var sharesOutstanding: Long = 0L,
  var fiftyDayAverage: Float = 0f,
  var fiftyTwoWeekRange: String = "",
  var marketCap: Long = 0L,
  var regularMarketDayRange: String = "",
  var regularMarketVolume: Long = 0L,
  var regularMarketPreviousClose: Float = 0f,
  var regularMarketOpen: Float = 0f,
  var forwardPE: Float = 0f,
  var epsTrailingTwelveMonths: Float = 0f,
  var epsForward: Float = 0f
)

enum class MarketState(val value: String) {
  PRE("PRE"),
  PREPRE("PREPRE"),
  REGULAR("REGULAR"),
  POST("POST"),
  POSTPOST("POSTPOST"),
  CLOSED("CLOSED"),
  NO_NETWORK("NO_NETWORK"),
  UNKNOWN("")
}

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
  // https://query1.finance.yahoo.com/v7/finance/quote?format=json&symbols=msft,aapl
  @GET("quote?format=json")
  fun getStockDataAsync(
    @Query(
        value = "symbols"
    ) symbols: String
  ): Deferred<Response<YahooResponse>>
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
  var high: MutableList<Float> = mutableListOf(),
  var low: MutableList<Float> = mutableListOf(),
  var open: MutableList<Float> = mutableListOf(),
  var close: MutableList<Float> = mutableListOf()
//  var volume: MutableList<Float> = mutableListOf()
)

class StockDataEntry(
  var dateTimePoint: Long,
  x: Float,
  high: Float,
  low: Float,
  open: Float,
  close: Float
) {
  var candleEntry: CandleEntry = CandleEntry(x, high, low, open, close)
}

class DataPoint(
  x: Float,
  y: Float
) : Entry(x, y), Comparable<DataPoint> {

  override fun compareTo(other: DataPoint): Int = x.compareTo(other.x)
}