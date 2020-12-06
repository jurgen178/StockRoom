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

package com.thecloudsite.stockroom.utils

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import androidx.annotation.RawRes
import androidx.core.text.bold
import androidx.core.text.color
import com.thecloudsite.stockroom.DividendCycle.Annual
import com.thecloudsite.stockroom.DividendCycle.Monthly
import com.thecloudsite.stockroom.DividendCycle.Quarterly
import com.thecloudsite.stockroom.DividendCycle.SemiAnnual
import com.thecloudsite.stockroom.OnlineMarketData
import com.thecloudsite.stockroom.R
import com.thecloudsite.stockroom.R.array
import com.thecloudsite.stockroom.R.color
import com.thecloudsite.stockroom.StockItem
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.database.AssetType
import com.thecloudsite.stockroom.database.Group
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// Rounding error
const val epsilon = 0.0000001

// asset.type
const val obsoleteAssetType: Int = 0x0001

data class StockOptionData(
  var sharesPerOption: Int = 0,
  var expirationDate: Long = 0,
  var strikePrice: Double = 0.0,
  var type: Int = AssetType.UnknownOption.value
)

/*
enum class DividendCycle(val value: Int) {
  Monthly(12),
  Quarterly(4),
  SemiAnnual(2),
  Annual(1),
}
*/

fun openNewTabWindow(
  url: String,
  context: Context
) {
  val uri = Uri.parse(url)
  val intents = Intent(Intent.ACTION_VIEW, uri)
//  val b = Bundle()
//  b.putBoolean("new_window", true)
//  intents.putExtras(b)
  context.startActivity(intents)
}

fun dividendSelectionToCycle(selection: Int): Int {
  return when (selection) {
    0 -> Monthly.value
    2 -> SemiAnnual.value
    3 -> Annual.value
    else -> Quarterly.value
  }
}

fun dividendCycleToSelection(cycle: Int): Int {
  return when (cycle) {
    Monthly.value -> 0
    SemiAnnual.value -> 2
    Annual.value -> 3
    else -> 1
  }
}

fun dividendCycleStr(
  cycle: Int,
  context: Context
): String {
  val stringList = context.resources.getStringArray(array.dividend_cycles)
  val selection = dividendCycleToSelection(cycle)

  return stringList[selection]
}

fun isValidSymbol(symbol: String): Boolean {
  val symbolUpper = symbol.toUpperCase(Locale.ROOT)

  return symbol.isNotBlank()
      &&
      // valid length
      (symbol.length in 1..20)
      &&
      // valid chars
      symbolUpper.matches("[A-Z0-9.^*:=-]+".toRegex())
      &&
      // at least one A-Z or 0-9
      symbolUpper.matches(".*[A-Z0-9]+.*".toRegex())
      &&
      // no trailing **
      symbolUpper.matches(".*(?<!\\*\\*)".toRegex())
}

// Used when export to Json as Gson cannot handle NaN.
fun validateDouble(value: Double): Double {
  return if (value.isFinite())
    value else
    0.0
}

fun enNumberStrToDouble(str: String): Double {
  var value: Double
  try {
    value = str.toDouble()
    if (value == 0.0) {
      val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
      value = numberFormat.parse(str)!!
          .toDouble()
    }
  } catch (e: Exception) {
    value = 0.0
  }

  return value
}

fun Resources.getRawTextFile(@RawRes id: Int) =
  openRawResource(id).bufferedReader()
      .use { it.readText() }

fun getMarketValues(onlineMarketData: OnlineMarketData): Triple<String, String, String> {
  val marketPrice = if (onlineMarketData.marketPrice > 5.0) {
    DecimalFormat("0.00").format(onlineMarketData.marketPrice)
  } else {
    DecimalFormat("0.00##").format(onlineMarketData.marketPrice)
  }
  val change = DecimalFormat("0.00##").format(onlineMarketData.marketChange)
  val changePercent = "(${
    DecimalFormat("0.00").format(
        onlineMarketData.marketChangePercent
    )
  }%)"

  return Triple(marketPrice, change, changePercent)
}

fun getChangeColor(
  change: Double,
  isPostMarket: Boolean,
  neutralColor: Int,
  context: Context
): Int = when {
  change > 0.0 -> {
    context.getColor(if (isPostMarket) color.postGreen else color.green)
  }
  change < 0.0 -> {
    context.getColor(if (isPostMarket) color.postRed else color.red)
  }
  else -> {
    neutralColor
  }
}

fun getChangeColor(
  capital: Double,
  asset: Double,
  isPostMarket: Boolean,
  neutralColor: Int,
  context: Context
): Int = when {
  capital > 0.0 && capital > asset -> {
    context.getColor(if (isPostMarket) color.postGreen else color.green)
  }
  capital > 0.0 && capital < asset -> {
    context.getColor(if (isPostMarket) color.postRed else color.red)
  }
  else -> {
    neutralColor
  }
}

// Gets the colored change string "asset (%change)"
fun getAssetChange(
  assets: List<Asset>,
  marketPrice: Double,
  isPostMarket: Boolean,
  neutralColor: Int,
  context: Context,
  bold: Boolean = true
): Triple<String, SpannableStringBuilder, Int> {

  val (quantity, asset) = getAssets(assets)

  return getAssetChange(
      quantity,
      asset,
      marketPrice,
      isPostMarket,
      neutralColor,
      context,
      bold
  )
}

// Gets the colored change string "asset (%change)"
fun getAssetChange(
  quantity: Double,
  asset: Double,
  marketPrice: Double,
  isPostMarket: Boolean,
  neutralColor: Int,
  context: Context,
  bold: Boolean = true
): Triple<String, SpannableStringBuilder, Int> {

  if (marketPrice > 0.0) {
    var changeStr: String = ""

    if (quantity > 0.0) {
      val capital = quantity * marketPrice

      val change = capital - asset
      changeStr += "${
        if (change > 0.0) {
          "+"
        } else {
          ""
        }
      }${
        DecimalFormat(
            "0.00"
        ).format(
            change
        )
      }"

      val changePercent = change * 100.0 / asset
      if (changePercent < 10000.0) {
        changeStr += " (${
          if (changePercent > 0.0) {
            "+"
          } else {
            ""
          }
        }${DecimalFormat("0.00").format(changePercent)}%)"
      }

      val assetChangeColor = getChangeColor(capital - asset, isPostMarket, neutralColor, context)

      val assetText = if (bold) {
        SpannableStringBuilder().bold { append(changeStr) }
      } else {
        SpannableStringBuilder().append(changeStr)
      }

      val assetChange = SpannableStringBuilder()

      // Omit the neutral color to use the default text color.
      if (assetChangeColor != neutralColor) {
        assetChange.color(assetChangeColor) { assetChange.append(assetText) }
      } else {
        assetChange.append(assetText)
      }

      return Triple(changeStr, assetChange, assetChangeColor)
    }
  }

  return Triple("", SpannableStringBuilder(), neutralColor)
}

fun getDividendStr(
  stockItem: StockItem,
  context: Context
): String {
  var annualDividendRate: Double = 0.0
  var annualDividendYield: Double = 0.0

  if (stockItem.stockDBdata.annualDividendRate >= 0.0) {
    annualDividendRate = stockItem.stockDBdata.annualDividendRate
    //val (totalQuantity, totalPrice) = getAssets(stockItem.assets)
    annualDividendYield = if (stockItem.onlineMarketData.marketPrice > 0.0) {
      annualDividendRate / stockItem.onlineMarketData.marketPrice
    } else {
      0.0
    }
  } else {
    annualDividendRate = stockItem.onlineMarketData.annualDividendRate
    annualDividendYield = stockItem.onlineMarketData.annualDividendYield
  }

  return if (annualDividendRate > 0.0) {
    "${context.getString(R.string.dividend_in_list)} ${
      DecimalFormat("0.00##").format(annualDividendRate)
    } (${
      DecimalFormat("0.0").format(annualDividendYield * 100.0)
    }%)"
  } else {
    ""
  }
}

fun getAssets(
  assetList: List<Asset>?,
  tagObsoleteAssetType: Int = 0
): Pair<Double, Double> {

  var totalQuantity: Double = 0.0
  var totalPrice: Double = 0.0

  if (assetList != null) {
    val assetListSorted = assetList.sortedBy { asset ->
      asset.date
    }

    if (tagObsoleteAssetType != 0) {
      assetListSorted.forEach { asset ->
        asset.type = asset.type and tagObsoleteAssetType.inv()
      }
    }

    for (i in assetListSorted.indices) {

      val asset = assetListSorted[i]

      // added shares
      if (asset.quantity > 0.0) {
        totalQuantity += asset.quantity
        totalPrice += asset.quantity * asset.price
      } else
      // removed shares
        if (asset.quantity < 0.0) {
          // removed all?
          if (-asset.quantity >= (totalQuantity - epsilon)) {
            // reset if more removed than owned
            totalQuantity = 0.0
            totalPrice = 0.0

            if (tagObsoleteAssetType != 0) {
              for (j in i downTo 0) {
                assetListSorted[j].type = assetListSorted[j].type or tagObsoleteAssetType
              }
            }
          } else {
            // adjust the total price for the removed shares
            if (totalQuantity > epsilon) {
              val averageSharePrice = totalPrice / totalQuantity
              totalQuantity += asset.quantity
              totalPrice = totalQuantity * averageSharePrice
            }
          }
        }
    }
  }

  return Pair(totalQuantity, totalPrice)
}

// Only gets the assets that are added.
fun getAddedAssets(
  assetList: List<Asset>?
): Pair<Double, Double> {

  var totalQuantity: Double = 0.0
  var totalPrice: Double = 0.0

  assetList?.forEach { asset ->
    if (asset.quantity > 0.0) {
      totalQuantity += asset.quantity
      totalPrice += asset.quantity * asset.price
    }
  }

  return Pair(totalQuantity, totalPrice)
}

fun getAssetsCapitalGain(assetList: List<Asset>?): Pair<Double, Double> {

  var totalQuantity: Double = 0.0
  var totalGain: Double = 0.0
  var totalLoss: Double = 0.0
  var bought: Double = 0.0
  var sold: Double = 0.0

  assetList?.sortedBy { asset ->
    asset.date
  }
      ?.forEach { asset ->
        if (asset.quantity > 0.0) {
          bought += asset.quantity * asset.price
        }
        if (asset.quantity < 0.0) {
          sold += -asset.quantity * asset.price
        }
        totalQuantity += asset.quantity

        if ((totalQuantity <= -epsilon)) {
          // Error, more shares sold than owned
          return Pair(0.0, 0.0)
        }
        if (totalQuantity < epsilon) {
          // totalQuantity is 0: -epsilon < totalQuantity < epsilon
          // reset if all shares are sold
          val gain = sold - bought
          if (gain > 0.0) {
            totalGain += gain
          } else
            if (gain < 0.0) {
              totalLoss -= gain
            }
          sold = 0.0
          bought = 0.0
        }
      }

  return Pair(totalGain, totalLoss)
}

fun getCapitalGainLossText(
  context: Context,
  capitalGain: Double,
  capitalLoss: Double,
  capitalTotalOptional: Double = 0.0,
  formatConcat: String = " - ",
  formatEnd: String = ""
): SpannableStringBuilder {

  // optional total with no rounding errors
  val capitalTotal =
    if (capitalTotalOptional == 0.0) {
      capitalGain - capitalLoss
    } else {
      capitalTotalOptional
    }

  return when {
    capitalLoss == 0.0 && capitalGain > 0.0 -> {
      SpannableStringBuilder().color(context.getColor(R.color.green)) {
        bold { append("${DecimalFormat("0.00").format(capitalGain)}$formatEnd") }
      }
    }
    capitalGain == 0.0 && capitalLoss > 0.0 -> {
      SpannableStringBuilder().color(context.getColor(R.color.red)) {
        bold { append("${DecimalFormat("0.00").format(-capitalLoss)}$formatEnd") }
      }
    }
    capitalGain > 0.0 && capitalLoss > 0.0 && capitalGain > capitalLoss -> {
      SpannableStringBuilder()
          .color(context.getColor(R.color.green)) {
            bold { append("${DecimalFormat("0.00").format(capitalGain)}$formatEnd") }
          }
          .color(context.getColor(R.color.red)) {
            bold { append("$formatConcat${DecimalFormat("0.00").format(capitalLoss)}$formatEnd") }
          }
          .append(" = ")
          .color(context.getColor(R.color.green)) {
            bold { append("${DecimalFormat("0.00").format(capitalTotal)}$formatEnd") }
          }
    }
    capitalGain > 0.0 && capitalLoss > 0.0 && capitalGain < capitalLoss -> {
      SpannableStringBuilder()
          .color(context.getColor(R.color.green)) {
            bold { append("${DecimalFormat("0.00").format(capitalGain)}$formatEnd") }
          }
          .color(context.getColor(R.color.red)) {
            bold { append("$formatConcat${DecimalFormat("0.00").format(capitalLoss)}$formatEnd") }
          }
          .append(" = ")
          .color(context.getColor(R.color.red)) {
            bold { append("${DecimalFormat("0.00").format(capitalTotal)}$formatEnd") }
          }
    }
    else -> {
      SpannableStringBuilder().bold {
        append("${DecimalFormat("0.00").format(0.0)}$formatEnd")
      }
    }
  }
}

fun parseStockOption(symbol: String): StockOptionData {

  val stockOption = StockOptionData()

  // named groups are not yet supported
  val match = "([A-Z.]+)(7?)\\s*(\\d+)([A-Z])(\\d+)".toRegex()
      .matchEntire(symbol.toUpperCase(Locale.ROOT))

  if (match != null && match.groups.size == 6) {
    val sym = match.groups[1]?.value

    stockOption.sharesPerOption = if (match.groups[2]?.value == "7") {
      10
    } else {
      100
    }

    val dateStr = match.groups[3]?.value
    try {
      stockOption.expirationDate =
        LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyMMdd"))
            .atStartOfDay(ZoneOffset.UTC)
            .toEpochSecond()
    } catch (e: Exception) {
    }

    when (match.groups[4]?.value.toString()) {
      "C" -> {
        stockOption.type = AssetType.CallOption.value
      }
      "P" -> {
        stockOption.type = AssetType.PutOption.value
      }
    }

    stockOption.strikePrice = match.groups[5]?.value?.toInt()
        ?.div(1000.0) ?: 0.0
  }

  return stockOption
}

fun isOnline(context: Context): Boolean {
  val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
  val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true
  return isConnected
  /*
  val connectivityManager =
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  val capabilities =
    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
  if (capabilities != null) {
    when {
      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
        Log.i("Internet", "NetworkCapabilities.TRANSPORT_CELLULAR")
      }
      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
        Log.i("Internet", "NetworkCapabilities.TRANSPORT_WIFI")
      }
      capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> {
        Log.i("Internet", "NetworkCapabilities.TRANSPORT_ETHERNET")
      }
    }
    return true
  }
  return false

   */
}

fun checkUrl(url: String): String {
  return if (!url.startsWith("http://") && !url.startsWith("https://")) {
    "https://${url}"
  } else {
    url
  }
}

fun getAssets(assetList: List<Asset>?): Pair<Double, Double> {

  var totalQuantity: Double = 0.0
  var totalPrice: Double = 0.0

  assetList?.sortedBy { item ->
    item.date
  }
      ?.forEach { asset ->

        // added shares
        if (asset.quantity > 0.0) {
          totalQuantity += asset.quantity
          totalPrice += asset.quantity * asset.price
        } else
        // removed shares
          if (asset.quantity < 0.0) {
            // removed all?
            if (-asset.quantity >= (totalQuantity + epsilon)) {
              // reset if more removed than owned
              totalQuantity = 0.0
              totalPrice = 0.0
            } else {
              // adjust the total price for the removed shares
              if (totalQuantity > epsilon) {
                val averageSharePrice = totalPrice / totalQuantity
                totalQuantity += asset.quantity
                totalPrice = totalQuantity * averageSharePrice
              }
            }
          }
      }

  return Pair(totalQuantity, totalPrice)
}

// Get the colored menu entries for the groups.
fun getGroupsMenuList(
  groups: List<Group>,
  backgroundListColor: Int,
  standardGroupName: String
): List<SpannableString> {
  val menuStrings: MutableList<SpannableString> = mutableListOf()

  val menuGroups: MutableList<Group> = mutableListOf()
  menuGroups.addAll(groups)
  menuGroups.add(Group(color = backgroundListColor, name = standardGroupName))

  val space: String = "    "
  val spacePos = space.length
  for (i in menuGroups.indices) {
    val grp: Group = menuGroups[i]
    val s = SpannableString("$space  ${grp.name}")
    s.setSpan(BackgroundColorSpan(grp.color), 0, spacePos, 0)

    // backgroundListColor is light color, make the group name readable
    val textColor = if (isWhiteColor(grp.color) || grp.color == backgroundListColor) {
      Color.BLACK
    } else {
      grp.color
    }

    s.setSpan(ForegroundColorSpan(textColor), spacePos, s.length, 0)
    menuStrings.add(s)
  }

  return menuStrings
}

fun isWhiteColor(color: Int): Boolean {
  val r = color shr 16 and 0xff
  val g = color shr 8 and 0xff
  val b = color and 0xff
  return r + g + b > 700
}
