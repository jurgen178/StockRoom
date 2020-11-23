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

import android.content.Context
import com.thecloudsite.stockroom.utils.getAssets
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.FULL

enum class FilterTypeEnum(val value: Int) {
  FilterNullType(0),
  FilterPercentageChangeGreaterThanType(1),
  FilterPercentageChangeLessThanType(2),
  FilterSymbolContainsType(3),
  FilterNoteContainsType(4),
  FilterAssetGreaterThanType(5),
  FilterAssetLessThanType(6),
  FilterProfitGreaterThanType(7),
  FilterProfitLessThanType(8),
  FilterProfitPercentageGreaterThanType(9),
  FilterProfitPercentageLessThanType(10),
  FilterDividendPercentageGreaterThanType(11),
  FilterDividendPercentageLessThanType(12),
  FilterFirstAssetSoldBeforeType(13),
  FilterFirstAssetSoldAfterType(14),
  FilterFirstAssetBoughtBeforeType(15),
  FilterFirstAssetBoughtAfterType(16),
  FilterLastAssetSoldBeforeType(17),
  FilterLastAssetSoldAfterType(18),
  FilterLastAssetBoughtBeforeType(19),
  FilterLastAssetBoughtAfterType(20),
  FilterLongTermType(21),
}

enum class FilterDataTypeEnum(val value: Int) {
  NoType(0),
  TextType(1),
  DoubleType(2),
  DateType(3),
}

object FilterFactory {
  fun create(
    type: FilterTypeEnum,
    context: Context
  ): IFilterType =
    when (type) {
      FilterTypeEnum.FilterNullType -> FilterNullType()
      FilterTypeEnum.FilterPercentageChangeGreaterThanType -> FilterPercentageChangeGreaterThanType(
          type, context
      )
      FilterTypeEnum.FilterPercentageChangeLessThanType -> FilterPercentageChangeLessThanType(
          type, context
      )
      FilterTypeEnum.FilterSymbolContainsType -> FilterSymbolContainsType(type, context)
      FilterTypeEnum.FilterNoteContainsType -> FilterNoteContainsType(type, context)
      FilterTypeEnum.FilterAssetGreaterThanType -> FilterAssetGreaterThanType(type, context)
      FilterTypeEnum.FilterAssetLessThanType -> FilterAssetLessThanType(type, context)
      FilterTypeEnum.FilterProfitGreaterThanType -> FilterProfitGreaterThanType(type, context)
      FilterTypeEnum.FilterProfitLessThanType -> FilterProfitLessThanType(type, context)
      FilterTypeEnum.FilterProfitPercentageGreaterThanType -> FilterProfitPercentageGreaterThanType(
          type, context
      )
      FilterTypeEnum.FilterProfitPercentageLessThanType -> FilterProfitPercentageLessThanType(
          type, context
      )
      FilterTypeEnum.FilterDividendPercentageGreaterThanType -> FilterDividendPercentageGreaterThanType(
          type, context
      )
      FilterTypeEnum.FilterDividendPercentageLessThanType -> FilterDividendPercentageLessThanType(
          type, context
      )
      FilterTypeEnum.FilterFirstAssetSoldBeforeType -> FilterFirstAssetSoldBeforeType(
          type, context
      )
      FilterTypeEnum.FilterFirstAssetSoldAfterType -> FilterFirstAssetSoldAfterType(type, context)
      FilterTypeEnum.FilterFirstAssetBoughtBeforeType -> FilterFirstAssetBoughtBeforeType(
          type, context
      )
      FilterTypeEnum.FilterFirstAssetBoughtAfterType -> FilterFirstAssetBoughtAfterType(
          type, context
      )
      FilterTypeEnum.FilterLastAssetSoldBeforeType -> FilterLastAssetSoldBeforeType(type, context)
      FilterTypeEnum.FilterLastAssetSoldAfterType -> FilterLastAssetSoldAfterType(type, context)
      FilterTypeEnum.FilterLastAssetBoughtBeforeType -> FilterLastAssetBoughtBeforeType(
          type, context
      )
      FilterTypeEnum.FilterLastAssetBoughtAfterType -> FilterLastAssetBoughtAfterType(type, context)
      FilterTypeEnum.FilterLongTermType -> FilterLongTermType(type, context)
    }

  fun create(
    id: String,
    context: Context
  ): IFilterType {
    FilterTypeEnum.values()
        .forEach { filter ->
          val filterType = create(filter, context)
          if (id == filterType.typeId.toString()) {
            return filterType
          }
        }
    return FilterNullType()
  }

  fun create(
    index: Int,
    context: Context
  ): IFilterType =
    // + 1, skip NullFilter
    if (index >= 0 && index + 1 < FilterTypeEnum.values().size) {
      val type: FilterTypeEnum = FilterTypeEnum.values()[index + 1]
      create(type, context)
    } else {
      FilterNullType()
    }
}

fun getFilterNameList(context: Context): List<String> {
  val filterList = mutableListOf<String>()

  FilterTypeEnum.values()
      .filter { type ->
        type != FilterTypeEnum.FilterNullType
      }
      .forEach { filter ->
        filterList.add(FilterFactory.create(filter, context).displayName)
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
  val typeId: FilterTypeEnum
  val dataType: FilterDataTypeEnum
  val displayName: String
  val desc: String
  var data: String
  var serializedData: String
}

class FilterNullType : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return true
  }

  override val typeId = FilterTypeEnum.FilterNullType
  override val dataType = FilterDataTypeEnum.NoType
  override val displayName = typeId.toString()
  override val desc = ""
  override var data = ""
  override var serializedData = data
}

//class FilterTestType(override val typeId: FilterTypeEnum) : IFilterType {
//  override fun filter(stockItem: StockItem): Boolean {
//    return stockItem.stockDBdata.symbol.isNotEmpty()
//  }
//
//  override val dataType = FilterDataTypeEnum.NoType
//  override val displayName = typeId.toString()
//  override var data = ""
//  override var serializedData = data
//  override val desc = ""
//}
//
//class FilterTextType(override val typeId: FilterTypeEnum) : IFilterType {
//  override fun filter(stockItem: StockItem): Boolean {
//    return stockItem.stockDBdata.symbol.isNotEmpty()
//  }
//
//  override val dataType = FilterDataTypeEnum.TextType
//  override val displayName = typeId.toString()
//  override var data = ""
//  override var serializedData = data
//  override val desc = ""
//}
//
//class FilterDoubleType(override val typeId: FilterTypeEnum) : IFilterType {
//  override fun filter(stockItem: StockItem): Boolean {
//    return stockItem.stockDBdata.symbol.startsWith("A")
//  }
//
//  override val dataType = FilterDataTypeEnum.DoubleType
//  override var displayName = typeId.toString()
//  override var data: String = ""
//    get() = field
//    set(value) {
//      field = value
//    }
//  override var serializedData = data
//  override val desc = ""
//}

// Change percentage greater than
class FilterPercentageChangeGreaterThanType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return stockItem.onlineMarketData.marketChangePercent > filterPercentageValue
  }

  var filterValue: Double = 0.0
  var filterPercentageValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = context.getString(R.string.filter_percentagechangegreater_name)
  override val desc = context.getString(R.string.filter_percentagechangegreater_desc)
  override var data: String = ""
    get() = DecimalFormat("0.##").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
    }
  override var serializedData = data
}

// Change percentage less than
class FilterPercentageChangeLessThanType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return stockItem.onlineMarketData.marketChangePercent < filterPercentageValue
  }

  var filterValue: Double = 0.0
  var filterPercentageValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = context.getString(R.string.filter_percentagechangeless_name)
  override val desc = context.getString(R.string.filter_percentagechangeless_desc)
  override var data: String = ""
    get() = DecimalFormat("0.##").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
      filterPercentageValue = filterValue / 100
    }
  override var serializedData = data
}

class FilterSymbolContainsType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return stockItem.stockDBdata.symbol.contains(data, ignoreCase = true)
  }

  override val dataType = FilterDataTypeEnum.TextType
  override var displayName = context.getString(R.string.filter_symbolcontainstype_name)
  override val desc = context.getString(R.string.filter_symbolcontainstype_desc)
  override var data = ""
  override var serializedData = data
}

class FilterNoteContainsType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return stockItem.stockDBdata.note.contains(data, ignoreCase = true)
  }

  override val dataType = FilterDataTypeEnum.TextType
  override var displayName = context.getString(R.string.filter_notecontainstype_name)
  override val desc = context.getString(R.string.filter_notecontainstype_desc)
  override var data = ""
  override var serializedData = data
}

// Asset greater than
class FilterAssetGreaterThanType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice) = getAssets(stockItem.assets)
    val asset = if (stockItem.onlineMarketData.marketPrice > 0.0) {
      totalQuantity * stockItem.onlineMarketData.marketPrice
    } else {
      totalPrice
    }
    return asset > filterValue
  }

  var filterValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = context.getString(R.string.filter_assetgreater_name)
  override val desc = context.getString(R.string.filter_assetgreater_desc)
  override var data: String = ""
    get() = DecimalFormat("0.00").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
    }
  override var serializedData = data
}

// Asset less than
class FilterAssetLessThanType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice) = getAssets(stockItem.assets)
    val asset = if (stockItem.onlineMarketData.marketPrice > 0.0) {
      totalQuantity * stockItem.onlineMarketData.marketPrice
    } else {
      totalPrice
    }
    return asset < filterValue
  }

  var filterValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = context.getString(R.string.filter_assetless_name)
  override val desc = context.getString(R.string.filter_assetless_desc)
  override var data: String = ""
    get() = DecimalFormat("0.00").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
    }
  override var serializedData = data
}

// Profit greater than
class FilterProfitGreaterThanType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice) = getAssets(stockItem.assets)
    val profit = if (stockItem.onlineMarketData.marketPrice > 0.0) {
      totalQuantity * stockItem.onlineMarketData.marketPrice - totalPrice
    } else {
      totalPrice
    }
    return profit > filterValue
  }

  var filterValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = context.getString(R.string.filter_profitgreater_name)
  override val desc = context.getString(R.string.filter_profitgreater_desc)
  override var data: String = ""
    get() = DecimalFormat("0.00").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
    }
  override var serializedData = data
}

// Profit less than
class FilterProfitLessThanType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice) = getAssets(stockItem.assets)
    val profit = if (stockItem.onlineMarketData.marketPrice > 0.0) {
      totalQuantity * stockItem.onlineMarketData.marketPrice - totalPrice
    } else {
      totalPrice
    }
    return profit < filterValue
  }

  var filterValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = context.getString(R.string.filter_profitless_name)
  override val desc = context.getString(R.string.filter_profitless_desc)
  override var data: String = ""
    get() = DecimalFormat("0.00").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
    }
  override var serializedData = data
}

// Profit Percentage greater than
class FilterProfitPercentageGreaterThanType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice) = getAssets(stockItem.assets)
    val profitPercentage =
      if (stockItem.onlineMarketData.marketPrice > 0.0 && totalPrice > 0.0) {
        (totalQuantity * stockItem.onlineMarketData.marketPrice - totalPrice) / totalPrice
      } else {
        totalPrice
      }
    return profitPercentage > filterPercentageValue
  }

  var filterValue: Double = 0.0
  var filterPercentageValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = context.getString(R.string.filter_profitpercentagegreater_name)
  override val desc = context.getString(R.string.filter_profitpercentagegreater_desc)
  override var data: String = ""
    get() = DecimalFormat("0.##").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
      filterPercentageValue = filterValue / 100
    }
  override var serializedData = data
}

// Profit Percentage less than
class FilterProfitPercentageLessThanType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice) = getAssets(stockItem.assets)
    val profitPercentage =
      if (stockItem.onlineMarketData.marketPrice > 0.0 && totalPrice > 0.0) {
        (totalQuantity * stockItem.onlineMarketData.marketPrice - totalPrice) / totalPrice
      } else {
        totalPrice
      }
    return profitPercentage < filterPercentageValue
  }

  var filterValue: Double = 0.0
  var filterPercentageValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = context.getString(R.string.filter_profitpercentageless_name)
  override val desc = context.getString(R.string.filter_profitpercentageless_desc)
  override var data: String = ""
    get() = DecimalFormat("0.##").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
      filterPercentageValue = filterValue / 100
    }
  override var serializedData = data
}

// Dividend Percentage greater than
class FilterDividendPercentageGreaterThanType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    val dividendPercentage =
      if (stockItem.stockDBdata.annualDividendRate >= 0.0) {
        if (stockItem.onlineMarketData.marketPrice > 0.0) {
          stockItem.stockDBdata.annualDividendRate / stockItem.onlineMarketData.marketPrice
        } else {
          0.0
        }
      } else {
        stockItem.onlineMarketData.annualDividendYield
      }
    return dividendPercentage > filterPercentageValue
  }

  var filterValue: Double = 0.0
  var filterPercentageValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = context.getString(R.string.filter_dividendpercentagegreater_name)
  override val desc = context.getString(R.string.filter_dividendpercentagegreater_desc)
  override var data: String = ""
    get() = DecimalFormat("0.##").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
      filterPercentageValue = filterValue / 100
    }
  override var serializedData = data
}

// Dividend Percentage less than
class FilterDividendPercentageLessThanType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    val dividendPercentage =
      if (stockItem.stockDBdata.annualDividendRate >= 0.0) {
        if (stockItem.onlineMarketData.marketPrice > 0.0) {
          stockItem.stockDBdata.annualDividendRate / stockItem.onlineMarketData.marketPrice
        } else {
          0.0
        }
      } else {
        stockItem.onlineMarketData.annualDividendYield
      }
    return dividendPercentage < filterPercentageValue
  }

  var filterValue: Double = 0.0
  var filterPercentageValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override var displayName = context.getString(R.string.filter_dividendpercentageless_name)
  override val desc = context.getString(R.string.filter_dividendpercentageless_desc)
  override var data: String = ""
    get() = DecimalFormat("0.##").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
      filterPercentageValue = filterValue / 100
    }
  override var serializedData = data
}

open class FilterDateType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    val assetSold = stockItem.assets.filter { asset ->
      asset.quantity < 0.0
    }

    return if (assetSold.isNotEmpty()) {
      val newestAssetSoldDate = assetSold.maxOf { asset ->
        asset.date
      }
      newestAssetSoldDate > filterDateValue
    } else {
      false
    }
  }

  var filterDateValue: Long = 0L

  override val dataType = FilterDataTypeEnum.DateType
  override val displayName = ""
  override val desc = ""
  override var data: String = ""
    get() = LocalDateTime.ofEpochSecond(filterDateValue, 0, ZoneOffset.UTC)
        .format(DateTimeFormatter.ofLocalizedDate(FULL))
    set(value) {
      field = value
      filterDateValue = try {
        value.toLong()
      } catch (e: Exception) {
        0L
      }
    }
  override var serializedData = ""
    get() = filterDateValue.toString()
    set(value) {
      field = value
    }
}

class FilterFirstAssetSoldBeforeType(
  override val typeId: FilterTypeEnum,
  context: Context
) : FilterDateType(typeId, context) {
  override fun filter(stockItem: StockItem): Boolean {
    val assetSold = stockItem.assets.filter { asset ->
      asset.quantity < 0.0
    }

    return if (assetSold.isNotEmpty()) {
      val firstAssetDate = assetSold.minOf { asset ->
        asset.date
      }
      firstAssetDate < filterDateValue
    } else {
      false
    }
  }

  override var displayName = context.getString(R.string.filter_firstassetsoldbefore_name)
  override val desc = context.getString(R.string.filter_firstassetsoldbefore_desc)
}

class FilterFirstAssetSoldAfterType(
  override val typeId: FilterTypeEnum,
  context: Context
) : FilterDateType(typeId, context) {
  override fun filter(stockItem: StockItem): Boolean {
    val assetSold = stockItem.assets.filter { asset ->
      asset.quantity < 0.0
    }

    return if (assetSold.isNotEmpty()) {
      val firstAssetDate = assetSold.minOf { asset ->
        asset.date
      }
      firstAssetDate > filterDateValue
    } else {
      false
    }
  }

  override var displayName = context.getString(R.string.filter_firstassetsoldafter_name)
  override val desc = context.getString(R.string.filter_firstassetsoldafter_desc)
}

class FilterFirstAssetBoughtBeforeType(
  override val typeId: FilterTypeEnum,
  context: Context
) : FilterDateType(typeId, context) {
  override fun filter(stockItem: StockItem): Boolean {
    val assetBought = stockItem.assets.filter { asset ->
      asset.quantity > 0.0
    }

    return if (assetBought.isNotEmpty()) {
      val firstAssetDate = assetBought.minOf { asset ->
        asset.date
      }
      firstAssetDate < filterDateValue
    } else {
      false
    }
  }

  override var displayName = context.getString(R.string.filter_firstassetboughtbefore_name)
  override val desc = context.getString(R.string.filter_firstassetboughtbefore_desc)
}

class FilterFirstAssetBoughtAfterType(
  override val typeId: FilterTypeEnum,
  context: Context
) : FilterDateType(typeId, context) {
  override fun filter(stockItem: StockItem): Boolean {
    val assetBought = stockItem.assets.filter { asset ->
      asset.quantity > 0.0
    }

    return if (assetBought.isNotEmpty()) {
      val firstAssetDate = assetBought.minOf { asset ->
        asset.date
      }
      firstAssetDate > filterDateValue
    } else {
      false
    }
  }

  override var displayName = context.getString(R.string.filter_firstassetboughtafter_name)
  override val desc = context.getString(R.string.filter_firstassetboughtafter_desc)
}

class FilterLastAssetSoldBeforeType(
  override val typeId: FilterTypeEnum,
  context: Context
) : FilterDateType(typeId, context) {
  override fun filter(stockItem: StockItem): Boolean {
    val assetSold = stockItem.assets.filter { asset ->
      asset.quantity < 0.0
    }

    return if (assetSold.isNotEmpty()) {
      val lastAssetDate = assetSold.maxOf { asset ->
        asset.date
      }
      lastAssetDate < filterDateValue
    } else {
      false
    }
  }

  override var displayName = context.getString(R.string.filter_lastassetsoldbefore_name)
  override val desc = context.getString(R.string.filter_lastassetsoldbefore_desc)
}

class FilterLastAssetSoldAfterType(
  override val typeId: FilterTypeEnum,
  context: Context
) : FilterDateType(typeId, context) {
  override fun filter(stockItem: StockItem): Boolean {
    val assetSold = stockItem.assets.filter { asset ->
      asset.quantity < 0.0
    }

    return if (assetSold.isNotEmpty()) {
      val lastAssetDate = assetSold.maxOf { asset ->
        asset.date
      }
      lastAssetDate > filterDateValue
    } else {
      false
    }
  }

  override var displayName = context.getString(R.string.filter_lastassetsoldafter_name)
  override val desc = context.getString(R.string.filter_lastassetsoldafter_desc)
}

class FilterLastAssetBoughtBeforeType(
  override val typeId: FilterTypeEnum,
  context: Context
) : FilterDateType(typeId, context) {
  override fun filter(stockItem: StockItem): Boolean {
    val assetBought = stockItem.assets.filter { asset ->
      asset.quantity > 0.0
    }

    return if (assetBought.isNotEmpty()) {
      val lastAssetDate = assetBought.maxOf { asset ->
        asset.date
      }
      lastAssetDate < filterDateValue
    } else {
      false
    }
  }

  override var displayName = context.getString(R.string.filter_lastassetboughtbefore_name)
  override val desc = context.getString(R.string.filter_lastassetboughtbefore_desc)
}

class FilterLastAssetBoughtAfterType(
  override val typeId: FilterTypeEnum,
  context: Context
) : FilterDateType(typeId, context) {
  override fun filter(stockItem: StockItem): Boolean {
    val assetBought = stockItem.assets.filter { asset ->
      asset.quantity > 0.0
    }

    return if (assetBought.isNotEmpty()) {
      val lastAssetDate = assetBought.maxOf { asset ->
        asset.date
      }
      lastAssetDate > filterDateValue
    } else {
      false
    }
  }

  override var displayName = context.getString(R.string.filter_lastassetboughtafter_name)
  override val desc = context.getString(R.string.filter_lastassetboughtafter_desc)
}

// Stocks are at least one year old.
class FilterLongTermType(
  override val typeId: FilterTypeEnum,
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    val assetBought = stockItem.assets.filter { asset ->
      asset.quantity > 0.0
    }

    return if (assetBought.isNotEmpty()) {
      val secondsNow = LocalDateTime.now()
          .toEpochSecond(ZoneOffset.UTC)
      val lastAssetDate = assetBought.maxOf { asset ->
        asset.date
      }
      // 365 plus one day
      val secondsPerYear: Long = 366 * 24 * 60 * 60
      secondsNow > lastAssetDate + secondsPerYear
    } else {
      false
    }
  }

  override val dataType = FilterDataTypeEnum.NoType
  override var displayName = context.getString(R.string.filter_longterm_name)
  override var data = ""
  override var serializedData = data
  override val desc = context.getString(R.string.filter_longterm_desc)
}
