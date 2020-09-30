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

package com.thecloudsite.stockroom.database

import android.content.Context
import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.thecloudsite.stockroom.R.array
import com.thecloudsite.stockroom.utils.epsilon

@Dao
interface StockRoomDao {

  // LiveData is a data holder class that can be observed within a given lifecycle.
  // Always holds/caches latest version of data. Notifies its active observers when the
  // data has changed. Since we are getting all the contents of the database,
  // we are notified whenever any of the database contents have changed.
  //@Query("SELECT * FROM stock_table ORDER BY symbol ASC")
  @Query("SELECT symbol FROM stock_table")
  fun getStockSymbols(): List<String>

  @Query("SELECT * FROM stock_table")
  fun getAll(): LiveData<List<StockDBdata>>

  @Query("DELETE FROM stock_table WHERE symbol = :symbol")
  fun deleteSymbol(symbol: String)

  @Transaction
  fun deleteStock(symbol: String) {
    deleteAssets(symbol)
    deleteEvents(symbol)
    deleteDividends(symbol)
    deleteSymbol(symbol)
  }

  @Transaction
  fun deleteAll() {
    deleteAllStockTable()
    deleteAllGroupTable()
    deleteAllAssetTable()
    deleteAllEventTable()
    deleteAllDividendTable()
  }

  @Query("DELETE FROM stock_table")
  fun deleteAllStockTable()

  @Query("DELETE FROM group_table")
  fun deleteAllGroupTable()

  @Query("DELETE FROM asset_table")
  fun deleteAllAssetTable()

  @Query("DELETE FROM event_table")
  fun deleteAllEventTable()

  @Query("DELETE FROM dividend_table")
  fun deleteAllDividendTable()

  fun setPredefinedGroups(context: Context) {
    val groups: MutableList<Group> = mutableListOf()
    groups.add(Group(color = Color.GREEN, name = "Kaufen"))
    groups.add(Group(color = Color.RED, name = "Verkaufen"))
    groups.add(Group(color = Color.BLUE, name = "Beobachten"))
    groups.add(Group(color = Color.MAGENTA, name = "Verschenken"))
    groups.add(Group(color = Color.YELLOW, name = "Die Katze fragen"))
    groups.add(Group(color = Color.GRAY, name = "Nicht mehr danach gucken"))
    groups.add(Group(color = Color.rgb(173, 216, 230), name = "Noch mehr kaufen"))
    groups.add(Group(color = Color.rgb(0, 191, 255), name = "Gruppe 1"))
    groups.add(Group(color = Color.rgb(160, 82, 45), name = "Gruppe 2"))
    groups.add(Group(color = Color.rgb(72, 209, 204), name = "Gruppe 3"))
    groups.add(Group(color = Color.BLACK, name = "Gruppe 4"))
    groups.add(Group(color = Color.rgb(255, 127, 39), name = "Gruppe 5"))

    val stringList = context.resources.getStringArray(array.predefined_groups)
    val size = minOf(groups.size, stringList.size)
    for (i: Int in 0 until size) {
      groups[i].name = stringList[i]
    }

    setGroups(groups)
  }

  // StockDBdata
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun insertStockDBdata(stockDBdata: StockDBdata)

  @Transaction
  fun insert(stockDBdata: StockDBdata) {
    val stockData = getStockDBdata(stockDBdata.symbol)

    if (stockData != null) {
      // Keep old values if defaults are used.
      if (stockDBdata.groupColor == 0) {
        stockDBdata.groupColor = stockData.groupColor
      }
      if (stockDBdata.alertBelow == 0.0) {
        stockDBdata.alertBelow = stockData.alertBelow
      }
      if (stockDBdata.alertAbove == 0.0) {
        stockDBdata.alertAbove = stockData.alertAbove
      }
      if (stockDBdata.data.isEmpty()) {
        stockDBdata.data = stockData.data
      }
      if (stockDBdata.notes.isEmpty()) {
        stockDBdata.notes = stockData.notes
      }
      if (stockDBdata.dividendNotes.isEmpty()) {
        stockDBdata.dividendNotes = stockData.dividendNotes
      }
      if (stockDBdata.portfolio.isEmpty()) {
        stockDBdata.portfolio = stockData.portfolio
      }
    }

    insertStockDBdata(stockDBdata)
  }

  @Query("SELECT * FROM stock_table WHERE symbol = :symbol")
  fun getStockDBdata(symbol: String): StockDBdata

  @Query("SELECT * FROM group_table")
  fun getGroups(): List<Group>

  @Query("SELECT * FROM group_table WHERE color = :color")
  fun getGroup(color: Int): Group

  @Query("SELECT * FROM group_table")
  fun getAllGroupTableLiveData(): LiveData<List<Group>>

  @Query("UPDATE group_table SET name = :name WHERE color = :color")
  fun updateGroupName(
    color: Int,
    name: String
  )

  @Query("UPDATE stock_table SET group_color = :colorNew WHERE group_color = :colorOld")
  fun updateStockGroupColors(
    colorOld: Int,
    colorNew: Int
  )

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun setGroup(group: Group)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun setGroups(groups: List<Group>)

  @Query("DELETE FROM group_table WHERE color = :color")
  fun deleteGroup(color: Int)

  @Query("SELECT * FROM stock_table")
  fun getAllProperties(): LiveData<List<StockDBdata>>

  // With @Transaction randomly the stock group does not get set?
  //@Transaction
  fun setGroup(
    symbol: String,
    name: String,
    color: Int
  ) {
    // updateGroup does not add, only update existing value
    // setGroup(Group()) update or adds if not exist
    if (color != 0) {
      setGroup(Group(color = color, name = name))
    }
    setStockGroupColor(symbol = symbol, color = color)
  }

  @Query("UPDATE stock_table SET portfolio = :portfolio")
  fun resetPortfolios(portfolio: String = "")

  @Query("UPDATE stock_table SET portfolio = :portfolio WHERE symbol = :symbol")
  fun setPortfolio(
    symbol: String,
    portfolio: String
  )

  @Query("UPDATE stock_table SET data = :data WHERE symbol = :symbol")
  fun setData(
    symbol: String,
    data: String
  )

  @Query("UPDATE stock_table SET portfolio = :portfolioNew WHERE portfolio = :portfolioOld")
  fun updatePortfolio(
    portfolioOld: String,
    portfolioNew: String
  )

  @Query("UPDATE stock_table SET group_color = :color WHERE symbol = :symbol")
  fun setStockGroupColor(
    symbol: String,
    color: Int
  )

  @Query("UPDATE stock_table SET alert_above = :alertAbove WHERE symbol = :symbol")
  fun updateAlertAbove(
    symbol: String,
    alertAbove: Double
  )

  @Query("UPDATE stock_table SET alert_below = :alertBelow WHERE symbol = :symbol")
  fun updateAlertBelow(
    symbol: String,
    alertBelow: Double
  )

  @Query("UPDATE stock_table SET notes = :notes WHERE symbol = :symbol")
  fun updateNotes(
    symbol: String,
    notes: String
  )

  @Query("UPDATE stock_table SET dividend_notes = :notes WHERE symbol = :symbol")
  fun updateDividendNotes(
    symbol: String,
    notes: String
  )

  // Assets
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun addAsset(asset: Asset)

  // id must match to delete the entry
  @Delete
  fun deleteAsset(asset: Asset)

  @Transaction
  @Query("SELECT * FROM stock_table")
  fun getAllAssetsLiveData(): LiveData<List<Assets>>
//    fun getAllAssets(): LiveData<List<Assets>> = getAllAssets1().getDistinct()

  @Query("SELECT * FROM asset_table")
  fun getAllAssetTableLiveData(): LiveData<List<Asset>>

  @Transaction
  @Query("SELECT * FROM stock_table WHERE symbol = :symbol")
  fun getAssetsLiveData(symbol: String): LiveData<Assets>

  @Transaction
  @Query("SELECT * FROM stock_table WHERE symbol = :symbol")
  fun getAssets(symbol: String): Assets
//    fun getAllAssets(): LiveData<List<Assets>> = getAllAssets1().getDistinct()

  @Query("DELETE FROM asset_table WHERE symbol = :symbol AND shares = :shares AND price = :price")
  fun deleteAsset(
    symbol: String,
    shares: Double,
    price: Double
  )

  @Query("DELETE FROM asset_table WHERE symbol = :symbol")
  fun deleteAssets(symbol: String)

  /*
  @Transaction
  fun updateAsset2(
    assetOld: Asset,
    assetNew: Asset
  ) {
    deleteAsset(
        assetOld.symbol,
        assetOld.shares,
        assetOld.price
    )
    addAsset(assetNew)
  }
  */

  @Transaction
  fun updateAsset2(
    assetOld: Asset,
    assetNew: Asset
  ) {
    // delete the exact asset including the id because duplicate entries are valid
    // dividends delete entries without matching the id to remove all duplicates
    deleteAsset(assetOld)
    addAsset(assetNew)
  }

  @Transaction
  fun updateAssets(
    symbol: String,
    assets: List<Asset>
  ) {
    deleteAssets(symbol)
    assets.forEach { asset ->
      if (asset.shares > epsilon && asset.price > epsilon) {
        addAsset(asset)
      }
    }
  }

  // Events
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun addEvent(event: Event)

  @Transaction
  @Query("SELECT * FROM stock_table")
  fun getAllEventsLiveData(): LiveData<List<Events>>
//    fun getAllEvents(): LiveData<List<Events>> = getAllEvents1().getDistinct()

  @Query("SELECT * FROM event_table")
  fun getAllEventTableLiveData(): LiveData<List<Event>>

  @Transaction
  @Query("SELECT * FROM stock_table WHERE symbol = :symbol")
  fun getEventsLiveData(symbol: String): LiveData<Events>

  @Transaction
  @Query("SELECT * FROM stock_table WHERE symbol = :symbol")
  fun getEvents(symbol: String): Events

  // id must match to delete the entry
  @Delete
  fun deleteEvent(event: Event)

//  @Query(
//      "DELETE FROM event_table WHERE symbol = :symbol AND title = :title AND note = :note AND datetime = :datetime"
//  )
//  fun deleteEvent(
//    symbol: String,
//    title: String,
//    note: String,
//    datetime: Long
//  )

  @Query("DELETE FROM event_table WHERE symbol = :symbol")
  fun deleteEvents(symbol: String)

  @Transaction
  fun updateEvent2(
    eventOld: Event,
    eventNew: Event
  ) {
    // delete the exact event including the id because duplicate entries are valid
    // dividends delete entries without matching the id to remove all duplicates
    deleteEvent(eventOld)
//    deleteEvent(
//        eventOld.symbol,
//        eventOld.title,
//        eventOld.note,
//        eventOld.datetime
//    )
    addEvent(eventNew)
  }

  @Transaction
  fun updateEvents(
    symbol: String,
    events: List<Event>
  ) {
    deleteEvents(symbol)
    events.forEach { event ->
      if (event.title.isNotEmpty()) {
        addEvent(event)
      }
    }
  }

//  @Query("SELECT * FROM stock_table WHERE symbol = :symbol")
//  fun getDividends(symbol: String): Dividends

  @Transaction
  @Query("SELECT * FROM stock_table")
  fun getAllDividendsLiveData(): LiveData<List<Dividends>>

  @Transaction
  @Query("SELECT * FROM dividend_table")
  fun getAllDividendTableLiveData(): LiveData<List<Dividend>>

  @Transaction
  @Query("SELECT * FROM stock_table WHERE symbol = :symbol")
  fun getDividendsLiveData(symbol: String): LiveData<Dividends>

  @Transaction
  fun updateDividend(dividend: Dividend) {
    deleteDividend(
        symbol = dividend.symbol,
        amount = dividend.amount,
        type = dividend.type,
        cycle = dividend.cycle,
        paydate = dividend.paydate,
        exdate = dividend.exdate
    )
    addDividend(dividend)
  }

  @Transaction
  fun updateDividend2(
    dividendOld: Dividend,
    dividendNew: Dividend
  ) {
    deleteDividend(
        symbol = dividendOld.symbol,
        amount = dividendOld.amount,
        type = dividendOld.type,
        cycle = dividendOld.cycle,
        paydate = dividendOld.paydate,
        exdate = dividendOld.exdate
    )
    addDividend(dividendNew)
  }

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun addDividend(dividend: Dividend)

  // autogenerated id doesn't match
//  @Delete
//  fun deleteDividend(dividend: Dividend)

  @Query(
      "DELETE FROM dividend_table WHERE symbol = :symbol AND amount = :amount AND type = :type AND cycle = :cycle AND paydate = :paydate AND exdate = :exdate"
  )
  fun deleteDividend(
    symbol: String,
    amount: Double,
    type: Int,
    cycle: Int,
    paydate: Long,
    exdate: Long
  )

  @Query("DELETE FROM dividend_table WHERE symbol = :symbol")
  fun deleteDividends(symbol: String)

  @Transaction
  @Query("SELECT * FROM stock_table WHERE symbol = :symbol")
  fun getDividends(symbol: String): Dividends

  @Transaction
  @Query("SELECT * FROM dividend_table ORDER BY amount ASC")
  fun allDividends(): LiveData<List<Dividend>>
}
