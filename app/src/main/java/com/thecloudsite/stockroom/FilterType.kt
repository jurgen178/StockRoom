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
import com.thecloudsite.stockroom.utils.getAssetsCapitalGain
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.FULL

enum class FilterTypeEnum {
  FilterNullType,
  FilterPercentageChangeType,
  FilterSymbolNameType,
  FilterDisplayNameType,
  FilterNoteType,
  FilterDividendNoteType,
  FilterPurchasePriceType,
  FilterProfitType,
  FilterProfitPercentageType,
  FilterAssetType,
  FilterDividendPercentageType,
  FilterQuantityType,
  FilterCapitalGainType,
  FilterFirstAssetSoldType,
  FilterFirstAssetBoughtType,
  FilterLastAssetSoldType,
  FilterLastAssetBoughtType,
  FilterLongTermType,
}

enum class FilterDataTypeEnum(val value: Int) {
  NoType(0),
  TextType(1),
  DoubleType(2),
  DateType(3),
  IntType(4),
}

enum class FilterModeTypeEnum(val value: Int) {
  AndType(0),
  OrType(1),
}

enum class FilterSubTypeEnum(var value: String) {
  GreaterThanType(""),
  LessThanType(""),
  BeforeDateType(""),
  AfterDateType(""),
  ContainsTextType(""),
  NotContainsTextType(""),
  IsEmptyTextType(""),
  IsNotEmptyTextType(""),
  StartsWithTextType(""),
  EndsWithTextType(""),
  IsTextType(""),
  IsNotTextType(""),
}

object FilterFactory {
  fun create(
    type: FilterTypeEnum,
    context: Context
  ): IFilterType =
    when (type) {
      FilterTypeEnum.FilterNullType -> FilterNullType(context)
      FilterTypeEnum.FilterPercentageChangeType -> FilterPercentageChangeType(context)
      FilterTypeEnum.FilterSymbolNameType -> FilterSymbolNameType(context)
      FilterTypeEnum.FilterDisplayNameType -> FilterDisplayNameType(context)
      FilterTypeEnum.FilterNoteType -> FilterNoteType(context)
      FilterTypeEnum.FilterDividendNoteType -> FilterDividendNoteType(context)
      FilterTypeEnum.FilterPurchasePriceType -> FilterPurchasePriceType(context)
      FilterTypeEnum.FilterProfitType -> FilterProfitType(context)
      FilterTypeEnum.FilterProfitPercentageType -> FilterProfitPercentageType(context)
      FilterTypeEnum.FilterAssetType -> FilterAssetType(context)
      FilterTypeEnum.FilterDividendPercentageType -> FilterDividendPercentageType(context)
      FilterTypeEnum.FilterQuantityType -> FilterQuantityType(context)
      FilterTypeEnum.FilterCapitalGainType -> FilterCapitalGainType(context)
      FilterTypeEnum.FilterFirstAssetSoldType -> FilterFirstAssetSoldType(context)
      FilterTypeEnum.FilterFirstAssetBoughtType -> FilterFirstAssetBoughtType(context)
      FilterTypeEnum.FilterLastAssetSoldType -> FilterLastAssetSoldType(context)
      FilterTypeEnum.FilterLastAssetBoughtType -> FilterLastAssetBoughtType(context)
      FilterTypeEnum.FilterLongTermType -> FilterLongTermType(context)
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
    return FilterNullType(context)
  }

  fun create(
    index: Int,
    context: Context
  ): IFilterType =

//    // + 1, skip NullFilter
//    if (index >= 0 && index + 1 < FilterTypeEnum.values().size) {
//      val type: FilterTypeEnum = FilterTypeEnum.values()[index + 1]
//      create(type, context)
//    } else {
//      FilterNullType()
//    }

    if (index >= 0 && index < FilterTypeEnum.values().size) {
      val type: FilterTypeEnum = FilterTypeEnum.values()[index]
      create(type, context)
    } else {
      FilterNullType(context)
    }
}

fun initSubTypeList(context: Context) {
  FilterSubTypeEnum.GreaterThanType.value = context.getString(R.string.filter_GreaterThanType)
  FilterSubTypeEnum.LessThanType.value = context.getString(R.string.filter_LessThanType)
  FilterSubTypeEnum.BeforeDateType.value = context.getString(R.string.filter_BeforeDateType)
  FilterSubTypeEnum.AfterDateType.value = context.getString(R.string.filter_AfterDateType)
  FilterSubTypeEnum.ContainsTextType.value = context.getString(R.string.filter_ContainsTextType)
  FilterSubTypeEnum.NotContainsTextType.value =
    context.getString(R.string.filter_NotContainsTextType)
  FilterSubTypeEnum.IsEmptyTextType.value = context.getString(R.string.filter_IsEmptyTextType)
  FilterSubTypeEnum.IsNotEmptyTextType.value = context.getString(R.string.filter_IsNotEmptyTextType)
  FilterSubTypeEnum.StartsWithTextType.value = context.getString(R.string.filter_StartsWithTextType)
  FilterSubTypeEnum.EndsWithTextType.value = context.getString(R.string.filter_EndsWithTextType)
  FilterSubTypeEnum.IsTextType.value = context.getString(R.string.filter_IsTextType)
  FilterSubTypeEnum.IsNotTextType.value = context.getString(R.string.filter_IsNotTextType)
}

fun getFilterTypeList(context: Context): List<String> {
  val filterList = mutableListOf<String>()

  FilterTypeEnum.values()
//      .filter { type ->
//        type != FilterTypeEnum.FilterNullType
//      }
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

private fun strToInt(str: String): Int {
  var value: Int = 0
  try {
    val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
    value = numberFormat.parse(str)!!
        .toInt()
  } catch (e: Exception) {
  }

  return value
}

interface IFilterType {
  fun filter(stockItem: StockItem): Boolean
  val typeId: FilterTypeEnum
  val dataType: FilterDataTypeEnum
  val subTypeList: List<FilterSubTypeEnum>
  var subTypeIndex: Int
  val displayName: String
  val desc: String
  var data: String
  val serializedData: String
}

open class FilterBaseType(
  context: Context
) : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return false
  }

  override val typeId = FilterTypeEnum.FilterNullType
  override val dataType = FilterDataTypeEnum.NoType
  override val subTypeList = listOf<FilterSubTypeEnum>()
  override var subTypeIndex = 0
    set(value) {
      field = if (value >= 0 && value < subTypeList.size) {
        value
      } else {
        0
      }
    }
  override val displayName = ""
  override val desc = ""
  override var data = ""
  override val serializedData
    get() = data
}

open class FilterTextType(
  context: Context
) : FilterBaseType(context) {

  override val typeId = FilterTypeEnum.FilterNullType
  override val dataType = FilterDataTypeEnum.TextType
}

open class FilterDoubleType(
  context: Context
) : FilterBaseType(context) {

  var filterValue: Double = 0.0

  override val typeId = FilterTypeEnum.FilterNullType
  override val dataType = FilterDataTypeEnum.DoubleType
  override val subTypeList =
    listOf(FilterSubTypeEnum.GreaterThanType, FilterSubTypeEnum.LessThanType)
  override var data: String = ""
    get() = DecimalFormat("0.00").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
    }
}

open class FilterDoublePercentageType(
  context: Context
) : FilterBaseType(context) {

  private var filterValue: Double = 0.0
  var filterPercentageValue: Double = 0.0

  override val typeId = FilterTypeEnum.FilterNullType
  override val dataType = FilterDataTypeEnum.DoubleType
  override val subTypeList =
    listOf(FilterSubTypeEnum.GreaterThanType, FilterSubTypeEnum.LessThanType)
  override var data: String = ""
    get() = DecimalFormat("0.##").format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
      filterPercentageValue = filterValue / 100
    }
}

open class FilterIntType(
  context: Context
) : FilterBaseType(context) {

  var filterValue: Int = 0

  override val dataType = FilterDataTypeEnum.IntType
  override val subTypeList =
    listOf(FilterSubTypeEnum.GreaterThanType, FilterSubTypeEnum.LessThanType)
  override var data: String = ""
    get() = filterValue.toString()
    set(value) {
      field = value
      filterValue = strToInt(value)
    }
}

open class FilterDateType(
  context: Context
) : FilterBaseType(context) {

  var filterDateValue: Long = 0L

  override val dataType = FilterDataTypeEnum.DateType
  override val subTypeList =
    listOf(FilterSubTypeEnum.BeforeDateType, FilterSubTypeEnum.AfterDateType)
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
  override val serializedData
    get() = filterDateValue.toString()
}

//class FilterTestType(override val typeId: FilterTypeEnum) : IFilterType {
//  override fun filter(stockItem: StockItem): Boolean {
//    return stockItem.stockDBdata.symbol.isNotEmpty()
//  }
//
//  override val dataType = FilterDataTypeEnum.NoType
//  override val displayName = typeId.toString()
//  override var data = ""
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
//  override val desc = ""
//}
//
//class FilterDoubleType(override val typeId: FilterTypeEnum) : IFilterType {
//  override fun filter(stockItem: StockItem): Boolean {
//    return stockItem.stockDBdata.symbol.startsWith("A")
//  }
//
//  override val dataType = FilterDataTypeEnum.DoubleType
//  override val displayName = typeId.toString()
//  override var data: String = ""
//    get() = field
//    set(value) {
//      field = value
//    }
//  override val desc = ""
//}

// No filtering, always returns true
class FilterNullType(
  context: Context
) : FilterBaseType(context) {
  override fun filter(stockItem: StockItem): Boolean {
    return true
  }

  override val typeId = FilterTypeEnum.FilterNullType
  override val displayName = context.getString(R.string.filter_null_name)
  override val desc = context.getString(R.string.filter_null_desc)
}

// Change percentage
class FilterPercentageChangeType(
  context: Context
) : FilterDoubleType(context) {
  override fun filter(stockItem: StockItem): Boolean {
    return when {
      subTypeList[subTypeIndex] == FilterSubTypeEnum.GreaterThanType -> {
        stockItem.onlineMarketData.marketChangePercent > filterValue
      }
      subTypeList[subTypeIndex] == FilterSubTypeEnum.LessThanType -> {
        stockItem.onlineMarketData.marketChangePercent < filterValue
      }
      else -> false
    }
  }

  override val typeId = FilterTypeEnum.FilterPercentageChangeType
  override val displayName = context.getString(R.string.filter_percentagechange_name)
  override val desc = context.getString(R.string.filter_percentagechange_desc)
}

class FilterSymbolNameType(
  context: Context
) : FilterTextType(context) {
  override fun filter(stockItem: StockItem): Boolean {
    return when {
      subTypeList[subTypeIndex] == FilterSubTypeEnum.ContainsTextType -> {
        stockItem.stockDBdata.symbol.contains(data, ignoreCase = true)
      }
      subTypeList[subTypeIndex] == FilterSubTypeEnum.NotContainsTextType -> {
        !stockItem.stockDBdata.symbol.contains(data, ignoreCase = true)
      }
      subTypeList[subTypeIndex] == FilterSubTypeEnum.StartsWithTextType -> {
        stockItem.stockDBdata.symbol.startsWith(data, ignoreCase = true)
      }
      subTypeList[subTypeIndex] == FilterSubTypeEnum.EndsWithTextType -> {
        stockItem.stockDBdata.symbol.endsWith(data, ignoreCase = true)
      }
      subTypeList[subTypeIndex] == FilterSubTypeEnum.IsTextType -> {
        stockItem.stockDBdata.symbol.equals(data, ignoreCase = true)
      }
      subTypeList[subTypeIndex] == FilterSubTypeEnum.IsNotTextType -> {
        !stockItem.stockDBdata.symbol.equals(data, ignoreCase = true)
      }
      else -> false
    }
  }

  override val subTypeList =
    listOf(
        FilterSubTypeEnum.ContainsTextType,
        FilterSubTypeEnum.NotContainsTextType,
        FilterSubTypeEnum.StartsWithTextType,
        FilterSubTypeEnum.EndsWithTextType,
        FilterSubTypeEnum.IsTextType,
        FilterSubTypeEnum.IsNotTextType
    )
  override val typeId = FilterTypeEnum.FilterSymbolNameType
  override val displayName = context.getString(R.string.filter_symbolname_name)
  override val desc = context.getString(R.string.filter_symbolname_desc)
}

class FilterDisplayNameType(
  context: Context
) : FilterTextType(context) {
  override fun filter(stockItem: StockItem): Boolean {
    return when {
      subTypeList[subTypeIndex] == FilterSubTypeEnum.ContainsTextType -> {
        getName(stockItem.onlineMarketData).contains(data, ignoreCase = true)
      }
      subTypeList[subTypeIndex] == FilterSubTypeEnum.NotContainsTextType -> {
        !getName(stockItem.onlineMarketData).contains(data, ignoreCase = true)
      }
      else -> false
    }
  }

  override val subTypeList =
    listOf(FilterSubTypeEnum.ContainsTextType, FilterSubTypeEnum.NotContainsTextType)
  override val typeId = FilterTypeEnum.FilterDisplayNameType
  override val displayName = context.getString(R.string.filter_displayname_name)
  override val desc = context.getString(R.string.filter_displayname_desc)
}

class FilterNoteType(
  context: Context
) : FilterTextType(context) {
  override fun filter(stockItem: StockItem): Boolean {
    return when {
      subTypeList[subTypeIndex] == FilterSubTypeEnum.ContainsTextType -> {
        stockItem.stockDBdata.note.contains(data, ignoreCase = true)
      }
      subTypeList[subTypeIndex] == FilterSubTypeEnum.NotContainsTextType -> {
        !stockItem.stockDBdata.note.contains(data, ignoreCase = true)
      }
      subTypeList[subTypeIndex] == FilterSubTypeEnum.IsEmptyTextType -> {
        stockItem.stockDBdata.note.isEmpty()
      }
      subTypeList[subTypeIndex] == FilterSubTypeEnum.IsNotEmptyTextType -> {
        stockItem.stockDBdata.note.isNotEmpty()
      }
      else -> false
    }
  }

  override val subTypeList =
    listOf(
        FilterSubTypeEnum.ContainsTextType,
        FilterSubTypeEnum.NotContainsTextType,
        FilterSubTypeEnum.IsEmptyTextType,
        FilterSubTypeEnum.IsNotEmptyTextType
    )
  override val typeId = FilterTypeEnum.FilterNoteType
  override val displayName = context.getString(R.string.filter_note_name)
  override val desc = context.getString(R.string.filter_note_desc)
}

class FilterDividendNoteType(
  context: Context
) : FilterTextType(context) {
  override fun filter(stockItem: StockItem): Boolean {
    return when {
      subTypeList[subTypeIndex] == FilterSubTypeEnum.ContainsTextType -> {
        stockItem.stockDBdata.dividendNote.contains(data, ignoreCase = true)
      }
      subTypeList[subTypeIndex] == FilterSubTypeEnum.NotContainsTextType -> {
        !stockItem.stockDBdata.dividendNote.contains(data, ignoreCase = true)
      }
      subTypeList[subTypeIndex] == FilterSubTypeEnum.IsEmptyTextType -> {
        stockItem.stockDBdata.dividendNote.isEmpty()
      }
      subTypeList[subTypeIndex] == FilterSubTypeEnum.IsNotEmptyTextType -> {
        stockItem.stockDBdata.dividendNote.isNotEmpty()
      }
      else -> false
    }
  }

  override val subTypeList =
    listOf(
        FilterSubTypeEnum.ContainsTextType,
        FilterSubTypeEnum.NotContainsTextType,
        FilterSubTypeEnum.IsEmptyTextType,
        FilterSubTypeEnum.IsNotEmptyTextType
    )
  override val typeId = FilterTypeEnum.FilterDividendNoteType
  override val displayName = context.getString(R.string.filter_dividendnote_name)
  override val desc = context.getString(R.string.filter_dividendnote_desc)
}

// Purchase price
class FilterPurchasePriceType(
  context: Context
) : FilterDoubleType(context) {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice) = getAssets(stockItem.assets)

    return when {
      subTypeList[subTypeIndex] == FilterSubTypeEnum.GreaterThanType -> {
        totalPrice > filterValue
      }
      subTypeList[subTypeIndex] == FilterSubTypeEnum.LessThanType -> {
        totalPrice < filterValue
      }
      else -> false
    }
  }

  override val typeId = FilterTypeEnum.FilterPurchasePriceType
  override val displayName = context.getString(R.string.filter_purchaseprice_name)
  override val desc = context.getString(R.string.filter_purchaseprice_desc)
}

// Profit
class FilterProfitType(
  context: Context
) : FilterDoubleType(context) {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice) = getAssets(stockItem.assets)
    val profit = if (stockItem.onlineMarketData.marketPrice > 0.0) {
      totalQuantity * stockItem.onlineMarketData.marketPrice - totalPrice
    } else {
      totalPrice
    }

    return when {
      subTypeList[subTypeIndex] == FilterSubTypeEnum.GreaterThanType -> {
        profit > filterValue
      }
      subTypeList[subTypeIndex] == FilterSubTypeEnum.LessThanType -> {
        profit < filterValue
      }
      else -> false
    }
  }

  override val typeId = FilterTypeEnum.FilterProfitType
  override val displayName = context.getString(R.string.filter_profit_name)
  override val desc = context.getString(R.string.filter_profit_desc)
}

// Profit Percentage
class FilterProfitPercentageType(
  context: Context
) : FilterDoublePercentageType(context) {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice) = getAssets(stockItem.assets)
    val profitPercentage =
      if (stockItem.onlineMarketData.marketPrice > 0.0 && totalPrice > 0.0) {
        (totalQuantity * stockItem.onlineMarketData.marketPrice - totalPrice) / totalPrice
      } else {
        totalPrice
      }

    return when {
      subTypeList[subTypeIndex] == FilterSubTypeEnum.GreaterThanType -> {
        profitPercentage > filterPercentageValue
      }
      subTypeList[subTypeIndex] == FilterSubTypeEnum.LessThanType -> {
        profitPercentage < filterPercentageValue
      }
      else -> false
    }
  }

  override val typeId = FilterTypeEnum.FilterProfitPercentageType
  override val displayName = context.getString(R.string.filter_profitpercentage_name)
  override val desc = context.getString(R.string.filter_profitpercentage_desc)
}

// Asset
class FilterAssetType(
  context: Context
) : FilterDoubleType(context) {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice) = getAssets(stockItem.assets)
    val asset = if (stockItem.onlineMarketData.marketPrice > 0.0) {
      totalQuantity * stockItem.onlineMarketData.marketPrice
    } else {
      totalPrice
    }

    return when {
      subTypeList[subTypeIndex] == FilterSubTypeEnum.GreaterThanType -> {
        asset > filterValue
      }
      subTypeList[subTypeIndex] == FilterSubTypeEnum.LessThanType -> {
        asset < filterValue
      }
      else -> false
    }
  }

  override val typeId = FilterTypeEnum.FilterAssetType
  override val displayName = context.getString(R.string.filter_asset_name)
  override val desc = context.getString(R.string.filter_asset_desc)
}

// Dividend Percentage
class FilterDividendPercentageType(
  context: Context
) : FilterDoublePercentageType(context) {
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

    return when {
      subTypeList[subTypeIndex] == FilterSubTypeEnum.GreaterThanType -> {
        dividendPercentage > filterPercentageValue
      }
      subTypeList[subTypeIndex] == FilterSubTypeEnum.LessThanType -> {
        dividendPercentage < filterPercentageValue
      }
      else -> false
    }
  }

  override val typeId = FilterTypeEnum.FilterDividendPercentageType
  override val displayName = context.getString(R.string.filter_dividendpercentage_name)
  override val desc = context.getString(R.string.filter_dividendpercentage_desc)
}

// Quantity
class FilterQuantityType(
  context: Context
) : FilterIntType(context) {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice) = getAssets(stockItem.assets)

    return when {
      subTypeList[subTypeIndex] == FilterSubTypeEnum.GreaterThanType -> {
        totalQuantity > filterValue
      }
      subTypeList[subTypeIndex] == FilterSubTypeEnum.LessThanType -> {
        totalQuantity < filterValue
      }
      else -> false
    }
  }

  override val typeId = FilterTypeEnum.FilterQuantityType
  override val displayName = context.getString(R.string.filter_quantity_name)
  override val desc = context.getString(R.string.filter_quantity_desc)
}

// CapitalGain
class FilterCapitalGainType(
  context: Context
) : FilterDoubleType(context) {
  override fun filter(stockItem: StockItem): Boolean {
    val (capitalGain, capitalLoss) = getAssetsCapitalGain(stockItem.assets)

    return when {
      subTypeList[subTypeIndex] == FilterSubTypeEnum.GreaterThanType -> {
        capitalGain - capitalLoss > filterValue
      }
      subTypeList[subTypeIndex] == FilterSubTypeEnum.LessThanType -> {
        capitalGain - capitalLoss < filterValue
      }
      else -> false
    }
  }

  override val typeId = FilterTypeEnum.FilterCapitalGainType
  override val displayName = context.getString(R.string.filter_capitalgain_name)
  override val desc = context.getString(R.string.filter_capitalgain_desc)
}

class FilterFirstAssetSoldType(
  context: Context
) : FilterDateType(context) {
  override fun filter(stockItem: StockItem): Boolean {
    val assetSold = stockItem.assets.filter { asset ->
      asset.quantity < 0.0
    }

    return if (assetSold.isNotEmpty()) {
      val firstAssetDate = assetSold.minOf { asset ->
        asset.date
      }

      when {
        subTypeList[subTypeIndex] == FilterSubTypeEnum.BeforeDateType -> {
          firstAssetDate < filterDateValue
        }
        subTypeList[subTypeIndex] == FilterSubTypeEnum.AfterDateType -> {
          firstAssetDate > filterDateValue
        }
        else -> false
      }
    } else {
      false
    }
  }

  override val typeId = FilterTypeEnum.FilterFirstAssetSoldType
  override val displayName = context.getString(R.string.filter_firstassetsold_name)
  override val desc = context.getString(R.string.filter_firstassetsold_desc)
}

class FilterFirstAssetBoughtType(
  context: Context
) : FilterDateType(context) {
  override fun filter(stockItem: StockItem): Boolean {
    val assetBought = stockItem.assets.filter { asset ->
      asset.quantity > 0.0
    }

    return if (assetBought.isNotEmpty()) {
      val firstAssetDate = assetBought.minOf { asset ->
        asset.date
      }

      when {
        subTypeList[subTypeIndex] == FilterSubTypeEnum.BeforeDateType -> {
          firstAssetDate < filterDateValue
        }
        subTypeList[subTypeIndex] == FilterSubTypeEnum.AfterDateType -> {
          firstAssetDate > filterDateValue
        }
        else -> false
      }
    } else {
      false
    }
  }

  override val typeId = FilterTypeEnum.FilterFirstAssetBoughtType
  override val displayName = context.getString(R.string.filter_firstassetbought_name)
  override val desc = context.getString(R.string.filter_firstassetbought_desc)
}

class FilterLastAssetSoldType(
  context: Context
) : FilterDateType(context) {
  override fun filter(stockItem: StockItem): Boolean {
    val assetSold = stockItem.assets.filter { asset ->
      asset.quantity < 0.0
    }

    return if (assetSold.isNotEmpty()) {
      val lastAssetDate = assetSold.maxOf { asset ->
        asset.date
      }
      when {
        subTypeList[subTypeIndex] == FilterSubTypeEnum.BeforeDateType -> {
          lastAssetDate < filterDateValue
        }
        subTypeList[subTypeIndex] == FilterSubTypeEnum.AfterDateType -> {
          lastAssetDate > filterDateValue
        }
        else -> false
      }
    } else {
      false
    }
  }

  override val typeId = FilterTypeEnum.FilterLastAssetSoldType
  override val displayName = context.getString(R.string.filter_lastassetsold_name)
  override val desc = context.getString(R.string.filter_lastassetsold_desc)
}

class FilterLastAssetBoughtType(
  context: Context
) : FilterDateType(context) {
  override fun filter(stockItem: StockItem): Boolean {
    val assetBought = stockItem.assets.filter { asset ->
      asset.quantity > 0.0
    }

    return if (assetBought.isNotEmpty()) {
      val lastAssetDate = assetBought.maxOf { asset ->
        asset.date
      }
      when {
        subTypeList[subTypeIndex] == FilterSubTypeEnum.BeforeDateType -> {
          lastAssetDate < filterDateValue
        }
        subTypeList[subTypeIndex] == FilterSubTypeEnum.AfterDateType -> {
          lastAssetDate > filterDateValue
        }
        else -> false
      }
    } else {
      false
    }
  }

  override val typeId = FilterTypeEnum.FilterLastAssetBoughtType
  override val displayName = context.getString(R.string.filter_lastassetbought_name)
  override val desc = context.getString(R.string.filter_lastassetbought_desc)
}

// Stocks are at least one year old.
class FilterLongTermType(
  context: Context
) : FilterBaseType(context) {
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

  override val typeId = FilterTypeEnum.FilterLongTermType
  override val displayName = context.getString(R.string.filter_longterm_name)
  override val desc = context.getString(R.string.filter_longterm_desc)
}
