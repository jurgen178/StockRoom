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

package com.thecloudsite.stockroom.database

import android.content.Context
import android.graphics.Color
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.thecloudsite.stockroom.OnlineMarketData
import com.thecloudsite.stockroom.R.raw
import com.thecloudsite.stockroom.R.string
import com.thecloudsite.stockroom.StockItemJson
import com.thecloudsite.stockroom.StockMarketDataApiFactory
import com.thecloudsite.stockroom.StockMarketDataRepository
import com.thecloudsite.stockroom.utils.getRawTextFile
import com.thecloudsite.stockroom.utils.isOnline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

/*
 * This is the backend. The database. This used to be done by the OpenHelper.
 * The fact that this has very few comments emphasizes its coolness.
 */
@Database(
    entities = [StoreData::class, StockDBdata::class, Group::class, Asset::class, Event::class, Dividend::class],
    version = 1,
    exportSchema = true
)

abstract class StockRoomDatabase : RoomDatabase() {

  abstract fun stockRoomDao(): StockRoomDao

  companion object {
    @Volatile
    private var INSTANCE: StockRoomDatabase? = null

    fun getDatabase(
      context: Context,
      scope: CoroutineScope
    ): StockRoomDatabase {
      // if the INSTANCE is not null, then return it,
      // if it is, then create the database
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
            context.applicationContext,
            StockRoomDatabase::class.java,
            "stockroom_database"
        )
            .addCallback(StockRoomDatabaseCallback(scope, context))
            .build()

        INSTANCE = instance
        // return instance
        instance
      }
    }

    private class StockRoomDatabaseCallback(
      private val scope: CoroutineScope,
      val context: Context
    ) : RoomDatabase.Callback() {
      override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        INSTANCE?.let { database ->
          scope.launch(Dispatchers.IO) {
            populateDatabase(database.stockRoomDao(), context)
          }
        }
      }
    }

    private fun importExampleJSON(
      stockRoomDao: StockRoomDao,
      json: String
    ) {
      val sType = object : TypeToken<List<StockItemJson>>() {}.type
      val gson = Gson()
      val stockItemJsonList = gson.fromJson<List<StockItemJson>>(json, sType)

      stockItemJsonList.forEach { stockItemJson ->
        val symbol = stockItemJson.symbol.toUpperCase(Locale.ROOT)
        stockRoomDao.insert(
            StockDBdata(
                symbol = symbol,
                // can be null if it is not in the json
                groupColor = stockItemJson.groupColor ?: 0,
                note = stockItemJson.note ?: "",
                alertBelow = stockItemJson.alertBelow ?: 0.0,
                alertAbove = stockItemJson.alertAbove ?: 0.0
            )
        )

        if(stockItemJson.assets != null) {
          stockRoomDao.updateAssets(symbol = symbol, assets = stockItemJson.assets!!.map { asset ->
            Asset(
                symbol = symbol,
                quantity = asset.quantity ?: 0.0,
                price = asset.price ?: 0.0,
                date = asset.date ?: 0L
            )
          })
        }

        if(stockItemJson.events != null) {
          stockRoomDao.updateEvents(symbol = symbol, events = stockItemJson.events!!.map { event ->
            Event(
                symbol = symbol,
                type = event.type ?: 0,
                title = event.title ?: "",
                note = event.note ?: "",
                datetime = event.datetime ?: 0L
            )
          })
        }
      }
    }

    fun populateDatabase(
      stockRoomDao: StockRoomDao,
      context: Context
    ) {
      // Add predefined values to the DB.
      stockRoomDao.setPredefinedGroups(context)

      if (isOnline(context)) {
        val stockMarketDataRepository: StockMarketDataRepository =
          StockMarketDataRepository { StockMarketDataApiFactory.yahooApi }

        data class AssetPreset(
          val symbol: String,
          val asset: Double,
          val gain: Double,
          val date: Long,
          val color: Int
        )

        val assets: List<AssetPreset> = listOf(
            AssetPreset("AAPL", 6500.0, 5240.0, 1601683200, Color.BLUE),
            AssetPreset("AMZN", 6500.0, 280.0, 1601683200, Color.MAGENTA),
            AssetPreset("GE", 3700.0, 2470.0, 1601683200, Color.BLACK),
            AssetPreset("BA", 5500.0, -640.0, 1601683200, Color.GREEN),
            AssetPreset("CVX", 4500.0, -508.0, 1601683200, Color.rgb(0, 191, 255)),
            AssetPreset("ANY", 8000.0, 6490.0, 1601683200, Color.YELLOW),
            AssetPreset("MSFT", 5200.0, 1450.0, 1601683200, Color.rgb(173, 216, 230)),
            AssetPreset("QCOM", 4200.0, 240.0, 1601683200, Color.rgb(0, 191, 255)),
            AssetPreset("RM", 3600.0, 1110.0, 1601683200, Color.RED),
            AssetPreset("TSLA", 7000.0, 2060.0, 1601683200, Color.rgb(72, 209, 204)),
        )

        val symbols = assets.map { asset ->
          asset.symbol
        }

        var onlinedata: List<OnlineMarketData>
        runBlocking {
          withContext(Dispatchers.IO) {
            onlinedata = stockMarketDataRepository.getStockData2(symbols)
          }
        }

        assets.forEach { asset ->
          stockRoomDao.insert(StockDBdata(symbol = asset.symbol, groupColor = asset.color))

          val data = onlinedata.find {
            it.symbol == asset.symbol
          }

          if (data != null) {
            val assetvalue = asset.asset + ((0..1000).random() - 500).toDouble() / 100
            val gainvalue = asset.gain + ((0..1000).random() - 500).toDouble() / 100

            val price = data.marketPrice * (1 - gainvalue / assetvalue)
            val quantity = ((assetvalue - gainvalue) / price).roundToInt()
                .toDouble()
            val price2 = assetvalue / quantity

            stockRoomDao.addAsset(
                Asset(symbol = asset.symbol, quantity = quantity, price = price2, date = asset.date)
            )
          }
        }
      } else {
        // if offline, preset with json
        val jsonText = context.resources.getRawTextFile(raw.example_stocks)
        importExampleJSON(stockRoomDao, jsonText)
      }

      // List is sorted alphabetically. Add comment about deleting the example list in the first entry.
      stockRoomDao.updateNote(
          symbol = "AAPL", note = context.getString(string.example_List_delete_all)
      )
      stockRoomDao.updateNote(
          symbol = "AMZN", note = context.getString(string.example_List_note)
      )

/*
stockRoomDao.insert(StockDBdata(symbol = "AAPL", groupColor = Color.BLUE))
stockRoomDao.addAsset(Asset(symbol = "AAPL", shares = 20.0, price = 100.0))
stockRoomDao.updateNotes(symbol = "AAPL", notes = context.getString(R.string.example_List_delete_all))
stockRoomDao.insert(StockDBdata(symbol = "AMZN", groupColor = Color.BLUE, alertAbove = 4000.0))
stockRoomDao.addAsset(Asset(symbol = "AMZN", shares = 2.0, price = 3000.0))
stockRoomDao.updateNotes(symbol = "AMZN", notes = context.getString(R.string.example_List_note))
stockRoomDao.insert(StockDBdata(symbol = "BA", groupColor = Color.YELLOW, alertBelow = 100.0))
stockRoomDao.addAsset(Asset(symbol = "BA", shares = 30.0, price = 200.0))
stockRoomDao.insert(StockDBdata(symbol = "CVX", groupColor = Color.YELLOW))
stockRoomDao.addAsset(Asset(symbol = "CVX", shares = 40.0, price = 100.0))
stockRoomDao.insert(StockDBdata(symbol = "DIS", groupColor = Color.YELLOW))
stockRoomDao.addAsset(Asset(symbol = "DIS", shares = 15.0, price = 150.0))
stockRoomDao.addEvent(
    Event(
        symbol = "DIS", type = 0, datetime = 1619870400, title = "Earnings report",
        note = "Check the DIS site"
    )
)
stockRoomDao.insert(StockDBdata(symbol = "FB", groupColor = Color.RED))
stockRoomDao.addAsset(Asset(symbol = "FB", shares = 12.0, price = 120.0))
stockRoomDao.insert(StockDBdata(symbol = "IBM", groupColor = Color.RED))
stockRoomDao.addAsset(Asset(symbol = "IBM", shares = 20.0, price = 200.0))
stockRoomDao.insert(StockDBdata(symbol = "MSFT", groupColor = Color.BLUE))
stockRoomDao.addAsset(Asset(symbol = "MSFT", shares = 20.0, price = 150.0))
stockRoomDao.insert(StockDBdata(symbol = "QCOM", groupColor = Color.GREEN))
stockRoomDao.addAsset(Asset(symbol = "QCOM", shares = 30.0, price = 100.0))
stockRoomDao.insert(StockDBdata(symbol = "T", groupColor = Color.rgb(72, 209, 204)))
stockRoomDao.addAsset(Asset(symbol = "T", shares = 100.0, price = 10.0))
stockRoomDao.insert(StockDBdata(symbol = "TSLA", groupColor = Color.rgb(72, 209, 204)))
stockRoomDao.addAsset(Asset(symbol = "TSLA", shares = 5.0, price = 1000.0))
stockRoomDao.insert(StockDBdata(symbol = "^GSPC", groupColor = 0))
 */
    }
  }
}
