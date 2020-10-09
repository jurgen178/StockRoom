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
import com.thecloudsite.stockroom.R.color
import com.thecloudsite.stockroom.database.Asset
import java.text.DecimalFormat
import java.util.Locale

// Rounding error
const val epsilon = 0.000001

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

  val (shares, asset) = getAssets(assets)

//  val shares = assets.sumByDouble {
//    it.shares
//  }

//  val asset: Double =
//    if (shares > 0.0) {
//      assets.sumByDouble {
//        it.shares * it.price
//      }
//    } else {
//      0.0
//    }

  if (marketPrice > 0.0) {
    var changeStr: String = ""

    if (shares > 0.0) {
      val capital = shares * marketPrice
//      val capital = assets.sumByDouble {
//        it.shares * marketPrice
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

fun checkBaseUrl(baseUrl: String): String {
  return if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
    "https://${baseUrl}"
  } else {
    baseUrl
  }
}

fun getAssets(assetList: List<Asset>?): Pair<Double, Double> {

  var totalShares: Double = 0.0
  var totalPrice: Double = 0.0

  assetList?.sortedBy { item ->
    item.date
  }
      ?.forEach { asset ->

        // added shares
        if (asset.shares > 0.0) {
          totalShares += asset.shares
          totalPrice += asset.shares * asset.price
        } else
        // removed shares
          if (asset.shares < 0.0) {
            // removed all?
            if (-asset.shares >= (totalShares + epsilon)) {
              // reset if more removed than owned
              totalShares = 0.0
              totalPrice = 0.0
            } else {
              // adjust the total price for the removed shares
              if (totalShares > epsilon) {
                val averageSharePrice = totalPrice / totalShares
                totalShares += asset.shares
                totalPrice = totalShares * averageSharePrice
              }
            }
          }
      }

  return Pair(totalShares, totalPrice)
}
