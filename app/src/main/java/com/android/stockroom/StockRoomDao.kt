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

package com.android.stockroom

import android.content.Context
import android.graphics.Color
import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction

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
  fun delete(symbol: String)

  @Transaction
  fun deleteAll() {
    deleteAllStockTable()
    deleteAllAssetTable()
    deleteAllEventTable()
  }

  @Query("DELETE FROM stock_table")
  fun deleteAllStockTable()

  @Query("DELETE FROM group_table")
  fun deleteAllGroupTable()

  @Query("DELETE FROM asset_table")
  fun deleteAllAssetTable()

  @Query("DELETE FROM event_table")
  fun deleteAllEventTable()

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

    val stringList = context.resources.getStringArray(R.array.predefined_groups)
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
      // Keep old values if not changed.
      if (stockDBdata.groupColor == 0) {
        stockDBdata.groupColor = stockData.groupColor
      }
      if (stockDBdata.alertBelow == 0f) {
        stockDBdata.alertBelow = stockData.alertBelow
      }
      if (stockDBdata.alertAbove == 0f) {
        stockDBdata.alertAbove = stockData.alertAbove
      }
      if (stockDBdata.notes.isEmpty()) {
        stockDBdata.notes = stockData.notes
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

  @Query("UPDATE stock_table SET group_color = :color WHERE symbol = :symbol")
  fun setStockGroupColor(
    symbol: String,
    color: Int
  )

  @Query("UPDATE stock_table SET alert_above = :alertAbove WHERE symbol = :symbol")
  fun updateAlertAbove(
    symbol: String,
    alertAbove: Float
  )

  @Query("UPDATE stock_table SET alert_below = :alertBelow WHERE symbol = :symbol")
  fun updateAlertBelow(
    symbol: String,
    alertBelow: Float
  )

  @Query("UPDATE stock_table SET notes = :notes WHERE symbol = :symbol")
  fun updateNotes(
    symbol: String,
    notes: String
  )

  // Assets
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  fun addAsset(asset: Asset)

  // id must match to delete the entry
//    @Delete
//    fun deleteAsset(asset: Asset)

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
    shares: Float,
    price: Float
  )

  @Query("DELETE FROM asset_table WHERE symbol = :symbol")
  fun deleteAssets(symbol: String)

  @Transaction
  fun updateAssets(
    symbol: String,
    assets: List<Asset>
  ) {
    deleteAssets(symbol)
    val epsilon = 0.000001f
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

  @Query(
      "DELETE FROM event_table WHERE symbol = :symbol AND title = :title AND note = :note AND datetime = :datetime"
  )
  fun deleteEvent(
    symbol: String,
    title: String,
    note: String,
    datetime: Long
  )

  @Query("DELETE FROM event_table WHERE symbol = :symbol")
  fun deleteEvents(symbol: String)

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
}
