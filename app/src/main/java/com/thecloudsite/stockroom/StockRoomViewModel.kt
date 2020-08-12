/*
 * Copyright (C) 2017 Google Inc.
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
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.thecloudsite.stockroom.StockRoomViewModel.AlertData
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.internal.toImmutableList
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.coroutines.CoroutineContext

data class AssetJson(
  var shares: Float,
  val price: Float
)

data class EventJson(
  val type: Int,
  val title: String,
  val note: String,
  val datetime: Long
)

data class StockItemJson
(
  var symbol: String,
  val portfolio: String,
  val data: String,
  val groupColor: Int,
  val groupName: String,
  val notes: String,
  val alertAbove: Float,
  val alertBelow: Float,
  var assets: List<AssetJson>,
  var events: List<EventJson>
)

object SharedHandler {
  val deleteStockHandler = MutableLiveData<String>()
}

object SharedRepository {
  val alertsData = MutableLiveData<List<AlertData>>()
  val alerts: LiveData<List<AlertData>>
    get() = alertsData

  val debugList = ArrayList<DebugData>()
  val debugLiveData = MutableLiveData<List<DebugData>>()
  val debugData: LiveData<List<DebugData>>
    get() = debugLiveData

  var postMarket: Boolean = true
  var notifications: Boolean = true

  var selectedPortfolio = MutableLiveData<String>("")
  val selectedPortfolioLiveData: LiveData<String>
    get() = selectedPortfolio

  var portfolios = MutableLiveData<HashSet<String>>()
  val portfoliosLiveData: LiveData<HashSet<String>>
    get() = portfolios
}

class StockRoomViewModel(application: Application) : AndroidViewModel(application) {

  private val repository: StockRoomRepository
  private val stockMarketDataRepository: StockMarketDataRepository =
    StockMarketDataRepository(StockApiFactory.yahooApi)

  // Using LiveData and caching returns has several benefits:
  // - We can put an observer on the data (instead of polling for changes) and only update the
  //   the UI when the data actually changes.
  // - Repository is completely separated from the UI through the ViewModel.
  val onlineMarketDataList: LiveData<List<OnlineMarketData>>
  val allProperties: LiveData<List<StockDBdata>>
  private val allAssets: LiveData<List<Assets>>
  val allEvents: LiveData<List<Events>>
  val allAssetTable: LiveData<List<Asset>>
  val allEventTable: LiveData<List<Event>>
  val allGroupTable: LiveData<List<Group>>

  // allStockItems -> allMediatorData -> allData(_data->dataStore) = allAssets + onlineMarketData
  val allStockItems: LiveData<StockItemSet>

  private var portfolioSymbols: HashSet<String> = HashSet<String>()

  private val dataStore = StockItemSet()
  private val _dataStore = MutableLiveData<StockItemSet>()
  private val allData: LiveData<StockItemSet>
    get() = _dataStore

  private val allMediatorData = MediatorLiveData<StockItemSet>()

  // Settings.
  private val sharedPreferences =
    PreferenceManager.getDefaultSharedPreferences(application /* Activity context */)

  private var postMarket: Boolean = sharedPreferences.getBoolean("postmarket", true)
  private var notifications: Boolean = sharedPreferences.getBoolean("notifications", true)

  private val settingSortmode = "SettingSortmode"

  var sorting = MutableLiveData<SortMode>()
  val sortingLiveData: LiveData<SortMode>
    get() = sorting

  private var _sortMode: SortMode = SortMode.values()[sharedPreferences.getInt(
      settingSortmode, SortMode.ByName.value
  )]
  private var sortMode: SortMode
    get() {
      return _sortMode
    }
    set(value) {
      _sortMode = value
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
    allProperties = repository.allProperties
    allAssets = repository.allAssets
    allEvents = repository.allEvents
    allAssetTable = repository.allAssetTable
    allEventTable = repository.allEventTable
    allGroupTable = repository.allGroupTable

    onlineMarketDataList = stockMarketDataRepository.onlineMarketDataList
    allStockItems = getMediatorData()

    // sharedPreferences.getBoolean("postmarket", true) doesn't work here anymore?
    SharedRepository.postMarket = postMarket
    SharedRepository.notifications = notifications
  }

  suspend fun getOnlineData(prevOnlineDataDelay: Long): Pair<Long, MarketState> {
    val stockdataResult = getStockData()
    val marketState = stockdataResult.first
    val errorMsg = stockdataResult.second

    // Set the delay depending on the market state.
    val onlineDataDelay = when (marketState) {
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
      MarketState.UNKNOWN -> {
        60 * 1000L
      }
    }

    if (errorMsg.isEmpty()) {
      val marketStateStr = when (marketState) {
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
      }

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

  fun updateOnlineDataManually(onlineMsg: String = "") {
    if (isOnline(getApplication())) {
      if (onlineMsg.isNotEmpty()) {
        logDebug(onlineMsg)
      }

      scope.launch {
        val stockDataResult = getStockData()
        if (stockDataResult.second.isNotEmpty()) {
          logDebugAsync("Error getting online data: ${stockDataResult.second}")
        }
      }
    } else {
      logDebug("Network is offline. No online data.")
    }
  }

  private fun updateAll() {
    allMediatorData.value = process(allData.value, true)
    if (SharedRepository.notifications) {
      processNotifications(allMediatorData.value)
    }
  }

  private fun getMediatorData(): LiveData<StockItemSet> {

    val liveDataProperties = allProperties
    val liveDataAssets = allAssets
    val liveDataEvents = allEvents
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
        if (liveDataOnline.value != null) {
          updateFromOnline(liveDataOnline.value!!)
        }
        allMediatorData.value = process(allData.value, true)
        updateOnlineDataManually()
      }
    }

    allMediatorData.addSource(liveDataProperties) { value ->
      if (value != null) {
        updateStockDataFromDB(value)
        updateOnlineDataManually()
        allMediatorData.value = process(allData.value, true)
      }
    }

    allMediatorData.addSource(liveDataAssets) { value ->
      if (value != null) {
        updateAssetsFromDB(value)
        allMediatorData.value = process(allData.value, false)
      }
    }

    allMediatorData.addSource(liveDataEvents) { value ->
      if (value != null) {
        updateEventsFromDB(value)
        allMediatorData.value = process(allData.value, false)
      }
    }

    allMediatorData.addSource(liveDataOnline) { value ->
      if (value != null) {
        updateFromOnline(value)
        allMediatorData.value = process(allData.value, true)
      }
    }

    return allMediatorData
  }

  private fun updateStockDataFromDB(stockDBdata: List<StockDBdata>) {
    synchronized(dataStore)
    {
      val stockDBdataPortfolios: HashSet<String> = hashSetOf("") // add Standard Portfolio
      val usedPortfolioSymbols: HashSet<String> = HashSet<String>()

      // Use only symbols matching the selected portfolio.
      val portfolio = SharedRepository.selectedPortfolio.value ?: ""
      val portfolioData = stockDBdata.filter { data ->
        data.portfolio == portfolio
      }

      portfolioData.forEach { data ->
        val symbol = data.symbol
        usedPortfolioSymbols.add(symbol)

        val dataStoreItem =
          dataStore.stockItems.find { ds ->
            symbol == ds.stockDBdata.symbol
          }

        if (dataStoreItem != null) {
          dataStoreItem.stockDBdata = data
        } else {
          dataStore.stockItems.add(
              StockItem(
                  onlineMarketData = OnlineMarketData(symbol = data.symbol),
                  stockDBdata = data,
                  assets = emptyList(),
                  events = emptyList()
              )
          )
        }
      }

      // only load online data for symbols in the portfolio set
      portfolioSymbols = usedPortfolioSymbols

      // Remove the item from the dataStore because it is not in the portfolio or was deleted from the DB.
      dataStore.stockItems.removeIf {
        // Remove if item is not found in the DB.
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

  private fun updateAssetsFromDB(assets: List<Assets>) {
    synchronized(dataStore)
    {
      // don't wait for valid online data if offline
      if (!dataStore.dataValid && !isOnline(getApplication())) {
        dataStore.dataValid = true
      }

      assets.forEach { asset ->
        val symbol = asset.stockDBdata.symbol
        val dataStoreItem =
          dataStore.stockItems.find { ds ->
            symbol == ds.stockDBdata.symbol
          }

        if (dataStoreItem != null) {
          dataStoreItem.assets = asset.assets
        }
      }
    }

    _dataStore.value = dataStore
  }

  private fun updateEventsFromDB(events: List<Events>) {
    synchronized(dataStore)
    {
      events.forEach { event ->
        val symbol = event.stockDBdata.symbol
        val dataStoreItem =
          dataStore.stockItems.find { ds ->
            symbol == ds.stockDBdata.symbol
          }

        if (dataStoreItem != null) {
          dataStoreItem.events = event.events
        }
      }
    }

    _dataStore.value = dataStore
  }

  private fun updateFromOnline(onlineMarketDataList: List<OnlineMarketData>) {
    synchronized(dataStore)
    {
      //val postMarket: Boolean = sharedPreferences.getBoolean("postmarket", true)

      // Work off a read-only copy to avoid the java.util.ConcurrentModificationException
      // while the next update.

      dataStore.dataValid = true

      onlineMarketDataList.toImmutableList()
          .forEach { onlineMarketDataItem ->
            val symbol = onlineMarketDataItem.symbol

            val dataStoreItem =
              dataStore.stockItems.find { ds ->
                symbol == ds.stockDBdata.symbol
              }

            if (dataStoreItem != null) {
              dataStoreItem.onlineMarketData = onlineMarketDataItem

/*
              if (postMarket) {
                if ((onlineMarketDataItem.marketState == MarketState.POST.value
                        || onlineMarketDataItem.marketState == MarketState.POSTPOST.value
                        || onlineMarketDataItem.marketState == MarketState.PREPRE.value
                        || onlineMarketDataItem.marketState == MarketState.CLOSED.value)
                    && onlineMarketDataItem.postMarketPrice > 0f
                ) {
                  dataStoreItem.onlineMarketData.marketPrice =
                    onlineMarketDataItem.postMarketPrice
                  dataStoreItem.onlineMarketData.marketChange =
                    onlineMarketDataItem.postMarketChange
                  dataStoreItem.onlineMarketData.marketChangePercent =
                    onlineMarketDataItem.postMarketChangePercent
                } else
                  if ((onlineMarketDataItem.marketState == MarketState.PRE.value)
                      && onlineMarketDataItem.preMarketPrice > 0f
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
    var name: String,
    var alertAbove: Float,
    var alertBelow: Float,
    var marketPrice: Float
  )

  private fun processNotifications(stockItemSet: StockItemSet?) {
    val newAlerts: MutableList<AlertData> = mutableListOf()

    stockItemSet?.stockItems?.forEach { stockItem ->
      //     allStockItems.value?.forEach { stockItem ->
      val marketPrice = stockItem.onlineMarketData.marketPrice

      if (marketPrice > 0f && SharedRepository.alertsData.value != null) {
        if (SharedRepository.alertsData.value!!.find {
              it.symbol == stockItem.stockDBdata.symbol
            } == null) {
          if (stockItem.stockDBdata.alertAbove > 0f && stockItem.stockDBdata.alertAbove < marketPrice) {
            val alertDataAbove = AlertData(
                symbol = stockItem.stockDBdata.symbol,
                name = stockItem.onlineMarketData.name,
                alertAbove = stockItem.stockDBdata.alertAbove,
                alertBelow = 0f,
                marketPrice = marketPrice
            )
            newAlerts.add(alertDataAbove)
          } else
            if (stockItem.stockDBdata.alertBelow > 0f && stockItem.stockDBdata.alertBelow > marketPrice) {
              val alertDataBelow = AlertData(
                  symbol = stockItem.stockDBdata.symbol,
                  name = stockItem.onlineMarketData.name,
                  alertAbove = 0f,
                  alertBelow = stockItem.stockDBdata.alertBelow,
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
    allMediatorData.value = process(allData.value, false)
  }

  fun sortMode() =
    sortMode

  private fun process(
    stockItemSet: StockItemSet?,
    runNotifications: Boolean
  ): StockItemSet? {

    if (stockItemSet != null) {
      if (runNotifications && SharedRepository.notifications) {
        processNotifications(stockItemSet)
      }

      return StockItemSet(
          dataValid = stockItemSet.dataValid,
          stockItems = sort(sortMode, stockItemSet).toMutableList()
      )
    }

    return stockItemSet
  }

  private fun sort(
    sortMode: SortMode,
    stockItemSet: StockItemSet?
  ): List<StockItem> =
    if (stockItemSet != null) {
      when (sortMode) {
        SortMode.ByName -> {
          stockItemSet.stockItems.sortedBy { item ->
            item.stockDBdata.symbol
          }
        }
        SortMode.ByAssets -> {
          stockItemSet.stockItems.sortedByDescending { item ->
            if (item.onlineMarketData.marketPrice > 0f) {
              item.assets.sumByDouble { it.shares.toDouble() * (item.onlineMarketData.marketPrice) }
            } else {
              item.assets.sumByDouble { it.shares.toDouble() * it.price }
            }
          }
        }
        SortMode.ByProfit -> {
          stockItemSet.stockItems.sortedByDescending { item ->
            if (item.onlineMarketData.marketPrice > 0f) {
              item.assets.sumByDouble { it.shares.toDouble() * (item.onlineMarketData.marketPrice - it.price) }
            } else {
              item.assets.sumByDouble { it.shares.toDouble() * it.price }
            }
          }
        }
        SortMode.ByChange -> {
          stockItemSet.stockItems.sortedByDescending { item ->
            item.onlineMarketData.marketChangePercent
          }
        }
        SortMode.ByDividend -> {
          stockItemSet.stockItems.sortedByDescending { item ->
            item.onlineMarketData.annualDividendYield
          }
        }
        SortMode.ByGroup -> {
          // Sort the group items alphabetically.
          stockItemSet.stockItems.sortedBy { item ->
            item.stockDBdata.symbol
          }
              .sortedBy { item ->
                item.stockDBdata.groupColor
              }
        }
        SortMode.ByUnsorted -> {
          stockItemSet.stockItems
        }
      }
    } else {
      emptyList()
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
      var groupColor: Int = 0
      var groupName: String = ""

      // get symbol
      val jsonObj: JSONObject = jsonArray[i] as JSONObject

      logDebug("Import JSONObject '$jsonObj'")

      val symbol = jsonObj.getString("symbol")
          .toUpperCase(Locale.ROOT)

      if (isValidSymbol(symbol)) {
        insert(symbol)
        imported++

        // get assets
        if (jsonObj.has("assets")) {
          val assetsObjArray = jsonObj.getJSONArray("assets")
          val assets: MutableList<Asset> = mutableListOf()

          for (j in 0 until assetsObjArray.length()) {
            val assetsObj: JSONObject = assetsObjArray[j] as JSONObject
            assets.add(
                Asset(
                    symbol = symbol,
                    shares = assetsObj.getDouble("shares")
                        .toFloat(),
                    price = assetsObj.getDouble("price")
                        .toFloat()
                )
            )
          }

          updateAssets(symbol = symbol, assets = assets)
        }

        if (jsonObj.has("position")) {
          val position = jsonObj.getJSONObject("position")
          val holdings = position.getJSONArray("holdings")
          val assets: MutableList<Asset> = mutableListOf()

          for (j in 0 until holdings.length()) {
            val holding: JSONObject = holdings[j] as JSONObject
            assets.add(
                Asset(
                    symbol = symbol,
                    shares = holding.getDouble("shares")
                        .toFloat(),
                    price = holding.getDouble("price")
                        .toFloat()
                )
            )
          }

          updateAssets(symbol = symbol, assets = assets)
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
            events.add(
                Event(
                    symbol = symbol,
                    datetime = eventsObj.getLong("datetime"),
                    note = eventsObj.getString("note"),
                    title = eventsObj.getString("title"),
                    type = eventsObj.getInt("type")
                )
            )
          }

          updateEvents(symbol, events)
        }

        // get properties
        if (jsonObj.has("portfolio")) {
          val portfolio = jsonObj.getString("portfolio")
              .trim()
          if (portfolio.isNotEmpty()) {
            setPortfolio(symbol = symbol, portfolio = portfolio)
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
          groupColor = jsonObj.getInt("groupColor")
          if (groupName.isNotEmpty()) {
            setGroup(symbol = symbol, color = groupColor, name = groupName)
          }
        }

        if (jsonObj.has("groupName")) {
          groupName = jsonObj.getString("groupName")
          if (groupColor != 0) {
            setGroup(symbol = symbol, color = groupColor, name = groupName)
          }
        }

        if (jsonObj.has("alertAbove")) {
          val alertAbove = jsonObj.getDouble("alertAbove")
              .toFloat()
          if (alertAbove > 0f) {
            updateAlertAbove(symbol, alertAbove)
          }
        }

        if (jsonObj.has("alertBelow")) {
          val alertBelow = jsonObj.getDouble("alertBelow")
              .toFloat()
          if (alertBelow > 0f) {
            updateAlertBelow(symbol, alertBelow)
          }
        }

        if (jsonObj.has("notes")) {
          val notes = jsonObj.getString("notes")
          if (notes.isNotEmpty()) {
            updateNotes(symbol, notes)
          }
        }

        if (jsonObj.has("properties")) {
          val properties = jsonObj.getJSONObject("properties")

          if (properties.has("alertAbove")) {
            val alertAbove = properties.getDouble("alertAbove")
                .toFloat()
            if (alertAbove > 0f) {
              updateAlertAbove(symbol, alertAbove)
            }
          }

          if (properties.has("alertBelow")) {
            val alertBelow = properties.getDouble("alertBelow")
                .toFloat()
            if (alertBelow > 0f) {
              updateAlertBelow(symbol, alertBelow)
            }
          }

          if (properties.has("notes")) {
            val notes = properties.getString("notes")
            if (notes.isNotEmpty()) {
              updateNotes(symbol, notes)
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

  private fun csvStrToFloat(value: String): Float {
    val s = value.replace("$", "")
    var f: Float
    try {
      f = s.toFloat()
      if (f == 0f) {
        val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
        f = numberFormat.parse(s)!!
            .toFloat()
      }
    } catch (e: Exception) {
      f = 0f
    }

    return f
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
      val msg = getApplication<Application>().getString(R.string.import_csv_symbolcolumn_error)
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

    // try columns "Cost Basis Per Share", "Price"
    val priceColumn = headerRow.indexOfFirst {
      it.compareTo(other = "Cost Basis Per Share", ignoreCase = true) == 0
          || it.compareTo(other = "Price", ignoreCase = true) == 0
    }

    val assetItems = HashMap<String, List<Asset>>()

    // skip header row
    rows.drop(1)
        .forEach { row ->
          val symbol = row[symbolColumn].toUpperCase(Locale.ROOT)
          val shares = if (sharesColumn >= 0) {
            csvStrToFloat(row[sharesColumn])
          } else {
            0f
          }
          val price = if (priceColumn >= 0) {
            csvStrToFloat(row[priceColumn])
          } else {
            0f
          }
          if (symbol.isNotEmpty()) {
            val asset = Asset(
                symbol = symbol,
                shares = shares,
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

    // Limit import to 100.
    var imported: Int = 0
    assetItems.forEach { (symbol, assets) ->
      if (imported < 100 && isValidSymbol(symbol)) {
        imported++
        insert(symbol)
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
    symbols.map { symbol ->
      symbol.replace("\"", "")
          .toUpperCase(Locale.ROOT)
    }
        // only a-z from 1..7 chars in length
//        .filter { symbol ->
//          symbol.matches("[A-Z]{1,7}".toRegex())
//        }
        .distinct()
        .take(100)
        .forEach { symbol ->
          if (isValidSymbol(symbol)) {
            insert(symbol)
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
    importListUri: Uri
  ) {
    try {
      context.contentResolver.openInputStream(importListUri)
          ?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
              val text: String = reader.readText()
              val fileName: String = importListUri.toString()

              when {
                fileName.endsWith(suffix = ".json", ignoreCase = true) -> {
                  importJSON(context, text)
                }
                fileName.endsWith(suffix = ".csv", ignoreCase = true) -> {
                  importCSV(context, text)
                }
                else -> {
                  importText(context, text)
                }
              }
            }
          }
    } catch (e: Exception) {
      Toast.makeText(
          context, getApplication<Application>().getString(R.string.import_error), Toast.LENGTH_LONG
      )
          .show()
      Log.d("Import JSON error", "Exception: $e")
      logDebug("Import JSON Exception: $e")
    }

    //updateAll()
  }

  private fun getAllDBdata(): List<StockItem> {
    val stockDBdata = allProperties.value!!
    val assets = allAssets.value!!
    val events = allEvents.value!!

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

      StockItem(
          onlineMarketData = OnlineMarketData(symbol = data.symbol),
          stockDBdata = data,
          assets = assetItem?.assets ?: emptyList(),
          events = eventItem?.events ?: emptyList()
      )
    }
  }

  fun exportList(
    context: Context,
    exportListUri: Uri
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

      StockItemJson(symbol = stockItem.stockDBdata.symbol,
          portfolio = stockItem.stockDBdata.portfolio,
          data = stockItem.stockDBdata.data,
          groupColor = stockItem.stockDBdata.groupColor,
          groupName = groupName,
          alertAbove = stockItem.stockDBdata.alertAbove,
          alertBelow = stockItem.stockDBdata.alertBelow,
          notes = stockItem.stockDBdata.notes,
          assets = stockItem.assets.map { asset ->
            AssetJson(shares = asset.shares, price = asset.price)
          },
          events = stockItem.events.map { event ->
            EventJson(
                type = event.type, title = event.title, note = event.note,
                datetime = event.datetime
            )
          }
      )
    }

    // Convert to a json string.
    val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    val jsonString = gson.toJson(stockItemsJson)

    // Write the json string.
    try {
      context.contentResolver.openOutputStream(exportListUri)
          ?.use { output ->
            output as FileOutputStream
            output.channel.truncate(0)
            output.write(jsonString.toByteArray())
          }

      val msg = getApplication<Application>().getString(R.string.export_msg, stockItemsJson.size)
      logDebug("Export JSON '$msg'")

      Toast.makeText(context, msg, Toast.LENGTH_LONG)
          .show()

    } catch (e: Exception) {
      //Toast.makeText(
      //    context, getApplication<Application>().getString(R.string.import_error), Toast.LENGTH_LONG
      //)
      //    .show()
      Log.d("Export JSON error", "Exception: $e")
      logDebug("Export JSON Exception: $e")
    }
  }

  /**
   * Launching a new coroutine to insert the data in a non-blocking way
   */
  // viewModelScope.launch(Dispatchers.IO) {
  fun insert(symbol: String) = scope.launch {
    if (symbol.isNotEmpty()) {
      repository.insert(StockDBdata(symbol.toUpperCase(Locale.ROOT)))
    }
  }

  fun setPredefinedGroups() = scope.launch {
    repository.setPredefinedGroups(getApplication())
  }

  // Run the alerts synchronous to avoid duplicate alerts when importing or any other fast insert.
  // Each alerts gets send, and then removed from the DB. If the alerts are send non-blocking,
  // the alert is still valid for some time and gets send multiple times.
  fun updateAlertAbove(
    symbol: String,
    alertAbove: Float
  ) {
    runBlocking {
      withContext(Dispatchers.IO) {
        if (symbol.isNotEmpty()) {
          repository.updateAlertAbove(symbol.toUpperCase(Locale.ROOT), alertAbove)
        }
      }
    }
  }

  fun updateAlertBelow(
    symbol: String,
    alertBelow: Float
  ) {
    runBlocking {
      withContext(Dispatchers.IO) {
        if (symbol.isNotEmpty()) {
          repository.updateAlertBelow(symbol.toUpperCase(Locale.ROOT), alertBelow)
        }
      }
    }
  }

  /*
   fun updateAlertAbove(
     symbol: String,
     alertAbove: Float
   ) = scope.launch {
     if (symbol.isNotEmpty()) {
       repository.updateAlertAbove(symbol.toUpperCase(Locale.ROOT), alertAbove)
     }
   }

   fun updateAlertBelow(
     symbol: String,
     alertBelow: Float
   ) = scope.launch {
     if (symbol.isNotEmpty()) {
       repository.updateAlertBelow(symbol.toUpperCase(Locale.ROOT), alertBelow)
     }
   }
    */

  fun updateNotes(
    symbol: String,
    notes: String
  ) = scope.launch {
    if (symbol.isNotEmpty()) {
      repository.updateNotes(symbol.toUpperCase(Locale.ROOT), notes)
    }
  }

  fun getStockDBdataSync(symbol: String): StockDBdata {
    var stockDBdata: StockDBdata = StockDBdata(symbol = symbol)
    runBlocking {
      withContext(Dispatchers.IO) {
        if (symbol.isNotEmpty()) {
          stockDBdata = repository.getStockDBdata(symbol.toUpperCase(Locale.ROOT))
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

  fun getGroupSync(color: Int): Group {
    var group: Group? = null
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
              group.name
            }
      }
    }

    return groups
  }

  // Get the colored menu entries for the groups.
  fun getGroupsMenuList(standardGroupName: String): List<SpannableString> {
    val menuStrings: MutableList<SpannableString> = mutableListOf()

    val space: String = "    "
    val spacePos = space.length
    val groups: MutableList<Group> = getGroupsSync().toMutableList()
    groups.add(Group(color = backgroundListColor, name = standardGroupName))
    for (i in groups.indices) {
      val grp: Group = groups[i]
      val s = SpannableString("$space  ${grp.name}")
      s.setSpan(BackgroundColorSpan(grp.color), 0, spacePos, 0)

      // backgroundListColor is light color, make the group name readable
      if (grp.color == backgroundListColor) {
        grp.color = Color.BLACK
      }

      s.setSpan(ForegroundColorSpan(grp.color), spacePos, s.length, 0)
      menuStrings.add(s)
    }

    return menuStrings
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
    repository.setGroup(symbol.toUpperCase(Locale.ROOT), name, color)
  }

  fun updateGroupName(
    color: Int,
    name: String
  ) = scope.launch {
    repository.updateGroupName(color, name)
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

  fun deleteGroup(color: Int) = scope.launch {
    repository.deleteGroup(color)
  }

  fun deleteAllGroups() = scope.launch {
    repository.deleteAllGroups()
  }

  fun getAssetsSync(symbol: String): Assets? {
    var assets: Assets? = null
    runBlocking {
      withContext(Dispatchers.IO) {
        if (symbol.isNotEmpty()) {
          assets = repository.getAssets(symbol.toUpperCase(Locale.ROOT))
        }
      }
    }

    return assets
  }

  fun addAsset(asset: Asset) = scope.launch {
    repository.addAsset(asset)
  }

  fun addEvent(event: Event) = scope.launch {
    repository.addEvent(event)
  }

  fun insert(
    symbol: String,
    shares: Float,
    price: Float
  ) = scope.launch {
    if (symbol.isNotEmpty()) {
      repository.addAsset(
          Asset(symbol = symbol.toUpperCase(Locale.ROOT), shares = shares, price = price)
      )
    }
  }

  fun delete(symbol: String) = scope.launch {
    if (symbol.isNotEmpty()) {
      val symbolUpper = symbol.toUpperCase(Locale.ROOT)
      repository.delete(symbolUpper)
      repository.deleteAssets(symbolUpper)
      repository.deleteEvents(symbolUpper)
    }
  }

  suspend fun getStockData(symbol: String): OnlineMarketData? {
    return stockMarketDataRepository.getStockData(symbol.toUpperCase(Locale.ROOT))
  }

  suspend fun getStockData(): Pair<MarketState, String> {
    // Get all stocks from the DB and filter the list to get only data for symbols of the portfolio.
    val symbols: List<String> = repository.getStockSymbols()
        .filter { symbol ->
          if (portfolioSymbols.isNotEmpty()) {
            portfolioSymbols.contains(symbol)
          } else {
            true
          }
        }
//   Log.d("Handlers", "Call stockMarketDataRepository.getStockData()")
//   logDebugAsync("update online data getStockData")
    return stockMarketDataRepository.getStockData(symbols)
  }

  fun deleteAsset(asset: Asset) =
    scope.launch {
      repository.deleteAsset(asset)
    }

  fun deleteAssets(symbol: String) =
    scope.launch {
      repository.deleteAssets(symbol.toUpperCase(Locale.ROOT))
    }

  fun updateAssets(
    symbol: String,
    assets: List<Asset>
  ) =
    scope.launch {
      repository.updateAssets(symbol = symbol.toUpperCase(Locale.ROOT), assets = assets)
    }

  fun getAssetsLiveData(symbol: String): LiveData<Assets> {
    return repository.getAssetsLiveData(symbol.toUpperCase(Locale.ROOT))
  }

  fun getEventsLiveData(symbol: String): LiveData<Events> {
    return repository.getEventsLiveData(symbol.toUpperCase(Locale.ROOT))
  }

  private fun updateEvents(
    symbol: String,
    events: List<Event>
  ) =
    scope.launch {
      repository.updateEvents(symbol = symbol.toUpperCase(Locale.ROOT), events = events)
    }

  fun deleteEvent(event: Event) =
    scope.launch {
      repository.deleteEvent(event)
    }

  fun deleteEvents(symbol: String) =
    scope.launch {
      repository.deleteEvents(symbol.toUpperCase(Locale.ROOT))
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
    val time: LocalDateTime = LocalDateTime.now()
    val t = time.format(
        DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
            .withZone(ZoneOffset.UTC)
    ) ?: time.toString()
    SharedRepository.debugList.add(DebugData("${t}>", value))
    SharedRepository.debugLiveData.value = SharedRepository.debugList
  }

  suspend fun logDebugAsync(value: String) {
    // Dispatchers.IO does not work ?
    withContext(Dispatchers.Main) {
      synchronized(SharedRepository.debugList) {
        logDebug(value)
      }
    }
  }
}
