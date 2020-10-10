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
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import com.thecloudsite.stockroom.database.Asset
import kotlin.math.absoluteValue

@RunWith(AndroidJUnit4::class)
class AssetTest {

  // Rounding error
  private val epsilon = 0.000001
  private val obsoleteAssetType
      : Int = 0x0001

  @Test
  @Throws(Exception::class)
  fun assetAddRemove() {
    fun getAssets(
      assetList: List<Asset>?,
      tagObsoleteAssetType: Int = 0
    ): Pair<Double, Double> {

      var totalShares: Double = 0.0
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
          if (asset.shares > 0.0) {
            totalShares += asset.shares
            totalPrice += asset.shares * asset.price
          } else
          // removed shares
            if (asset.shares < 0.0) {
              // removed all?
              if (-asset.shares >= (totalShares - com.thecloudsite.stockroom.utils.epsilon)) {
                // reset if more removed than owned
                totalShares = 0.0
                totalPrice = 0.0

                if (tagObsoleteAssetType != 0) {
                  for (j in i downTo 0) {
                    assetListSorted[j].type = assetListSorted[j].type or tagObsoleteAssetType
                  }
                }
              } else {
                // adjust the total price for the removed shares
                if (totalShares > com.thecloudsite.stockroom.utils.epsilon) {
                  val averageSharePrice = totalPrice / totalShares
                  totalShares += asset.shares
                  totalPrice = totalShares * averageSharePrice
                }
              }
            }
        }
      }

      return Pair(totalShares, totalPrice)
    }

    val assetList1 = listOf(
        Asset(
            symbol = "s1",
            shares = 10.0,
            price = 20.0,
            date = 1
        ),
        Asset(
            symbol = "s1",
            shares = 20.0,
            price = 50.0,
            date = 2
        ),
        Asset(
            symbol = "s1",
            shares = -10.0,
            price = 0.0,
            date = 3
        ),
        Asset(
            symbol = "s1",
            shares = 100.0,
            price = 20.0,
            date = 4
        ),
        Asset(
            symbol = "s1",
            shares = -50.0,
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
            shares = 0.0,
            price = 0.0,
            date = 1
        ),
        Asset(
            symbol = "s1",
            shares = -20.0,
            price = 50.0,
            date = 2
        ),
        Asset(
            symbol = "s1",
            shares = 20.0,
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
            shares = 20.0,
            price = 20.0,
            date = 1
        ),
        Asset(
            symbol = "s1",
            shares = -20.0,
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
            shares = 0.0,
            price = 0.0,
            date = 1
        ),
        Asset(
            symbol = "s1",
            shares = -epsilon / 2,
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
            shares = -30.0,
            price = 0.0,
            date = 3,
            type = 0xff00
        ),
        Asset(
            symbol = "s1",
            shares = 20.0,
            price = 50.0,
            date = 2
        ),
        Asset(
            symbol = "s1",
            shares = 100.0,
            price = 20.0,
            date = 4
        ),
        Asset(
            symbol = "s1",
            shares = -50.0,
            price = 0.0,
            date = 5,
            type = obsoleteAssetType
        ),
        Asset(
            symbol = "s1",
            shares = 10.0,
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

      var totalShares: Double = 0.0
      var totalGain: Double = 0.0
      var totalBought: Double = 0.0
      var totalSold: Double = 0.0

      assetList?.sortedBy { asset ->
        asset.date
      }
          ?.forEach { asset ->
            if (asset.shares > 0.0) {
              totalBought += asset.shares * asset.price
            }
            if (asset.shares < 0.0) {
              totalSold += -asset.shares * asset.price
            }
            totalShares += asset.shares

            if ((totalShares <= -com.thecloudsite.stockroom.utils.epsilon)) {
              // Error, more shares sold than owned
              return Double.NEGATIVE_INFINITY
            }
            if ((totalShares < com.thecloudsite.stockroom.utils.epsilon)) {
              // reset if more removed than owned
              totalGain += totalSold - totalBought
            }
          }

      return totalGain
    }

    val assetList1 = listOf(
        Asset(
            symbol = "s1",
            shares = 10.0,
            price = 20.0,
            date = 1
        ),
        Asset(
            symbol = "s1",
            shares = 20.0,
            price = 50.0,
            date = 2
        ),
        Asset(
            symbol = "s1",
            shares = -30.0,
            price = 100.0,
            date = 3
        ),
        Asset(
            symbol = "s1",
            shares = 100.0,
            price = 20.0,
            date = 4
        ),
        Asset(
            symbol = "s1",
            shares = -50.0,
            price = 0.0,
            date = 5
        )
    )
    val totalGain1 = getAssetsCapitalGain(assetList1)
    assertEquals(1800.0, totalGain1, epsilon)

    val assetList2 = listOf(
        Asset(
            symbol = "s1",
            shares = 0.0,
            price = 0.0,
            date = 1
        ),
        Asset(
            symbol = "s1",
            shares = -20.0,
            price = 50.0,
            date = 2
        ),
        Asset(
            symbol = "s1",
            shares = 20.0,
            price = 2.0,
            date = 3
        )
    )
    val totalGain2 = getAssetsCapitalGain(assetList2)
    assertEquals(Double.NEGATIVE_INFINITY, totalGain2, epsilon)

    val assetList3 = listOf(
        Asset(
            symbol = "s1",
            shares = 20.0,
            price = 30.0,
            date = 1
        ),
        Asset(
            symbol = "s1",
            shares = -20.0,
            price = 10.0,
            date = 2
        )
    )
    val totalGain3 = getAssetsCapitalGain(assetList3)
    assertEquals(-400.0, totalGain3, epsilon)
  }
}
