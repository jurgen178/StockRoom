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

import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import com.thecloudsite.stockroom.utils.checkUrl
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

// https://android.jlelse.eu/android-networking-in-2019-retrofit-with-kotlins-coroutines-aefe82c4d777

// https://api.coingecko.com/api/v3/coins/list
// https://api.coingecko.com/api/v3/simple/supported_vs_currencies

// https://api.coingecko.com/api/v3/simple/price?ids=cartesi&vs_currencies=usd
// {"cartesi":{"usd":1.24}}

// https://api.coingecko.com/api/v3/coins/cartesi

// https://api.coingecko.com/api/v3/coins/cartesi/market_chart?vs_currency=usd&days=10
// {"prices":[[1619625666845,0.6476699905697568],[1619629346430,0.6281418599606636],[1619632894402,0.6059910967268739],[1619636660847,0.6104612592139378],[1619640036142,0.614744809646869],[1619643737476,0.5964704609349185],[1619647401790,0.6035420239511216],[1619650863357,0.5966409608755056],[1619654439638,0.6023990725132919],[1619658440051,0.5994810706866857],[1619661847749,0.6002388209808079],[1619665320398,0.6024006879798155],[1619668987030,0.6004665467341181],[1619672639041,0.5862480715711115],[1619676206939,0.5951259328829168],[1619679826463,0.5940200145799716],[1619683304361,0.603884694731556],[1619686999431,0.6014750931964677],

// https://api.coingecko.com/api/v3/coins/cartesi/market_chart/range?vs_currency=usd&from=1609625666&to=1619625666

// https://api.coingecko.com/api/v3/coins/cartesi?localization=false&tickers=false&market_data=true&community_data=false&developer_data=false&sparkline=false


// coinpaprika tickers are updated every 2-3min
// https://api.coinpaprika.com/

// https://api.coinpaprika.com/v1/coins
// {"id":"ada-cardano","name":"Cardano","symbol":"ADA","rank":5,"is_new":false,"is_active":true,"type":"coin"},{"id":"doge-dogecoin","name":"Dogecoin","symbol":"DOGE","rank":6,"is_new":false,"is_active":true,"type":"coin"},{"id":"usdt-tether","name":"Tether","symbol":"USDT","rank":7,"is_new":false,"is_active":true,"type":"token"},{"id":"dot-polkadot","name":"Polkadot","symbol":"DOT","rank":8,"is_new":false,"is_active":true,"type":"coin"},

// https://api.coinpaprika.com/v1/tickers
// ,{"id":"ada-cardano","name":"Cardano","symbol":"ADA","rank":4,"circulating_supply":31112484646,"total_supply":45000000000,"max_supply":45000000000,"beta_value":0.994793,"first_data_at":"2017-10-01T00:00:00Z","last_updated":"2021-05-17T04:53:47Z","quotes":{"USD":{"price":2.13071104,"volume_24h":8561645413.6285,"volume_24h_change_24h":-17.2,"market_cap":66291714517,"market_cap_change_24h":-8.91,"percent_change_15m":-0.23,"percent_change_30m":0.61,"percent_change_1h":1.08,"percent_change_6h":-4.41,"percent_change_12h":-4.83,"percent_change_24h":-8.91,"percent_change_7d":20.51,"percent_change_30d":43.97,"percent_change_1y":4064.5,"ath_price":2.46475647,"ath_date":"2021-05-16T07:31:20Z","percent_from_price_ath":-13.55}}},

// https://api.coinpaprika.com/v1/tickers/ada-cardano
// {"id":"ada-cardano","name":"Cardano","symbol":"ADA","rank":4,"circulating_supply":31112484646,"total_supply":45000000000,"max_supply":45000000000,"beta_value":0.994793,"first_data_at":"2017-10-01T00:00:00Z","last_updated":"2021-05-17T04:48:39Z","quotes":{"USD":{"price":2.12002421,"volume_24h":8503083622.9605,"volume_24h_change_24h":-16.85,"market_cap":65959220682,"market_cap_change_24h":-8.4,"percent_change_15m":-0.27,"percent_change_30m":0.45,"percent_change_1h":0.39,"percent_change_6h":-4.21,"percent_change_12h":-5.03,"percent_change_24h":-8.4,"percent_change_7d":20.16,"percent_change_30d":43.97,"percent_change_1y":4064.5,"ath_price":2.46475647,"ath_date":"2021-05-16T07:31:20Z","percent_from_price_ath":-13.99}}}

// https://api.coinpaprika.com/v1/coins/btc-bitcoin/ohlcv/historical?start=2019-01-01&end=2019-01-20
// [{"time_open":"2019-01-01T00:00:00Z","time_close":"2019-01-01T23:59:59Z","open":3743.13383814,"high":3848.768792,"low":3695.32467935,"close":3846.6792974,"volume":3062073034,"market_cap":65338972677},{"time_open":"2019-01-02T00:00:00Z","time_close":"2019-01-02T23:59:59Z","open":3852.19783968,"high":3951.20469616,"low":3811.88806393,"close":3941.99122065,"volume":3627095860,"market_cap":67250129005},

// No percentage change.
// https://docs.cloud.coinbase.com/exchange/reference/exchangerestapi_getproducts
// https://api.exchange.coinbase.com/products
// https://api.pro.coinbase.com/products
// https://api.pro.coinbase.com/products/ADA-USD/ticker
// https://api.pro.coinbase.com/products/ADA-USD/candles?start=2021-07-10T12:00:00&granularity=300


// The API is rate limited by an allowance measured in Cryptowatch Credits, rather than a fixed number of calls per time window.
// https://docs.cryptowat.ch/rest-api/
// https://api.cryptowat.ch/markets/prices
// https://api.cryptowat.ch/markets/coinbase-pro/adausd/summary


// https://docs.kaiko.com/#assets
// https://reference-data-api.kaiko.io/v1/assets


// The limit is set to 60 requests per minute enforced over a 10 minute window.
// Data is updated every 5min.
// https://alternative.me/crypto/api/
// https://api.alternative.me/v2/ticker/?limit=0
// https://api.alternative.me/v2/listings/


// https://api.binance.com/api/v3/ticker/price


//StockApiFactory to create the Yahoo Api
object StockMarketDataApiFactory {
    // https://query1.finance.yahoo.com/v1/test/getcrumb
    // https://github.com/pstadler/ticker.sh/blob/acquire-yahoo-finance-session/ticker.sh

    // https://query2.finance.yahoo.com/v6/finance/quote?symbols=msft
    // https://query1.finance.yahoo.com/v7/finance/quote?format=json&symbols=msft,aapl
    // https://query2.finance.yahoo.com/v7/finance/quote?symbols=msft&crumb=JoH2gz8LJk/

    // v7 erfordert crumb
    private var defaultUrl = "https://query2.finance.yahoo.com/v7/finance/"
    private var url = ""

    //Creating Auth Interceptor to add api_key query in front of all the requests.
    /*
      private val authInterceptor = Interceptor { chain ->
        val newUrl = chain.request()
            .url()
            .newBuilder()
            .build()

        val newRequest = chain.request()
            .newBuilder()
            .url(newUrl)
            .build()

        chain.proceed(newRequest)
      }
    */

    // building http request url
    private fun retrofit(): Retrofit = Retrofit.Builder()
        .client(
            OkHttpClient().newBuilder()
//      .addInterceptor(authInterceptor)
                .build()
        )
        .baseUrl(url)
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    fun update(_url: String) {
        if (url != _url) {
            if (_url.isBlank()) {
                url = ""
                marketDataApi = null
            } else {
                url = checkUrl(_url)
                marketDataApi = try {
                    retrofit().create(YahooApiMarketData::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    init {
        update(defaultUrl)
    }

    var marketDataApi: YahooApiMarketData? = null
}

object YahooCrumbDataApiFactory {
    // https://query1.finance.yahoo.com/v1/test/getcrumb
    // https://github.com/pstadler/ticker.sh/blob/acquire-yahoo-finance-session/ticker.sh

    // https://query2.finance.yahoo.com/v6/finance/quote?symbols=msft
    // https://query1.finance.yahoo.com/v7/finance/quote?format=json&symbols=msft,aapl
    // https://query2.finance.yahoo.com/v7/finance/quote?symbols=msft&crumb=JoH2gz8LJk/

    private var defaultUrl = "https://query1.finance.yahoo.com/v1/"
    private var url = ""

    // building http request url
    private fun retrofit(): Retrofit = Retrofit.Builder()
        .client(
            OkHttpClient().newBuilder()
                .build()
        )
        .baseUrl(url)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    fun update(_url: String) {
        if (url != _url) {
            if (_url.isBlank()) {
                url = ""
                yahooCrumbDataApi = null
            } else {
                url = checkUrl(_url)
                yahooCrumbDataApi = try {
                    retrofit().create(YahooApiCrumbData::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    init {
        update(defaultUrl)
    }

    var yahooCrumbDataApi: YahooApiCrumbData? = null
}

//StockApiFactory to create the Coingecko Api
object StockMarketDataCoingeckoApiFactory {
    // https://api.coingecko.com/api/v3/coins/cartesi/tickers
    private var defaultUrl = "https://api.coingecko.com/api/v3/coins/"
    private var url = ""

    // building http request url
    private fun retrofit(): Retrofit = Retrofit.Builder()
        .client(
            OkHttpClient().newBuilder()
                .build()
        )
        .baseUrl(url)
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    fun update(_url: String) {
        if (url != _url) {
            if (_url.isBlank()) {
                url = ""
                marketDataApi = null
            } else {
                url = checkUrl(_url)
                marketDataApi = try {
                    retrofit().create(CoingeckoApiMarketData::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    init {
        update(defaultUrl)
    }

    var marketDataApi: CoingeckoApiMarketData? = null
}

object StockMarketDataCoinpaprikaApiFactory {
    // https://api.coinpaprika.com/v1/tickers/
    private var defaultUrl = "https://api.coinpaprika.com/v1/tickers/"
    private var url = ""

    // building http request url
    private fun retrofit(): Retrofit = Retrofit.Builder()
        .client(
            OkHttpClient().newBuilder()
                .build()
        )
        .baseUrl(url)
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    fun update(_url: String) {
        if (url != _url) {
            if (_url.isBlank()) {
                url = ""
                marketDataApi = null
            } else {
                url = checkUrl(_url)
                marketDataApi = try {
                    retrofit().create(CoinpaprikaApiMarketData::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    init {
        update(defaultUrl)
    }

    var marketDataApi: CoinpaprikaApiMarketData? = null
}

object StockMarketDataGeminiApiFactory {
    // https://api.gemini.com/v1/pricefeed
    private var defaultUrl = "https://api.gemini.com/v1/"
    private var url = ""

    // building http request url
    private fun retrofit(): Retrofit = Retrofit.Builder()
        .client(
            OkHttpClient().newBuilder()
                .build()
        )
        .baseUrl(url)
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    fun update(_url: String) {
        if (url != _url) {
            if (_url.isBlank()) {
                url = ""
                marketDataApi = null
            } else {
                url = checkUrl(_url)
                marketDataApi = try {
                    retrofit().create(GeminiApiMarketData::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    init {
        update(defaultUrl)
    }

    var marketDataApi: GeminiApiMarketData? = null
}

object StockRawMarketDataApiFactory {
    // https://query2.finance.yahoo.com/v6/finance/quote?symbols=msft
    // https://query1.finance.yahoo.com/v7/finance/quote?format=json&symbols=msft,aapl

    private var defaultUrl = "https://query2.finance.yahoo.com/v7/finance/"
    private var url = ""

    // building http request url
    private fun retrofit(): Retrofit = Retrofit.Builder()
        .client(
            OkHttpClient().newBuilder()
//      .addInterceptor(authInterceptor)
                .build()
        )
        .baseUrl(url)
        .addConverterFactory(ScalarsConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    fun update(_url: String) {
        if (url != _url) {
            if (_url.isBlank()) {
                url = ""
                yahooApi = null
            } else {
                url = checkUrl(_url)
                yahooApi = try {
                    retrofit().create(YahooApiRawMarketData::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    init {
        update(defaultUrl)
    }

    var yahooApi: YahooApiRawMarketData? = null
}

object CoingeckoSymbolsApiFactory {

    // https://api.coingecko.com/api/v3/coins/

    private var defaultUrl = "https://api.coingecko.com/api/v3/coins/"
    private var url = ""

    // building http request url
    private fun retrofit(): Retrofit = Retrofit.Builder()
        .client(
            OkHttpClient().newBuilder()
                .build()
        )
        .baseUrl(url)
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    fun update(_url: String) {
        if (url != _url) {
            if (_url.isBlank()) {
                url = ""
                dataProviderApi = null
            } else {
                url = checkUrl(_url)
                dataProviderApi = try {
                    retrofit().create(DataProviderSymbolsDataCoingecko::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    init {
        update(defaultUrl)
    }

    var dataProviderApi: DataProviderSymbolsDataCoingecko? = null
}

object CoinpaprikaSymbolsApiFactory {

    // https://api.coinpaprika.com/v1/coins/

    private var defaultUrl = "https://api.coinpaprika.com/v1/"
    private var url = ""

    // building http request url
    private fun retrofit(): Retrofit = Retrofit.Builder()
        .client(
            OkHttpClient().newBuilder()
                .build()
        )
        .baseUrl(url)
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    fun update(_url: String) {
        if (url != _url) {
            if (_url.isBlank()) {
                url = ""
                dataProviderApi = null
            } else {
                url = checkUrl(_url)
                dataProviderApi = try {
                    retrofit().create(DataProviderSymbolsDataCoinpaprika::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    init {
        update(defaultUrl)
    }

    var dataProviderApi: DataProviderSymbolsDataCoinpaprika? = null
}

object GeminiSymbolsApiFactory {

    // https://api.gemini.com/v1/symbols

    private var defaultUrl = "https://api.gemini.com/v1/"
    private var url = ""

    // building http request url
    private fun retrofit(): Retrofit = Retrofit.Builder()
        .client(
            OkHttpClient().newBuilder()
                .build()
        )
        .baseUrl(url)
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    fun update(_url: String) {
        if (url != _url) {
            if (_url.isBlank()) {
                url = ""
                dataProviderApi = null
            } else {
                url = checkUrl(_url)
                dataProviderApi = try {
                    retrofit().create(DataProviderSymbolsDataGemini::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    init {
        update(defaultUrl)
    }

    var dataProviderApi: DataProviderSymbolsDataGemini? = null
}

object StockYahooChartDataApiFactory {

    // https://query1.finance.yahoo.com/v7/finance/chart/?symbol=aapl&interval=1d&range=3mo
    // https://query1.finance.yahoo.com/v8/finance/chart/?symbol=aapl&interval=1d&range=3mo

    private var defaultUrl = "https://query2.finance.yahoo.com/v8/finance/"
    private var url = ""

    //Creating Auth Interceptor to add api_key query in front of all the requests.
/*
  private val authInterceptor = Interceptor { chain ->
    val newUrl = chain.request()
        .url()
        .newBuilder()
        .build()

    val newRequest = chain.request()
        .newBuilder()
        .url(newUrl)
        .build()

    chain.proceed(newRequest)
  }
*/

    // building http request url
    private fun retrofit(): Retrofit = Retrofit.Builder()
        .client(
            OkHttpClient().newBuilder()
//      .addInterceptor(authInterceptor)
                .build()
        )
        .baseUrl(url)
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    fun update(_url: String) {
        if (url != _url) {
            if (_url.isBlank()) {
                url = ""
                chartDataApi = null
            } else {
                url = checkUrl(_url)
                chartDataApi = try {
                    retrofit().create(YahooApiChartData::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    init {
        update(defaultUrl)
    }

    var chartDataApi: YahooApiChartData? = null
}

object StockCoingeckoChartDataApiFactory {

    // https://api.coingecko.com/api/v3/coins/cartesi/market_chart?vs_currency=usd&days=1

    private var defaultUrl = "https://api.coingecko.com/api/v3/coins/"
    private var url = ""

    // building http request url
    private fun retrofit(): Retrofit = Retrofit.Builder()
        .client(
            OkHttpClient().newBuilder()
                .build()
        )
        .baseUrl(url)
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    fun update(_url: String) {
        if (url != _url) {
            if (_url.isBlank()) {
                url = ""
                chartDataApi = null
            } else {
                url = checkUrl(_url)
                chartDataApi = try {
                    retrofit().create(CoingeckoApiChartData::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    init {
        update(defaultUrl)
    }

    var chartDataApi: CoingeckoApiChartData? = null
}

object StockCoinpaprikaChartDataApiFactory {

    // https://api.coinpaprika.com/v1/coins/btc-bitcoin/ohlcv/historical?start=2019-01-01&end=2019-01-20

    private var defaultUrl = "https://api.coinpaprika.com/v1/coins/"
    private var url = ""

    // building http request url
    private fun retrofit(): Retrofit = Retrofit.Builder()
        .client(
            OkHttpClient().newBuilder()
                .build()
        )
        .baseUrl(url)
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    fun update(_url: String) {
        if (url != _url) {
            if (_url.isBlank()) {
                url = ""
                chartDataApi = null
            } else {
                url = checkUrl(_url)
                chartDataApi = try {
                    retrofit().create(CoinpaprikaApiChartData::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    init {
        update(defaultUrl)
    }

    var chartDataApi: CoinpaprikaApiChartData? = null
}

object StockGeminiChartDataApiFactory {

    // https://api.gemini.com/v2/candles/btcusd/1day
    // [[1638590400000,52055.9,52644.42,42074.62,49639.7,5200.9713649173],
    // [1638504000000,56380.25,57673.58,51619.3,52055.9,1784.7723694009],
    // [1638417600000,56362.19,57277.92,56000,56380.25,1142.2925018029],

    private var defaultUrl = "https://api.gemini.com/v2/candles/"
    private var url = ""

    // building http request url
    private fun retrofit(): Retrofit = Retrofit.Builder()
        .client(
            OkHttpClient().newBuilder()
                .build()
        )
        .baseUrl(url)
        .addConverterFactory(MoshiConverterFactory.create())
        .addCallAdapterFactory(CoroutineCallAdapterFactory())
        .build()

    fun update(_url: String) {
        if (url != _url) {
            if (_url.isBlank()) {
                url = ""
                chartDataApi = null
            } else {
                url = checkUrl(_url)
                chartDataApi = try {
                    retrofit().create(GeminiApiChartData::class.java)
                } catch (e: Exception) {
                    null
                }
            }
        }
    }

    init {
        update(defaultUrl)
    }

    var chartDataApi: GeminiApiChartData? = null
}

/*
https://query1.finance.yahoo.com/v7/finance/download/TSLA?events=split&interval=1d&period1=1598572800&period2=1600453099

This returns TSLA.csv containing:

Date,Stock Splits
2020-08-31,5:1
You can also set the start date to 0 to get a full history of splits.

https://query1.finance.yahoo.com/v7/finance/download/AAPL?events=split&interval=1d&period1=0&period2=1600453099

This returns AAPL.csv containing:

Date,Stock Splits
2000-06-21,2:1
2005-02-28,2:1
2020-08-31,4:1
1987-06-16,2:1
2014-06-09,7:1
 */