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

package com.example.android.stockroom

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
  @ColumnInfo(name = "group_color") val groupColor: Int = 0,
  val notes: String = "",
  @ColumnInfo(name = "alert_above") val alertAbove: Float = 0f,
  @ColumnInfo(name = "alert_below") val alertBelow: Float = 0f
)

@Entity(tableName = "group_table")
data class Group(
  @PrimaryKey var color: Int,
  val name: String
)

@Entity(tableName = "asset_table")
data class Asset(
  @PrimaryKey(autoGenerate = true) var id: Long? = null,
  val symbol: String,
  var shares: Float = 0f,
  val price: Float = 0f
)

@Entity(tableName = "event_table")
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

