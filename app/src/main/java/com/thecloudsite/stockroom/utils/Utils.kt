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
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.text.SpannableStringBuilder
import androidx.annotation.RawRes
import androidx.core.text.bold
import androidx.core.text.color
import com.thecloudsite.stockroom.DividendCycle.Annual
import com.thecloudsite.stockroom.DividendCycle.Monthly
import com.thecloudsite.stockroom.DividendCycle.Quarterly
import com.thecloudsite.stockroom.DividendCycle.SemiAnnual
import com.thecloudsite.stockroom.R
import com.thecloudsite.stockroom.R.color
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.database.AssetType
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// Rounding error
const val epsilon = 0.000001

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

fun Resources.getRawTextFile(@RawRes id: Int) =
  openRawResource(id).bufferedReader()
      .use { it.readText() }

// Gets the colored change string "asset (%change)"
fun getAssetChange(
  assets: List<Asset>,
  marketPrice: Double,
  context: Context
): SpannableStringBuilder {

  val (amount, asset) = getAssets(assets)

//  val amount = assets.sumByDouble {
//    it.amount
//  }

//  val asset: Double =
//    if (amount > 0.0) {
//      assets.sumByDouble {
//        it.amount * it.price
//      }
//    } else {
//      0.0
//    }

  if (marketPrice > 0.0) {
    var changeStr: String = ""

    if (amount > 0.0) {
      val capital = amount * marketPrice
//      val capital = assets.sumByDouble {
//        it.amount * marketPrice
//      }

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
      changeStr += " (${
        if (changePercent > 0.0) {
          "+"
        } else {
          ""
        }
      }${DecimalFormat("0.00").format(changePercent)}%)"

      val assetChangeColor = when {
        capital > asset -> {
          context.getColor(color.green)
        }
        capital < asset -> {
          context.getColor(color.red)
        }
        else -> {
          context.getColor(color.material_on_background_emphasis_medium)
        }
      }

      val assetChange = SpannableStringBuilder()
          .color(assetChangeColor) {
            bold { append(changeStr) }
          }

      return assetChange
    }
  }

  return SpannableStringBuilder()
}

fun getAssets(
  assetList: List<Asset>?,
  tagObsoleteAssetType: Int = 0
): Pair<Double, Double> {

  var totalAmount: Double = 0.0
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
      if (asset.amount > 0.0) {
        totalAmount += asset.amount
        totalPrice += asset.amount * asset.price
      } else
      // removed shares
        if (asset.amount < 0.0) {
          // removed all?
          if (-asset.amount >= (totalAmount - epsilon)) {
            // reset if more removed than owned
            totalAmount = 0.0
            totalPrice = 0.0

            if (tagObsoleteAssetType != 0) {
              for (j in i downTo 0) {
                assetListSorted[j].type = assetListSorted[j].type or tagObsoleteAssetType
              }
            }
          } else {
            // adjust the total price for the removed shares
            if (totalAmount > epsilon) {
              val averageSharePrice = totalPrice / totalAmount
              totalAmount += asset.amount
              totalPrice = totalAmount * averageSharePrice
            }
          }
        }
    }
  }

  return Pair(totalAmount, totalPrice)
}

fun getAssetsCapitalGain(assetList: List<Asset>?): Pair<Double, Double> {

  var totalAmount: Double = 0.0
  var totalGain: Double = 0.0
  var totalLoss: Double = 0.0
  var bought: Double = 0.0
  var sold: Double = 0.0

  assetList?.sortedBy { asset ->
    asset.date
  }
      ?.forEach { asset ->
        if (asset.amount > 0.0) {
          bought += asset.amount * asset.price
        }
        if (asset.amount < 0.0) {
          sold += -asset.amount * asset.price
        }
        totalAmount += asset.amount

        if ((totalAmount <= -epsilon)) {
          // Error, more shares sold than owned
          return Pair(0.0, 0.0)
        }
        if (totalAmount < epsilon) {
          // totalShares are 0: -epsilon < totalShares < epsilon
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
        append("0.00$formatEnd")
      }
    }
  }
}

fun parseStockOption(symbol: String): StockOptionData {

  val stockOption = StockOptionData()

  // named groups are not yet supported
  val match = "([A-Z.]+)(7?)\\s*(\\d+)([A-Z])(\\d+)".toRegex()
      .matchEntire(symbol.toUpperCase())

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

  var totalAmount: Double = 0.0
  var totalPrice: Double = 0.0

  assetList?.sortedBy { item ->
    item.date
  }
      ?.forEach { asset ->

        // added shares
        if (asset.amount > 0.0) {
          totalAmount += asset.amount
          totalPrice += asset.amount * asset.price
        } else
        // removed shares
          if (asset.amount < 0.0) {
            // removed all?
            if (-asset.amount >= (totalAmount + epsilon)) {
              // reset if more removed than owned
              totalAmount = 0.0
              totalPrice = 0.0
            } else {
              // adjust the total price for the removed shares
              if (totalAmount > epsilon) {
                val averageSharePrice = totalPrice / totalAmount
                totalAmount += asset.amount
                totalPrice = totalAmount * averageSharePrice
              }
            }
          }
      }

  return Pair(totalAmount, totalPrice)
}
