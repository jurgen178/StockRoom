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

  val helper: MigrationTestHelper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    StockRoomDatabase::class.java.canonicalName,
    FrameworkSQLiteOpenHelperFactory()
  )

  @Test
  @Throws(IOException::class)
  fun migrate1To2_Test() {
    helper.createDatabase(TEST_DB, 1)
      .apply {
        val values = ContentValues()
        values.put("id", 4)
        values.put("symbol", "ktln")
        values.put("quantity", 1.0)
        values.put("price", 42.0)
        values.put("type", 0)
        values.put("note", "note")
        values.put("date", 123L)
        values.put("sharesPerQuantity", 2)
        values.put("expirationDate", 3L)
        values.put("premium", 1.2)
        values.put("commission", 2.3)

        this.insert("asset_table", SQLiteDatabase.CONFLICT_REPLACE, values)

        this.execSQL(
          "INSERT INTO asset_table (id, symbol, quantity, price, type, note, date, sharesPerQuantity, expirationDate, premium, commission) VALUES (42, 'ktln2' , 1.02 , 42.02 , 02 , 'note2' , 1232 , 22 , 32 , 1.22 , 2.32);"
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

    // Expect 10 columns
    assertEquals(12, cursor.columnCount)

    // Expect 3 entries
    assertEquals(2, cursor.count)

    var stringEntry: String = ""

    stringEntry = cursor.getString(cursor.getColumnIndex("symbol"))
    assertEquals("ktln", stringEntry)
  }

  val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
      val TABLE_NAME = "asset_table"
      val TABLE_NAME_TEMP = "asset_table_temp"

      database.execSQL(
        """
          CREATE TABLE `${TABLE_NAME_TEMP}` (
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
          INSERT INTO `${TABLE_NAME_TEMP}` (symbol, quantity, price, type, account, note, date, sharesPerQuantity, expirationDate, premium, commission)
          SELECT symbol, quantity, price, type, '', note, date, sharesPerQuantity, expirationDate, premium, commission FROM `${TABLE_NAME}`  
          """.trimIndent()
      )
      database.execSQL("DROP TABLE `${TABLE_NAME}`")
      database.execSQL("ALTER TABLE `${TABLE_NAME_TEMP}` RENAME TO `${TABLE_NAME}`")
    }
  }
}
