package com.thecloudsite.stockroom

import android.content.Context
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.text.SpannableStringBuilder
import androidx.annotation.RawRes
import androidx.core.text.bold
import androidx.core.text.color
import java.text.DecimalFormat
import java.util.Locale

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

fun Resources.getRawTextFile(@RawRes id: Int) =
  openRawResource(id).bufferedReader()
      .use { it.readText() }

// Gets the colored change string "asset (%change)"
fun getAssetChange(
  assets: List<Asset>,
  marketPrice: Float,
  context: Context
): SpannableStringBuilder {
  val shares = assets.sumByDouble {
    it.shares.toDouble()
  }
      .toFloat()

  val asset: Float =
    if (shares > 0f) {
      assets.sumByDouble {
        it.shares.toDouble() * it.price
      }
          .toFloat()
    } else {
      0f
    }

  if (marketPrice > 0f) {
    var changeStr: String = ""

    if (shares > 0f) {
      val capital = assets.sumByDouble {
        it.shares.toDouble() * marketPrice
      }
          .toFloat()

      val change = capital - asset
      changeStr += "${if (change > 0f) {
        "+"
      } else {
        ""
      }}${DecimalFormat(
          "0.00"
      ).format(
          change
      )}"

      val changePercent = change * 100f / asset
      changeStr += " (${if (changePercent > 0f) {
        "+"
      } else {
        ""
      }}${DecimalFormat("0.00").format(changePercent)}%)"

      val assetChangeColor = when {
        capital > asset -> {
          context.getColor(R.color.green)
        }
        capital < asset -> {
          context.getColor(R.color.red)
        }
        else -> {
          context.getColor(R.color.material_on_background_emphasis_medium)
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
