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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thecloudsite.stockroom.database.Asset
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class AssetType(val value: Int) {
  Stock(0),
  CallOption(1),
  PutOption(2),
  UnknownOption(3),
}

@RunWith(AndroidJUnit4::class)
class AssetTest {

  // Rounding error
  private val epsilon = 0.000001
  private val obsoleteAssetType
      : Int = 0x0001

  @Test
  @Throws(Exception::class)
  fun optionFormat() {

    //    The OCC option symbol consists of four parts:
    //
    //    Root symbol of the underlying stock or ETF, padded with spaces to 6 characters
    //    Expiration date, 6 digits in the format yymmdd
    //    Option type, either P or C, for put or call
    //    Strike price, as the price x 1000, front padded with 0s to 8 digits

    data class StockOptionData(
      var sharesPerOption: Int = 0,
      var expirationDate: Long = 0,
      var strikePrice: Double = 0.0,
      var type: Int = AssetType.UnknownOption.value
    )

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

    fun getDate(date: Long): String {
      val localDateTime = LocalDateTime.ofEpochSecond(date, 0, ZoneOffset.UTC)
      return localDateTime.format(DateTimeFormatter.ofPattern("yyMMdd"))
    }

    // 100 per order
    val option1 = parseStockOption("QQQ230120C00295000")
    assertEquals(100, option1.sharesPerOption)
    assertEquals("230120", getDate(option1.expirationDate))
    assertEquals(295.0, option1.strikePrice, epsilon)
    assertEquals("C", option1.type)

    // represents a mini call option (10 shares) on AAPL, with a strike price of $470, expiring on Nov 1, 2013
    val miniOption = parseStockOption("AAPL7 131101P00470000")
    assertEquals(10, miniOption.sharesPerOption)
    assertEquals("131101", getDate(miniOption.expirationDate))
    assertEquals(470.0, miniOption.strikePrice, epsilon)
    assertEquals("P", miniOption.type)

    // the standard call option (100 shares), with the same strike and expiration date
    val option2 = parseStockOption("AAPL  131101C00470000")
    assertEquals(100, option2.sharesPerOption)
    assertEquals("131101", getDate(option2.expirationDate))
    assertEquals(470.0, option2.strikePrice, epsilon)
    assertEquals("C", option2.type)

    for (i in 0..10000) {
      val option1 = parseStockOption("QQQ230120C00295000")
    }
  }

  @Test
  @Throws(Exception::class)
  fun assetAddRemove() {
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
              if (-asset.quantity >= (totalQuantity - com.thecloudsite.stockroom.utils.epsilon)) {
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
                if (totalQuantity > com.thecloudsite.stockroom.utils.epsilon) {
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

    val assetList1 = listOf(
        Asset(
            symbol = "s1",
            quantity = 10.0,
            price = 20.0,
            date = 1
        ),
        Asset(
            symbol = "s1",
            quantity = 20.0,
            price = 50.0,
            date = 2
        ),
        Asset(
            symbol = "s1",
            quantity = -10.0,
            price = 0.0,
            date = 3
        ),
        Asset(
            symbol = "s1",
            quantity = 100.0,
            price = 20.0,
            date = 4
        ),
        Asset(
            symbol = "s1",
            quantity = -50.0,
            price = 0.0,
            date = 5
        )
    )
    val (totalShares1, totalPrice1) = getAssets(assetList1)
    assertEquals(70.0, totalShares1, epsilon)
    assertEquals(1633.333333, totalPrice1, epsilon)

    val assetList2 = listOf(
        Asset(
            symbol = "s1",
            quantity = 0.0,
            price = 0.0,
            date = 1
        ),
        Asset(
            symbol = "s1",
            quantity = -20.0,
            price = 50.0,
            date = 2
        ),
        Asset(
            symbol = "s1",
            quantity = 20.0,
            price = 2.0,
            date = 3
        )
    )
    val (totalShares2, totalPrice2) = getAssets(assetList2)
    assertEquals(20.0, totalShares2, epsilon)
    assertEquals(40.0, totalPrice2, epsilon)

    val assetList3 = listOf(
        Asset(
            symbol = "s1",
            quantity = 20.0,
            price = 20.0,
            date = 1
        ),
        Asset(
            symbol = "s1",
            quantity = -20.0,
            price = 50.0,
            date = 2
        )
    )
    val (totalShares3, totalPrice3) = getAssets(assetList3)
    assertEquals(0.0, totalShares3, epsilon)
    assertEquals(0.0, totalPrice3, epsilon)

    val assetList4 = listOf(
        Asset(
            symbol = "s1",
            quantity = 0.0,
            price = 0.0,
            date = 1
        ),
        Asset(
            symbol = "s1",
            quantity = -epsilon / 2,
            price = 50.0,
            date = 2
        )
    )
    val (totalShares4, totalPrice4) = getAssets(assetList4)
    assertEquals(0.0, totalShares4, epsilon)
    assertEquals(0.0, totalPrice4, epsilon)

    val assetList5 = listOf(
        Asset(
            symbol = "s1",
            quantity = -30.0,
            price = 0.0,
            date = 3,
            type = 0xff00
        ),
        Asset(
            symbol = "s1",
            quantity = 20.0,
            price = 50.0,
            date = 2
        ),
        Asset(
            symbol = "s1",
            quantity = 100.0,
            price = 20.0,
            date = 4
        ),
        Asset(
            symbol = "s1",
            quantity = -50.0,
            price = 0.0,
            date = 5,
            type = obsoleteAssetType
        ),
        Asset(
            symbol = "s1",
            quantity = 10.0,
            price = 20.0,
            date = 1
        )
    )
    val (totalShares5, totalPrice5) = getAssets(assetList5, obsoleteAssetType)
    assertEquals(50.0, totalShares5, epsilon)
    assertEquals(1000.0, totalPrice5, epsilon)
    assertEquals(0xff00 or obsoleteAssetType, assetList5[0].type)
    assertEquals(obsoleteAssetType, assetList5[1].type)
    assertEquals(0, assetList5[2].type)
    assertEquals(0, assetList5[3].type)
    assertEquals(obsoleteAssetType, assetList5[4].type)
  }

  @Test
  @Throws(Exception::class)
  fun assetCapitalGain() {
    fun getAssetsCapitalGain(assetList: List<Asset>?): Double {

      var totalAmount: Double = 0.0
      var totalGain: Double = 0.0
      var totalBought: Double = 0.0
      var totalSold: Double = 0.0

      assetList?.sortedBy { asset ->
        asset.date
      }
          ?.forEach { asset ->
            if (asset.quantity > 0.0) {
              totalBought += asset.quantity * asset.price
            }
            if (asset.quantity < 0.0) {
              totalSold += -asset.quantity * asset.price
            }
            totalAmount += asset.quantity

            if ((totalAmount <= -com.thecloudsite.stockroom.utils.epsilon)) {
              // Error, more shares sold than owned
              return 0.0
            }
            if (totalAmount < com.thecloudsite.stockroom.utils.epsilon) {
              // totalShares are 0: -epsilon < totalShares < epsilon
              // reset if all shares are sold
              totalGain += totalSold - totalBought
              totalSold = 0.0
              totalBought = 0.0
            }
          }

      return totalGain
    }

    val assetList1 = listOf(
        Asset(
            symbol = "s1",
            quantity = 10.0,
            price = 20.0,
            date = 1
        ),
        Asset(
            symbol = "s1",
            quantity = 20.0,
            price = 50.0,
            date = 2
        ),
        Asset(
            symbol = "s1",
            quantity = -30.0,
            price = 100.0,
            date = 3
        ),
        Asset(
            symbol = "s1",
            quantity = 100.0,
            price = 20.0,
            date = 4
        ),
        Asset(
            symbol = "s1",
            quantity = -50.0,
            price = 0.0,
            date = 5
        )
    )
    val totalGain1 = getAssetsCapitalGain(assetList1)
    assertEquals(1800.0, totalGain1, epsilon)

    val assetList2 = listOf(
        Asset(
            symbol = "s1",
            quantity = 0.0,
            price = 0.0,
            date = 1
        ),
        Asset(
            symbol = "s1",
            quantity = -20.0,
            price = 50.0,
            date = 2
        ),
        Asset(
            symbol = "s1",
            quantity = 20.0,
            price = 2.0,
            date = 3
        )
    )
    val totalGain2 = getAssetsCapitalGain(assetList2)
    assertEquals(0.0, totalGain2, epsilon)

    val assetList3 = listOf(
        Asset(
            symbol = "s1",
            quantity = 20.0,
            price = 30.0,
            date = 1
        ),
        Asset(
            symbol = "s1",
            quantity = -20.0,
            price = 10.0,
            date = 2
        ),  // 400.0 loss
        Asset(
            symbol = "s1",
            quantity = 2.0,
            price = 3.0,
            date = 3
        ),
        Asset(
            symbol = "s1",
            quantity = -2.0,
            price = 5.0,
            date = 4
        ),  // 4.0 gain
        Asset(
            symbol = "s1",
            quantity = 2.0,
            price = 5.0,
            date = 5
        )
    )
    val totalGain3 = getAssetsCapitalGain(assetList3)
    assertEquals(-400.0 + 4.0, totalGain3, epsilon)
  }
}
