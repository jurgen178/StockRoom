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

enum class FilterTypeEnum(val value: Int) {
  FilterTestType(0),
  FilterTextType(1),
  FilterDoubleType(2),
}

enum class FilterDataTypeEnum(val value: Int) {
  NoType(0),
  TextType(10),
  DoubleType(2),
}

object FilterFactory {
  fun create(type: FilterTypeEnum): IFilterType =
    when (type) {
      FilterTypeEnum.FilterTestType -> FilterTestType()
      FilterTypeEnum.FilterTextType -> FilterTextType()
      FilterTypeEnum.FilterDoubleType -> FilterDoubleType()
    }

  fun create(index: Int): IFilterType =
    if (index >= 0 && index < FilterTypeEnum.values().size) {
      val type: FilterTypeEnum = FilterTypeEnum.values()[index]
      create(type)
    } else {
      FilterNullType()
    }
}

fun getFilterDescriptionList(): List<String> {
  val filterList = mutableListOf<String>()

  FilterTypeEnum.values()
      .forEach { filter ->
        filterList.add(FilterFactory.create(filter).desc)
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
  var data: String
  val desc: String
  val type: FilterDataTypeEnum
}

class FilterNullType : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return true
  }

  override var data = ""
  override val desc = "null"
  override val type = FilterDataTypeEnum.NoType
}

class FilterTestType : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return stockItem.stockDBdata.symbol.isNotEmpty()
  }

  override var data = ""
  override val desc = "test"
  override val type = FilterDataTypeEnum.NoType
}

class FilterTextType : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return stockItem.stockDBdata.symbol.isNotEmpty()
  }

  override var data = ""
  override val desc = "Text"
  override val type = FilterDataTypeEnum.TextType
}

class FilterDoubleType : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return stockItem.stockDBdata.symbol.isNotEmpty()
  }

  override var data: String = ""
    get() = field
    set(value) {
      field = value
    }
  override val desc = "Double"
  override val type = FilterDataTypeEnum.DoubleType
}

