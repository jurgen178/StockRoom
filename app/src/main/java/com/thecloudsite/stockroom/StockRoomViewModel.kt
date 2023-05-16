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
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.text.SpannableString
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.thecloudsite.stockroom.DividendCycle.Quarterly
import com.thecloudsite.stockroom.MainActivity.Companion.onlineDataTimerDelay
import com.thecloudsite.stockroom.StockRoomViewModel.AlertData
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.database.Assets
import com.thecloudsite.stockroom.database.Dividend
import com.thecloudsite.stockroom.database.Dividends
import com.thecloudsite.stockroom.database.Event
import com.thecloudsite.stockroom.database.Events
import com.thecloudsite.stockroom.database.Group
import com.thecloudsite.stockroom.database.StockDBdata
import com.thecloudsite.stockroom.database.StockRoomDatabase
import com.thecloudsite.stockroom.database.StoreData
import com.thecloudsite.stockroom.list.DebugData
import com.thecloudsite.stockroom.utils.getAssets
import com.thecloudsite.stockroom.utils.getGroupsMenuList
import com.thecloudsite.stockroom.utils.isOnline
import com.thecloudsite.stockroom.utils.isValidSymbol
import com.thecloudsite.stockroom.utils.saveTextToFile
import com.thecloudsite.stockroom.utils.validateDouble
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.internal.toImmutableList
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.NumberFormat
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.coroutines.CoroutineContext
import kotlin.math.absoluteValue

data class AssetJson(
    var quantity: Double,
    val price: Double,
    val type: Int?,
    var account: String?,
    var note: String?,
    var date: Long?,
    var sharesPerQuantity: Int?,
    var expirationDate: Long?,
    var premium: Double?,
    var fee: Double?
)

data class EventJson(
    val title: String,
    val datetime: Long,
    val note: String?,
    val type: Int?
)

data class DividendJson(
    var amount: Double,
    val cycle: Int,
    val paydate: Long,
    val exdate: Long?,
    val type: Int?,
    val account: String?,
    val note: String?
)

data class StockItemJson
    (
    var symbol: String,
    val name: String?,
    val portfolio: String,
    val type: Int?,
    val data: String?,
    val groupColor: Int?,
    val groupName: String?,
    val marker: Int?,
    val note: String?,
    var dividendNote: String?,
    val annualDividendRate: Double?,
    val alertAbove: Double?,
    val alertAboveNote: String?,
    val alertBelow: Double?,
    val alertBelowNote: String?,
    var assets: List<AssetJson>?,
    var events: List<EventJson>?,
    var dividends: List<DividendJson>?
)

enum class DataProvider(val value: Int) {
    Standard(0),
    Coingecko(1),
    Coinpaprika(2),
    Gemini(3),
    None(4),
}

fun dataProviderFromInt(
    value: Int
) = DataProvider.values().first { it.value == value }

data class StockSymbol
    (
    val symbol: String = "",
    val type: DataProvider = DataProvider.Standard
)

object SharedHandler {
    val deleteStockHandler = MutableLiveData<String>()
}

val displayedViewsDefaultSet: MutableSet<String> = mutableSetOf(
    "00_StockRoomChartFragment",
    "02_StockRoomListFragment",
    "03_StockRoomTileFragment",
    "09_SummaryGroupFragment"
)

val displayedViewsSet: MutableSet<String> = mutableSetOf(
    "00_StockRoomChartFragment",
    "01_StockRoomOverviewFragment",
    "02_StockRoomListFragment",
    "03_StockRoomTileFragment",
    "04_StockRoomSmallListFragment",
    "05_StockRoomSmallTile1Fragment",
    "06_StockRoomSmallTile2Fragment",
    "07_StockRoomTableFragment",
    "08_StockRoomTreemapFragment",
    "09_SummaryGroupFragment",
    "10_AllNewsFragment",
    "11_TransactionsFragment",
    "12_AllTransactionsFragment",
    "13_GainLossTimelineFragment",
    "14_AllGainLossTimelineFragment",
    "15_AssetTimelineFragment",
    "16_AllAssetTimelineFragment",
    "17_EventTimelineFragment",
    "18_AllEventTimelineFragment",
    "19_DividendTimelineFragment",
    "20_AllDividendTimelineFragment"
)

object SharedRepository {
    val alertsData = MutableLiveData<List<AlertData>>()
    val alerts: LiveData<List<AlertData>>
        get() = alertsData

    val filterMap = MutableLiveData<Filters>(Filters())
    val filterMapLiveData: LiveData<Filters>
        get() = filterMap

    val debugList = ArrayList<DebugData>()
    val debugLiveData = MutableLiveData<List<DebugData>>()
    val debugData: LiveData<List<DebugData>>
        get() = debugLiveData

    var displayedViewsList: MutableList<String> = mutableListOf()
    var postMarket: Boolean = true
    var notifications: Boolean = true

    // Get the DB data first, then enable the online query.
    // There are no symbols from DB to query if online task runs at start and would result in
    // an error with a delay for the next online task.
    var dbDataValid = false

    var selectedSymbol: StockSymbol = StockSymbol()
    var selectedPortfolio = MutableLiveData("")
    val selectedPortfolioLiveData: LiveData<String>
        get() = selectedPortfolio

    var portfolios = MutableLiveData<HashSet<String>>()
    val portfoliosLiveData: LiveData<HashSet<String>>
        get() = portfolios

    var yahooCookieReady = MutableLiveData<Boolean>()
    val yahooCookieReadyLiveData: LiveData<Boolean> = yahooCookieReady

    var yahooCrumb = MutableLiveData<String>()
    val yahooCrumbLiveData: LiveData<String> = yahooCrumb

    var statsCounter = 0
    var statsCounterMax = 0
    var responseCounterStart = 0
    var lastStatsCounters = IntArray(5) { -1 }
}

class StockRoomViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: StockRoomRepository
    private val stockMarketDataRepository: StockMarketDataRepository =
        StockMarketDataRepository(
            { StockMarketDataApiFactory.marketDataApi },
            { StockMarketDataCoingeckoApiFactory.marketDataApi },
            { StockMarketDataCoinpaprikaApiFactory.marketDataApi },
            { StockMarketDataGeminiApiFactory.marketDataApi }
        )

    // Using LiveData and caching returns has several benefits:
    // - We can put an observer on the data (instead of polling for changes) and only update the
    //   the UI when the data actually changes.
    // - Repository is completely separated from the UI through the ViewModel.
    val onlineMarketDataList: MediatorLiveData<List<OnlineMarketData>>
    val allStockDBdata: LiveData<List<StockDBdata>>
    val allAssets: LiveData<List<Assets>>
    val allEvents: LiveData<List<Events>>
    val allDividends: LiveData<List<Dividends>>
    val allAssetTable: LiveData<List<Asset>>
    val allEventTable: LiveData<List<Event>>
    val allDividendTable: LiveData<List<Dividend>>
    val allStoreDataTable: LiveData<List<StoreData>>
    val allGroupTable: LiveData<List<Group>>

    // allStockItems -> allMediatorData -> allData(_data->dataStore) = allAssets + onlineMarketData
    val allStockItems: LiveData<List<StockItem>>
    //val allStockItemsDB: LiveData<List<StockItem>>

    private var filterList: List<IFilterType> = emptyList()
    private var filterMode: FilterModeTypeEnum = FilterModeTypeEnum.AndType

    private var portfolioSymbols: HashSet<StockSymbol> = HashSet()

    private val dataStore: MutableList<StockItem> = mutableListOf()
    private val _dataStore = MutableLiveData<List<StockItem>>()
    private val allData: LiveData<List<StockItem>>
        get() = _dataStore

    private val allMediatorData = MediatorLiveData<List<StockItem>>()

    // all DB entries unfiltered
    private val dataStoreDB: MutableList<StockItem> = mutableListOf()
    private val _dataStoreDB = MutableLiveData<List<StockItem>>()
    private val allDataDB: LiveData<List<StockItem>>
        get() = _dataStoreDB

    private val allMediatorDataDB = MediatorLiveData<List<StockItem>>()

    //private var assetDataValid = false
    //private var eventDataValid = false
    //private var dividendDataValid = false
    //private var onlineDataValid = false

    private var onlineDataStatus: Pair<Long, MarketState> =
        Pair(onlineDataTimerDelay, MarketState.UNKNOWN)
    private var nextUpdate: Long = onlineDataTimerDelay
    private var onlineUpdateTime: Long = onlineDataTimerDelay
    private var isActive = false
    private var onlineNow = false
    private var onlineBefore = false

    // Settings.
    private val sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(application /* Activity context */)

    private var displayedViews: MutableSet<String>? =
        sharedPreferences.getStringSet("displayed_views", displayedViewsDefaultSet)
    private var postMarket: Boolean = sharedPreferences.getBoolean("postmarket", true)
    private var notifications: Boolean = sharedPreferences.getBoolean("notifications", true)

    private val settingSortmode = "SettingSortmode"

    private var sorting = MutableLiveData<SortMode>()
    val sortingLiveData: LiveData<SortMode>
        get() = sorting

    private var sortMode: SortMode
        get() {
            val index = sharedPreferences.getInt(settingSortmode, SortMode.ByName.value)
            return if (index >= 0 && index < SortMode.values().size) {
                SortMode.values()[index]
            } else {
                SortMode.ByName
            }
        }
        set(value) {
            with(sharedPreferences.edit()) {
                putInt(settingSortmode, value.value)
                commit()
            }
        }

//  private val postMarketLiveData = MediatorLiveData<Boolean>()
//  val postMarketData: LiveData<Boolean>
//    get() = Storage.postMarket

    private val parentJob = Job()
    private val coroutineContext: CoroutineContext
        get() = parentJob + Dispatchers.Default
    private val scope = CoroutineScope(coroutineContext)

    private val backgroundListColor = application.getColor(R.color.backgroundListColor)

    init {
        val stockRoomDao = StockRoomDatabase.getDatabase(application, viewModelScope)
            .stockRoomDao()

        repository = StockRoomRepository(stockRoomDao)
        allStockDBdata = repository.allStockDBdata
        allAssets = repository.allAssets
        allEvents = repository.allEvents
        allDividends = repository.allDividends
        allAssetTable = repository.allAssetTable
        allEventTable = repository.allEventTable
        allDividendTable = repository.allDividendTable
        allStoreDataTable = repository.allStoreTable
        allGroupTable = repository.allGroupTable

        onlineMarketDataList = stockMarketDataRepository.onlineMarketDataList
        allStockItems = getMediatorData()
        //allStockItemsDB = getMediatorDataDB()

        // This setting requires the app to be restarted.
        // Initialize the setting only once at start.
        if (SharedRepository.displayedViewsList.isEmpty()) {

            if (displayedViews != null && displayedViews!!.isNotEmpty()) {
                SharedRepository.displayedViewsList =
                    displayedViews?.toMutableList()
                        ?.filter { fragment ->
                            displayedViewsSet.contains(fragment)
                        }
                        ?.sortedBy { fragment ->
                            fragment
                        }
                        ?.toMutableList()!!
            }

            if (SharedRepository.displayedViewsList.isEmpty()) {
                SharedRepository.displayedViewsList.add("02_StockRoomListFragment")
            }

            sharedPreferences.edit()
                .putStringSet("displayed_views", SharedRepository.displayedViewsList.toSet())
                .apply()
        }

        // sharedPreferences.getBoolean("postmarket", true) doesn't work here anymore?
        SharedRepository.postMarket = postMarket
        SharedRepository.notifications = notifications

        SharedRepository.responseCounterStart = responseCounter
    }

    private suspend fun getOnlineData(prevOnlineDataDelay: Long): Pair<Long, MarketState> {
        val stockdataResult = getStockData()
        val marketState = stockdataResult.marketState
        val errorMsg = stockdataResult.msg

        // Set the delay depending on the market state.
        val onlineDataDelay = when {
            stockdataResult.delayInMs > 0 -> {
                stockdataResult.delayInMs
            }

            MainActivity.realtimeOverride -> {
                2 * 1000L
            }

            else -> {
                when (marketState) {
                    MarketState.REGULAR -> {
                        2 * 1000L
                    }

                    MarketState.PRE, MarketState.POST -> {
                        60 * 1000L
                    }

                    MarketState.PREPRE, MarketState.POSTPOST -> {
                        15 * 60 * 1000L
                    }

                    MarketState.CLOSED -> {
                        60 * 60 * 1000L
                    }

                    MarketState.NO_NETWORK -> {
                        // increase delay: 2s, 4s, 8s, 16s, 32s, 1m, 2m, 2m, 2m, ....
                        maxOf(2 * 60 * 1000L, prevOnlineDataDelay * 2)
                        // 10 * 1000L
                    }

                    MarketState.NO_SYMBOL -> {
                        60 * 1000L
                    }

                    MarketState.QUOTA_EXCEEDED -> {
                        60 * 1000L
                    }

                    MarketState.UNKNOWN -> {
                        60 * 1000L
                    }
                }
            }
        }

        if (errorMsg.isEmpty()) {

            val marketStateStr = Enum.toString(marketState)

            val delaystr = when {
                onlineDataDelay >= 60 * 60 * 1000L -> {
                    "${onlineDataDelay / (60 * 60 * 1000L)}h"
                }

                onlineDataDelay >= 60 * 1000L -> {
                    "${onlineDataDelay / (60 * 1000L)}m"
                }

                else -> {
                    "${onlineDataDelay / 1000L}s"
                }
            }

            logDebugAsync("Received online data ($marketStateStr, interval=$delaystr)")
        } else {
            logDebugAsync("Error getting online data: $errorMsg")
        }

        return Pair(onlineDataDelay, marketState)
    }

    // https://developer.android.com/codelabs/basic-android-kotlin-training-getting-data-internet#5
    fun getMarsPhotos() {
        viewModelScope.launch {
            try {
                val listResult = MarsApi.retrofitService.getPhotos()
                val a = listResult
            } catch (e: Exception) {
                val a = e
            }
        }
    }

//    fun getYahooCrumb() = scope.launch {
//        viewModelScope.launch {
//            stockMarketDataRepository.getYahooCookie()
//        }
//        viewModelScope.launch {
//            stockMarketDataRepository.getYahooCrumb()
//        }
//    }

    fun getYahooCookie() {

        yahooCookieJar.reset()

        viewModelScope.launch {
            try {
//https://github.com/premnirmal/StockTicker/commit/2d3a80c625e0d36cdca582fcec47830950170aff
//https://github.com/premnirmal/StockTicker/commit/feb2f74bf02162f4c779e91b5ffee645337bc281

                val cookieResponse = YahooCookieApi.retrofitYahooCookieService.getCookie()

                val html = cookieResponse.body() ?: ""
                val url = cookieResponse.raw().request.url.toString()

                // csrfToken only in html for EU
//              <div class="actions couple">
//                <input type="hidden" name="csrfToken" value="F-yGgtE">
//                <input type="hidden" name="sessionId" value="3_cc-session_8a0f040c-115c-492e-acb8-eccf6a1306b3">
//                <input type="hidden" name="originalDoneUrl" value="https://finance.yahoo.com/?guccounter&#x3D;1">
//                <input type="hidden" name="namespace" value="yahoo">

                val match = Regex("csrfToken\" value=\"(.+)\">").find(html)

                // EU?
                if (!match?.groupValues.isNullOrEmpty()) {
                    val csrfToken = match?.groupValues?.last().toString()
                    val sessionId = url.split("=").last()

                    val requestBody = FormBody.Builder()
                        .add("csrfToken", csrfToken)
                        .add("sessionId", sessionId)
                        .addEncoded("originalDoneUrl", "https://finance.yahoo.com/?guccounter=1")
                        .add("namespace", "yahoo")
                        .add("agree", "agree")
                        .build()

                    val cookieConsent =
                        YahooCookieApi.retrofitYahooCookieService.cookieConsent(url, requestBody)

                    // Continue with the crumb only when the consent was successful.
                    SharedRepository.yahooCookieReady.postValue(cookieConsent.isSuccessful)

                } else {

                    // Non-EU, no consent required. Continue to get the crumb.
                    SharedRepository.yahooCookieReady.postValue(true)

                }
            } catch (e: Exception) {
            }
        }
    }

    fun getYahooCrumb() {
        viewModelScope.launch {
            try {
                val crumb = YahooCrumbApi.retrofitYahooCrumbService.getCrumb()

                SharedRepository.yahooCrumb.postValue(crumb)

            } catch (e: Exception) {
            }
        }
    }

    private fun onlineTask() {
        onlineBefore = onlineNow
        onlineNow = isOnline(getApplication())

        if (onlineBefore && !onlineNow) {
            logDebug("Network is offline.")
        } else
            if (!onlineBefore && onlineNow) {
                logDebug("Network is online.")
                // Schedule now
                isActive = false
                nextUpdate = 0
            }

        if (onlineNow) {
            //stockRoomViewModel.logDebug(
            //    "${onlineDataStatus.first} ${onlineUpdateTime + onlineDataTimerDelay} >= $nextUpdate"
            //)

            // isActive : only one getOnlineData at a time
            // next update depends on the market state
            if (!isActive && onlineUpdateTime + onlineDataTimerDelay >= nextUpdate) {
                logDebug("Schedule to get online data.")
                isActive = true

                scope.launch {
                    onlineDataStatus = getOnlineData(onlineDataStatus.first)
                    nextUpdate = onlineDataStatus.first
                    onlineUpdateTime = 0L
                    isActive = false
                }
            }

            onlineUpdateTime += onlineDataTimerDelay

            SharedRepository.statsCounter++
            //logDebug("responseCounter $responseCounter")
            // runs every 2s
            // 30 * 2s = 1min
            if (SharedRepository.statsCounter >= 30) {
                SharedRepository.statsCounter = 0
                val count = responseCounter - SharedRepository.responseCounterStart
                if (count > SharedRepository.statsCounterMax) {
                    SharedRepository.statsCounterMax = count
                }

                SharedRepository.responseCounterStart = responseCounter

                // sum of initial array [-1, -1, -1, -1, -1] is -5
                val lastCounts = if (SharedRepository.lastStatsCounters.sum() == -5) {
                    ""
                } else {
                    SharedRepository.lastStatsCounters.filter { it >= 0 }
                        .joinToString(
                            prefix = "[",
                            separator = ",",
                            postfix = "]"
                        )
                }
                logDebug(
                    "Internet access count $count/min $lastCounts[${SharedRepository.statsCounterMax}]"
                )
                SharedRepository.lastStatsCounters.forEachIndexed { i, _ ->
                    val reverseIndex = SharedRepository.lastStatsCounters.size - i - 1
                    if (reverseIndex > 0) {
                        SharedRepository.lastStatsCounters[reverseIndex] =
                            SharedRepository.lastStatsCounters[reverseIndex - 1]
                    }
                }

                SharedRepository.lastStatsCounters[0] = count
            }
        }
    }

    fun runOnlineTask() {
        if (SharedRepository.dbDataValid) {
            synchronized(onlineUpdateTime)
            {
                onlineTask()
            }
        }
    }

    fun runOnlineTaskNow(msg: String = "") {
        if (SharedRepository.dbDataValid) {
            synchronized(onlineUpdateTime)
            {
                if (msg.isNotEmpty()) {
                    logDebug(msg)
                }

                // Reset to run now.
                onlineUpdateTime = 0
                nextUpdate = 0
                onlineTask()
            }
        }
    }

    /*
      fun updateOnlineDataManually(msg: String = "") {
        if (isOnline(getApplication())) {
          if (msg.isNotEmpty()) {
            logDebug(msg)
          }

          scope.launch {
            val stockDataResult = getStockData()
            if (stockDataResult.second.isEmpty()) {
              val marketState = stockDataResult.first
              val marketStateStr = Enum.toString(marketState)
              logDebugAsync("Received online data ($marketStateStr)")
            } else {
              logDebugAsync("Error getting online data: ${stockDataResult.second}")
            }
          }
        } else {
          logDebug("Network is offline. No online data.")
        }
      }
     */

    fun updateAll() {
        allMediatorData.value = allData.value?.let { process(it, true) }
//    if (SharedRepository.notifications) {
//      if (allMediatorData.value != null) {
//        processNotifications(allMediatorData.value!!)
//      }
//    }
    }

    private fun getMediatorDataDB(): LiveData<List<StockItem>> {

        val liveDataProperties = allStockDBdata
        val liveDataAssets = allAssets
        val liveDataEvents = allEvents
        val liveDataDividends = allDividends

        allMediatorDataDB.addSource(liveDataProperties) { value ->
            if (value != null) {
                updateStockDataFromDB_DB(value)
                allMediatorDataDB.value = allDataDB.value
            }
        }

        allMediatorDataDB.addSource(liveDataAssets) { value ->
            if (value != null) {
                updateAssetsFromDB_DB(value)
                allMediatorDataDB.value = allDataDB.value
            }
        }

        allMediatorDataDB.addSource(liveDataEvents) { value ->
            if (value != null) {
                updateEventsFromDB_DB(value)
                allMediatorDataDB.value = allDataDB.value
            }
        }

        allMediatorDataDB.addSource(liveDataDividends) { value ->
            if (value != null) {
                updateDividendsFromDB_DB(value)
                allMediatorDataDB.value = allDataDB.value
            }
        }

        return allMediatorDataDB
    }

    private fun getMediatorData(): LiveData<List<StockItem>> {

        val liveDataProperties = allStockDBdata
        val liveDataAssets = allAssets
        val liveDataEvents = allEvents
        val liveDataDividends = allDividends
        val liveDataOnline = onlineMarketDataList

        // reload the DB when portfolio is changed.
        allMediatorData.addSource(SharedRepository.selectedPortfolioLiveData) { value ->
            if (value != null) {
                if (liveDataProperties.value != null) {
                    updateStockDataFromDB(liveDataProperties.value!!)
                }
                if (liveDataAssets.value != null) {
                    updateAssetsFromDB(liveDataAssets.value!!)
                }
                if (liveDataEvents.value != null) {
                    updateEventsFromDB(liveDataEvents.value!!)
                }
                if (liveDataDividends.value != null) {
                    updateDividendsFromDB(liveDataDividends.value!!)
                }
                if (liveDataOnline.value != null) {
                    updateFromOnline(liveDataOnline.value!!)
                }
                allMediatorData.value = allData.value?.let { process(it, true) }
                runOnlineTaskNow()
            }
        }

        allMediatorData.addSource(liveDataProperties) { value ->
            if (value != null) {
                updateStockDataFromDB(value)
                // Symbols are ready for the online task.
                SharedRepository.dbDataValid = true
                //logDebug("runOnlineTaskNow in addSource")
                //if (dataStore.allDataReady) {
                runOnlineTaskNow()
                //}
                //dataValidate()
                allMediatorData.value = allData.value?.let { process(it, true) }
            }
        }

        allMediatorData.addSource(liveDataAssets) { value ->
            if (value != null) {
                updateAssetsFromDB(value)
                //dataValidate()
                allMediatorData.value = allData.value?.let { process(it, false) }
            }
        }

        allMediatorData.addSource(liveDataEvents) { value ->
            if (value != null) {
                updateEventsFromDB(value)
                //dataValidate()
                allMediatorData.value = allData.value?.let { process(it, false) }
            }
        }

        // observe dividends, used for export
        allMediatorData.addSource(liveDataDividends) { value ->
            if (value != null) {
                updateDividendsFromDB(value)
                //dataValidate()
                allMediatorData.value = allData.value?.let { process(it, false) }
            }
        }

        allMediatorData.addSource(liveDataOnline) { value ->
            if (value != null) {
                updateFromOnline(value)
                //dataValidate()
                allMediatorData.value = allData.value?.let { process(it, true) }
            }
        }

        allMediatorData.addSource(SharedRepository.filterMapLiveData) { value ->
            if (value != null) {
                // Set filterlist and update.
                filterList = value.filterList
                filterMode = value.filterMode
                allMediatorData.value = allData.value?.let { process(it, false) }
            }
        }

        return allMediatorData
    }

    /*
      private fun dataValidate() {
        synchronized(dataStore)
        {
          if (!dataStore.allDataReady) {
            // don't wait for valid online data if offline
            if (!onlineDataValid && !isOnline(getApplication())) {
              onlineDataValid = true
            }

            if (dbDataValid && assetDataValid && eventDataValid && onlineDataValid) {
              dataStore.allDataReady = true
            }
          }
        }
      }
    */

    private fun updateStockDataFromDB(stockDBdata: List<StockDBdata>) {
        synchronized(dataStore)
        {
            val stockDBdataPortfolios: HashSet<String> = hashSetOf("") // add Standard Portfolio
            val usedPortfolioSymbols: HashSet<StockSymbol> = HashSet()

            // Use only symbols matching the selected portfolio.
            var portfolio = SharedRepository.selectedPortfolio.value ?: ""
            var portfolioData = stockDBdata.filter { data ->
                data.portfolio == portfolio
            }

            // selected portfolio has no entries
            // revert back to the default portfolio and re-read the selection
            if (portfolio.isNotEmpty() && portfolioData.isEmpty()) {
                SharedRepository.selectedPortfolio.value = ""
                portfolio = ""
                portfolioData = stockDBdata.filter { data ->
                    data.portfolio == portfolio
                }
            }

            portfolioData.forEach { data ->
                val symbol = data.symbol
                usedPortfolioSymbols.add(
                    StockSymbol(
                        symbol = symbol,
                        type = dataProviderFromInt(data.type)
                    )
                )

                val dataStoreItem =
                    dataStore.find { ds ->
                        symbol == ds.stockDBdata.symbol
                    }

                if (dataStoreItem != null) {
                    dataStoreItem.stockDBdata = data
                } else {
                    dataStore.add(
                        StockItem(
                            onlineMarketData = OnlineMarketData(symbol = data.symbol),
                            stockDBdata = data,
                            assets = emptyList(),
                            events = emptyList(),
                            dividends = emptyList()
                        )
                    )
                }
            }

            // only load online data for symbols in the portfolio set
            portfolioSymbols = usedPortfolioSymbols
            logDebug("portfolioSymbols.size = ${portfolioSymbols.size}")

            // Remove the item from the dataStore because it is not in the portfolio or was deleted from the DB.
            dataStore.removeIf {
                portfolioData.find { sd ->
                    it.stockDBdata.symbol == sd.symbol
                } == null
            }

            // Get the portfolios from the unfiltered list.
            stockDBdata.forEach { data ->
                stockDBdataPortfolios.add(data.portfolio)

                // Test
                /*
                        stockDBdataPortfolios.add("data.portfolio1")
                        stockDBdataPortfolios.add("data.portfolio2")
                        stockDBdataPortfolios.add("data.portfolio3")
                */
            }

            // get all used portfolios
            // triggers the portfolio menu
            SharedRepository.portfolios.postValue(stockDBdataPortfolios)
        }

        _dataStore.value = dataStore
    }

    // Only unfiltered DB items.
    private fun updateStockDataFromDB_DB(stockDBdata: List<StockDBdata>) {
        synchronized(dataStoreDB)
        {
            stockDBdata.forEach { data ->
                val symbol = data.symbol

                val dataStoreItem =
                    dataStoreDB.find { ds ->
                        symbol == ds.stockDBdata.symbol
                    }

                if (dataStoreItem != null) {
                    dataStoreItem.stockDBdata = data
                } else {
                    dataStoreDB.add(
                        StockItem(
                            onlineMarketData = OnlineMarketData(symbol = data.symbol),
                            stockDBdata = data,
                            assets = emptyList(),
                            events = emptyList(),
                            dividends = emptyList()
                        )
                    )
                }
            }

            // Remove the item from the dataStore because it is not in the portfolio or was deleted from the DB.
            dataStoreDB.removeIf {
                stockDBdata.find { sd ->
                    it.stockDBdata.symbol == sd.symbol
                } == null
            }
        }

        _dataStoreDB.value = dataStoreDB
    }

    private fun updateAssetsFromDB(assets: List<Assets>) {
        synchronized(dataStore)
        {
            //assetDataValid = true

            assets.forEach { asset ->

                val symbol = asset.stockDBdata.symbol
                val dataStoreItem =
                    dataStore.find { ds ->
                        symbol == ds.stockDBdata.symbol
                    }

                if (dataStoreItem != null) {
                    dataStoreItem.assets = asset.assets
                } else {
                    val portfolio = SharedRepository.selectedPortfolio.value ?: ""
                    if (asset.stockDBdata.portfolio == portfolio) {
                        dataStore.add(
                            StockItem(
                                onlineMarketData = OnlineMarketData(symbol = asset.stockDBdata.symbol),
                                stockDBdata = asset.stockDBdata,
                                assets = asset.assets,
                                events = emptyList(),
                                dividends = emptyList()
                            )
                        )
                    }
                }
            }

            /*
                 // Remove the item from the dataStore because Item was deleted from the DB.
                 if (dataStore.stockItems.size > assets.size) {
                   dataStore.stockItems.removeIf {
                     // Remove if item is not found in the DB.
                     assets.find { ds ->
                       it.stockDBdata.symbol == ds.stockDBdata.symbol
                     } == null
                   }
                 }
                 */
        }

        _dataStore.value = dataStore
    }

    // Only unfiltered DB items.
    private fun updateAssetsFromDB_DB(assets: List<Assets>) {
        synchronized(dataStoreDB)
        {
            // Search the stock item and insert the data.
            assets.forEach { asset ->

                val symbol = asset.stockDBdata.symbol
                val dataStoreItem =
                    dataStore.find { ds ->
                        symbol == ds.stockDBdata.symbol
                    }

                if (dataStoreItem != null) {
                    dataStoreItem.assets = asset.assets
                } else {
                    dataStoreDB.add(
                        StockItem(
                            onlineMarketData = OnlineMarketData(symbol = asset.stockDBdata.symbol),
                            stockDBdata = asset.stockDBdata,
                            assets = asset.assets,
                            events = emptyList(),
                            dividends = emptyList()
                        )
                    )
                }
            }
        }

        _dataStoreDB.value = dataStoreDB
    }

    private fun updateEventsFromDB(events: List<Events>) {
        synchronized(dataStore)
        {
            //eventDataValid = true

            events.forEach { event ->
                val symbol = event.stockDBdata.symbol
                val dataStoreItem =
                    dataStore.find { ds ->
                        symbol == ds.stockDBdata.symbol
                    }

                if (dataStoreItem != null) {
                    dataStoreItem.events = event.events
                } else {
                    val portfolio = SharedRepository.selectedPortfolio.value ?: ""
                    if (event.stockDBdata.portfolio == portfolio) {
                        dataStore.add(
                            StockItem(
                                onlineMarketData = OnlineMarketData(symbol = event.stockDBdata.symbol),
                                stockDBdata = event.stockDBdata,
                                assets = emptyList(),
                                events = event.events,
                                dividends = emptyList()
                            )
                        )
                    }
                }

                /*
                // Remove the item from the dataStore because Item was deleted from the DB.
                if (dataStore.stockItems.size > events.size) {
                  dataStore.stockItems.removeIf {
                    // Remove if item is not found in the DB.
                    events.find { event ->
                      it.stockDBdata.symbol == event.stockDBdata.symbol
                    } == null
                  }
                }
              */
            }
        }

        _dataStore.value = dataStore
    }

    // Only unfiltered DB items.
    private fun updateEventsFromDB_DB(events: List<Events>) {
        synchronized(dataStoreDB)
        {
            // Search the stock item and insert the data.
            events.forEach { event ->
                val symbol = event.stockDBdata.symbol
                val dataStoreItem =
                    dataStoreDB.find { ds ->
                        symbol == ds.stockDBdata.symbol
                    }

                if (dataStoreItem != null) {
                    dataStoreItem.events = event.events
                } else {
                    dataStoreDB.add(
                        StockItem(
                            onlineMarketData = OnlineMarketData(symbol = event.stockDBdata.symbol),
                            stockDBdata = event.stockDBdata,
                            assets = emptyList(),
                            events = event.events,
                            dividends = emptyList()
                        )
                    )
                }
            }
        }

        _dataStoreDB.value = dataStoreDB
    }

    private fun updateDividendsFromDB(dividends: List<Dividends>) {
        synchronized(dataStore)
        {
            //dividendDataValid = true

            dividends.forEach { dividend ->

                val symbol = dividend.stockDBdata.symbol
                val dataStoreItem =
                    dataStore.find { ds ->
                        symbol == ds.stockDBdata.symbol
                    }

                if (dataStoreItem != null) {
                    dataStoreItem.dividends = dividend.dividends
                } else {
                    val portfolio = SharedRepository.selectedPortfolio.value ?: ""
                    if (dividend.stockDBdata.portfolio == portfolio) {
                        dataStore.add(
                            StockItem(
                                onlineMarketData = OnlineMarketData(symbol = dividend.stockDBdata.symbol),
                                stockDBdata = dividend.stockDBdata,
                                assets = emptyList(),
                                events = emptyList(),
                                dividends = dividend.dividends
                            )
                        )
                    }
                }
            }
        }

        _dataStore.value = dataStore
    }

    // Only unfiltered DB items.
    private fun updateDividendsFromDB_DB(dividends: List<Dividends>) {
        synchronized(dataStoreDB)
        {
            // Search the stock item and insert the data.
            dividends.forEach { dividend ->

                val symbol = dividend.stockDBdata.symbol
                val dataStoreItem =
                    dataStoreDB.find { ds ->
                        symbol == ds.stockDBdata.symbol
                    }

                if (dataStoreItem != null) {
                    dataStoreItem.dividends = dividend.dividends
                } else {
                    dataStoreDB.add(
                        StockItem(
                            onlineMarketData = OnlineMarketData(symbol = dividend.stockDBdata.symbol),
                            stockDBdata = dividend.stockDBdata,
                            assets = emptyList(),
                            events = emptyList(),
                            dividends = dividend.dividends
                        )
                    )
                }
            }
        }

        _dataStoreDB.value = dataStoreDB
    }

    private fun updateFromOnline(onlineMarketDataList: List<OnlineMarketData>) {
        synchronized(dataStore)
        {
            //onlineDataValid = true
            //dataStore.allDataReady = true

            // Work off a read-only copy to avoid the java.util.ConcurrentModificationException
            // while the next update.
            onlineMarketDataList.toImmutableList()
                .forEach { onlineMarketDataItem ->
                    val symbol = onlineMarketDataItem.symbol

                    val dataStoreItem =
                        dataStore.find { ds ->
                            symbol.equals(ds.stockDBdata.symbol, true)
                        }

                    if (dataStoreItem != null) {
                        dataStoreItem.onlineMarketData = onlineMarketDataItem

                        /*
                                      if (postMarket) {
                                        if ((onlineMarketDataItem.marketState == MarketState.POST.value
                                                || onlineMarketDataItem.marketState == MarketState.POSTPOST.value
                                                || onlineMarketDataItem.marketState == MarketState.PREPRE.value
                                                || onlineMarketDataItem.marketState == MarketState.CLOSED.value)
                                            && onlineMarketDataItem.postMarketPrice > 0.0
                                        ) {
                                          dataStoreItem.onlineMarketData.marketPrice =
                                            onlineMarketDataItem.postMarketPrice
                                          dataStoreItem.onlineMarketData.marketChange =
                                            onlineMarketDataItem.postMarketChange
                                          dataStoreItem.onlineMarketData.marketChangePercent =
                                            onlineMarketDataItem.postMarketChangePercent
                                        } else
                                          if ((onlineMarketDataItem.marketState == MarketState.PRE.value)
                                              && onlineMarketDataItem.preMarketPrice > 0.0
                                          ) {
                                            dataStoreItem.onlineMarketData.marketPrice =
                                              onlineMarketDataItem.preMarketPrice
                                            dataStoreItem.onlineMarketData.marketChange =
                                              onlineMarketDataItem.preMarketChange
                                            dataStoreItem.onlineMarketData.marketChangePercent =
                                              onlineMarketDataItem.preMarketChangePercent
                                          }
                                      }
                        */
                    }
                }

            _dataStore.value = dataStore
        }
    }

    data class AlertData(
        var symbol: String,
        var symbolDisplayName: String,
        var name: String,

        var alertAbove: Double = 0.0,
        var alertAboveNote: String = "",

        var alertBelow: Double = 0.0,
        var alertBelowNote: String = "",

        var marketPrice: Double = 0.0,

//    var alertGainAbove: Double = 0.0,
//    var alertGainBelow: Double = 0.0,
//    var gain: Double = 0.0,
//
//    var alertLossAbove: Double = 0.0,
//    var alertLossBelow: Double = 0.0,
//    var loss: Double = 0.0,
    )

    private fun processNotifications(stockItems: List<StockItem>) {
        val newAlerts: MutableList<AlertData> = mutableListOf()

        stockItems.forEach { stockItem ->
            //     allStockItems.value?.forEach { stockItem ->
            val marketPrice = stockItem.onlineMarketData.marketPrice

            if (marketPrice > 0.0 && SharedRepository.alertsData.value != null) {
                if (SharedRepository.alertsData.value!!.find {
                        it.symbol == stockItem.stockDBdata.symbol
                    } == null) {

                    if (stockItem.stockDBdata.alertAbove > 0.0 && stockItem.stockDBdata.alertAbove < marketPrice) {
                        val alertDataAbove = AlertData(
                            symbol = stockItem.stockDBdata.symbol,
                            symbolDisplayName = stockItem.stockDBdata.name.ifEmpty { stockItem.stockDBdata.symbol },
                            name = getName(stockItem.onlineMarketData),

                            alertAbove = stockItem.stockDBdata.alertAbove,
                            alertAboveNote = stockItem.stockDBdata.alertAboveNote,
                            marketPrice = marketPrice
                        )
                        newAlerts.add(alertDataAbove)
                    } else
                        if (stockItem.stockDBdata.alertBelow > 0.0 && stockItem.stockDBdata.alertBelow > marketPrice) {
                            val alertDataBelow = AlertData(
                                symbol = stockItem.stockDBdata.symbol,
                                symbolDisplayName = stockItem.stockDBdata.name.ifEmpty { stockItem.stockDBdata.symbol },
                                name = getName(stockItem.onlineMarketData),

                                alertBelow = stockItem.stockDBdata.alertBelow,
                                alertBelowNote = stockItem.stockDBdata.alertBelowNote,
                                marketPrice = marketPrice
                            )
                            newAlerts.add(alertDataBelow)
                        }
                }
            }
        }

        SharedRepository.alertsData.value = newAlerts
    }

    fun resetAlerts() {
        SharedRepository.alertsData.value = emptyList()
    }

// https://proandroiddev.com/mediatorlivedata-to-the-rescue-5d27645b9bc3

    fun updateSortMode(_sortMode: SortMode) {
        sortMode = _sortMode
        sorting.postValue(sortMode)
        allMediatorData.value = allData.value?.let { process(it, false) }
    }

    fun sortMode() =
        sortMode

    private fun process(
        stockItems: List<StockItem>,
        runNotifications: Boolean
    ): List<StockItem> {
        if (runNotifications && SharedRepository.notifications) {
            processNotifications(stockItems)
        }

        return sort(sortMode, filter(stockItems))
    }

    private fun filter(
        stockItems: List<StockItem>
    ): List<StockItem> =
        if (filterList.isEmpty()) {
            stockItems
        } else {
            try {
                when (filterMode) {

                    FilterModeTypeEnum.AndType -> {
                        stockItems.filter { item ->
                            var match = true
                            // All filters must return true to match.
                            for (filterType in filterList) {
                                if (!filterType.filter(item)) {
                                    match = false
                                    break
                                }
                            }
                            match
                        }
                    }

                    FilterModeTypeEnum.OrType -> {
                        stockItems.filter { item ->
                            var match = false
                            // First filter match return true.
                            for (filterType in filterList) {
                                if (filterType.filter(item)) {
                                    match = true
                                    break
                                }
                            }
                            match
                        }
                    }
                }
            } catch (e: Exception) {
                // Return no results in case of exception (for example wrong regex).
                emptyList()
            }
        }

    private fun sort(
        sortMode: SortMode,
        stockItems: List<StockItem>
    )
            : List<StockItem> =
        when (sortMode) {
            SortMode.ByChangePercentage -> {
                stockItems.sortedByDescending { item ->
                    item.onlineMarketData.marketChangePercent
                }
            }

            SortMode.ByName -> {
                stockItems.sortedBy { item ->
                    item.stockDBdata.name.ifEmpty { item.stockDBdata.symbol }
                }
            }

            SortMode.ByPurchaseprice -> {
                stockItems.sortedByDescending { item ->
                    val (totalQuantity, totalPrice, totalFee) = getAssets(item.assets)
                    totalPrice + totalFee
                }
            }

            SortMode.ByAssets -> {
                stockItems.sortedByDescending { item ->
                    val (totalQuantity, totalPrice, totalFee) = getAssets(item.assets)
                    if (item.onlineMarketData.marketPrice > 0.0) {
                        totalQuantity * item.onlineMarketData.marketPrice
//              item.assets.sumOf {
//                it.shares * item.onlineMarketData.marketPrice
//              }
                    } else {
                        totalPrice + totalFee
//              item.assets.sumOf {
//                it.shares * it.price
//              }
                    }
                }
            }

            SortMode.ByProfit -> {
                stockItems.sortedByDescending { item ->
                    val (totalQuantity, totalPrice, totalFee) = getAssets(item.assets)
                    if (item.onlineMarketData.marketPrice > 0.0) {
                        totalQuantity * item.onlineMarketData.marketPrice - totalPrice - totalFee
//              item.assets.sumOf {
//                it.shares * (item.onlineMarketData.marketPrice - it.price)
//              }
                    } else {
                        totalPrice
//              item.assets.sumOf {
//                it.shares * it.price
//              }
                    }
                }
            }

            SortMode.ByProfitPercentage -> {
                stockItems.sortedByDescending { item ->
                    val (totalQuantity, totalPrice, totalFee) = getAssets(item.assets)
                    val total = totalPrice + totalFee
                    if (item.onlineMarketData.marketPrice > 0.0 && total > 0.0) {
                        (totalQuantity * item.onlineMarketData.marketPrice - total) / total
                    } else {
                        totalPrice
                    }
                }
            }

            SortMode.ByMarketCap -> {
                stockItems.sortedByDescending { item ->
                    item.onlineMarketData.marketCap
                }
            }

            SortMode.ByDividendPercentage -> {
                stockItems.sortedByDescending { item ->

                    // Use stockDBdata.annualDividendRate if available.
                    // stockDBdata.annualDividendRate can be 0.0
                    if (item.stockDBdata.annualDividendRate >= 0.0) {
                        if (item.onlineMarketData.marketPrice > 0.0) {
                            item.stockDBdata.annualDividendRate / item.onlineMarketData.marketPrice
                        } else {
                            0.0
                        }
                    } else {
                        item.onlineMarketData.annualDividendYield
                    }
                }
            }

            SortMode.ByGroup -> {
                // Sort the group items alphabetically.
                stockItems.sortedBy { item ->
                    item.stockDBdata.name.ifEmpty { item.stockDBdata.symbol }
                }
                    // Sort by HUE color. Put items with no color to the end.
                    // Items with groupColor = 0 would be at the beginning.
                    .sortedByDescending { item ->
                        if (item.stockDBdata.groupColor == 0) {
                            //Int.MIN_VALUE

                            -1f
                        } else {
                            //item.stockDBdata.groupColor

                            val hsv = FloatArray(3)
                            Color.colorToHSV(item.stockDBdata.groupColor, hsv)
                            // hsv[0] --> hue
                            // hsv[1] --> saturation
                            // hsv[2] --> value
                            hsv[0]
                        }
                    }
            }

            SortMode.ByMarker -> {
                // Sort the group items alphabetically.
                stockItems.sortedBy { item ->
                    item.stockDBdata.name.ifEmpty { item.stockDBdata.symbol }
                }
                    .sortedByDescending { item ->
                        // Sort with smallest marker number first, then all not marked items (marker=0)
                        if (item.stockDBdata.marker == 0) {
                            0
                        } else {
                            // 1..10 -> 10..1
                            11 - item.stockDBdata.marker
                        }
                    }
            }

            SortMode.ByActivity -> {
                stockItems.sortedByDescending { item ->

//          // Get the most recent sold activity.
//          if (item.assets.isNotEmpty()) {
//            item.assets.filter { asset ->
//              asset.quantity > 0.0
//            }
//                .maxOf { asset ->
//                  asset.date
//                }
//          } else {
//            0
//          }

                    // Get the most recent activity.
                    if (item.assets.isNotEmpty()) {
                        item.assets.maxOf { asset ->
                            asset.date
                        }
                    } else {
                        0
                    }
                }

//        SortMode.ByUnsorted -> {
//          stockItemSet.stockItems
//        }
            }
        }

    /*
    [{
     "annualDividendRate": 4.5,
     "annualDividendYield": 0.04550971,
     "change": 0.060005188,
     "changeInPercent": 0.06068486,
     "currency": "USD",
     "isPostMarket": false,
     "lastTradePrice": 98.94,
     "name": "AbbVie Inc.",
     "position": {
         "holdings": [{
             "id": 5,
             "price": 82.09,
             "shares": 350.0,
             "symbol": "ABBV"
         }],
         "symbol": "ABBV"
     },
     "properties": {
         "alertAbove": 100.0,
         "alertBelow": 0.0,
         "notes": "",
         "symbol": "ABBV"
     },
     "stockExchange": "NYQ",
     "symbol": "ABBV"
    }, {
     "annualDividendRate": 0.0,
     "annualDividendYield": 0.0,
     "change": -0.19000053,
    */

    /*
    {
      "annualDividendRate": 0.0,
      "annualDividendYield": 0.0,
      "change": -0.010099411,
      "changeInPercent": -0.107899696,
      "currency": "USD",
      "isPostMarket": true,
      "lastTradePrice": 9.3499,
      "name": "Dynavax Technologies Corporatio",
      "position": {
        "holdings": [
          {
            "id": 209,
            "price": 5.17,
            "shares": 3600.0002,
            "symbol": "DVAX"
          }
        ],
        "symbol": "DVAX"
      },
      "properties": {
        "alertAbove": 0.0,
        "alertBelow": 8.0,
        "notes": "Verkauft 3000@9,24 am 26.6.2020",
        "symbol": "DVAX"
      },
      "stockExchange": "NMS",
      "symbol": "DVAX"
    },
    {

    [{
      "alertAbove": 11.0,
      "alertBelow": 12.0,
      "assets": [{
          "price": 2.0,
          "shares": 1.0
      }],
      "events": [{
          "datetime": 1,
          "note": "n1"
          "title": "t1"
          "type": 0
      }],
      "groupColor": 123,
      "groupName": "a",
      "notes": "notes1",
      "symbol": "s1"
    }, {
    */

    private fun importJSON(
        context: Context,
        json: String
    ) {
        var imported: Int = 0
        val jsonArray = JSONArray(json)
        val size = jsonArray.length()
        for (i in 0 until size) {

            // get symbol
            val jsonObj: JSONObject = jsonArray[i] as JSONObject

            logDebug("Import JSONObject '$jsonObj'")

            val symbol = jsonObj.getString("symbol")
                .uppercase(Locale.ROOT)

            if (isValidSymbol(symbol)) {
                var portfolio = SharedRepository.selectedPortfolio.value ?: ""
                var type = 0

                if (jsonObj.has("portfolio")) {
                    portfolio = jsonObj.getString("portfolio")
                    //if (portfolio.isNotEmpty()) {
                    //  setPortfolio(symbol = symbol, portfolio = portfolio)
                    //}
                }

                if (jsonObj.has("type")) {
                    type = jsonObj.getInt("type")
                }

                insert(symbol = symbol, portfolio = portfolio.trim(), type = type)
                imported++

                if (jsonObj.has("name")) {
                    val name = jsonObj.getString("name")
                    if (name.isNotEmpty()) {
                        setName(symbol, name)
                    }
                }

                // get assets
                if (jsonObj.has("assets")) {
                    val assetsObjArray = jsonObj.getJSONArray("assets")
                    val assets: MutableList<Asset> = mutableListOf()

                    for (j in 0 until assetsObjArray.length()) {
                        val assetsObj: JSONObject = assetsObjArray[j] as JSONObject

                        val quantity = when {
                            assetsObj.has("quantity") -> {
                                assetsObj.getDouble("quantity")
                            }

                            assetsObj.has("shares") -> {
                                assetsObj.getDouble("shares")
                            }

                            else -> {
                                null
                            }
                        }

                        if (quantity != null && assetsObj.has("price")) {
                            val price = assetsObj.getDouble("price").absoluteValue
                            if (quantity != 0.0 && price >= 0.0) {
                                assets.add(
                                    Asset(
                                        symbol = symbol,
                                        quantity = quantity,
                                        price = price,
                                        note = if (assetsObj.has("note")) {
                                            assetsObj.getString("note")
                                        } else {
                                            ""
                                        },
                                        type = if (assetsObj.has("type")) {
                                            assetsObj.getInt("type")
                                        } else {
                                            0
                                        },
                                        account = if (assetsObj.has("account")) {
                                            assetsObj.getString("account")
                                        } else {
                                            ""
                                        },
                                        date = if (assetsObj.has("date")) {
                                            assetsObj.getLong("date")
                                        } else {
                                            0L
                                        },
                                        sharesPerQuantity = if (assetsObj.has("sharesPerQuantity")) {
                                            assetsObj.getInt("sharesPerQuantity")
                                        } else {
                                            1
                                        },
                                        expirationDate = if (assetsObj.has("expirationDate")) {
                                            assetsObj.getLong("expirationDate")
                                        } else {
                                            0L
                                        },
                                        premium = if (assetsObj.has("premium")) {
                                            assetsObj.getDouble("premium")
                                        } else {
                                            0.0
                                        },
                                        fee = when {
                                            assetsObj.has("fee") -> {
                                                assetsObj.getDouble("fee")
                                            }

                                            assetsObj.has("commission") -> {
                                                assetsObj.getDouble("commission")
                                            }

                                            else -> {
                                                0.0
                                            }
                                        }
                                    )
                                )
                            }
                        }
                    }

                    if (assets.isNotEmpty()) {
                        updateAssets(symbol = symbol, assets = assets)
                    }
                }

                if (jsonObj.has("position")) {
                    val positionObj = jsonObj.getJSONObject("position")
                    if (positionObj.has("holdings")) {
                        val holdings = positionObj.getJSONArray("holdings")
                        val assets: MutableList<Asset> = mutableListOf()

                        for (j in 0 until holdings.length()) {
                            val holdingObj: JSONObject = holdings[j] as JSONObject
                            if (holdingObj.has("shares") && holdingObj.has("price")) {
                                val shares = holdingObj.getDouble("shares")
                                val price = holdingObj.getDouble("price")
                                if (shares > 0.0 && price >= 0.0) {
                                    assets.add(
                                        Asset(
                                            symbol = symbol,
                                            quantity = shares,
                                            price = price
                                        )
                                    )
                                }
                            }
                        }

                        if (assets.isNotEmpty()) {
                            updateAssets(symbol = symbol, assets = assets)
                        }
                    }
                }

                // get events
                if (jsonObj.has("events")) {
                    val eventsObjArray = jsonObj.getJSONArray("events")
                    val events: MutableList<Event> = mutableListOf()

                    /*
                      "events": [{
                            "datetime": 1,
                            "note": "n1"
                            "title": "t1"
                            "type": 0
                        }],
                     */

                    for (j in 0 until eventsObjArray.length()) {
                        val eventsObj: JSONObject = eventsObjArray[j] as JSONObject

                        if (eventsObj.has("datetime") && eventsObj.has("title")) {
                            val title = eventsObj.getString("title")
                                .trim()
                            if (title.isNotEmpty()) {
                                events.add(
                                    Event(
                                        symbol = symbol,
                                        datetime = eventsObj.getLong("datetime"),
                                        note = if (eventsObj.has("note")) {
                                            eventsObj.getString("note")
                                        } else {
                                            ""
                                        },
                                        title = title,
                                        type = if (eventsObj.has("type")) {
                                            eventsObj.getInt("type")
                                        } else {
                                            0
                                        }
                                    )
                                )
                            }
                        }
                    }

                    if (events.isNotEmpty()) {
                        updateEvents(symbol, events)
                    }
                }

                // get dividends
                if (jsonObj.has("dividends")) {
                    val dividendsObjArray = jsonObj.getJSONArray("dividends")

                    for (j in 0 until dividendsObjArray.length()) {
                        val dividendsObj: JSONObject = dividendsObjArray[j] as JSONObject

                        if (dividendsObj.has("amount") && dividendsObj.has("paydate")) {
                            val amount = dividendsObj.getDouble("amount")
                            val paydate = dividendsObj.getLong("paydate")

                            if (amount > 0.0 && paydate > 0L) {
                                val dividend = Dividend(
                                    symbol = symbol,
                                    amount = amount,
                                    paydate = paydate,
                                    type = if (dividendsObj.has("type")) {
                                        dividendsObj.getInt("type")
                                    } else {
                                        0
                                    },
                                    account = if (dividendsObj.has("account")) {
                                        dividendsObj.getString("account")
                                    } else {
                                        ""
                                    },
                                    cycle = if (dividendsObj.has("cycle")) {
                                        dividendsObj.getInt("cycle")
                                    } else {
                                        Quarterly.value
                                    },
                                    exdate = if (dividendsObj.has("exdate")) {
                                        dividendsObj.getLong("exdate")
                                    } else {
                                        0L
                                    },
                                    note = if (dividendsObj.has("note")) {
                                        dividendsObj.getString("note")
                                    } else {
                                        ""
                                    },
                                )

                                updateDividend(dividend)
                            }
                        }
                    }
                }

                if (jsonObj.has("data")) {
                    val data = jsonObj.getString("data")
                        .trim()
                    if (data.isNotEmpty()) {
                        setData(symbol = symbol, data = data)
                    }
                }

                if (jsonObj.has("groupColor")) {
                    val groupColor = jsonObj.getInt("groupColor")

                    // Get matching group name.
                    if (groupColor != 0 && jsonObj.has("groupName")) {
                        val groupName = jsonObj.getString("groupName")
                            .trim()
                        if (groupName.isNotEmpty()) {
                            setGroup(symbol = symbol, color = groupColor, name = groupName)
                        }
                    }
                }

                if (jsonObj.has("marker")) {
                    val marker = jsonObj.getInt("marker")
                    if (marker in 0..10) {
                        setMarker(symbol = symbol, marker = marker)
                    }
                }

                if (jsonObj.has("notes")) {
                    val notes = jsonObj.getString("notes")
                    if (notes.isNotEmpty()) {
                        updateNote(symbol, notes)
                    }
                }

                if (jsonObj.has("note")) {
                    val note = jsonObj.getString("note")
                    if (note.isNotEmpty()) {
                        updateNote(symbol, note)
                    }
                }

                if (jsonObj.has("dividendNotes")) {
                    val dividendNotes = jsonObj.getString("dividendNotes")
                    if (dividendNotes.isNotEmpty()) {
                        updateDividendNote(symbol, dividendNotes)
                    }
                }

                if (jsonObj.has("dividendNote")) {
                    val dividendNote = jsonObj.getString("dividendNote")
                    if (dividendNote.isNotEmpty()) {
                        updateDividendNote(symbol, dividendNote)
                    }
                }

                if (jsonObj.has("annualDividendRate")) {
                    val annualDividendRate = jsonObj.getDouble("annualDividendRate")
                    if (annualDividendRate >= 0.0) {
                        updateAnnualDividendRate(symbol, annualDividendRate)
                    }
                }

                if (jsonObj.has("alertAbove")) {
                    val alertAboveNote = if (jsonObj.has("alertAboveNote")) {
                        jsonObj.getString("alertAboveNote")
                    } else {
                        ""
                    }
                    val alertAbove = jsonObj.getDouble("alertAbove")
                    if (alertAbove > 0.0) {
                        updateAlertAbove(symbol, alertAbove, alertAboveNote)
                    }
                }

                if (jsonObj.has("alertBelow")) {
                    val alertBelowNote = if (jsonObj.has("alertBelowNote")) {
                        jsonObj.getString("alertBelowNote")
                    } else {
                        ""
                    }
                    val alertBelow = jsonObj.getDouble("alertBelow")
                    if (alertBelow > 0.0) {
                        updateAlertBelow(symbol, alertBelow, alertBelowNote)
                    }
                }

                if (jsonObj.has("properties")) {
                    val properties = jsonObj.getJSONObject("properties")

                    if (properties.has("alertAbove")) {
                        val alertAbove = properties.getDouble("alertAbove")
                        if (alertAbove > 0.0) {
                            updateAlertAbove(symbol, alertAbove, "")
                        }
                    }

                    if (properties.has("alertBelow")) {
                        val alertBelow = properties.getDouble("alertBelow")
                        if (alertBelow > 0.0) {
                            updateAlertBelow(symbol, alertBelow, "")
                        }
                    }

                    if (properties.has("notes")) {
                        val notes = properties.getString("notes")
                        if (notes.isNotEmpty()) {
                            updateNote(symbol, notes)
                        }
                    }
                }
            }
        }

        val msg = getApplication<Application>().getString(
            R.string.import_msg, imported.toString()
        )
        logDebug("Import JSON  '$msg'")

        Toast.makeText(
            context, msg, Toast.LENGTH_LONG
        )
            .show()
    }

    private fun csvStrToDouble(str: String)
            : Double {
        val s = str.replace("$", "")
            .replace(",", "")
        var value: Double
        try {
            value = s.toDouble()
            if (value == 0.0) {
                val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                value = numberFormat.parse(s)!!
                    .toDouble()
            }
        } catch (e: Exception) {
            value = 0.0
        }

        return value
    }

    private fun importCSV(
        context: Context,
        csv: String
    ) {
        val reader = csvReader {
            skipEmptyLine = true
            skipMissMatchedRow = true
        }
        val rows: List<List<String>> = reader.readAll(csv)

        // Fidelity CSV
        // "Account Name/Number","Symbol","Description","Quantity","Last Price","Last Price Change",
        // "Current Value","Today's Gain/Loss Dollar","Today's Gain/Loss Percent","Total Gain/Loss Dollar",
        // "Total Gain/Loss Percent","Cost Basis Per Share","Cost Basis Total","Type","Dividend Quantity",
        // "Grant ID"," Grant Price","Gain Per TSRU","Offering Period Ends","Offering Period Begins","Total Balances"

        // try columns "Symbol", "Name"
        val headerRow = rows.first()
        val symbolColumn = headerRow.indexOfFirst {
            it.compareTo(other = "Symbol", ignoreCase = true) == 0
                    || it.compareTo(other = "Name", ignoreCase = true) == 0
        }

        if (symbolColumn == -1) {
            val msg =
                getApplication<Application>().getString(
                    R.string.import_csv_column_error, "Symbol|Name"
                )
            logDebug("Import CSV  '$msg'")

            Toast.makeText(
                context, msg,
                Toast.LENGTH_LONG
            )
                .show()
            return
        }

        // try columns "Quantity", "Shares"
        val sharesColumn = headerRow.indexOfFirst {
            it.compareTo(other = "Quantity", ignoreCase = true) == 0
                    || it.compareTo(other = "Shares", ignoreCase = true) == 0
        }

        if (sharesColumn == -1) {
            val msg =
                getApplication<Application>().getString(
                    R.string.import_csv_column_error, "Shares|Quantity"
                )
            logDebug("Import CSV  '$msg'")

            Toast.makeText(
                context, msg,
                Toast.LENGTH_LONG
            )
                .show()
            return
        }

        // try columns "Cost Basis Per Share", "Price"
        val priceColumn = headerRow.indexOfFirst {
            it.compareTo(other = "Cost Basis Per Share", ignoreCase = true) == 0
                    || it.compareTo(other = "Price", ignoreCase = true) == 0
        }

        if (priceColumn == -1) {
            val msg = getApplication<Application>().getString(
                R.string.import_csv_column_error, "Price|Cost Basis Per Share"
            )
            logDebug("Import CSV  '$msg'")

            Toast.makeText(
                context, msg,
                Toast.LENGTH_LONG
            )
                .show()
            return
        }

        val assetItems = HashMap<String, List<Asset>>()

        // skip header row
        rows.drop(1)
            .forEach { row ->
                val symbol = row[symbolColumn].uppercase(Locale.ROOT)
                if (symbol.isNotEmpty() && isValidSymbol(symbol)) {
                    val shares = csvStrToDouble(row[sharesColumn])
                    val price = csvStrToDouble(row[priceColumn])

                    if (shares > 0.0 && price >= 0.0) {
                        val asset = Asset(
                            symbol = symbol,
                            quantity = shares,
                            price = price
                        )

                        if (assetItems.containsKey(symbol)) {
                            val list = assetItems[symbol]!!.toMutableList()
                            list.add(asset)
                            assetItems[symbol] = list
                        } else {
                            assetItems[symbol] = listOf(asset)
                        }
                    }
                }
            }

        // Limit import to 100.
        var imported: Int = 0
        val portfolio = SharedRepository.selectedPortfolio.value ?: ""
        assetItems.forEach { (symbol, assets) ->
            if (imported < 100) {
                imported++
                insert(symbol = symbol, portfolio = portfolio, type = 0)
                // updateAssets filters out empty shares@price
                updateAssets(symbol = symbol, assets = assets)
            }
        }

        val msg = getApplication<Application>().getString(
            R.string.import_msg, imported.toString()
        )
        logDebug("Import CSV  '$msg'")

        Toast.makeText(
            context, msg, Toast.LENGTH_LONG
        )
            .show()
    }

    private fun importText(
        context: Context,
        text: String
    ) {
        logDebug("Import TXT  '$text'")

        val symbols = text.split("[ ,;\r\n\t]".toRegex())

        var imported = 0
        val portfolio = SharedRepository.selectedPortfolio.value ?: ""
        symbols.map { symbol ->
            symbol.replace("\"", "")
                .uppercase(Locale.ROOT)
        }
            // only a-z from 1..7 chars in length
//        .filter { symbol ->
//          symbol.matches("[A-Z]{1,7}".toRegex())
//        }
            .distinct()
            .take(100)
            .forEach { symbol ->
                if (isValidSymbol(symbol)) {
                    insert(symbol = symbol, portfolio = portfolio, type = 0)
                    imported++
                }
            }

        val msg = getApplication<Application>().getString(
            R.string.import_msg, imported.toString()
        )
        logDebug("Import TXT  '$msg'")

        Toast.makeText(
            context, msg, Toast.LENGTH_LONG
        )
            .show()
    }

    fun importList(
        context: Context,
        uri: Uri
    ) {
        try {
            context.contentResolver.openInputStream(uri)
                ?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        val text: String = reader.readText()

                        // https://developer.android.com/training/secure-file-sharing/retrieve-info

                        when (val type = context.contentResolver.getType(uri)) {
                            "application/json", "text/x-json" -> {
                                importJSON(context, text)
                            }

                            "text/csv",
                            "text/comma-separated-values",
                            "application/octet-stream" -> {
                                importCSV(context, text)
                            }

                            "text/plain" -> {
                                importText(context, text)
                            }

                            else -> {
                                val msg = getApplication<Application>().getString(
                                    R.string.import_mimetype_error, type
                                )
                                throw IllegalArgumentException(msg)
                            }
                        }
                    }
                }
        } catch (e: Exception) {
            Toast.makeText(
                context, getApplication<Application>().getString(R.string.import_error, e.message),
                Toast.LENGTH_LONG
            )
                .show()
            Log.d("Import error", "Exception: $e")
            logDebug("Import Exception: $e")
        }
    }

    private fun getAllDBdata(): List<StockItem> {
        val stockDBdata = allStockDBdata.value ?: emptyList()
        val assets = allAssets.value ?: emptyList()
        val events = allEvents.value ?: emptyList()
        val dividends = allDividends.value ?: emptyList()

        return stockDBdata.map { data ->
            val symbol = data.symbol

            val assetItem =
                assets.find { item ->
                    symbol == item.stockDBdata.symbol
                }

            val eventItem =
                events.find { item ->
                    symbol == item.stockDBdata.symbol
                }

            val dividendItem =
                dividends.find { item ->
                    symbol == item.stockDBdata.symbol
                }

            StockItem(
                onlineMarketData = OnlineMarketData(symbol = data.symbol),
                stockDBdata = data,
                assets = assetItem?.assets ?: emptyList(),
                events = eventItem?.events ?: emptyList(),
                dividends = dividendItem?.dividends ?: emptyList()
            )
        }
    }

    fun exportJSON(
        context: Context,
        exportJsonUri: Uri
    ) {
        // Gets the groups to resolve the group name
        // Group name is stored per entry and not as one list to simplify the json structure.
        val groups: List<Group> = getGroupsSync()

        // Export items sorted alphabetically.
        val stockItemList = getAllDBdata().sortedBy { data ->
            data.stockDBdata.symbol
        }

        val stockItemsJson = stockItemList.map { stockItem ->

            val groupName = groups.find { group ->
                group.color == stockItem.stockDBdata.groupColor
            }?.name ?: ""

            // Convert empty or 0 values to null to exclude them from serialized to JSON.
            val nameValue = if (stockItem.stockDBdata.name.isNotEmpty()) {
                stockItem.stockDBdata.name
            } else {
                null
            }
            val typeValue = if (stockItem.stockDBdata.type != 0) {
                stockItem.stockDBdata.type
            } else {
                null
            }
            val dataValue = if (stockItem.stockDBdata.data.isNotEmpty()) {
                stockItem.stockDBdata.data
            } else {
                null
            }
            val groupColorValue = if (stockItem.stockDBdata.groupColor != 0) {
                stockItem.stockDBdata.groupColor
            } else {
                null
            }
            val markerValue = if (stockItem.stockDBdata.marker != 0) {
                stockItem.stockDBdata.marker
            } else {
                null
            }
            val groupNameValue = if (groupName.isNotEmpty()) {
                groupName
            } else {
                null
            }
            val noteValue = if (stockItem.stockDBdata.note.isNotEmpty()) {
                stockItem.stockDBdata.note
            } else {
                null
            }
            val dividendNoteValue = if (stockItem.stockDBdata.dividendNote.isNotEmpty()) {
                stockItem.stockDBdata.dividendNote
            } else {
                null
            }
            val annualDividendRate = validateDouble(stockItem.stockDBdata.annualDividendRate)
            val annualDividendRateValue = if (annualDividendRate >= 0.0) {
                annualDividendRate
            } else {
                null
            }
            val alertAbove = validateDouble(stockItem.stockDBdata.alertAbove)
            val alertAboveValue = if (alertAbove > 0.0) {
                alertAbove
            } else {
                null
            }
            val alertAboveNoteValue = if (stockItem.stockDBdata.alertAboveNote.isNotEmpty()) {
                stockItem.stockDBdata.alertAboveNote
            } else {
                null
            }
            val alertBelow = validateDouble(stockItem.stockDBdata.alertBelow)
            val alertBelowValue = if (alertBelow > 0.0) {
                alertBelow
            } else {
                null
            }
            val alertBelowNoteValue = if (stockItem.stockDBdata.alertBelowNote.isNotEmpty()) {
                stockItem.stockDBdata.alertBelowNote
            } else {
                null
            }
            val assetsValue = if (stockItem.assets.isEmpty()) {
                null
            } else {
                stockItem.assets.sortedBy { item ->
                    item.date
                }
                    .map { asset ->
                        AssetJson(
                            quantity = validateDouble(asset.quantity),
                            price = validateDouble(asset.price),
                            type = if (asset.type != 0) asset.type else null,
                            account = if (asset.account.isNotEmpty()) asset.account else null,
                            note = if (asset.note.isNotEmpty()) asset.note else null,
                            date = if (asset.date != 0L) asset.date else null,
                            sharesPerQuantity = if (asset.sharesPerQuantity != 1) asset.sharesPerQuantity else null,
                            expirationDate = if (asset.expirationDate != 0L) asset.expirationDate else null,
                            premium = if (asset.premium > 0.0) validateDouble(asset.premium) else null,
                            fee = if (asset.fee > 0.0) validateDouble(
                                asset.fee
                            ) else null,
                        )
                    }
            }

            val eventsValue = if (stockItem.events.isEmpty()) {
                null
            } else {
                stockItem.events.sortedBy { item ->
                    item.datetime
                }
                    .map { event ->
                        EventJson(
                            title = event.title,
                            datetime = event.datetime,
                            type = if (event.type != 0) event.type else null,
                            note = if (event.note.isNotEmpty()) event.note else null
                        )
                    }
            }

            val dividendsValue = if (stockItem.dividends.isEmpty()) {
                null
            } else {
                stockItem.dividends.sortedBy { item ->
                    item.paydate
                }
                    .map { dividend ->
                        DividendJson(
                            amount = validateDouble(dividend.amount),
                            cycle = dividend.cycle,
                            paydate = dividend.paydate,
                            type = if (dividend.type != 0) dividend.type else null,
                            account = if (dividend.account.isNotEmpty()) dividend.account else null,
                            exdate = if (dividend.exdate != 0L) dividend.exdate else null,
                            note = if (dividend.note.isNotEmpty()) dividend.note else null
                        )
                    }
            }

            StockItemJson(
                symbol = stockItem.stockDBdata.symbol,
                name = nameValue,
                // Empty portfolio is the default portfolio, don't exclude from the export with a null value.
                // Otherwise importing would set the current portfolio to missing portfolio json entries.
                portfolio = stockItem.stockDBdata.portfolio,

                type = typeValue,
                data = dataValue,
                groupColor = groupColorValue,
                groupName = groupNameValue,
                marker = markerValue,
                note = noteValue,
                dividendNote = dividendNoteValue,
                annualDividendRate = annualDividendRateValue,
                alertAbove = alertAboveValue,
                alertAboveNote = alertAboveNoteValue,
                alertBelow = alertBelowValue,
                alertBelowNote = alertBelowNoteValue,
                assets = assetsValue,
                events = eventsValue,
                dividends = dividendsValue
            )
        }

        // Convert to a json string.
        val gson: Gson = GsonBuilder()
            .setPrettyPrinting()
            .create()
        val jsonString = gson.toJson(stockItemsJson)

        val msg =
            getApplication<Application>().getString(R.string.export_msg, stockItemsJson.size)
        saveTextToFile(jsonString, msg, context, exportJsonUri)
        logDebug("Export JSON '$msg'")
    }

    // Get the colored menu entries for the groups.
    fun getGroupsMenuList(standardGroupName: String, colorRef: Int): List<SpannableString> {
        val groups: MutableList<Group> = getGroupsSync().toMutableList()
        return getGroupsMenuList(
            groups,
            backgroundListColor,
            colorRef,
            standardGroupName
        )
    }

    /*
     * Launching a new coroutine to insert the data in a non-blocking way
     */
// viewModelScope.launch(Dispatchers.IO) {
    fun insert(
        symbol: String,
        portfolio: String,
        type: Int
    ) = scope.launch {
        if (symbol.isNotEmpty()) {
            repository.insert(
                StockDBdata(
                    symbol = symbol.uppercase(Locale.ROOT),
                    portfolio = portfolio,
                    type = type
                )
            )
        }
    }

    fun setPredefinedGroups() = scope.launch {
        repository.setPredefinedGroups(getApplication())
    }

    // Run the alerts synchronous to avoid duplicate alerts.
// Each alerts gets send, and then removed/updated from the DB. If the alerts are send non-blocking,
// the alert is still valid for some time and gets send multiple times.
    fun updateAlertAboveSync(
        symbol: String,
        alertAbove: Double,
        alertAboveNote: String
    ) {
        runBlocking {
            withContext(Dispatchers.IO) {
                if (symbol.isNotEmpty()) {
                    repository.updateAlertAbove(
                        symbol.uppercase(Locale.ROOT), alertAbove, alertAboveNote
                    )
                }
            }
        }
    }

    fun updateAlertBelowSync(
        symbol: String,
        alertBelow: Double,
        alertBelowNote: String
    ) {
        runBlocking {
            withContext(Dispatchers.IO) {
                if (symbol.isNotEmpty()) {
                    repository.updateAlertBelow(
                        symbol.uppercase(Locale.ROOT), alertBelow, alertBelowNote
                    )
                }
            }
        }
    }

    // Do not use the Sync version of these functions for the import.
    private fun updateAlertAbove(
        symbol: String,
        alertAbove: Double,
        alertAboveNote: String
    ) = scope.launch {
        if (symbol.isNotEmpty()) {
            repository.updateAlertAbove(symbol.uppercase(Locale.ROOT), alertAbove, alertAboveNote)
        }
    }

    private fun updateAlertBelow(
        symbol: String,
        alertBelow: Double,
        alertBelowNote: String
    ) = scope.launch {
        if (symbol.isNotEmpty()) {
            repository.updateAlertBelow(symbol.uppercase(Locale.ROOT), alertBelow, alertBelowNote)
        }
    }

    fun updateNote(
        symbol: String,
        note: String
    ) = scope.launch {
        if (symbol.isNotEmpty()) {
            repository.updateNote(symbol.uppercase(Locale.ROOT), note)
        }
    }

    fun updateDividendNote(
        symbol: String,
        note: String
    ) = scope.launch {
        if (symbol.isNotEmpty()) {
            repository.updateDividendNote(symbol.uppercase(Locale.ROOT), note)
        }
    }

    fun updateAnnualDividendRate(
        symbol: String,
        annualDividendRate: Double
    ) = scope.launch {
        if (symbol.isNotEmpty()) {
            repository.updateAnnualDividendRate(symbol.uppercase(Locale.ROOT), annualDividendRate)
        }
    }

    fun getStockDBdataSync(symbol: String): StockDBdata {
        var stockDBdata: StockDBdata = StockDBdata(symbol = symbol)
        runBlocking {
            withContext(Dispatchers.IO) {
                if (symbol.isNotEmpty()) {
                    stockDBdata = repository.getStockDBdata(symbol.uppercase(Locale.ROOT))
                }
            }
        }

        return stockDBdata
    }

    fun setPortfolio(
        symbol: String,
        portfolio: String
    ) = scope.launch {
        repository.setPortfolio(symbol, portfolio)
    }

    fun getTypeSync(symbol: String)
            : Int {
        var type: Int?
        runBlocking {
            withContext(Dispatchers.IO) {
                type = repository.getType(symbol)
            }
        }

        return type ?: 0
    }

    fun setType(
        symbol: String,
        type: Int
    ) = scope.launch {
        repository.setType(symbol, type)
    }

    private fun setData(
        symbol: String,
        data: String
    ) = scope.launch {
        repository.setData(symbol, data)
    }

    fun updatePortfolio(
        portfolioOld: String,
        portfolioNew: String
    ) = scope.launch {
        repository.updatePortfolio(portfolioOld, portfolioNew)
    }

    fun updateAccount(
        accountOld: String,
        accountNew: String
    ) = scope.launch {
        repository.updateAccount(accountOld, accountNew)
    }

    fun getGroupSync(color: Int)
            : Group {
        var group: Group?
        runBlocking {
            withContext(Dispatchers.IO) {
                group = repository.getGroup(color)
            }
        }

        return group ?: Group(color = color, name = "")
    }

    fun getGroupsSync(): List<Group> {
        var groups: List<Group> = emptyList()
        runBlocking {
            withContext(Dispatchers.IO) {
                groups = repository.getGroups()
                    .sortedBy { group ->
                        group.name.lowercase(Locale.ROOT)
                    }
            }
        }

        return groups
    }

    suspend fun getStockData(symbol: StockSymbol): OnlineMarketData? {
        return stockMarketDataRepository.getStockData(
            StockSymbol(
                symbol = symbol.symbol.uppercase(Locale.ROOT),
                symbol.type
            )
        )
    }

    private suspend fun getStockData()
            : MarketDataResult {
        // When the stock data activity is selected, query only the selected symbol.
        val selectedSymbol = SharedRepository.selectedSymbol

        val symbols: List<StockSymbol> = if (selectedSymbol.symbol.isNotEmpty()) {
            listOf(selectedSymbol)
        } else {
//      // Get all stocks from the DB.
//      repository.getStockSymbols()
//          .filter { symbol ->
//            if (portfolioSymbols.isNotEmpty()) {
//              portfolioSymbols.contains(symbol)
//            } else {
//              false
//            }
//          }
            val portfolioSymbolsCopy = portfolioSymbols
            portfolioSymbolsCopy.toList()
        }

        val symbollistStr = symbols.map { symbol ->
            symbol.symbol
        }.take(5)
            .joinToString(",")

        when {
            symbols.size > 5 -> {
                logDebugAsync("Query ${symbols.size} stocks: ${symbollistStr},...")
            }

            symbols.size == 1 -> {
                logDebugAsync("Query 1 stock: $symbollistStr")
            }

            symbols.isEmpty() -> {
                logDebugAsync("No stocks to query.")
            }

            else -> {
                logDebugAsync("Query ${symbols.size} stocks: $symbollistStr")
            }
        }

//   Log.d("Handlers", "Call stockMarketDataRepository.getStockData()")
//   logDebugAsync("update online data getStockData")
        return stockMarketDataRepository.getStockData(symbols)
    }

    fun setName(
        symbol: String,
        name: String
    ) = scope.launch {
        repository.setName(symbol, name)
    }

    fun setGroup(
        color: Int,
        name: String
    ) = scope.launch {
        repository.setGroup(color, name)
    }

    fun setGroup(
        symbol: String,
        name: String,
        color: Int
    ) = scope.launch {
        repository.setGroup(symbol.uppercase(Locale.ROOT), name, color)
    }

    fun updateGroupName(
        color: Int,
        name: String
    ) = scope.launch {
        repository.updateGroupName(color, name)
    }

    fun renameSymbolSync(
        symbolOld: String,
        symbolNew: String
    ): Boolean {
        var renamed: Boolean = false
        runBlocking {
            withContext(Dispatchers.IO) {
                renamed = repository.renameSymbol(symbolOld, symbolNew)
            }
        }

        return renamed
    }

    fun updateStockGroupColors(
        colorOld: Int,
        colorNew: Int
    ) = scope.launch {
        repository.updateStockGroupColors(colorOld, colorNew)
    }

    fun setStockGroupColor(
        symbol: String,
        color: Int
    ) = scope.launch {
        repository.setStockGroupColor(symbol, color)
    }

    fun setMarker(
        symbol: String,
        marker: Int
    ) = scope.launch {
        repository.setMarker(symbol, marker)
    }

    fun deleteGroup(color: Int) = scope.launch {
        repository.deleteGroup(color)
    }

    fun deleteAllGroups() = scope.launch {
        repository.deleteAllGroups()
    }

    fun getAssetsSync(symbol: String)
            : Assets
    ? {
        var assets: Assets? = null
        runBlocking {
            withContext(Dispatchers.IO) {
                if (symbol.isNotEmpty()) {
                    assets = repository.getAssets(symbol.uppercase(Locale.ROOT))
                }
            }
        }

        return assets
    }

    fun addAsset(asset: Asset) = scope.launch {

        repository.addAsset(asset)

//    val stockOptionData = parseStockOption(asset.symbol)
//    if (stockOptionData.expirationDate == 0L && stockOptionData.strikePrice == 0.0) {
//      repository.addAsset(asset)
//    } else {
//      repository.addAsset(
//          Asset(
//              symbol = asset.symbol,
//              quantity = asset.quantity,
//              price = stockOptionData.strikePrice,
//              type = asset.type,
//              note = asset.note,
//              date = asset.date,
//              sharesPerQuantity = stockOptionData.sharesPerOption,
//              expirationDate = stockOptionData.expirationDate,
//              premium = asset.premium,
//              fee = asset.fee,
//              )
//      )
//    }
    }

    fun moveAsset(fromAsset: Asset, toAsset: Asset) = scope.launch {
        repository.moveAsset(fromAsset, toAsset)
    }

    fun addEvent(event: Event) = scope.launch {
        repository.addEvent(event)
    }

    fun addAsset(
        symbol: String,
        quantity: Double,
        price: Double,
        date: Long
    ) = scope.launch {
        if (symbol.isNotEmpty()) {
            repository.addAsset(
                Asset(
                    symbol = symbol.uppercase(Locale.ROOT),
                    quantity = quantity,
                    price = price,
                    date = date
                )
            )
        }
    }

    fun deleteStock(symbol: String) = scope.launch {
        if (symbol.isNotEmpty()) {
            repository.deleteStock(symbol.uppercase(Locale.ROOT))
        }
    }

    fun deleteAsset(asset: Asset) =
        scope.launch {
            repository.deleteAsset(asset)
        }

    fun deleteAssets(symbol: String) =
        scope.launch {
            repository.deleteAssets(symbol.uppercase(Locale.ROOT))
        }

    fun updateAsset2(
        assetOld: Asset,
        assetNew: Asset
    ) =
        scope.launch {
            repository.updateAsset2(assetOld, assetNew)
        }

    fun updateAssets(
        symbol: String,
        assets: List<Asset>
    ) =
        scope.launch {
            repository.updateAssets(symbol = symbol.uppercase(Locale.ROOT), assets = assets)
        }

    fun getStockDBLiveData(symbol: String): LiveData<StockDBdata> {
        return repository.getStockDBLiveData(symbol.uppercase(Locale.ROOT))
    }

    fun getAssetsLiveData(symbol: String)
            : LiveData<Assets> {
        return repository.getAssetsLiveData(symbol.uppercase(Locale.ROOT))
    }

    fun getEventsLiveData(symbol: String): LiveData<Events> {
        return repository.getEventsLiveData(symbol.uppercase(Locale.ROOT))
    }

    fun getDividendsLiveData(symbol: String)
            : LiveData<Dividends> {
        return repository.getDividendsLiveData(symbol.uppercase(Locale.ROOT))
    }

    fun updateEvent2(
        eventOld: Event,
        eventNew: Event
    ) =
        scope.launch {
            repository.updateEvent2(eventOld, eventNew)
        }

    private fun updateEvents(
        symbol: String,
        events: List<Event>
    ) =
        scope.launch {
            repository.updateEvents(symbol = symbol.uppercase(Locale.ROOT), events = events)
        }

    fun updateDividend2(
        dividendOld: Dividend,
        dividendNew: Dividend
    ) =
        scope.launch {
            repository.updateDividend2(dividendOld, dividendNew)
        }

    fun updateDividend(dividend: Dividend) = scope.launch {
        repository.updateDividend(dividend)
    }

    fun deleteDividend(dividend: Dividend) = scope.launch {
        repository.deleteDividend(dividend)
    }

    fun deleteDividends(symbol: String) = scope.launch {
        repository.deleteDividends(symbol.uppercase(Locale.ROOT))
    }

    fun deleteEvent(event: Event) =
        scope.launch {
            repository.deleteEvent(event)
        }

    fun deleteEvents(symbol: String) =
        scope.launch {
            repository.deleteEvents(symbol.uppercase(Locale.ROOT))
        }

    fun resetPortfolios() {
        logDebug("Reset all portfolios.")

        SharedRepository.selectedPortfolio.postValue("")
        SharedRepository.portfolios.postValue(hashSetOf(""))

        scope.launch {
            repository.resetPortfolios()
        }
    }

    fun deleteAll() {
        logDebug("Deleted all data.")

        SharedRepository.selectedPortfolio.postValue("")
        SharedRepository.portfolios.postValue(hashSetOf(""))

        scope.launch {
            repository.deleteAll()
        }
    }

    fun logDebug(value: String) {
        val time: ZonedDateTime = ZonedDateTime.now()
        val t = time.format(
            DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
        ) ?: time.toString()
        SharedRepository.debugList.add(DebugData("${t}>", value))
        SharedRepository.debugLiveData.value = SharedRepository.debugList
    }

    private suspend fun logDebugAsync(value: String) {
        // Dispatchers.IO does not work here
        withContext(Dispatchers.Main) {
            synchronized(SharedRepository.debugList) {
                logDebug(value)
            }
        }
    }
}
