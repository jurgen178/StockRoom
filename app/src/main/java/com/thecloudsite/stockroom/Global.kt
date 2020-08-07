package com.thecloudsite.stockroom

import android.content.Context
import android.content.res.Resources
import android.text.SpannableStringBuilder
import androidx.annotation.RawRes
import androidx.core.text.bold
import androidx.core.text.color
import java.text.DecimalFormat

fun Resources.getRawTextFile(@RawRes id: Int) =
  openRawResource(id).bufferedReader()
      .use { it.readText() }

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

