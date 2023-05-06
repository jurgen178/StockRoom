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

import android.content.Context
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.Entry
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.coroutines.Deferred
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Path
import retrofit2.http.Query

// https://www.google.com/finance?q=msft
// http://www.google.com/finance?q=INDEXNASDAQ:.IXIC
// http://www.google.com/finance/getprices?q=.IXIC&x=INDEXNASDAQ&i=120&p=10m&f=d,c,v,o,h,l&df=cpct&auto=1&ts=1307994768643

// https://finance.yahoo.com/quote/MSFT?p=MSFT&.tsrc=fin-srch HTTP/1.1

// https://finance.yahoo.com/quotes/GOOG,MAPP,API,v3,Json,not

// https://api.nyse.com/docs/

// query stock names
// https://autoc.finance.yahoo.com/autoc?query=a&region=US&lang=en-US&guccounter=1

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
    return (if (onlineMarketData.name1.isNotEmpty()) {
        onlineMarketData.name1
    } else {
        onlineMarketData.name2
    })
        .replace("&amp;", "&")
        .replace("&quot;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
}

const val currencyScale = 0.8f
fun getCurrency(onlineMarketData: OnlineMarketData): String {

    if (onlineMarketData.quoteType == "EQUITY"
        || onlineMarketData.quoteType == "ETF"
        || onlineMarketData.quoteType == "OPTION"
        || onlineMarketData.quoteType == "MUTUALFUND"
        || onlineMarketData.quoteType == "CURRENCY"
        || onlineMarketData.quoteType == "CRYPTOCURRENCY"
    ) {
        val currency = onlineMarketData.currency
        if (currency.isNotEmpty()) {
            return " $currency"
        }
    }

    return ""
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

    var market: String = "",
    var fullExchangeName: String = "",
    var currency: String = "",
    var financialCurrency: String = "",
    var coinImageUrl: String = "",

    var quoteType: String = "",
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
    QUOTA_EXCEEDED("QUOTA_EXCEEDED"),
    UNKNOWN("")
}

fun getMarketText(
    context: Context,
    marketStateStr: String
): String {
    return when (marketStateStr) {
        MarketState.PRE.value -> {
            context.getString(R.string.marketStatePRE)
        }

        MarketState.PREPRE.value -> {
            context.getString(R.string.marketStatePREPRE)
        }

        MarketState.REGULAR.value -> {
            context.getString(R.string.marketStateREGULAR)
        }

        MarketState.POST.value -> {
            context.getString(R.string.marketStatePOST)
        }

        MarketState.POSTPOST.value -> {
            context.getString(R.string.marketStatePOSTPOST)
        }

        MarketState.CLOSED.value -> {
            context.getString(R.string.marketStateCLOSED)
        }

        else -> {
            marketStateStr
        }
    }
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

        MarketState.NO_NETWORK -> {
            "network not available"
        }

        MarketState.UNKNOWN -> {
            "unknown"
        }

        MarketState.QUOTA_EXCEEDED -> {
            "Quota exceeded"
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
    // https://query1.finance.yahoo.com/v1/test/getcrumb
    // https://github.com/pstadler/ticker.sh/blob/acquire-yahoo-finance-session/ticker.sh

    // https://query2.finance.yahoo.com/v6/finance/quote?symbols=msft
    // https://query1.finance.yahoo.com/v7/finance/quote?format=json&symbols=msft,aapl
    // https://query2.finance.yahoo.com/v7/finance/quote?symbols=msft&crumb=JoH2gz8LJk/
    @GET("quote?format=json")
    fun getStockDataAsync(
        @Query(
            value = "symbols"
        ) symbols: String,
        @Query(
            value = "crumb"
        ) crumb: String
    ): Deferred<Response<YahooResponse>>
}

@JsonClass(generateAdapter = true)
data class CoingeckoImage(
    var small: String,
)

@JsonClass(generateAdapter = true)
data class CoingeckoDescription(
    var en: String,
)

@JsonClass(generateAdapter = true)
data class CoingeckoMarketCap(
    var usd: Long,
)

@JsonClass(generateAdapter = true)
data class CoingeckoCurrentPrice(
    var usd: Double,
)

@JsonClass(generateAdapter = true)
data class CoingeckoMarketData(
    var current_price: CoingeckoCurrentPrice,
    var price_change_24h: Double,
    var price_change_percentage_24h: Double,
    var market_cap: CoingeckoMarketCap,
)

@JsonClass(generateAdapter = true)
data class CoingeckoResponse(
    val name: String,
    val description: CoingeckoDescription,
    val image: CoingeckoImage,
    val market_data: CoingeckoMarketData,
)
/*
{
{
  "id": "cartesi",
  "symbol": "ctsi",
  "name": "Cartesi",
  "asset_platform_id": "ethereum",
  "platforms": {
    "ethereum": "0x491604c0fdf08347dd1fa4ee062a822a5dd06b5d",
    "polygon-pos": "0x2727ab1c2d22170abc9b595177b2d5c6e1ab7b7b"
  },
  "block_time_in_minutes": 0,
  "hashing_algorithm": null,
  "categories": [
    "Infrastructure"
  ],
  "public_notice": null,
  "additional_notices": [],
  "description": {
    "en": "What is Cartesi?\r\nEase of Adoption: Developers can work in a familiar environment with no artificial limitations and with access to all their favorite tools;\r\n\r\nPortability: Cartesi aims to make DApps portable across the most important public blockchains that support smart contracts;\r\n\r\nPrivacy: DApp states can be kept private among application participants.\r\n\r\nCTSI Overview and Use Cases\r\n\r\nCTSI is a utility token that works as a crypto-fuel for Noether. \r\nStakers receive CTSI rewards by staking their tokens and participating in the network. \r\nNode runners are selected randomly according to a PoS system and gain the right to create the next block. \r\nUsers of the network pay CTSI fees to insert data on the side-chain. \r\nCTSI will also be used for DApps to outsource the execution of verifiable and enforceable computation to entities running Descartes nodes. \r\n"
  },
  "links": {
    "homepage": [
      "https://cartesi.io/",
      "",
      ""
    ],
    "blockchain_site": [
      "https://etherscan.io/token/0x491604c0fdf08347dd1fa4ee062a822a5dd06b5d",
      "https://ethplorer.io/address/0x491604c0fdf08347dd1fa4ee062a822a5dd06b5d",
      "https://explorer.cartesi.io",
      "https://explorer-mainnet.maticvigil.com/address/0x2727Ab1c2D22170ABc9b595177B2D5C6E1Ab7B7B/transactions",
      ""
    ],
    "official_forum_url": [
      "",
      "",
      ""
    ],
    "chat_url": [
      "https://discordapp.com/invite/Pt2NrnS",
      "https://www.linkedin.com/company/cartesiproject/",
      ""
    ],
    "announcement_url": [
      "https://medium.com/cartesi",
      ""
    ],
    "twitter_screen_name": "cartesiproject",
    "facebook_username": "cartesiproject",
    "bitcointalk_thread_identifier": 5211981,
    "telegram_channel_identifier": "cartesiproject",
    "subreddit_url": "https://www.reddit.com/r/cartesi/",
    "repos_url": {
      "github": [
        "https://github.com/cartesi/machine-emulator",
        "https://github.com/cartesi/arbitration-dlib",
        "https://github.com/cartesi/machine-solidity-step"
      ],
      "bitbucket": []
    }
  },
  "image": {
    "thumb": "https://assets.coingecko.com/coins/images/11038/thumb/cartesi.png?1592288021",
    "small": "https://assets.coingecko.com/coins/images/11038/small/cartesi.png?1592288021",
    "large": "https://assets.coingecko.com/coins/images/11038/large/cartesi.png?1592288021"
  },
  "country_origin": "SG",
  "genesis_date": null,
  "contract_address": "0x491604c0fdf08347dd1fa4ee062a822a5dd06b5d",
  "sentiment_votes_up_percentage": 91.11,
  "sentiment_votes_down_percentage": 8.89,
  "market_cap_rank": 154,
  "coingecko_rank": 146,
  "coingecko_score": 41.984,
  "developer_score": 39.727,
  "community_score": 31.926,
  "liquidity_score": 55.088,
  "public_interest_score": 0,
  "market_data": {
    "current_price": {
      "aed": 5.87,
      "ars": 149.73,


    "price_change_24h": 0.498375,
    "price_change_percentage_24h": 46.52526,

 */

interface CoingeckoApiMarketData {
    // https://api.coingecko.com/api/v3/coins/cartesi?localization=false&tickers=false&market_data=true&community_data=false&developer_data=false&sparkline=false
    @GET("{symbol}?localization=false&tickers=false&market_data=true&community_data=false&developer_data=false&sparkline=false")
    fun getStockDataAsync(
        @Path(
            value = "symbol"
        ) symbol: String
    ): Deferred<Response<CoingeckoResponse>>
}


@JsonClass(generateAdapter = true)
data class CoinpaprikaMarketData(
    var price: Double,
    var percent_change_24h: Double,
    var market_cap: Long,
)

@JsonClass(generateAdapter = true)
data class CoinpaprikaMarket(
    var USD: CoinpaprikaMarketData,
)

@JsonClass(generateAdapter = true)
data class CoinpaprikaResponse(
    val name: String,
    val quotes: CoinpaprikaMarket,
)

interface CoinpaprikaApiMarketData {
    // https://api.coinpaprika.com/v1/tickers/ada-cardano
    @GET("{symbol}")
    fun getStockDataAsync(
        @Path(
            value = "symbol"
        ) symbol: String
    ): Deferred<Response<CoinpaprikaResponse>>
}
/*
{"id":"ada-cardano","name":"Cardano","symbol":"ADA","rank":5,"circulating_supply":31946328269,
"total_supply":32704886184,"max_supply":45000000000,"beta_value":1.00354,
"first_data_at":"2017-10-01T00:00:00Z","last_updated":"2021-10-16T17:15:44Z",
"quotes":{"USD":{"price":2.1919832006499,"volume_24h":2351858079.2294,
 "volume_24h_change_24h":-18.5,"market_cap":70025814888,
  "market_cap_change_24h":-1.54,"percent_change_15m":-0.54,
  "percent_change_30m":-0.63,"percent_change_1h":-0.73,
  "percent_change_6h":-1.75,"percent_change_12h":-1.02,
  "percent_change_24h":-1.54,"percent_change_7d":-3.9,
  "percent_change_30d":-10.03,"percent_change_1y":2032.09,
  "ath_price":3.0950278974839,"ath_date":"2021-09-02T06:01:10Z",
  "percent_from_price_ath":-29.18}}}
*/



@JsonClass(generateAdapter = true)
data class GeminiMarketData(
    val pair: String,
    var price: Double,
    var percentChange24h: Double,
)

interface GeminiApiMarketData {
    // https://api.gemini.com/v1/pricefeed
    @GET("pricefeed")
    fun getStockDataAsync(): Deferred<Response<List<GeminiMarketData>>>
}
/*
https://api.gemini.com/v1/pricefeed
[{"pair":"ENJUSD","price":"3.1012","percentChange24h":"-0.0920"},{"pair":"PAXGUSD","price":"1786.18","percentChange24h":"0.0105"},{"pair":"DOGEBTC","price":"0.00000373","percentChange24h":"0.0000"},{"pair":"COMPUSD","price":"256.1","percentChange24h":"-0.0563"},{"pair":"NMRUSD","price":"40.954","percentChange24h":"0.0075"},{"pair":"SNXUSD","price":"6.9281","percentChange24h":"-0.0503"},{"pair":"MANAUSD","price":"3.93955","percentChange24h":"-0.1048"},{"pair":"LINKBTC","price":"0.00043418","percentChange24h":"-0.0043"},
*/

interface YahooApiRawMarketData {
    @GET("quote?format=json")
    fun getStockDataAsync(
        @Query(
            value = "symbols"
        ) symbols: String,
        @Query(
            value = "crumb"
        ) crumb: String
    ): Deferred<Response<String>>
}
// https://query1.finance.yahoo.com/v7/finance/quote?format=json&symbols=msft,aapl

interface YahooApiCrumbData {
    // https://query1.finance.yahoo.com/v1/test/getcrumb
    // https://github.com/pstadler/ticker.sh/blob/acquire-yahoo-finance-session/ticker.sh

    // https://query2.finance.yahoo.com/v6/finance/quote?symbols=msft
    // https://query1.finance.yahoo.com/v7/finance/quote?format=json&symbols=msft,aapl
    // https://query2.finance.yahoo.com/v7/finance/quote?symbols=msft&crumb=JoH2gz8LJk/

    //@Headers("Content-Type:application/json; charset=UTF-8")
    //@Headers("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
    @GET("getcrumb")
    fun getCrumbDataAsync(): Deferred<Response<String>>
}

interface YahooApiFinancePageData {
    // https://finance.yahoo.com

    //@Headers("Content-Type:application/json; charset=UTF-8")
    //@Headers("Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
    @GET(" ")
    fun getWebDataAsync(): Deferred<Response<String>>
}

interface YahooCookieApiService {
    // https://android-kotlin-fun-mars-server.appspot.com/photos
    @GET(" ")
    suspend fun getCookie(): String
}


/*
preflight () {
    curl --silent --output /dev/null --cookie-jar "$COOKIE_FILE" "https://finance.yahoo.com" \
    -H "Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,* /*;q=0.8"
    curl --silent -b "$COOKIE_FILE" "https://query1.finance.yahoo.com/v1/test/getcrumb" \
    > "$CRUMB_FILE"
}

-b name=data
--cookie name=data	Send the data to the HTTP server as a cookie. It is
supposedly the data previously received from the server in a "Set-Cookie:" line.
The data should be in the format "NAME1=VALUE1; NAME2=VALUE2".

-c filename
--cookie-jar file name	Save cookies to file after a completed operation.
Curl writes all cookies previously read from a specified file as well
as all cookies received from remote server(s). If no cookies are known,
no file will be written. To write to stdout, set the file name to a single dash, "-".

.addHeader("Cookie", "cookie-name=cookie-value")

 */

*/


interface MarsApiService {
    // https://android-kotlin-fun-mars-server.appspot.com/photos
    @GET("photos")
    suspend fun getPhotos(): String
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

/*
[{"low":[103.72000122070312,103.31999969482422,103.1500015258789,104.0199966430664,104.1500015258789,105.20999908447266,104.91000366210938,104.2300033569336,105.95999908447266,105.5999984741211,107.06999969482422,105.36000061035156,108.06999969482422,107.80999755859375,105.45999908447266,104.94999694824219,104.06999969482422,105.80000305175781,106.440002
 */
interface CoingeckoApiChartData {
    // https://api.coingecko.com/api/v3/coins/cartesi/market_chart?vs_currency=usd&days=1

    @GET("{symbol}/market_chart")
    fun getCoingeckoChartDataAsync(
        @Path(
            value = "symbol"
        ) symbol: String,
        @Query(
            value = "vs_currency"
        ) vs_currency: String,
        @Query(
            value = "days"
        ) days: String
    ): Deferred<Response<CoingeckoChartData>>
}
/*
{
  "prices": [
    [
      1620524698519,
      1.6048302372840442
    ],
    [
      1620524967406,
      1.5635617095120644
    ],
    [
      1620525298772,
      1.56956596141516
    ],

 */

@JsonClass(generateAdapter = true)
data class CoingeckoChartData(
    var prices: List<MutableList<Double>>? = null
)


interface CoinpaprikaApiChartData {
// https://api.coinpaprika.com/v1/coins/btc-bitcoin/ohlcv/historical?start=2019-01-01&end=2019-01-20
// https://api.coinpaprika.com/v1/coins/btc-bitcoin/ohlcv/historical?start=1610514967&end=1620524967

    @GET("{symbol}/ohlcv/historical")
    fun getCoinpaprikaChartDataAsync(
        @Path(
            value = "symbol"
        ) symbol: String,
        @Query(
            value = "start"
        ) start: String,
        @Query(
            value = "end"
        ) end: String
    ): Deferred<Response<List<CoinpaprikaChartData>>>
}
// [{"time_open":"2019-01-01T00:00:00Z","time_close":"2019-01-01T23:59:59Z","open":3743.13383814,"high":3848.768792,"low":3695.32467935,"close":3846.6792974,"volume":3062073034,"market_cap":65338972677},{"time_open":"2019-01-02T00:00:00Z","time_close":"2019-01-02T23:59:59Z","open":3852.19783968,"high":3951.20469616,"low":3811.88806393,"close":3941.99122065,"volume":3627095860,"market_cap":67250129005},

@JsonClass(generateAdapter = true)
data class CoinpaprikaChartData(
    var time_open: String,
    var open: Double,
    var high: Double,
    var low: Double,
    var close: Double
)


interface GeminiApiChartData {
    // https://api.gemini.com/v2/candles/btcusd/1day
    // [[1638590400000,52055.9,52644.42,42074.62,49639.7,5200.9713649173],
    // [1638504000000,56380.25,57673.58,51619.3,52055.9,1784.7723694009],
    // [1638417600000,56362.19,57277.92,56000,56380.25,1142.2925018029],

    @GET("{symbol}/{timeframe}")
    fun getGeminiChartDataAsync(
        @Path(
            value = "symbol"
        ) symbol: String,
        @Path(
            value = "timeframe"
        ) timeframe: String
    ): Deferred<Response<List<MutableList<Double>>>>
}

data class DataProviderSymbolEntry(
    var id: String,
    var name: String,
)

interface DataProviderSymbolsDataCoingecko {
    // https://api.coingecko.com/api/v3/coins/list
    @GET("list")
    fun getDataProviderSymbolsDataAsync(
    ): Deferred<Response<List<DataProviderCoingeckoSymbolEntry>>>
}
// [{"id":"01coin","symbol":"zoc","name":"01coin"},{"id":"0-5x-long-algorand-token","symbol":"algohalf","name":"0.5X Long Algorand Token"},{"id":

@JsonClass(generateAdapter = true)
data class DataProviderCoingeckoSymbolEntry(
    var id: String,
    var symbol: String,
    var name: String,
)
/*
[
{"id":"01coin","symbol":"zoc","name":"01coin"},
{"id":"0-5x-long-algorand-token","symbol":"algohalf","name":"0.5X Long Algorand Token"},
{"id":"0-5x-long-altcoin-index-token","symbol":"althalf","name":"0.5X Long Altcoin Index Token"},
{"id":"0-5x-long-balancer-token","symbol":"balhalf","name":"0.5X Long Balancer Token"},
{"id":"0-5x-long-bitcoin-cash-token","symbol":"bchhalf","name":"0.5X Long Bitcoin Cash Token"},
{"id":"0-5x-long-bitcoin-sv-token","symbol":"bsvhalf","name":"0.5X Long Bitcoin SV Token"},
 */

interface DataProviderSymbolsDataCoinpaprika {
    // https://api.coinpaprika.com/v1/coins

    @GET("coins")
    fun getDataProviderSymbolsDataAsync(
    ): Deferred<Response<List<DataProviderCoinpaprikaSymbolEntry>>>
}

// [{"id":"btc-bitcoin","name":"Bitcoin","symbol":"BTC","rank":1,"is_new":false,"is_active":true,"type":"coin"},{"id":
@JsonClass(generateAdapter = true)
data class DataProviderCoinpaprikaSymbolEntry(
    var id: String,
    var symbol: String,
    var name: String,
    var is_active: Boolean,
)


interface DataProviderSymbolsDataGemini {
    // https://api.gemini.com/v1/symbols

    @GET("symbols")
    fun getDataProviderSymbolsDataAsync(
    ): Deferred<Response<List<String>>>
}
// ["btcusd","btcgusd","btcdai","btcgbp","btceur","btcsgd","ethbtc","ethusd",


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

class CandleEntryRef(
    x: Float,
    shadowH: Float,
    shadowL: Float,
    open: Float,
    close: Float,
    var refCandleEntry: CandleEntry,
) : CandleEntry(x, shadowH, shadowL, open, close), Comparable<CandleEntryRef> {
    override fun compareTo(other: CandleEntryRef): Int = x.compareTo(other.x)
}

class DataPointRef(
    x: Float,
    y: Float,
    var refY: Float,
) : Entry(x, y), Comparable<DataPointRef> {

    override fun compareTo(other: DataPointRef): Int = x.compareTo(other.x)
}

class DataPoint(
    x: Float,
    y: Float
) : Entry(x, y), Comparable<DataPoint> {

    override fun compareTo(other: DataPoint): Int = x.compareTo(other.x)
}