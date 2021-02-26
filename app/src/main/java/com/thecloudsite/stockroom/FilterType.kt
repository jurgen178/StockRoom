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
import android.text.SpannableStringBuilder
import androidx.core.text.backgroundColor
import androidx.core.text.color
import com.thecloudsite.stockroom.R.array
import com.thecloudsite.stockroom.R.string
import com.thecloudsite.stockroom.database.Group
import com.thecloudsite.stockroom.utils.DecimalFormat0To2Digits
import com.thecloudsite.stockroom.utils.DecimalFormat2Digits
import com.thecloudsite.stockroom.utils.formatInt
import com.thecloudsite.stockroom.utils.getAssets
import com.thecloudsite.stockroom.utils.getAssetsCapitalGain
import com.thecloudsite.stockroom.utils.getGroupsMenuList
import com.thecloudsite.stockroom.utils.isSimilarColor
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.FULL
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.IGNORE_CASE

enum class FilterTypeEnum {
  FilterNullType,
  FilterPercentageChangeType,
  FilterSymbolNameType,
  FilterDisplayNameType,
  FilterQuoteType,
  FilterStockExchangeNameType,
  FilterMarketCapType,
  FilterGroupType,
  FilterNoteType,
  FilterDividendNoteType,
  FilterAlertType,
  FilterAlertNoteType,
  FilterEventType,
  FilterEventDetailType,
  FilterPurchasePriceType,
  FilterProfitType,
  FilterProfitPercentageType,
  FilterAssetType,
  FilterAssetNoteType,
  FilterCommissionType,
  FilterDividendPercentageType,
  FilterDividendPaidType,
  FilterDividendPaidYTDType,
  FilterQuantityType,
  FilterCapitalGainType,
  FilterPostMarketType,
  FilterAssetSoldDateType,
  FilterAssetBoughtDateType,
  FilterLongTermType,
}

enum class FilterDataTypeEnum(val value: Int) {
  NoType(0),
  TextType(1),
  DoubleType(2),
  DateType(3),
  IntType(4),
  SelectionType(5),
}

enum class FilterModeTypeEnum(val value: Int) {
  AndType(0),
  OrType(1),
}

// Do not change existing names. Breaks serialization with saved filters.
enum class FilterSubTypeEnum(var value: String) {
  NoType(""),
  GreaterThanType(""),
  LessThanType(""),
  EqualType(""),
  BeforeDateType(""),
  AfterDateType(""),
  ContainsTextType(""),
  NotContainsTextType(""),
  SimilarTextType(""),
  NotSimilarTextType(""),
  IsEmptyTextType(""),
  IsNotEmptyTextType(""),
  StartsWithTextType(""),
  EndsWithTextType(""),
  IsTextType(""),
  IsNotTextType(""),
  IsType(""),
  IsNotType(""),
  MatchRegexTextType(""),
  NotMatchRegexTextType(""),
  IsPresentType(""),
  IsOnePresentType(""),
  IsAllPresentType(""),
  IsNotPresentType(""),
  IsUsedType(""),
  IsNotUsedType(""),
  IsMarketLargeCapType(""),
  IsMarketSmallCapType(""),
  IsMarketMidCapType(""),
  IsMarketMicroCapType(""),
  IsMarketNanoCapType(""),
}

val regexOption = setOf(IGNORE_CASE, DOT_MATCHES_ALL)
const val similarDistance = 0.3

object SharedFilterGroupList {
  var groups: List<Group> = emptyList()
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
      FilterTypeEnum.FilterMarketCapType -> FilterMarketCapType(context)
      FilterTypeEnum.FilterQuoteType -> FilterQuoteType(context)
      FilterTypeEnum.FilterStockExchangeNameType -> FilterStockExchangeNameType(context)
      FilterTypeEnum.FilterGroupType -> FilterGroupType(context)
      FilterTypeEnum.FilterNoteType -> FilterNoteType(context)
      FilterTypeEnum.FilterDividendNoteType -> FilterDividendNoteType(context)
      FilterTypeEnum.FilterAlertNoteType -> FilterAlertNoteType(context)
      FilterTypeEnum.FilterEventDetailType -> FilterEventDetailType(context)
      FilterTypeEnum.FilterPurchasePriceType -> FilterPurchasePriceType(context)
      FilterTypeEnum.FilterProfitType -> FilterProfitType(context)
      FilterTypeEnum.FilterProfitPercentageType -> FilterProfitPercentageType(context)
      FilterTypeEnum.FilterAssetType -> FilterAssetType(context)
      FilterTypeEnum.FilterAssetNoteType -> FilterAssetNoteType(context)
      FilterTypeEnum.FilterCommissionType -> FilterCommissionType(context)
      FilterTypeEnum.FilterDividendPercentageType -> FilterDividendPercentageType(context)
      FilterTypeEnum.FilterDividendPaidType -> FilterDividendPaidType(context)
      FilterTypeEnum.FilterDividendPaidYTDType -> FilterDividendPaidYTDType(context)
      FilterTypeEnum.FilterQuantityType -> FilterQuantityType(context)
      FilterTypeEnum.FilterCapitalGainType -> FilterCapitalGainType(context)
      FilterTypeEnum.FilterPostMarketType -> FilterPostMarketType(context)
      FilterTypeEnum.FilterAlertType -> FilterAlertType(context)
      FilterTypeEnum.FilterEventType -> FilterEventType(context)
      FilterTypeEnum.FilterAssetSoldDateType -> FilterAssetSoldDateType(context)
      FilterTypeEnum.FilterAssetBoughtDateType -> FilterAssetBoughtDateType(context)
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

interface IFilterType {
  fun filter(stockItem: StockItem): Boolean
  fun dataReady()
  val typeId: FilterTypeEnum
  val dataType: FilterDataTypeEnum
  val subTypeList: List<FilterSubTypeEnum>
  val selectionList: List<SpannableStringBuilder>
  var subType: FilterSubTypeEnum
  val displayName: String
  val desc: String
  var data: String
  val serializedData: String
  val displayData: SpannableStringBuilder
}

open class FilterBaseType : IFilterType {
  override fun filter(stockItem: StockItem): Boolean {
    return false
  }

  override fun dataReady() {
  }

  override val typeId = FilterTypeEnum.FilterNullType
  override val dataType = FilterDataTypeEnum.NoType
  override val subTypeList = listOf<FilterSubTypeEnum>()
  override val selectionList = listOf<SpannableStringBuilder>()
  override var subType = FilterSubTypeEnum.NoType
    set(value) {
      field = if (subTypeList.contains(value)) {
        value
      } else {
        FilterSubTypeEnum.NoType
      }
    }
  override val displayName = ""
  override val desc = ""
  override var data = ""
  override val serializedData
    get() = data
  override val displayData: SpannableStringBuilder
    get() = SpannableStringBuilder().append(data)
}

open class FilterTextBaseType : FilterBaseType() {

  override val dataType = FilterDataTypeEnum.TextType
  override val displayData: SpannableStringBuilder
    get() {
      return when (subType) {
        FilterSubTypeEnum.IsEmptyTextType,
        FilterSubTypeEnum.IsNotEmptyTextType
        -> SpannableStringBuilder()
        else -> super.displayData
      }
    }
}

open class FilterRegexTextType : FilterTextBaseType() {

  var regex: Regex = "".toRegex()

  // Setup the regex when the data is ready to avoid creating a regex
  // from the string for each comparison.
  override fun dataReady() {
    if (subType == FilterSubTypeEnum.MatchRegexTextType
      || subType == FilterSubTypeEnum.NotMatchRegexTextType
    ) {
      try {
        regex = data.toRegex(regexOption)
      } catch (e: Exception) {
      }
    }
  }
}

open class FilterDoubleBaseType : FilterBaseType() {

  var filterValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override val subTypeList =
    listOf(
      FilterSubTypeEnum.GreaterThanType,
      FilterSubTypeEnum.LessThanType
    )
  override var data: String = ""
    get() = DecimalFormat(DecimalFormat2Digits).format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
    }
}

open class FilterDoublePercentageBaseType : FilterBaseType() {

  private var filterValue: Double = 0.0
  var filterPercentageValue: Double = 0.0

  override val dataType = FilterDataTypeEnum.DoubleType
  override val subTypeList =
    listOf(
      FilterSubTypeEnum.GreaterThanType,
      FilterSubTypeEnum.LessThanType
    )
  override var data: String = ""
    get() = DecimalFormat(DecimalFormat0To2Digits).format(filterValue)
    set(value) {
      field = value
      filterValue = strToDouble(value)
      filterPercentageValue = filterValue / 100
    }
}

open class FilterIntBaseType : FilterBaseType() {

  var filterValue: Int = 0

  override val dataType = FilterDataTypeEnum.IntType
  override val subTypeList =
    listOf(
      FilterSubTypeEnum.GreaterThanType,
      FilterSubTypeEnum.LessThanType,
      FilterSubTypeEnum.EqualType
    )
  override var data: String = ""
    get() = filterValue.toString()
    set(value) {
      field = value
      filterValue = strToInt(value)
    }
}

open class FilterDateBaseType : FilterBaseType() {

  var filterDateValue: Long = 0L

  override val dataType = FilterDataTypeEnum.DateType
  override val subTypeList =
    listOf(
      FilterSubTypeEnum.BeforeDateType,
      FilterSubTypeEnum.AfterDateType
    )
  override var data: String = ""
    get() = ZonedDateTime.ofInstant(
      Instant.ofEpochSecond(filterDateValue),
      ZoneOffset.systemDefault()
    )
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

open class FilterSelectionBaseType(open val context: Context) : FilterBaseType() {

  var filterSelectionIndex: Int = 0

  override val dataType = FilterDataTypeEnum.SelectionType
  override val subTypeList =
    listOf(
      FilterSubTypeEnum.IsType,
      FilterSubTypeEnum.IsNotType
    )
}

open class FilterQuoteTypeBaseType(override val context: Context) : FilterSelectionBaseType(
  context
) {

  var filterQuoteValue: String = ""

  override val selectionList: List<SpannableStringBuilder>
    get() {
      val quoteTypeNames = context.resources.getStringArray(array.quoteTypes)
      return quoteTypeNames.map {
        SpannableStringBuilder().append(it)
      }
    }
  override var data: String = ""
    get() = filterSelectionIndex.toString()
    set(value) {
      field = value
      filterSelectionIndex = strToInt(value)

      val quoteTypeNames = context.resources.getStringArray(array.quoteTypeNames)
      filterQuoteValue =
        if (filterSelectionIndex >= 0 && filterSelectionIndex < quoteTypeNames.size) {
          quoteTypeNames[filterSelectionIndex]
        } else {
          ""
        }
    }

  override val displayData: SpannableStringBuilder
    get() {
      val quoteTypes = context.resources.getStringArray(array.quoteTypes)

      return if (filterSelectionIndex >= 0 && filterSelectionIndex < quoteTypes.size) {
        SpannableStringBuilder().append(quoteTypes[filterSelectionIndex])
      } else {
        SpannableStringBuilder()
      }
    }
}

open class FilterGroupBaseType(override val context: Context) : FilterSelectionBaseType(context) {

  var filterGroupColorValue: Int = 0

  override val selectionList: List<SpannableStringBuilder>
    get() {
      return getGroupsMenuList(
        SharedFilterGroupList.groups,
        0,
        context.getColor(R.color.black),
        context.getString(string.standard_group)
      ).map {
        SpannableStringBuilder().append(it)
      }
    }

  override var data: String = ""
    get() = filterSelectionIndex.toString()
    set(value) {
      field = value
      filterSelectionIndex = strToInt(value)

      filterGroupColorValue =
        if (filterSelectionIndex >= 0 && filterSelectionIndex < SharedFilterGroupList.groups.size) {
          SharedFilterGroupList.groups[filterSelectionIndex].color
        } else {
          0
        }
    }

  override val displayData: SpannableStringBuilder
    get() {
      val group =
        if (filterSelectionIndex >= 0 && filterSelectionIndex < SharedFilterGroupList.groups.size) {
          SharedFilterGroupList.groups[filterSelectionIndex]
        } else {
          null
        }
      return if (group != null) {
        if (isSimilarColor(group.color, context.getColor(R.color.backgroundListColor))) {
          SpannableStringBuilder().backgroundColor(context.getColor(R.color.colorPrimary)) {
            color(group.color) {
              append(
                " ${group.name} "
              )
            }
          }
        } else {
          SpannableStringBuilder().color(group.color) {
            append(group.name)
          }
        }
      } else {
        SpannableStringBuilder().append(
          context.getString(R.string.standard_group)
        )
      }
    }
}

open class FilterBooleanBaseType(val context: Context) : FilterBaseType() {

  override val dataType = FilterDataTypeEnum.NoType
  override val subTypeList =
    listOf(
      FilterSubTypeEnum.IsType,
      FilterSubTypeEnum.IsNotType
    )

  // Boolean uses the subType for the bool content.
  override var data: String = ""
}

open class FilterDividendBaseType() : FilterDoubleBaseType() {

  var secondsYTD: Long = 0L

  override fun dataReady() {
    super.dataReady()

//    val datetimeYTD = LocalDateTime.of(LocalDateTime.now().year, 1, 1, 0, 0, 0)
//    secondsYTD = datetimeYTD.toEpochSecond(ZoneOffset.UTC)

    val datetimeYTD =
      ZonedDateTime.of(ZonedDateTime.now().year, 1, 1, 0, 0, 0, 0, ZoneOffset.systemDefault())
    secondsYTD = datetimeYTD.toEpochSecond()
  }
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
) : FilterBaseType() {
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
) : FilterDoubleBaseType() {
  override fun filter(stockItem: StockItem): Boolean {
    return when (subType) {
      FilterSubTypeEnum.GreaterThanType -> {
        stockItem.onlineMarketData.marketChangePercent > filterValue
      }
      FilterSubTypeEnum.LessThanType -> {
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
) : FilterRegexTextType() {
  override fun filter(stockItem: StockItem): Boolean {
    return when (subType) {
      FilterSubTypeEnum.ContainsTextType -> {
        stockItem.stockDBdata.symbol.contains(data, ignoreCase = true)
      }
      FilterSubTypeEnum.NotContainsTextType -> {
        !stockItem.stockDBdata.symbol.contains(data, ignoreCase = true)
      }
      FilterSubTypeEnum.StartsWithTextType -> {
        stockItem.stockDBdata.symbol.startsWith(data, ignoreCase = true)
      }
      FilterSubTypeEnum.EndsWithTextType -> {
        stockItem.stockDBdata.symbol.endsWith(data, ignoreCase = true)
      }
      FilterSubTypeEnum.IsTextType -> {
        stockItem.stockDBdata.symbol.equals(data, ignoreCase = true)
      }
      FilterSubTypeEnum.IsNotTextType -> {
        !stockItem.stockDBdata.symbol.equals(data, ignoreCase = true)
      }
      FilterSubTypeEnum.SimilarTextType -> {
        // 0.0: identical, 1.0: different
        getLevenshteinDistance(stockItem.stockDBdata.symbol, data) < similarDistance
      }
      FilterSubTypeEnum.NotSimilarTextType -> {
        // 0.0: identical, 1.0: different
        getLevenshteinDistance(stockItem.stockDBdata.symbol, data) >= similarDistance
      }
      FilterSubTypeEnum.MatchRegexTextType -> {
        regex.containsMatchIn(stockItem.stockDBdata.symbol)
      }
      FilterSubTypeEnum.NotMatchRegexTextType -> {
        !regex.containsMatchIn(stockItem.stockDBdata.symbol)
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
      FilterSubTypeEnum.IsNotTextType,
      FilterSubTypeEnum.SimilarTextType,
      FilterSubTypeEnum.NotSimilarTextType,
      FilterSubTypeEnum.MatchRegexTextType,
      FilterSubTypeEnum.NotMatchRegexTextType,
    )
  override val typeId = FilterTypeEnum.FilterSymbolNameType
  override val displayName = context.getString(R.string.filter_symbolname_name)
  override val desc = context.getString(R.string.filter_symbolname_desc)
}

class FilterDisplayNameType(
  context: Context
) : FilterRegexTextType() {
  override fun filter(stockItem: StockItem): Boolean {
    return when (subType) {
      FilterSubTypeEnum.ContainsTextType -> {
        getName(stockItem.onlineMarketData).contains(data, ignoreCase = true)
      }
      FilterSubTypeEnum.NotContainsTextType -> {
        !getName(stockItem.onlineMarketData).contains(data, ignoreCase = true)
      }
      FilterSubTypeEnum.MatchRegexTextType -> {
        regex.containsMatchIn(getName(stockItem.onlineMarketData))
      }
      FilterSubTypeEnum.NotMatchRegexTextType -> {
        !regex.containsMatchIn(getName(stockItem.onlineMarketData))
      }
      else -> false
    }
  }

  override val subTypeList =
    listOf(
      FilterSubTypeEnum.ContainsTextType,
      FilterSubTypeEnum.NotContainsTextType,
      FilterSubTypeEnum.MatchRegexTextType,
      FilterSubTypeEnum.NotMatchRegexTextType
    )
  override val typeId = FilterTypeEnum.FilterDisplayNameType
  override val displayName = context.getString(R.string.filter_displayname_name)
  override val desc = context.getString(R.string.filter_displayname_desc)
}

class FilterStockExchangeNameType(
  context: Context
) : FilterRegexTextType() {
  override fun filter(stockItem: StockItem): Boolean {
    return when (subType) {
      FilterSubTypeEnum.ContainsTextType -> {
        stockItem.onlineMarketData.fullExchangeName.contains(data, ignoreCase = true)
      }
      FilterSubTypeEnum.NotContainsTextType -> {
        !stockItem.onlineMarketData.fullExchangeName.contains(data, ignoreCase = true)
      }
      FilterSubTypeEnum.MatchRegexTextType -> {
        regex.containsMatchIn(stockItem.onlineMarketData.fullExchangeName)
      }
      FilterSubTypeEnum.NotMatchRegexTextType -> {
        !regex.containsMatchIn(stockItem.onlineMarketData.fullExchangeName)
      }
      else -> false
    }
  }

  override val subTypeList =
    listOf(
      FilterSubTypeEnum.ContainsTextType,
      FilterSubTypeEnum.NotContainsTextType,
      FilterSubTypeEnum.MatchRegexTextType,
      FilterSubTypeEnum.NotMatchRegexTextType
    )
  override val typeId = FilterTypeEnum.FilterStockExchangeNameType
  override val displayName = context.getString(R.string.filter_stockexchangename_name)
  override val desc = context.getString(R.string.filter_stockexchangename_desc)
}

// Market Cap
class FilterMarketCapType(
  val context: Context
) : FilterIntBaseType() {
  override fun filter(stockItem: StockItem): Boolean {

    // MarketCap is in Millions
    val filterValueM: Long = filterValue * 1_000_000L

    return when (subType) {
      FilterSubTypeEnum.GreaterThanType -> {
        stockItem.onlineMarketData.marketCap > filterValueM
      }
      FilterSubTypeEnum.LessThanType -> {
        stockItem.onlineMarketData.marketCap > 0.0 && stockItem.onlineMarketData.marketCap < filterValueM
      }
      // Large-cap is more than 10B
      FilterSubTypeEnum.IsMarketLargeCapType -> {
        stockItem.onlineMarketData.marketCap > 10_000_000_000L
      }
      // Mid-cap is between 2B and 10B
      FilterSubTypeEnum.IsMarketMidCapType -> {
        stockItem.onlineMarketData.marketCap in 2_000_000_001L..10_000_000_000L
      }
      // Small-cap is between 300M and 2B
      FilterSubTypeEnum.IsMarketSmallCapType -> {
        stockItem.onlineMarketData.marketCap in 300_000_001L..2_000_000_000L
      }
      // Micro-cap is between 50M and 300M
      FilterSubTypeEnum.IsMarketMicroCapType -> {
        stockItem.onlineMarketData.marketCap in 50_000_000L..300_000_000L
      }
      // Nano-cap is below 50M
      FilterSubTypeEnum.IsMarketNanoCapType -> {
        stockItem.onlineMarketData.marketCap > 0.0 && stockItem.onlineMarketData.marketCap < 50_000_000L
      }
      else -> false
    }
  }

  override val subTypeList =
    listOf(
      FilterSubTypeEnum.GreaterThanType,
      FilterSubTypeEnum.LessThanType,
      FilterSubTypeEnum.IsMarketLargeCapType,
      FilterSubTypeEnum.IsMarketMidCapType,
      FilterSubTypeEnum.IsMarketSmallCapType,
      FilterSubTypeEnum.IsMarketMicroCapType,
      FilterSubTypeEnum.IsMarketNanoCapType,
    )

  override val typeId = FilterTypeEnum.FilterMarketCapType
  override val displayName = context.getString(R.string.filter_marketcap_name)
  override val desc = context.getString(R.string.filter_marketcap_desc)
  override val displayData: SpannableStringBuilder
    get() {
      if (subType == FilterSubTypeEnum.GreaterThanType
        || subType == FilterSubTypeEnum.LessThanType
      ) {
        return SpannableStringBuilder()
          .append(data)
          .append(formatInt(filterValue * 1_000_000L, context).second)
      }

      return SpannableStringBuilder()
    }
}

class FilterQuoteType(
  context: Context
) : FilterQuoteTypeBaseType(context) {
  override fun filter(stockItem: StockItem): Boolean {
    return when (subType) {
      FilterSubTypeEnum.IsType -> {
        stockItem.onlineMarketData.quoteType == filterQuoteValue
      }
      FilterSubTypeEnum.IsNotType -> {
        stockItem.onlineMarketData.quoteType != filterQuoteValue
      }
      else -> false
    }
  }

  override val typeId = FilterTypeEnum.FilterQuoteType
  override val displayName = context.getString(R.string.filter_quotetype_name)
  override val desc = context.getString(R.string.filter_quotetype_desc)
}

class FilterGroupType(
  context: Context
) : FilterGroupBaseType(context) {
  override fun filter(stockItem: StockItem): Boolean {
    return when (subType) {
      FilterSubTypeEnum.IsType -> {
        stockItem.stockDBdata.groupColor == filterGroupColorValue
      }
      FilterSubTypeEnum.IsNotType -> {
        stockItem.stockDBdata.groupColor != filterGroupColorValue
      }
      else -> false
    }
  }

  override val typeId = FilterTypeEnum.FilterGroupType
  override val displayName = context.getString(R.string.filter_group_name)
  override val desc = context.getString(R.string.filter_group_desc)
}

class FilterNoteType(
  context: Context
) : FilterRegexTextType() {
  override fun filter(stockItem: StockItem): Boolean {
    return when (subType) {
      FilterSubTypeEnum.ContainsTextType -> {
        stockItem.stockDBdata.note.contains(data, ignoreCase = true)
      }
      FilterSubTypeEnum.NotContainsTextType -> {
        !stockItem.stockDBdata.note.contains(data, ignoreCase = true)
      }
      FilterSubTypeEnum.IsEmptyTextType -> {
        stockItem.stockDBdata.note.isEmpty()
      }
      FilterSubTypeEnum.IsNotEmptyTextType -> {
        stockItem.stockDBdata.note.isNotEmpty()
      }
      FilterSubTypeEnum.MatchRegexTextType -> {
        regex.containsMatchIn(stockItem.stockDBdata.note)
      }
      FilterSubTypeEnum.NotMatchRegexTextType -> {
        !regex.containsMatchIn(stockItem.stockDBdata.note)
      }
      else -> false
    }
  }

  override val subTypeList =
    listOf(
      FilterSubTypeEnum.ContainsTextType,
      FilterSubTypeEnum.NotContainsTextType,
      FilterSubTypeEnum.IsEmptyTextType,
      FilterSubTypeEnum.IsNotEmptyTextType,
      FilterSubTypeEnum.MatchRegexTextType,
      FilterSubTypeEnum.NotMatchRegexTextType
    )
  override val typeId = FilterTypeEnum.FilterNoteType
  override val displayName = context.getString(R.string.filter_note_name)
  override val desc = context.getString(R.string.filter_note_desc)
}

class FilterDividendNoteType(
  context: Context
) : FilterRegexTextType() {
  override fun filter(stockItem: StockItem): Boolean {
    return when (subType) {
      FilterSubTypeEnum.ContainsTextType -> {
        stockItem.stockDBdata.dividendNote.contains(data, ignoreCase = true)
      }
      FilterSubTypeEnum.NotContainsTextType -> {
        !stockItem.stockDBdata.dividendNote.contains(data, ignoreCase = true)
      }
      FilterSubTypeEnum.IsEmptyTextType -> {
        stockItem.stockDBdata.dividendNote.isEmpty()
      }
      FilterSubTypeEnum.IsNotEmptyTextType -> {
        stockItem.stockDBdata.dividendNote.isNotEmpty()
      }
      FilterSubTypeEnum.MatchRegexTextType -> {
        regex.containsMatchIn(stockItem.stockDBdata.dividendNote)
      }
      FilterSubTypeEnum.NotMatchRegexTextType -> {
        !regex.containsMatchIn(stockItem.stockDBdata.dividendNote)
      }
      else -> false
    }
  }

  override val subTypeList =
    listOf(
      FilterSubTypeEnum.ContainsTextType,
      FilterSubTypeEnum.NotContainsTextType,
      FilterSubTypeEnum.IsEmptyTextType,
      FilterSubTypeEnum.IsNotEmptyTextType,
      FilterSubTypeEnum.MatchRegexTextType,
      FilterSubTypeEnum.NotMatchRegexTextType
    )
  override val typeId = FilterTypeEnum.FilterDividendNoteType
  override val displayName = context.getString(R.string.filter_dividendnote_name)
  override val desc = context.getString(R.string.filter_dividendnote_desc)
}

class FilterAssetNoteType(
  context: Context
) : FilterRegexTextType() {
  override fun filter(stockItem: StockItem): Boolean {
    return when (subType) {
      FilterSubTypeEnum.ContainsTextType -> {
        stockItem.assets.any { asset ->
          asset.note.contains(data, ignoreCase = true)
        }
      }
      FilterSubTypeEnum.NotContainsTextType -> {
        stockItem.assets.none { asset ->
          asset.note.contains(data, ignoreCase = true)
        }
      }
      FilterSubTypeEnum.IsEmptyTextType -> {
        stockItem.assets.none { asset ->
          asset.note.isNotEmpty()
        }
      }
      FilterSubTypeEnum.IsNotEmptyTextType -> {
        stockItem.assets.any { asset ->
          asset.note.isNotEmpty()
        }
      }
      FilterSubTypeEnum.MatchRegexTextType -> {
        stockItem.assets.find { asset ->
          regex.containsMatchIn(asset.note)
        } != null
      }
      FilterSubTypeEnum.NotMatchRegexTextType -> {
        stockItem.assets.find { asset ->
          regex.containsMatchIn(asset.note)
        } == null
      }
      else -> false
    }
  }

  override val subTypeList =
    listOf(
      FilterSubTypeEnum.ContainsTextType,
      FilterSubTypeEnum.NotContainsTextType,
      FilterSubTypeEnum.IsEmptyTextType,
      FilterSubTypeEnum.IsNotEmptyTextType,
      FilterSubTypeEnum.MatchRegexTextType,
      FilterSubTypeEnum.NotMatchRegexTextType
    )
  override val typeId = FilterTypeEnum.FilterAssetNoteType
  override val displayName = context.getString(R.string.filter_assetnote_name)
  override val desc = context.getString(R.string.filter_assetnote_desc)
}

class FilterAlertNoteType(
  context: Context
) : FilterRegexTextType() {
  override fun filter(stockItem: StockItem): Boolean {
    return when (subType) {
      FilterSubTypeEnum.ContainsTextType -> {
        stockItem.stockDBdata.alertAboveNote.contains(data, ignoreCase = true)
            || stockItem.stockDBdata.alertBelowNote.contains(data, ignoreCase = true)
      }
      FilterSubTypeEnum.NotContainsTextType -> {
        !(stockItem.stockDBdata.alertAboveNote.contains(data, ignoreCase = true)
            || stockItem.stockDBdata.alertBelowNote.contains(data, ignoreCase = true))
      }
      FilterSubTypeEnum.IsEmptyTextType -> {
        stockItem.stockDBdata.alertAboveNote.isEmpty()
            && stockItem.stockDBdata.alertBelowNote.isEmpty()
      }
      FilterSubTypeEnum.IsNotEmptyTextType -> {
        stockItem.stockDBdata.alertAboveNote.isNotEmpty()
            || stockItem.stockDBdata.alertBelowNote.isNotEmpty()
      }
      FilterSubTypeEnum.MatchRegexTextType -> {
        regex.containsMatchIn(stockItem.stockDBdata.alertAboveNote)
            || regex.containsMatchIn(stockItem.stockDBdata.alertBelowNote)
      }
      FilterSubTypeEnum.NotMatchRegexTextType -> {
        !(regex.containsMatchIn(stockItem.stockDBdata.alertAboveNote)
            || regex.containsMatchIn(stockItem.stockDBdata.alertBelowNote))
      }
      else -> false
    }
  }

  override val subTypeList =
    listOf(
      FilterSubTypeEnum.ContainsTextType,
      FilterSubTypeEnum.NotContainsTextType,
      FilterSubTypeEnum.IsEmptyTextType,
      FilterSubTypeEnum.IsNotEmptyTextType,
      FilterSubTypeEnum.MatchRegexTextType,
      FilterSubTypeEnum.NotMatchRegexTextType
    )
  override val typeId = FilterTypeEnum.FilterAlertNoteType
  override val displayName = context.getString(R.string.filter_alertnote_name)
  override val desc = context.getString(R.string.filter_alertnote_desc)
}

class FilterEventDetailType(
  context: Context
) : FilterRegexTextType() {
  override fun filter(stockItem: StockItem): Boolean {
    return when (subType) {
      FilterSubTypeEnum.ContainsTextType -> {
        stockItem.events.any { event ->
          event.title.contains(data, ignoreCase = true) ||
              event.note.contains(data, ignoreCase = true)
        }
      }
      FilterSubTypeEnum.NotContainsTextType -> {
        stockItem.events.none { event ->
          event.title.contains(data, ignoreCase = true) ||
              event.note.contains(data, ignoreCase = true)
        }
      }
      FilterSubTypeEnum.IsEmptyTextType -> {
        stockItem.events.none { event ->
          event.title.isNotEmpty() ||
              event.note.isNotEmpty()
        }
      }
      FilterSubTypeEnum.IsNotEmptyTextType -> {
        stockItem.events.any { event ->
          event.title.isNotEmpty() ||
              event.note.isNotEmpty()
        }
      }
      FilterSubTypeEnum.MatchRegexTextType -> {
        stockItem.events.find { event ->
          regex.containsMatchIn(event.title) ||
              regex.containsMatchIn(event.note)
        } != null
      }
      FilterSubTypeEnum.NotMatchRegexTextType -> {
        stockItem.events.find { event ->
          regex.containsMatchIn(event.title) ||
              regex.containsMatchIn(event.note)
        } == null
      }
      else -> false
    }
  }

  override val subTypeList =
    listOf(
      FilterSubTypeEnum.ContainsTextType,
      FilterSubTypeEnum.NotContainsTextType,
      FilterSubTypeEnum.IsEmptyTextType,
      FilterSubTypeEnum.IsNotEmptyTextType,
      FilterSubTypeEnum.MatchRegexTextType,
      FilterSubTypeEnum.NotMatchRegexTextType
    )
  override val typeId = FilterTypeEnum.FilterEventDetailType
  override val displayName = context.getString(R.string.filter_eventdetail_name)
  override val desc = context.getString(R.string.filter_eventdetail_desc)
}

// Purchase price
class FilterPurchasePriceType(
  context: Context
) : FilterDoubleBaseType() {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice, totalCommission) = getAssets(stockItem.assets)

    return when (subType) {
      FilterSubTypeEnum.GreaterThanType -> {
        totalPrice > filterValue
      }
      FilterSubTypeEnum.LessThanType -> {
        totalPrice > 0.0 && totalPrice < filterValue
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
) : FilterDoubleBaseType() {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice, totalCommission) = getAssets(stockItem.assets)
    val profit = if (stockItem.onlineMarketData.marketPrice > 0.0) {
      totalQuantity * stockItem.onlineMarketData.marketPrice - totalPrice
    } else {
      totalPrice
    }

    return when (subType) {
      FilterSubTypeEnum.GreaterThanType -> {
        profit > filterValue
      }
      FilterSubTypeEnum.LessThanType -> {
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
) : FilterDoublePercentageBaseType() {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice, totalCommission) = getAssets(stockItem.assets)
    val profitPercentage =
      if (stockItem.onlineMarketData.marketPrice > 0.0 && totalPrice > 0.0) {
        (totalQuantity * stockItem.onlineMarketData.marketPrice - totalPrice) / totalPrice
      } else {
        totalPrice
      }

    return when (subType) {
      FilterSubTypeEnum.GreaterThanType -> {
        profitPercentage > filterPercentageValue
      }
      FilterSubTypeEnum.LessThanType -> {
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
  val context: Context
) : FilterDoubleBaseType() {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice, totalCommission) = getAssets(stockItem.assets)
    val asset = if (stockItem.onlineMarketData.marketPrice > 0.0) {
      totalQuantity * stockItem.onlineMarketData.marketPrice
    } else {
      totalPrice
    }

    return when (subType) {
      FilterSubTypeEnum.GreaterThanType -> {
        asset > filterValue
      }
      FilterSubTypeEnum.LessThanType -> {
        asset > 0.0 && asset < filterValue
      }
      FilterSubTypeEnum.IsPresentType -> {
        stockItem.assets.isNotEmpty()
      }
      FilterSubTypeEnum.IsNotPresentType -> {
        stockItem.assets.isEmpty()
      }
      else -> false
    }
  }

  override val subTypeList =
    listOf(
      FilterSubTypeEnum.GreaterThanType,
      FilterSubTypeEnum.LessThanType,
      FilterSubTypeEnum.IsPresentType,
      FilterSubTypeEnum.IsNotPresentType,
    )

  override val typeId = FilterTypeEnum.FilterAssetType
  override var data: String = ""
    get() = when (subType) {
      FilterSubTypeEnum.GreaterThanType,
      FilterSubTypeEnum.LessThanType -> DecimalFormat(DecimalFormat0To2Digits).format(
        filterValue
      )
      else -> ""
    }
    set(value) {
      field = value
      filterValue = strToDouble(value)
    }
  override val displayName = context.getString(R.string.filter_asset_name)
  override val desc = context.getString(R.string.filter_asset_desc)
  override val displayData: SpannableStringBuilder
    get() = when (subType) {
      FilterSubTypeEnum.GreaterThanType,
      FilterSubTypeEnum.LessThanType -> SpannableStringBuilder().append(data)
      else -> SpannableStringBuilder().append("")
    }
}

// Commission
class FilterCommissionType(
  val context: Context
) : FilterDoubleBaseType() {
  override fun filter(stockItem: StockItem): Boolean {
//    var commission = 0.0
//    stockItem.assets.forEach { item ->
//      commission += item.commission
//    }

    val (totalQuantity, totalPrice, totalCommission) = getAssets(stockItem.assets)

    return when (subType) {
      FilterSubTypeEnum.GreaterThanType -> {
        totalCommission > filterValue
      }
      FilterSubTypeEnum.LessThanType -> {
        totalCommission > 0.0 && totalCommission < filterValue
      }
      FilterSubTypeEnum.IsPresentType -> {
        totalCommission > 0.0
      }
      FilterSubTypeEnum.IsNotPresentType -> {
        totalCommission == 0.0
      }
      else -> false
    }
  }

  override val subTypeList =
    listOf(
      FilterSubTypeEnum.GreaterThanType,
      FilterSubTypeEnum.LessThanType,
      FilterSubTypeEnum.IsPresentType,
      FilterSubTypeEnum.IsNotPresentType,
    )

  override val typeId = FilterTypeEnum.FilterCommissionType
  override var data: String = ""
    get() = when (subType) {
      FilterSubTypeEnum.GreaterThanType,
      FilterSubTypeEnum.LessThanType -> DecimalFormat(DecimalFormat0To2Digits).format(
        filterValue
      )
      else -> ""
    }
    set(value) {
      field = value
      filterValue = strToDouble(value)
    }
  override val displayName = context.getString(R.string.filter_commission_name)
  override val desc = context.getString(R.string.filter_commission_desc)
  override val displayData: SpannableStringBuilder
    get() = when (subType) {
      FilterSubTypeEnum.GreaterThanType,
      FilterSubTypeEnum.LessThanType -> SpannableStringBuilder().append(data)
      else -> SpannableStringBuilder().append("")
    }
}

// Dividend Percentage
class FilterDividendPercentageType(
  context: Context
) : FilterDoublePercentageBaseType() {
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

    return when (subType) {
      FilterSubTypeEnum.GreaterThanType -> {
        dividendPercentage > filterPercentageValue
      }
      FilterSubTypeEnum.LessThanType -> {
        dividendPercentage > 0.0 && dividendPercentage < filterPercentageValue
      }
      else -> false
    }
  }

  override val typeId = FilterTypeEnum.FilterDividendPercentageType
  override val displayName = context.getString(R.string.filter_dividendpercentage_name)
  override val desc = context.getString(R.string.filter_dividendpercentage_desc)
}

// Dividend Paid
class FilterDividendPaidType(
  context: Context
) : FilterDividendBaseType() {
  override fun filter(stockItem: StockItem): Boolean {
    val totalDividendPaid: Double = stockItem.dividends.filter { dividend ->
      dividend.type == DividendType.Received.value
    }
      .sumByDouble { dividend ->
        dividend.amount
      }

    return when (subType) {
      FilterSubTypeEnum.GreaterThanType -> {
        totalDividendPaid > filterValue
      }
      FilterSubTypeEnum.LessThanType -> {
        totalDividendPaid > 0.0 && totalDividendPaid < filterValue
      }
      else -> false
    }
  }

  override val typeId = FilterTypeEnum.FilterDividendPaidType
  override val displayName = context.getString(R.string.filter_dividendpaid_name)
  override val desc = context.getString(R.string.filter_dividendpaid_desc)
}

// Dividend Paid YTD
class FilterDividendPaidYTDType(
  context: Context
) : FilterDividendBaseType() {
  override fun filter(stockItem: StockItem): Boolean {
    val totalDividendPaidYTD: Double = stockItem.dividends.filter { dividend ->
      dividend.type == DividendType.Received.value
          && dividend.paydate >= secondsYTD
    }
      .sumByDouble { dividend ->
        dividend.amount
      }

    return when (subType) {
      FilterSubTypeEnum.GreaterThanType -> {
        totalDividendPaidYTD > filterValue
      }
      FilterSubTypeEnum.LessThanType -> {
        totalDividendPaidYTD > 0.0 && totalDividendPaidYTD < filterValue
      }
      else -> false
    }
  }

  override val typeId = FilterTypeEnum.FilterDividendPaidYTDType
  override val displayName = context.getString(R.string.filter_dividendpaidytd_name)
  override val desc = context.getString(R.string.filter_dividendpaidytd_desc)
}

// Quantity
class FilterQuantityType(
  context: Context
) : FilterIntBaseType() {
  override fun filter(stockItem: StockItem): Boolean {
    val (totalQuantity, totalPrice, totalCommission) = getAssets(stockItem.assets)

    return when (subType) {
      FilterSubTypeEnum.GreaterThanType -> {
        totalQuantity > filterValue
      }
      FilterSubTypeEnum.LessThanType -> {
        totalQuantity > 0.0 && totalQuantity < filterValue
      }
      FilterSubTypeEnum.EqualType -> {
        totalQuantity.toInt() == filterValue
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
) : FilterDoubleBaseType() {
  override fun filter(stockItem: StockItem): Boolean {
    val (capitalGain, capitalLoss, gainLossMap) = getAssetsCapitalGain(stockItem.assets)

    return when (subType) {
      FilterSubTypeEnum.GreaterThanType -> {
        capitalGain - capitalLoss > filterValue
      }
      FilterSubTypeEnum.LessThanType -> {
        capitalGain - capitalLoss < filterValue
      }
      else -> false
    }
  }

  override val typeId = FilterTypeEnum.FilterCapitalGainType
  override val displayName = context.getString(R.string.filter_capitalgain_name)
  override val desc = context.getString(R.string.filter_capitalgain_desc)
}

// Post market
class FilterPostMarketType(
  context: Context
) : FilterBooleanBaseType(context) {
  override fun filter(stockItem: StockItem): Boolean {

    return when (subType) {
      FilterSubTypeEnum.IsUsedType -> {
        stockItem.onlineMarketData.postMarketData
      }
      FilterSubTypeEnum.IsNotUsedType -> {
        !stockItem.onlineMarketData.postMarketData
      }
      else -> false
    }
  }

  override val subTypeList =
    listOf(
      FilterSubTypeEnum.IsUsedType,
      FilterSubTypeEnum.IsNotUsedType
    )

  override val typeId = FilterTypeEnum.FilterPostMarketType
  override val displayName = context.getString(R.string.filter_postmarket_name)
  override val desc = context.getString(R.string.filter_postmarket_desc)
}

// Alert
class FilterAlertType(
  context: Context
) : FilterBooleanBaseType(context) {
  override fun filter(stockItem: StockItem): Boolean {

    return when (subType) {
      FilterSubTypeEnum.IsPresentType -> {
        stockItem.stockDBdata.alertAbove > 0.0 || stockItem.stockDBdata.alertBelow > 0.0
      }
      FilterSubTypeEnum.IsOnePresentType -> {
        (stockItem.stockDBdata.alertAbove > 0.0).xor(stockItem.stockDBdata.alertBelow > 0.0)
      }
      FilterSubTypeEnum.IsAllPresentType -> {
        stockItem.stockDBdata.alertAbove > 0.0 && stockItem.stockDBdata.alertBelow > 0.0
      }
      FilterSubTypeEnum.IsNotPresentType -> {
        !(stockItem.stockDBdata.alertAbove > 0.0 || stockItem.stockDBdata.alertBelow > 0.0)
      }
      else -> false
    }
  }

  override val subTypeList =
    listOf(
      FilterSubTypeEnum.IsPresentType,
      FilterSubTypeEnum.IsOnePresentType,
      FilterSubTypeEnum.IsAllPresentType,
      FilterSubTypeEnum.IsNotPresentType
    )

  override val typeId = FilterTypeEnum.FilterAlertType
  override val displayName = context.getString(R.string.filter_alert_name)
  override val desc = context.getString(R.string.filter_alert_desc)
}

// Event
class FilterEventType(
  context: Context
) : FilterBooleanBaseType(context) {
  override fun filter(stockItem: StockItem): Boolean {

    return when (subType) {
      FilterSubTypeEnum.IsPresentType -> {
        stockItem.events.isNotEmpty()
      }
      FilterSubTypeEnum.IsNotPresentType -> {
        stockItem.events.isEmpty()
      }
      else -> false
    }
  }

  override val subTypeList =
    listOf(
      FilterSubTypeEnum.IsPresentType,
      FilterSubTypeEnum.IsNotPresentType
    )

  override val typeId = FilterTypeEnum.FilterEventType
  override val displayName = context.getString(R.string.filter_event_name)
  override val desc = context.getString(R.string.filter_event_desc)
}

class FilterAssetSoldDateType(
  context: Context
) : FilterDateBaseType() {
  override fun filter(stockItem: StockItem): Boolean {
    val assetSold = stockItem.assets.filter { asset ->
      asset.quantity < 0.0
    }

    return if (assetSold.isNotEmpty()) {

      when (subType) {
        FilterSubTypeEnum.BeforeDateType -> {
          val firstAssetDate = assetSold.minOf { asset ->
            asset.date
          }
          // firstAssetDate > 0 && firstAssetDate < filterDateValue
          firstAssetDate in 1 until filterDateValue
        }
        FilterSubTypeEnum.AfterDateType -> {
          val lastAssetDate = assetSold.maxOf { asset ->
            asset.date
          }
          lastAssetDate > filterDateValue
        }
        else -> false
      }
    } else {
      false
    }
  }

  override val typeId = FilterTypeEnum.FilterAssetSoldDateType
  override val displayName = context.getString(R.string.filter_assetsolddate_name)
  override val desc = context.getString(R.string.filter_assetsolddate_desc)
}

class FilterAssetBoughtDateType(
  context: Context
) : FilterDateBaseType() {
  override fun filter(stockItem: StockItem): Boolean {
    val assetBought = stockItem.assets.filter { asset ->
      asset.quantity > 0.0
    }

    return if (assetBought.isNotEmpty()) {

      when (subType) {
        FilterSubTypeEnum.BeforeDateType -> {
          val firstAssetDate = assetBought.minOf { asset ->
            asset.date
          }
          // firstAssetDate > 0 && firstAssetDate < filterDateValue
          firstAssetDate in 1 until filterDateValue
        }
        FilterSubTypeEnum.AfterDateType -> {
          val lastAssetDate = assetBought.maxOf { asset ->
            asset.date
          }
          lastAssetDate > filterDateValue
        }
        else -> false
      }
    } else {
      false
    }
  }

  override val typeId = FilterTypeEnum.FilterAssetBoughtDateType
  override val displayName = context.getString(R.string.filter_assetboughtdate_name)
  override val desc = context.getString(R.string.filter_assetboughtdate_desc)
}

// Stocks are at least one year old.
class FilterLongTermType(
  context: Context
) : FilterBaseType() {
  override fun filter(stockItem: StockItem): Boolean {
    val assetBought = stockItem.assets.filter { asset ->
      asset.quantity > 0.0
    }

    return if (assetBought.isNotEmpty()) {
      val secondsNow = ZonedDateTime.now()
        .toEpochSecond()
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
