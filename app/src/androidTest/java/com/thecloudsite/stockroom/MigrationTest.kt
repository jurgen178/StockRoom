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
                val stockValues = ContentValues()
                stockValues.put("symbol", "ktln0")
                stockValues.put("portfolio", "test portfolio")
                stockValues.put("data", "data")
                stockValues.put("group_color", 123L)
                stockValues.put("note", "note")
                stockValues.put("dividend_note", "dividend_note")
                stockValues.put("annual_dividend_rate", 1.1)
                stockValues.put("alert_above", 2.2)
                stockValues.put("alert_above_note", "alert_above_note")
                stockValues.put("alert_below", 3.3)
                stockValues.put("alert_below_note", "alert_below_note")

                // Use insert to add the data.
                this.insert("stock_table", SQLiteDatabase.CONFLICT_REPLACE, stockValues)

                // Use execSQL to add the data.
                this.execSQL(
                    "INSERT INTO stock_table (symbol, portfolio, data, group_color, note, dividend_note, annual_dividend_rate, alert_above, alert_above_note, alert_below, alert_below_note) VALUES ('ktln00' , 'testportfolio' , 'data0', 1230, 'note0', 'dividend_note0', 1.0, 2.0, 'above_note0', 3.0, 'below_note0');"
                )

                val assetValues = ContentValues()
                assetValues.put("id", 4)
                assetValues.put("symbol", "ktln1")
                assetValues.put("quantity", 1.0)
                assetValues.put("price", 42.0)
                assetValues.put("type", 0)
                assetValues.put("note", "note")
                assetValues.put("date", 123L)
                assetValues.put("sharesPerQuantity", 2)
                assetValues.put("expirationDate", 3L)
                assetValues.put("premium", 1.2)
                assetValues.put("commission", 2.3)

                // Use insert to add the data.
                this.insert("asset_table", SQLiteDatabase.CONFLICT_REPLACE, assetValues)

                // Use execSQL to add the data.
                this.execSQL(
                    "INSERT INTO asset_table (id, symbol, quantity, price, type, note, date, sharesPerQuantity, expirationDate, premium, commission) VALUES (42, 'ktln11' , 1.02 , 42.02 , 02 , 'note2' , 1232 , 22 , 32 , 1.22 , 2.32);"
                )

                val dividendValues = ContentValues()
                dividendValues.put("id", 4)
                dividendValues.put("symbol", "ktln2")
                dividendValues.put("amount", 42.0)
                dividendValues.put("cycle", 2)
                dividendValues.put("paydate", 123L)
                dividendValues.put("type", 0)
                dividendValues.put("exdate", 3L)
                dividendValues.put("note", "note")

                // Use insert to add the data.
                this.insert("dividend_table", SQLiteDatabase.CONFLICT_REPLACE, dividendValues)

                // Use execSQL to add the data.
                this.execSQL(
                    "INSERT INTO dividend_table (id, symbol, amount, cycle, paydate, type, exdate, note) VALUES (43, 'ktln22' , 1.03 , 3 , 033 , 0, 123, 'note3');"
                )

                // Prepare for the next version.
                this.close()
            }

        // Re-open the database with version 2 and provide
        // MIGRATION_1_2 as the migration process.
        val db = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
        val cursor0 = db.query("SELECT * FROM stock_table")
        cursor0.moveToFirst()

        // Expect 13 columns
        assertEquals(13, cursor0.columnCount)

        // Expect 2 entries (one from insert, and one from execSQL)
        assertEquals(2, cursor0.count)

        // Expect first entry
        assertEquals("ktln0", cursor0.getString(cursor0.getColumnIndex("symbol")))
        // Expect default value
        assertEquals(0, cursor0.getInt(cursor0.getColumnIndex("type")))

        // asset test
        val cursor1 = db.query("SELECT * FROM asset_table")
        cursor1.moveToFirst()

        // Expect 12 columns
        assertEquals(12, cursor1.columnCount)

        // Expect 2 entries (one from insert, and one from execSQL)
        assertEquals(2, cursor1.count)

        // Expect first entry
        assertEquals("ktln1", cursor1.getString(cursor1.getColumnIndex("symbol")))
        // Expect default value
        assertEquals("", cursor1.getString(cursor1.getColumnIndex("account")))

        // dividend test
        val cursor2 = db.query("SELECT * FROM dividend_table")
        cursor2.moveToFirst()

        // Expect 9 columns
        assertEquals(9, cursor2.columnCount)

        // Expect first entry
        assertEquals("ktln2", cursor2.getString(cursor2.getColumnIndex("symbol")))
        // Expect default value
        assertEquals("", cursor2.getString(cursor2.getColumnIndex("account")))

        // Expect 2 entries (one from insert, and one from execSQL)
        assertEquals(2, cursor2.count)
    }

    // Add type and marker to stock table.
    // Add account to Asset table.
    // Add account to Dividend table.
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {

            // Add type and marker to stock table.
            val STOCK_TABLE_NAME = "stock_table"
            val STOCK_TABLE_NAME_TEMP = "stock_table_temp"

            database.execSQL(
                """
          CREATE TABLE `${STOCK_TABLE_NAME_TEMP}` (
            symbol TEXT PRIMARY KEY NOT NULL,
            portfolio TEXT NOT NULL, 
            type INTEGER NOT NULL, 
            data TEXT NOT NULL, 
            marker INTEGER NOT NULL, 
            group_color INTEGER NOT NULL, 
            note TEXT NOT NULL,
            dividend_note TEXT NOT NULL, 
            annual_dividend_rate REAL NOT NULL,
            alert_above REAL NOT NULL, 
            alert_above_note TEXT NOT NULL,
            alert_below REAL NOT NULL, 
            alert_below_note TEXT NOT NULL
          )
          """.trimIndent()
            )
            database.execSQL(
                """
          INSERT INTO `${STOCK_TABLE_NAME_TEMP}` (symbol, portfolio, type, data, marker, group_color, note, dividend_note, annual_dividend_rate, alert_above, alert_above_note, alert_below, alert_below_note)
          SELECT symbol, portfolio, 0, data, 0, group_color, note, dividend_note, annual_dividend_rate, alert_above, alert_above_note, alert_below, alert_below_note FROM `${STOCK_TABLE_NAME}`  
          """.trimIndent()
            )
            database.execSQL("DROP TABLE `${STOCK_TABLE_NAME}`")
            database.execSQL("ALTER TABLE `${STOCK_TABLE_NAME_TEMP}` RENAME TO `${STOCK_TABLE_NAME}`")

            // Add account to Asset table.
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

            // Add account to Dividend table.
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

    @Test
    @Throws(IOException::class)
    fun migrate2To3_Test() {
        helper.createDatabase(TEST_DB, 2)
            .apply {

                val assetValues = ContentValues()
                assetValues.put("id", 4)
                assetValues.put("symbol", "ktln1")
                assetValues.put("quantity", 1.0)
                assetValues.put("price", 42.0)
                assetValues.put("type", 0)
                assetValues.put("account", "acct")
                assetValues.put("note", "note")
                assetValues.put("date", 123L)
                assetValues.put("sharesPerQuantity", 2)
                assetValues.put("expirationDate", 3L)
                assetValues.put("premium", 1.2)
                assetValues.put("commission", 2.3)

                // Use insert to add the data.
                this.insert("asset_table", SQLiteDatabase.CONFLICT_REPLACE, assetValues)

                // Use execSQL to add the data.
                this.execSQL(
                    "INSERT INTO asset_table (id, symbol, quantity, price, type, account, note, date, sharesPerQuantity, expirationDate, premium, commission) VALUES (42, 'ktln11' , 1.02 , 42.02 , 02 , 'acct2', 'note2' , 1232 , 22 , 32 , 1.22 , 2.32);"
                )

                // Prepare for the next version.
                this.close()
            }

        // Re-open the database with version 3 and provide
        // MIGRATION_2_3 as the migration process.
        val db = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)

        // MigrationTestHelper automatically verifies the schema changes,
        // but you need to validate that the data was migrated properly.
        // asset test
        val cursor1 = db.query("SELECT * FROM asset_table")
        cursor1.moveToFirst()

        // Expect 12 columns
        assertEquals(12, cursor1.columnCount)

        // Expect 2 entries (one from insert, and one from execSQL)
        assertEquals(2, cursor1.count)

        // Expect first entry
        assertEquals("ktln1", cursor1.getString(cursor1.getColumnIndex("symbol")))
        // Expect default value
        assertEquals("2.3", cursor1.getString(cursor1.getColumnIndex("fee")))
    }

    // Rename commission to fee.
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {

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
            fee REAL NOT NULL
          )
          """.trimIndent()
            )
            database.execSQL(
                """
          INSERT INTO `${ASSET_TABLE_NAME_TEMP}` (symbol, quantity, price, type, account, note, date, sharesPerQuantity, expirationDate, premium, fee)
          SELECT symbol, quantity, price, type, account, note, date, sharesPerQuantity, expirationDate, premium, commission FROM `${ASSET_TABLE_NAME}`  
          """.trimIndent()
            )
            database.execSQL("DROP TABLE `${ASSET_TABLE_NAME}`")
            database.execSQL("ALTER TABLE `${ASSET_TABLE_NAME_TEMP}` RENAME TO `${ASSET_TABLE_NAME}`")
        }
    }
}
