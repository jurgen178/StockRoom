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

package com.thecloudsite.stockroom

import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.ZoneOffset

enum class FilterTypeEnum(val value: Int) {
  FilterNullType(0),
  FilterTestType(1),
  FilterTextType(2),
  FilterDoubleType(3),
  FilterLongTermType(4),
}

enum class FilterDataTypeEnum(val value: Int) {
  NoType(0),
  TextType(1),
  DoubleType(2),
}

object FilterFactory {
  fun create(type: FilterTypeEnum): IFilterType =
    when (type) {
      FilterTypeEnum.FilterNullType -> FilterNullType()
      FilterTypeEnum.FilterTestType -> FilterTestType(type)
      FilterTypeEnum.FilterTextType -> FilterTextType(type)
      FilterTypeEnum.FilterDoubleType -> FilterDoubleType(type)
      FilterTypeEnum.FilterLongTermType -> FilterLongTermType(type)
    }

  fun create(id: String): IFilterType {
    FilterTypeEnum.values()
        .forEach { filter ->
          val filterType = create(filter)
          if (id == filterType.id.toString()) {
            return filterType
          }
        }
    return FilterNullType()
  }

  fun create(index: Int): IFilterType =
    // + 1, skip NullFilter
    if (index >= 0 && index + 1 < FilterTypeEnum.values().size) {
      val type: FilterTypeEnum = FilterTypeEnum.values()[index + 1]
      create(type)
    } else {
      FilterNullType()
    }
}

fun getFilterNameList(): List<String> {
  val filterList = mutableListOf<String>()

  FilterTypeEnum.values()
      .filter { type ->
        type != FilterTypeEnum.FilterNullType
      }
      .forEach { filter ->
        filterList.add(FilterFactory.create(filter).displayName)
      }

  return filterList
}

private fun strToDouble(str: String): Double {
  var value: Double = 0.0
  try {
    val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
    value = numberFormat.parse(str)!!
        .toDouble()
  } catch (e: Exception) {
  }

  return value
}

interface IFilterType {
  fun filter(stockItem: StockItem): Boolean
  val id: FilterTypeEnum
  val dataType: FilterDataTypeEnum
  val displayName: String
  var data: String
}

class FilterNullType : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return true
  }

  override val id = FilterTypeEnum.FilterNullType
  override val dataType = FilterDataTypeEnum.NoType
  override val displayName = id.toString()
  override var data = ""
}

class FilterTestType(override val id: FilterTypeEnum) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return stockItem.stockDBdata.symbol.isNotEmpty()
  }

  override val dataType = FilterDataTypeEnum.NoType
  override val displayName = id.toString()
  override var data = ""
}

class FilterTextType(override val id: FilterTypeEnum) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return stockItem.stockDBdata.symbol.isNotEmpty()
  }

  override val dataType = FilterDataTypeEnum.TextType
  override val displayName = id.toString()
  override var data = ""
}

class FilterDoubleType(override val id: FilterTypeEnum) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return stockItem.stockDBdata.symbol.startsWith("A")
  }

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = id.toString()
  override var data: String = ""
    get() = field
    set(value) {
      field = value
    }
}

// Stocks are at least one year old.
class FilterLongTermType(override val id: FilterTypeEnum) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    val secondsNow = LocalDateTime.now()
        .toEpochSecond(ZoneOffset.UTC)
    val newestAssetDate = stockItem.assets.maxOf { asset ->
      asset.date
    }

    // 365 plus one day
    val secondsPerYear: Long = 366 * 24 * 60 * 60
    return secondsNow > newestAssetDate + secondsPerYear
  }

  override val dataType = FilterDataTypeEnum.NoType
  override var displayName = id.toString()
  override var data = ""
}

