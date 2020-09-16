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

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation

/**
 * A basic class representing an entity that is a row in a one-column database table.
 *
 * @ Entity - You must annotate the class as an entity and supply a table name if not class name.
 * @ PrimaryKey - You must identify the primary key.
 * @ ColumnInfo - You must supply the column name if it is different from the variable name.
 *
 * See the documentation for the full rich set of annotations.
 * https://developer.android.com/topic/libraries/architecture/room.html
 */

@Entity(tableName = "stock_table")
data class StockDBdata(
  @PrimaryKey val symbol: String,
  var portfolio: String = "",
  var data: String = "",
  @ColumnInfo(name = "group_color") var groupColor: Int = 0,
  var notes: String = "",
  @ColumnInfo(name = "dividend_notes") var dividendNotes: String = "",
  @ColumnInfo(name = "alert_above") var alertAbove: Double = 0.0,
  @ColumnInfo(name = "alert_below") var alertBelow: Double = 0.0
)

@Entity(tableName = "group_table")
data class Group(
  @PrimaryKey var color: Int,
  var name: String
)

@Entity(
    tableName = "asset_table"
)
data class Asset(
  @PrimaryKey(autoGenerate = true) var id: Long? = null,
  val symbol: String,
  var shares: Double,
  var price: Double,
  var date: Long = 0L,
  var commission: Double = 0.0,
  var type: Int = 0
)

@Entity(
    tableName = "event_table"
)
data class Event(
  @PrimaryKey(autoGenerate = true) var id: Long? = null,
  val symbol: String,
  val type: Int,
  val title: String,
  val note: String,
  val datetime: Long
)

data class Assets(
  @Embedded
  val stockDBdata: StockDBdata,
  @Relation(
      parentColumn = "symbol",
      entityColumn = "symbol"
  )
  val assets: List<Asset>
)

data class Events(
  @Embedded
  val stockDBdata: StockDBdata,
  @Relation(
      parentColumn = "symbol",
      entityColumn = "symbol"
  )
  val events: List<Event>
)

data class Dividends(
  @Embedded
  val stockDBdata: StockDBdata,
  @Relation(
      parentColumn = "symbol",
      entityColumn = "symbol"
  )
  val dividends: List<Dividend>
)

@Entity(
    tableName = "dividend_table"/*,
    foreignKeys = [ForeignKey(
        entity = StockDBdata::class,
        parentColumns = ["symbol"],
        childColumns = ["symbol"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("symbol"), Index("id")]*/
)
data class Dividend(
  @PrimaryKey(autoGenerate = true) var id: Long? = null,
  var symbol: String,
  var amount: Double,
  val type: Int,
  val cycle: Int,
  val paydate: Long,
  val exdate: Long
)
