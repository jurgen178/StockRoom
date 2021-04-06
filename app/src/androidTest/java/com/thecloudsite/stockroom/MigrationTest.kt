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

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thecloudsite.stockroom.database.StockRoomDatabase
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class MigrationTest {
  private val TEST_DB = "stockroom_database_test"

  private val helper: MigrationTestHelper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    StockRoomDatabase::class.java.canonicalName,
    FrameworkSQLiteOpenHelperFactory()
  )

  @Test
  @Throws(IOException::class)
  fun migrate1To2_Test() {
    helper.createDatabase(TEST_DB, 1)
      .apply {
        val assetValues = ContentValues()
        assetValues.put("id", 4)
        assetValues.put("symbol", "ktln")
        assetValues.put("quantity", 1.0)
        assetValues.put("price", 42.0)
        assetValues.put("type", 0)
        assetValues.put("note", "note")
        assetValues.put("date", 123L)
        assetValues.put("sharesPerQuantity", 2)
        assetValues.put("expirationDate", 3L)
        assetValues.put("premium", 1.2)
        assetValues.put("commission", 2.3)

        this.insert("asset_table", SQLiteDatabase.CONFLICT_REPLACE, assetValues)

        this.execSQL(
          "INSERT INTO asset_table (id, symbol, quantity, price, type, note, date, sharesPerQuantity, expirationDate, premium, commission) VALUES (42, 'ktln2' , 1.02 , 42.02 , 02 , 'note2' , 1232 , 22 , 32 , 1.22 , 2.32);"
        )

        val dividendValues = ContentValues()
        dividendValues.put("id", 4)
        dividendValues.put("symbol", "ktln")
        dividendValues.put("amount", 42.0)
        dividendValues.put("cycle", 2)
        dividendValues.put("paydate", 123L)
        dividendValues.put("type", 0)
        dividendValues.put("exdate", 3L)
        dividendValues.put("note", "note")

        this.insert("dividend_table", SQLiteDatabase.CONFLICT_REPLACE, dividendValues)

        this.execSQL(
          "INSERT INTO dividend_table (id, symbol, amount, cycle, paydate, type, exdate, note) VALUES (43, 'ktln3' , 1.03 , 3 , 033 , 0, 123, 'note3');"
        )

        // Prepare for the next version.
        this.close()
      }

    // Re-open the database with version 2 and provide
    // MIGRATION_1_2 as the migration process.
    val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

    // MigrationTestHelper automatically verifies the schema changes,
    // but you need to validate that the data was migrated properly.
    val cursor = db.query("SELECT * FROM asset_table")
    cursor.moveToFirst()

    // Expect 12 columns
    assertEquals(12, cursor.columnCount)

    // Expect 2 entries
    assertEquals(2, cursor.count)

    var stringEntry: String = ""

    stringEntry = cursor.getString(cursor.getColumnIndex("symbol"))
    assertEquals("ktln", stringEntry)

    // dividend test
    val cursor2 = db.query("SELECT * FROM dividend_table")
    cursor2.moveToFirst()

    // Expect 9 columns
    assertEquals(9, cursor2.columnCount)

    // Expect 2 entries
    assertEquals(2, cursor2.count)
  }

  private val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {

      // Add account to Asset table
      val ASSET_TABLE_NAME = "asset_table"
      val ASSET_TABLE_NAME_TEMP = "asset_table_temp"

      database.execSQL(
        """
          CREATE TABLE `${ASSET_TABLE_NAME_TEMP}` (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            symbol TEXT NOT NULL,
            quantity REAL NOT NULL,
            price REAL NOT NULL,
            type INTEGER NOT NULL, 
            account TEXT NOT NULL, 
            note TEXT NOT NULL, 
            date INTEGER NOT NULL, 
            sharesPerQuantity INTEGER NOT NULL, 
            expirationDate INTEGER NOT NULL, 
            premium REAL NOT NULL,
            commission REAL NOT NULL
          )
          """.trimIndent()
      )
      database.execSQL(
        """
          INSERT INTO `${ASSET_TABLE_NAME_TEMP}` (symbol, quantity, price, type, account, note, date, sharesPerQuantity, expirationDate, premium, commission)
          SELECT symbol, quantity, price, type, '', note, date, sharesPerQuantity, expirationDate, premium, commission FROM `${ASSET_TABLE_NAME}`  
          """.trimIndent()
      )
      database.execSQL("DROP TABLE `${ASSET_TABLE_NAME}`")
      database.execSQL("ALTER TABLE `${ASSET_TABLE_NAME_TEMP}` RENAME TO `${ASSET_TABLE_NAME}`")

      // Add account to Dividend table
      val DIVIDEND_TABLE_NAME = "dividend_table"
      val DIVIDEND_TABLE_NAME_TEMP = "dividend_table_temp"

      database.execSQL(
        """
          CREATE TABLE `${DIVIDEND_TABLE_NAME_TEMP}` (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            symbol TEXT NOT NULL,
            amount REAL NOT NULL,
            cycle INTEGER NOT NULL, 
            paydate INTEGER NOT NULL, 
            type INTEGER NOT NULL, 
            account TEXT NOT NULL, 
            exdate INTEGER NOT NULL, 
            note TEXT NOT NULL
          )
          """.trimIndent()
      )
      database.execSQL(
        """
          INSERT INTO `${DIVIDEND_TABLE_NAME_TEMP}` (symbol, amount, cycle, paydate, type, account, exdate, note)
          SELECT symbol, amount, cycle, paydate, type, '', exdate, note FROM `${DIVIDEND_TABLE_NAME}`  
          """.trimIndent()
      )
      database.execSQL("DROP TABLE `${DIVIDEND_TABLE_NAME}`")
      database.execSQL("ALTER TABLE `${DIVIDEND_TABLE_NAME_TEMP}` RENAME TO `${DIVIDEND_TABLE_NAME}`")
    }
  }
}
