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

import android.content.Context
import android.graphics.Color
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * This is the backend. The database. This used to be done by the OpenHelper.
 * The fact that this has very few comments emphasizes its coolness.
 */
@Database(
    entities = [StockDBdata::class, Group::class, Asset::class, Event::class], version = 1,
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
            // Wipes and rebuilds instead of migrating if no Migration object.
            // Migration is not part of this codelab.
            .fallbackToDestructiveMigration()
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

    fun populateDatabase(stockRoomDao: StockRoomDao, context: Context) {
      // Add predefined values to the DB.
      stockRoomDao.setPredefinedGroups(context)

      stockRoomDao.insert(StockDBdata(symbol = "MSFT", groupColor = Color.BLUE))
      stockRoomDao.insert(StockDBdata(symbol = "AAPL", groupColor = Color.BLUE))
      stockRoomDao.insert(StockDBdata(symbol = "AMZN", groupColor = Color.MAGENTA))
      stockRoomDao.insert(StockDBdata(symbol = "TSLA", groupColor = Color.YELLOW))
      stockRoomDao.insert(StockDBdata(symbol = "^IXIC", groupColor = Color.BLACK))
    }
  }
}
