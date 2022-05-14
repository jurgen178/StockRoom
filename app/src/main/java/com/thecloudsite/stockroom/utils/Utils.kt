/*
 * Copyright (C) 2021
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
import android.content.SharedPreferences
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.text.backgroundColor
import androidx.core.text.bold
import androidx.core.text.color
import androidx.preference.PreferenceManager
import com.thecloudsite.stockroom.*
import com.thecloudsite.stockroom.DividendCycle.*
import com.thecloudsite.stockroom.MainActivity.Companion.symbolDisplayNameMap
import com.thecloudsite.stockroom.R.array
import com.thecloudsite.stockroom.R.color
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.database.AssetType
import com.thecloudsite.stockroom.database.Group
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sign

var useWhiteOnRedGreen: Boolean = false

// https://developer.android.com/reference/java/text/DecimalFormat
// #,## add thousand separator: 1.234,56
const val DecimalFormat1Digit = "#,##0.0"
const val DecimalFormat0To1Digit = "#,##0.#"
const val DecimalFormat2Digits = "#,##0.00"
const val DecimalFormat4Digits = "#,##0.0000"
const val DecimalFormat0To2Digits = "#,##0.##"
const val DecimalFormat0To4Digits = "#,##0.####"
const val DecimalFormat0To6Digits = "#,##0.######"
const val DecimalFormat2To4Digits = "#,##0.00##"
const val DecimalFormat2To8Digits = "#,##0.00######"
const val DecimalFormatQuantityDigits = "#,##0.########"

const val MaxChartOverlays = 4

// First entry match first color.
val chartOverlayColors: List<Int> =
    listOf(
        0xff808080.toInt(), // Gray
        0xffFF70AE.toInt(), // Red
        0xffA4E500.toInt(), // Green
        0xffFFD800.toInt()  // Yellow
    )

// Rounding error
const val epsilon = 0.00000001

// SpannableStrings scale
const val feeScale = 0.8f

// asset.type
const val obsoleteAssetType: Int = 0x0001

data class StockOptionData(
    var sharesPerOption: Int = 0,
    var expirationDate: Long = 0,
    var strikePrice: Double = 0.0,
    var type: Int = AssetType.UnknownOption.value
)

data class FormatIntResult(
    var boldText: SpannableStringBuilder,
    var withBrackets: String,
    var text: String,
)

enum class DividendCycleStrIndex(val value: Int) {
    Monthly(0),
    Quarterly(1),
    SemiAnnual(2),
    Annual(3)
}

data class AssetChange(
    var displayStr: String,
    var displayColorStr: SpannableStringBuilder,
    var color: Int,
    var value: Double,
)

// > 5:       2 digits
// < 0.0001:  4..8 digits
// 0.0001..5: 2..4 digits
private fun to2To8DigitsImpl(value: Double): String = when {
    value > 5.0 -> {
        DecimalFormat(DecimalFormat2Digits).format(value)
    }
    value < 0.0001 -> {
        DecimalFormat(DecimalFormat2To8Digits).format(value)
    }
    else -> {
        DecimalFormat(DecimalFormat2To4Digits).format(value)
    }
}

fun to2To8Digits(value: Double): String = to2To8DigitsImpl(value)
fun to2To8Digits(value: Float): String = to2To8DigitsImpl(value.toDouble())

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
    val symbolUpper = symbol.uppercase(Locale.ROOT)

    return symbol.isNotBlank()
            &&
            // valid length
            (symbol.length in 1..50)
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

// Lowest amount greater 0 is 0.0001
fun minValueCheck(value: Double): Double =
    if (value > 0.0 && value < 0.0001) {
        0.0001
    } else {
        value
    }

fun getFormatStr(value: Double): String =
    if (value >= 1.0) {
        DecimalFormat2Digits
    } else {
        DecimalFormat2To4Digits
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

    return minValueCheck(value)
}

fun formatLong(
    value: Long,
    context: Context
): String {
    return when {
        value >= 900000000000L -> {
            "${DecimalFormat(DecimalFormat0To2Digits).format(value / 1000000000000.0)}${
                context.getString(
                    R.string.trillion_abbr
                )
            }"
        }
        value >= 900000000L -> {
            "${DecimalFormat(DecimalFormat0To2Digits).format(value / 1000000000.0)}${
                context.getString(
                    R.string.billion_abbr
                )
            }"
        }
        value >= 900000L -> {
            "${DecimalFormat(DecimalFormat0To2Digits).format(value / 1000000.0)}${
                context.getString(
                    R.string.million_abbr
                )
            }"
        }
        value == Long.MIN_VALUE -> {
            // requested value is not in the JSON data
            context.getString(R.string.onlinedata_not_applicable)
        }
        else -> {
            DecimalFormat(DecimalFormat0To2Digits).format(value)
        }
    }
}

fun Resources.getRawTextFile(@RawRes id: Int) =
    openRawResource(id).bufferedReader()
        .use { it.readText() }

fun getMarketValues(onlineMarketData: OnlineMarketData): Triple<String, String, String> {

    val marketPrice = to2To8Digits(onlineMarketData.marketPrice)
    val change =
        DecimalFormat("+$DecimalFormat2To4Digits;-$DecimalFormat2To4Digits").format(onlineMarketData.marketChange)
    val changePercent =
        "(${DecimalFormat("+$DecimalFormat2Digits;-$DecimalFormat2Digits").format(onlineMarketData.marketChangePercent)}%)"

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
): AssetChange {

    val (quantity, asset, fee) = getAssets(assets)

    return getAssetChange(
        quantity,
        asset + fee,
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
): AssetChange {

    if (marketPrice > 0.0) {
        var displayStr: String = ""

        if (quantity > 0.0) {
            val capital = quantity * marketPrice

            val changeValue = capital - asset
            displayStr += DecimalFormat("+$DecimalFormat2Digits;-$DecimalFormat2Digits").format(
                changeValue
            )

            val changePercent = changeValue * 100.0 / asset
            if (changePercent < 10000.0) {
                displayStr += " (${
                    DecimalFormat("+$DecimalFormat2Digits;-$DecimalFormat2Digits").format(
                        changePercent
                    )
                }%)"
            }

            val assetChangeColor =
                getChangeColor(changeValue, isPostMarket, neutralColor, context)

            val assetText = if (bold) {
                SpannableStringBuilder().bold { append(displayStr) }
            } else {
                SpannableStringBuilder().append(displayStr)
            }

            val displayColorStr = SpannableStringBuilder()

            // Use the default text color if the assetChangeColor is the neutralcolor.
            if (assetChangeColor != neutralColor) {
                displayColorStr.color(assetChangeColor) { displayColorStr.append(assetText) }
            } else {
                displayColorStr.append(assetText)
            }

            return AssetChange(displayStr, displayColorStr, assetChangeColor, changeValue)
        }
    }

    return AssetChange("", SpannableStringBuilder(), neutralColor, 0.0)
}

fun getDividendStr(
    stockItem: StockItem
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
        "${
            DecimalFormat(DecimalFormat2To4Digits).format(annualDividendRate)
        } (${
            DecimalFormat(DecimalFormat1Digit).format(annualDividendYield * 100.0)
        }%)"
    } else {
        ""
    }
}

fun getDividendStr(
    stockItem: StockItem,
    context: Context
): String {

    val annualDividendRate = if (stockItem.stockDBdata.annualDividendRate >= 0.0) {
        stockItem.stockDBdata.annualDividendRate
    } else {
        stockItem.onlineMarketData.annualDividendRate
    }

    return if (annualDividendRate > 0.0) {
        "${context.getString(R.string.dividend_in_list)} ${getDividendStr(stockItem)}"
    } else {
        ""
    }
}

fun getAssets(
    assetList: List<Asset>?
): Triple<Double, Double, Double> {
    //return getAssetUseLastAverage(assetList)
    return getAssetsRemoveOldestFirst(assetList)
}

fun getAssetUseLastAverage(
    assetList: List<Asset>?
): Pair<Double, Double> {

    var totalQuantity: Double = 0.0
    var totalPrice: Double = 0.0
    var totalFee: Double = 0.0

    assetList?.sortedBy { item ->
        item.date
    }
        ?.forEach { asset ->

            totalFee += asset.fee

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
                        totalFee = 0.0
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

    return Pair(totalQuantity, totalPrice + totalFee)
}

// Adds a marker for assets that are obsolete (bought and then sold)
fun getAssets(
    assetList: List<Asset>?,
    tagObsoleteAssetType: Int
): Triple<Double, Double, Double> {
    //return getAssetUseLastAverage(assetList, tagObsoleteAssetType)
    return getAssetsRemoveOldestFirst(assetList, tagObsoleteAssetType)
}

fun getAssetUseLastAverage(
    assetList: List<Asset>?,
    tagObsoleteAssetType: Int
): Pair<Double, Double> {

    var totalQuantity: Double = 0.0
    var totalPrice: Double = 0.0
    var totalFee: Double = 0.0

    if (assetList != null) {
        val assetListSorted = assetList.sortedBy { asset ->
            asset.date
        }

        // Reset the obsolete marker
        if (tagObsoleteAssetType != 0) {
            assetListSorted.forEach { asset ->
                asset.type = asset.type and tagObsoleteAssetType.inv()
            }
        }

        for (i in assetListSorted.indices) {

            val asset = assetListSorted[i]
            totalFee += asset.fee

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
                        totalFee = 0.0

                        // Mark all assets down to the beginning as obsolete because they are bought and all sold.
                        if (tagObsoleteAssetType != 0) {
                            for (j in i downTo 0) {
                                assetListSorted[j].type =
                                    assetListSorted[j].type or tagObsoleteAssetType
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

    return Pair(totalQuantity, totalPrice + totalFee)
}

fun tagTransferItemsInAssetList(assetList: MutableList<AssetListData>) {

    var i: Int = 0
    while (i < assetList.size) {
        // Transfer: same date and price, but quantity have different sign
        if (i < assetList.size - 1 &&
            (assetList[i].asset.date - assetList[i + 1].asset.date).absoluteValue <= 1
            && (assetList[i].asset.price - assetList[i + 1].asset.price).absoluteValue < epsilon
            && (assetList[i].asset.quantity + assetList[i + 1].asset.quantity).absoluteValue < epsilon
            && (assetList[i].asset.account != assetList[i + 1].asset.account)
        ) {
            assetList[i].transferItem = true
            assetList[i + 1].transferItem = true
        }

        i++
    }
}

fun removeTransferAssets(assetList: List<Asset>): MutableList<Asset> {

    return assetList.toMutableList()

    // Remove transfer items.
    val assetListCopy: MutableList<Asset> = mutableListOf()
    var i: Int = 0
    while (i < assetList.size) {
        // Transfer: same date and price, but quantity have different sign
        if (i < assetList.size - 1 &&
            (assetList[i].date - assetList[i + 1].date).absoluteValue <= 1
            && (assetList[i].price - assetList[i + 1].price).absoluteValue < epsilon
            && (assetList[i].quantity + assetList[i + 1].quantity).absoluteValue < epsilon
            && (assetList[i].account != assetList[i + 1].account)
        ) {
            i++
        } else {
            assetListCopy.add(assetList[i])
        }

        i++
    }

    return assetListCopy
}

fun getAssetsRemoveOldestFirst(
    assetList: List<Asset>?,
    tagObsoleteAssetType: Int = 0
): Triple<Double, Double, Double> {

    var totalQuantity: Double = 0.0
    var totalPrice: Double = 0.0
    var totalFee: Double = 0.0

    if (assetList != null) {
        val assetListSorted = assetList.sortedBy { asset ->
            asset.date
        }

        // Deep copy list.
        // Sold values (negative quantities) will be subtracted from the quantities from the beginning.
        var j: Long = 0
        val assetListSortedCopy1 = assetListSorted.map { asset ->
            Asset(
                symbol = "",
                price = asset.price,
                account = asset.account,
                quantity = asset.quantity,
                fee = asset.fee,
                type = asset.type and tagObsoleteAssetType.inv(),
                id = j++
            )
        }

        // Remove transfer items.
        val assetListSortedCopy = removeTransferAssets(assetListSortedCopy1)

        //var k = 0
        for (i in assetListSortedCopy.indices) {

            val asset = assetListSortedCopy[i]

            // remove shares from the beginning
            if (asset.quantity < 0.0) {
                var quantityToRemove = -asset.quantity
                // (Does not work with different accounts anymore)
                // for (j in k until i) {
                for (j in 0 until i) {
                    if (asset.account == assetListSortedCopy[j].account && assetListSortedCopy[j].quantity > 0.0) {
                        //if (assetListSortedCopy[j].quantity > 0.0) {
                        if (quantityToRemove > assetListSortedCopy[j].quantity) {
                            quantityToRemove -= assetListSortedCopy[j].quantity
                            assetListSortedCopy[j].quantity = 0.0
                            assetListSortedCopy[j].fee = 0.0
                        } else {
                            assetListSortedCopy[j].quantity -= quantityToRemove
                            // (Does not work with different accounts anymore)
                            // Start with the index in the next iteration where it left off.
                            // k = j
                            break
                        }
                    }
                }

                // Sold entry is subtracted already. Set to 0.
                assetListSortedCopy[i].quantity = 0.0

                // Fees are not counted when stock is sold.
                assetListSortedCopy[i].fee = 0.0
            }
        }

        // Mark all removed entry with the obsolete flag.
        for (i in assetListSortedCopy.indices) {
            if (tagObsoleteAssetType != 0 && assetListSortedCopy[i].quantity < epsilon) {
                // Set the type in the original list (not in assetListSorted2).
                assetListSorted[assetListSortedCopy[i].id!!.toInt()].type =
                    assetListSortedCopy[i].type or tagObsoleteAssetType
            }
        }

        assetListSortedCopy.forEach { asset ->
            totalQuantity += asset.quantity
            totalPrice += asset.quantity * asset.price
            totalFee += asset.fee
        }
    }

    if (totalQuantity < epsilon) {
        totalQuantity = 0.0
        totalPrice = 0.0
    }

    return Triple(totalQuantity, totalPrice, totalFee)
}

fun getTotalFee(
    assetList: List<Asset>
): Double {

    val totalFee: Double = assetList.sumOf { asset ->
        asset.fee
    }

    return totalFee
}

// Only gets the assets that are added.
fun getAddedAssets(
    assetList: List<Asset>?
): Pair<Double, Double> {

    var totalQuantity: Double = 0.0
    var totalPrice: Double = 0.0
    var totalFee: Double = 0.0

    assetList?.forEach { asset ->
        if (asset.quantity > 0.0) {
            totalQuantity += asset.quantity
            totalPrice += asset.quantity * asset.price
            totalFee += asset.fee
        }
    }

    return Pair(totalQuantity, totalPrice + totalFee)
}

fun getAddedDeletedAssets(
    assetList: List<Asset>?,
    onlineMarketData: OnlineMarketData
): Pair<Double, Double> {

    var gain: Double = 0.0
    var loss: Double = 0.0

    assetList?.forEach { asset ->
        val marketChange = onlineMarketData.marketPrice - asset.price
        if (marketChange > 0.0) {
            gain += asset.quantity * marketChange
        }
        if (marketChange < 0.0) {
            loss -= asset.quantity * marketChange
        }
    }

    return Pair(gain, loss)
}

//fun getAssetsCapitalGain(assetList: List<Asset>?): Pair<Double, Double> {
//
//  var totalQuantity: Double = 0.0
//  var totalGain: Double = 0.0
//  var totalLoss: Double = 0.0
//  var bought: Double = 0.0
//  var sold: Double = 0.0
//
//  val epsilon = 0.0001
//
//  assetList?.sortedBy { asset ->
//    asset.date
//  }
//      ?.forEach { asset ->
//        if (asset.quantity > 0.0) {
//          bought += asset.quantity * asset.price
//        }
//        if (asset.quantity < 0.0) {
//          sold += -asset.quantity * asset.price
//        }
//        totalQuantity += asset.quantity
//
//        if ((totalQuantity <= -epsilon)) {
//          // Error, more shares sold than owned
//          return Pair(0.0, 0.0)
//        }
//        if (totalQuantity < epsilon) {
//          // totalQuantity is 0: -epsilon < totalQuantity < epsilon
//          // reset if all shares are sold
//          val gain = sold - bought
//          if (gain > 0.0) {
//            totalGain += gain
//          } else
//            if (gain < 0.0) {
//              totalLoss -= gain
//            }
//          sold = 0.0
//          bought = 0.0
//        }
//      }
//
//  return Pair(totalGain, totalLoss)
//}

data class GainLoss(
    var gain: Double = 0.0,
    var loss: Double = 0.0,
    var lastTransactionDate: Long = 0L,
)

fun getAssetsCapitalGainV1(assetList: List<Asset>?): Triple<Double, Double, Map<Int, GainLoss>> {

    var totalQuantity: Double = 0.0
    var totalGain: Double = 0.0
    var totalLoss: Double = 0.0
    var bought: Double = 0.0
    var sold: Double = 0.0

    // gain/loss for each year
    val totalGainLossMap: MutableMap<Int, GainLoss> = mutableMapOf()

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
                return Triple(0.0, 0.0, hashMapOf())
            }
            if (totalQuantity < epsilon) {
                // totalQuantity is 0: -epsilon < totalQuantity < epsilon
                // reset if all shares are sold
                val localDateTime =
                    ZonedDateTime.ofInstant(
                        Instant.ofEpochSecond(asset.date),
                        ZoneOffset.systemDefault()
                    )
                val year = localDateTime.year
                if (!totalGainLossMap.containsKey(year)) {
                    totalGainLossMap[year] = GainLoss()
                }

                val gain = sold - bought
                if (gain > 0.0) {
                    totalGain += gain
                    totalGainLossMap[year]?.gain = totalGainLossMap[year]?.gain!! + gain
                } else
                    if (gain < 0.0) {
                        totalLoss -= gain
                        totalGainLossMap[year]?.loss = totalGainLossMap[year]?.loss!! - gain
                    }

                sold = 0.0
                bought = 0.0
            }
        }

    return Triple(totalGain, totalLoss, totalGainLossMap)
}

fun getAssetsCapitalGain(assetList: List<Asset>?): Triple<Double, Double, Map<Int, GainLoss>> {

    if (assetList == null) {
        return Triple(0.0, 0.0, hashMapOf())
    }

    var totalGain: Double = 0.0
    var totalLoss: Double = 0.0
    var bought: Double = 0.0
    var sold: Double = 0.0
    var lastTransactionDate: Long = 0L

    // gain/loss for each year
    val totalGainLossMap: MutableMap<Int, GainLoss> = mutableMapOf()

    // Deep copy of the list.
    val assetListCopy: List<Asset> = removeTransferAssets(
        assetList.sortedBy { asset ->
            asset.date
        }.map { it.copy() })

    // Each neg quantity triggers a gain/loss
    var k = 0
    for (i in assetListCopy.indices) {

        val asset = assetListCopy[i]
        if (asset.quantity < 0.0) {

            sold = -asset.quantity * asset.price
            lastTransactionDate = asset.date
            bought = asset.fee
            var quantityToRemove = -asset.quantity

            for (j in k until i) {

                if (asset.account == assetListCopy[j].account) {

                    bought += assetListCopy[j].fee
                    assetListCopy[j].fee = 0.0

                    if (assetListCopy[j].quantity > 0.0) {

                        // Start removing the quantity from the beginning.
                        if (quantityToRemove > assetListCopy[j].quantity) {
                            // more quantities left than bought with this transaction
                            // add the (quantity) * (price) to the bought value
                            bought += assetListCopy[j].quantity * assetListCopy[j].price
                            quantityToRemove -= assetListCopy[j].quantity
                            assetListCopy[j].quantity = 0.0
                        } else {
                            // less quantities left than bought with this transaction,
                            // add the (remaining quantity) * (price) to the bought value
                            assetListCopy[j].quantity -= quantityToRemove
                            bought += quantityToRemove * assetListCopy[j].price
                            // Start with the index in the next iteration where it left off.
                            k = j
                            break
                        }
                    }
                }
            }

            val localDateTime =
                ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(asset.date),
                    ZoneOffset.systemDefault()
                )
            val year = localDateTime.year

            if (!totalGainLossMap.containsKey(year)) {
                totalGainLossMap[year] = GainLoss()
            }

            // sold and bought are for the same quantity
            val gain = sold - bought
            if (gain > 0.0) {
                totalGain += gain
                totalGainLossMap[year]?.gain = totalGainLossMap[year]?.gain!! + gain
                totalGainLossMap[year]?.lastTransactionDate = lastTransactionDate
            } else
                if (gain < 0.0) {
                    totalLoss -= gain
                    totalGainLossMap[year]?.loss = totalGainLossMap[year]?.loss!! - gain
                    totalGainLossMap[year]?.lastTransactionDate = lastTransactionDate
                }
        }
    }

    return Triple(totalGain, totalLoss, totalGainLossMap)
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
        capitalLoss.absoluteValue < 0.001 && capitalGain > 0.0 -> {
            SpannableStringBuilder().color(context.getColor(R.color.green)) {
                bold { append("${DecimalFormat(DecimalFormat2Digits).format(capitalGain)}$formatEnd") }
            }
        }
        capitalGain.absoluteValue < 0.001 && capitalLoss > 0.0 -> {
            SpannableStringBuilder().color(context.getColor(R.color.red)) {
                bold { append("${DecimalFormat(DecimalFormat2Digits).format(-capitalLoss)}$formatEnd") }
            }
        }
        capitalGain > 0.0 && capitalLoss > 0.0 && capitalGain > capitalLoss -> {
            SpannableStringBuilder()
                .color(context.getColor(R.color.green)) {
                    bold { append("${DecimalFormat(DecimalFormat2Digits).format(capitalGain)}$formatEnd") }
                }
                .color(context.getColor(R.color.red)) {
                    bold {
                        append(
                            "$formatConcat${
                                DecimalFormat(DecimalFormat2Digits).format(
                                    capitalLoss
                                )
                            }$formatEnd"
                        )
                    }
                }
                .append(" = ")
                .color(context.getColor(R.color.green)) {
                    bold { append("${DecimalFormat(DecimalFormat2Digits).format(capitalTotal)}$formatEnd") }
                }
        }
        capitalGain > 0.0 && capitalLoss > 0.0 && capitalGain < capitalLoss -> {
            SpannableStringBuilder()
                .color(context.getColor(R.color.green)) {
                    bold { append("${DecimalFormat(DecimalFormat2Digits).format(capitalGain)}$formatEnd") }
                }
                .color(context.getColor(R.color.red)) {
                    bold {
                        append(
                            "$formatConcat${
                                DecimalFormat(DecimalFormat2Digits).format(
                                    capitalLoss
                                )
                            }$formatEnd"
                        )
                    }
                }
                .append(" = ")
                .color(context.getColor(R.color.red)) {
                    bold { append("${DecimalFormat(DecimalFormat2Digits).format(capitalTotal)}$formatEnd") }
                }
        }
        else -> {
            SpannableStringBuilder().bold {
                append("${DecimalFormat(DecimalFormat2Digits).format(0.0)}$formatEnd")
            }
        }
    }
}

fun parseStockOption(symbol: String): StockOptionData {

    val stockOption = StockOptionData()

    // named groups are not yet supported
    val match = "([A-Z.]+)(7?)\\s*(\\d+)([A-Z])(\\d+)".toRegex()
        .matchEntire(symbol.uppercase(Locale.ROOT))

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
                ZonedDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyMMdd"))
                    .toEpochSecond() // in GMT
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
    val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val networkCapabilities = connectivityManager.activeNetwork ?: return false
    val actNw = connectivityManager.getNetworkCapabilities(networkCapabilities) ?: return false

    return when {
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
        actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
        else -> false
    }
}

//fun isOnline(context: Context): Boolean {
//  val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//  val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
//  val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true
//  return isConnected
//}

fun checkUrl(url: String): String {
    return if (!url.startsWith("http://") && !url.startsWith("https://")) {
        "https://${url}"
    } else {
        url
    }
}

// Get the colored menu entries for the groups.
fun getGroupsMenuList(
    groups: List<Group>,
    backgroundListColor: Int,
    colorRef: Int,
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
        //val textColor = if (isWhiteColor(grp.color, SharedRepository.blackColor) || grp.color == backgroundListColor) {
        val textColor = if (isWhiteColor(grp.color, colorRef) || grp.color == backgroundListColor) {
            colorRef
        } else {
            grp.color
        }

        s.setSpan(ForegroundColorSpan(textColor), spacePos, s.length, 0)
        menuStrings.add(s)
    }

    return menuStrings
}

fun getMarkerColor(context: Context, index: Int): Int {
    return when (index) {
        1 -> Color.RED
        2 -> 0xFFFF6A00.toInt() // orange
        3 -> Color.YELLOW
        4 -> Color.GREEN
        5 -> Color.CYAN
        6 -> Color.BLUE
        7 -> 0xFF9000FF.toInt() // purple
        8 -> Color.MAGENTA
        9 -> Color.GRAY
        10 -> Color.BLACK
        else -> context.getColor(R.color.backgroundListColor)
    }
}

fun getMarkerSymbol(context: Context, index: Int): String {
    return when (index) {
        // https://www.alt-codes.net/animal-symbols.php
        1 -> "\uD83D\uDC31" // Cat
        2 -> "\uD83D\uDC34" // Horse
        3 -> "\uD83D\uDC10" // Goat
        4 -> "\uD83D\uDC30" // Rabbit
        5 -> "\uD83D\uDC28" // Koala
        6 -> "\uD83D\uDC25" // Chicken
        7 -> "\uD83E\uDD86" // Duck
        8 -> "\uD83E\uDD84" // Unicorn
        9 -> "\uD83D\uDC20" // Fish
        10 -> "\uD83D\uDC0C" // Snail
        else -> context.getString(R.string.no_marker) // No marker
    }
}

fun getMarkerText(
    context: Context,
    index: Int,
    setBackgroundColor: Boolean = true
): SpannableStringBuilder =
    if (index == 0) {
        if (setBackgroundColor) {
            SpannableStringBuilder()
                .backgroundColor(getMarkerColor(context, index))
                {
                    append(" ${getMarkerSymbol(context, index)} ")
                }
        } else {
            SpannableStringBuilder()
                .append(" ${getMarkerSymbol(context, index)} ")
        }
    } else {
        SpannableStringBuilder()
            .backgroundColor(getMarkerColor(context, index))
            {
                append("${getMarkerSymbol(context, index)}          ")
            }
            .backgroundColor(Color.WHITE)
            {
                color(Color.BLACK)
                {
                    append(" $index ")
                }
            }
    }

fun isWhiteColor(color: Int, colorRef: Int): Boolean {
    val r = color shr 16 and 0xff
    val g = color shr 8 and 0xff
    val b = color and 0xff
    val rr = colorRef shr 16 and 0xff
    val gr = colorRef shr 8 and 0xff
    val br = colorRef and 0xff
    return (rr - r).absoluteValue + (gr - g).absoluteValue + (br - b).absoluteValue > 650
}

fun isSimilarColor(color: Int, colorRef: Int): Boolean {
    val r = color shr 16 and 0xff
    val g = color shr 8 and 0xff
    val b = color and 0xff
    val rr = colorRef shr 16 and 0xff
    val gr = colorRef shr 8 and 0xff
    val br = colorRef and 0xff
    return (rr - r).absoluteValue + (gr - g).absoluteValue + (br - b).absoluteValue < 100
}

fun setAppTheme(context: Context) {
    val sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context /* Activity context */)
    val appTheme = sharedPreferences.getString("app_theme", "0")

    if (appTheme != null) {
        when (appTheme) {
            "0" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            }
            "1" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            "2" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            "3" -> {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO_BATTERY)
            }
        }
    }
}

fun getAppThemeColorRedGreen(context: Context) {
    val sharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context /* Activity context */)

    val appThemeColorRedGreen = sharedPreferences.getString("app_theme_textcolor_redgreen", "0")

    // Use white on red/green background when this setting is set and the night mode is off.
    useWhiteOnRedGreen = appThemeColorRedGreen == "1" &&
            context.resources?.configuration?.uiMode?.and(Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_NO
}

fun saveTextToFile(
    text: String,
    msg: String,
    context: Context,
    exportJsonUri: Uri
) {
    // Write the text string.
    try {
        context.contentResolver.openOutputStream(exportJsonUri)
            ?.use { output ->
                output as FileOutputStream
                output.channel.truncate(0)
                output.write(text.toByteArray())
            }

        Toast.makeText(context, msg, Toast.LENGTH_LONG)
            .show()

    } catch (e: Exception) {
        Toast.makeText(
            context, context.getString(R.string.export_error, e.message),
            Toast.LENGTH_LONG
        )
            .show()
        Log.d("Export JSON error", "Exception: $e")
    }
}

// https://begriffs.com/pdf/dec2frac.pdf
fun frac(x: Double): Pair<Int?, Int> {

    val eps = 0.0000001

    if (x.isFinite()) {
        val sign = x.sign.toInt()
        val xAbs = x.absoluteValue

        var z = xAbs

        var n = z.toInt()
        var d0: Int = 0
        var d1: Int = 1
        var x0 = 1.0
        var x1 = 0.0

        while ((z - z.toInt().toDouble()) > eps && (x0 - x1).absoluteValue > eps) {
            z = 1 / (z - z.toInt().toDouble())
            val d = d1 * z.toInt() + d0
            n = (xAbs * d).roundToInt()

            x0 = x1
            x1 = n.toDouble() / d.toDouble()

            d0 = d1
            d1 = d
        }

        if (d1 > 0) {
            return Pair(n * sign, d1)
        }
    }

    return Pair(null, 0)
}

fun updateFilterList(
    context: Context,
    filterDataViewModel: FilterDataViewModel,
    preferences: SharedPreferences? = null
) {

    val sharedPreferences =
        preferences ?: PreferenceManager.getDefaultSharedPreferences(context /* Activity context */)
    val selectedFilter = sharedPreferences.getString("selectedFilter", "")
    val filterActive = sharedPreferences.getBoolean("filterActive", false)
    val filterData = sharedPreferences.getString("filterSetting", "")
    if (filterData != null) {
        filterDataViewModel.setSerializedStr(filterData, selectedFilter, filterActive)
    }

}

fun setGroupBackground(
    context: Context,
    marker: Int,
    color: Int,
    textViewGroup: TextView,
    textViewGroupSep: TextView,
    textViewGroupMarker: TextView
) {
    // Set top textview to group color
    setBackgroundColor(textViewGroup, color)

    if (marker == 0) {
        // No marker, set separator and bottom textview to the same group color.
        // TextView
        textViewGroupSep.setBackgroundColor(color)
        // TextView with gradient background set.
        // Use helper function to set background to avoid reset the rounded edges.
        setBackgroundColor(textViewGroupMarker, color)
    } else {
        val markerColor = getMarkerColor(context, marker)

        // Set separator textview to background color.
        textViewGroupSep.setBackgroundColor(
            context.getColor(R.color.backgroundListColor)
        )

        // Set bottom textview to marker cmolor.
        setBackgroundColor(textViewGroupMarker, markerColor)
    }
}

// Separate the name from the currency in the trading pair.
// btcusd to btc-usd
fun idToName(symbol: String): String {
    return symbol.replace(Regex("(\\w+)(\\w{3})"), "$1-$2")
}

// Get the display name for the symbol if used outside of the stockdata class (asset, events)
fun getSymbolDisplayName(symbol: String): String {
    return if (symbolDisplayNameMap.containsKey(symbol)) symbolDisplayNameMap[symbol]!! else symbol
}
