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

enum class FilterTypeEnum(val value: Int) {
  Null(0),
  FilterTestType(1),
}

object FilterFactory {
  fun create(type: FilterTypeEnum): IFilterType {
    return when (type) {
      FilterTypeEnum.FilterTestType -> FilterTestType()
      else -> FilterNullType()
    }
  }
}

fun getFilterDescriptionList(): List<String> {
  val filterList = mutableListOf<String>()

  FilterTypeEnum.values().forEach { filter ->
    filterList.add(FilterFactory.create(filter).desc)
  }

  return filterList
}

interface IFilterType {
  fun filter(stockItem: StockItem): Boolean
  val desc: String
}

class FilterNullType : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return true
  }

  override val desc = "null"
}

class FilterTestType : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return stockItem.stockDBdata.symbol.isNotEmpty()
  }

  override val desc = "test"
}

