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
import com.thecloudsite.stockroom.utils.GainLoss
import com.thecloudsite.stockroom.utils.getAssetUseLastAverage
import com.thecloudsite.stockroom.utils.getAssets
import com.thecloudsite.stockroom.utils.getAssetsCapitalGain
import com.thecloudsite.stockroom.utils.getAssetsRemoveOldestFirst
import com.thecloudsite.stockroom.utils.parseStockOption
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
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
  private val epsilon = 0.0000001
  private val obsoleteAssetType
      : Int = 0x0001
  private val removedAssetType
      : Int = 0x0002

  @Test
  @Throws(Exception::class)
  fun test1() {
    data class Pairs(
      var x: Int,
      var y: Int,
    )

    val liste: MutableList<Pairs> = mutableListOf()
    liste.add(Pairs(x = 1, y = 1))
  }

  @Test
  @Throws(Exception::class)
  fun optionFormat() {

    //    The OCC option symbol consists of four parts:
    //
    //    Root symbol of the underlying stock or ETF, padded with spaces to 6 characters
    //    Expiration date, 6 digits in the format yymmdd
    //    Option type, either P or C, for put or call
    //    Strike price, as the price x 1000, front padded with 0s to 8 digits

    fun getDate(date: Long): String {
      val localDateTime = LocalDateTime.ofEpochSecond(date, 0, ZoneOffset.UTC)
      return localDateTime.format(DateTimeFormatter.ofPattern("yyMMdd"))
    }

    // 100 per order
    val option1 = parseStockOption("QQQ230120C00295000")
    assertEquals(100, option1.sharesPerOption)
    assertEquals("230120", getDate(option1.expirationDate))
    assertEquals(295.0, option1.strikePrice, epsilon)
    assertEquals(AssetType.CallOption.value, option1.type)

    // represents a mini call option (10 shares) on AAPL, with a strike price of $470, expiring on Nov 1, 2013
    val miniOption = parseStockOption("AAPL7 131101P00470000")
    assertEquals(10, miniOption.sharesPerOption)
    assertEquals("131101", getDate(miniOption.expirationDate))
    assertEquals(470.0, miniOption.strikePrice, epsilon)
    assertEquals(AssetType.PutOption.value, miniOption.type)

    // the standard call option (100 shares), with the same strike and expiration date
    val option2 = parseStockOption("AAPL  131101C00470000")
    assertEquals(100, option2.sharesPerOption)
    assertEquals("131101", getDate(option2.expirationDate))
    assertEquals(470.0, option2.strikePrice, epsilon)
    assertEquals(AssetType.CallOption.value, option2.type)

    for (i in 0..10000) {
      val option1 = parseStockOption("QQQ230120C00295000")
    }
  }

  @Test
  @Throws(Exception::class)
  fun assetAddRemove() {
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
    val (totalShares1, totalPrice1) = getAssetUseLastAverage(assetList1)
    assertEquals(70.0, totalShares1, epsilon)
    assertEquals(1633.3333333333333, totalPrice1, epsilon)

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
    val (totalShares2, totalPrice2) = getAssetUseLastAverage(assetList2)
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
    val (totalShares3, totalPrice3) = getAssetUseLastAverage(assetList3)
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
    val (totalShares4, totalPrice4) = getAssetUseLastAverage(assetList4)
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
    val (totalShares5, totalPrice5) = getAssetUseLastAverage(assetList5, obsoleteAssetType)
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
  fun assetAddRemove2() {

    val assetList1 = listOf(
        Asset(
            symbol = "s1",
            quantity = 20.0,
            price = 20.0,
            date = 1
        ),
        Asset(
            symbol = "s1",
            quantity = -10.0,
            price = 50.0,
            date = 2
        )
    )

    val (totalShares1, totalPrice1) = getAssets(assetList1)
    assertEquals(10.0, totalShares1, epsilon)
    assertEquals(200.0, totalPrice1, epsilon)

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
            date = 1592179200,
            price = 0.7659,
            quantity = 2000.0, symbol = "s2"
        ),
        Asset(
            date = 1592265600,
            price = 0.7774,
            quantity = 3200.0, symbol = "s2"
        ),
        Asset(
            date = 1592265600,
            price = 0.7774,
            quantity = 6800.0, symbol = "s2"
        ),
        Asset(
            date = 1597708800,
            price = 1.6991,
            quantity = 30000.0, symbol = "s2"
        ),
        Asset(
            date = 1599116400,
            price = 1.23,
            quantity = 8000.0, symbol = "s2"
        ),
        Asset(
            date = 1603152000,
            price = 1.19,
            quantity = 255.0, symbol = "s2"
        ),
        Asset(
            date = 1603152000,
            price = 1.19,
            quantity = 75.0, symbol = "s2"
        ),
        Asset(
            date = 1603152000,
            price = 1.19,
            quantity = 1100.0, symbol = "s2"
        ),
        Asset(
            date = 1603152000,
            price = 1.19,
            quantity = 5281.0, symbol = "s2"
        ),
        Asset(
            date = 1603152000,
            price = 1.19,
            quantity = 400.0, symbol = "s2"
        ),
        Asset(
            date = 1603152000,
            price = 1.19,
            quantity = 400.0, symbol = "s2"
        ),
        Asset(
            date = 1603152000,
            price = 1.19,
            quantity = 1000.0, symbol = "s2"
        ),
        Asset(
            date = 1603152000,
            price = 1.19,
            quantity = 614.0, symbol = "s2"
        ),
        Asset(
            date = 1603238400,
            price = 1.1597,
            quantity = 10000.0, symbol = "s2"
        ),
        Asset(
            date = 1605484800,
            price = 1.0363,
            quantity = 875.0, symbol = "s2"
        ),
        Asset(
            date = 1607040000,
            price = 1.06,
            quantity = -10000.0, symbol = "s2"
        ),
        Asset(
            date = 1607040000,
            price = 1.0604,
            quantity = -10000.0, symbol = "s2"
        ),
        Asset(
            date = 1607040000,
            price = 1.07,
            quantity = -10000.0, symbol = "s2"
        ),
        Asset(
            date = 1607040000,
            price = 1.0703,
            quantity = -10000.0, symbol = "s2"
        ),
        Asset(
            date = 1607385600,
            price = 1.0804,
            quantity = -10000.0, symbol = "s2"
        ),
        Asset(
            date = 1607385600,
            price = 1.085,
            quantity = -5000.0, symbol = "s2"
        ),
        Asset(
            date = 1607472000,
            price = 1.07,
            quantity = -2200.0, symbol = "s2"
        ),
        Asset(
            date = 1607472000,
            price = 1.0649,
            quantity = -7800.0, symbol = "s2"
        ),
    )

    // 1.17 bei 15000 (17547), 1.16 bei 5000 (5798.50)
    val (totalShares3, totalPrice3) = getAssetsRemoveOldestFirst(assetList3)
    val totalSP = totalPrice3 / totalShares3
    assertEquals(5000.0, totalShares3, epsilon)
    assertEquals(5690.525, totalPrice3, epsilon)
    assertEquals(1.138105, totalSP, epsilon)

    val assetList4 = listOf(
        Asset(
            symbol = "s1",
            quantity = 300.0,
            price = 76.55,
            date = 1
        ),
        Asset(
            symbol = "s1",
            quantity = 250.0,
            price = 81.43,
            date = 2
        ),
        Asset(
            symbol = "s1",
            quantity = 200.0,
            price = 82.45,
            date = 3
        ),
        Asset(
            symbol = "s1",
            quantity = -100.0,
            price = 91.73,
            date = 4
        )
    )

    // UseLastAverage: 650@79.75 = 51837.50
    val (totalShares42, totalPrice42) = getAssetUseLastAverage(assetList4)
    assertEquals(79.75, totalPrice42 / totalShares42, epsilon)
    assertEquals(650.0, totalShares42, epsilon)
    assertEquals(51837.50, totalPrice42, epsilon)

    // RemoveOldestFirst: 650@80.24 = 52157.50
    val (totalShares43, totalPrice43) = getAssetsRemoveOldestFirst(assetList4)
    assertEquals(80.24230769230769, totalPrice43 / totalShares43, epsilon)
    assertEquals(650.0, totalShares43, epsilon)
    assertEquals(52157.50, totalPrice43, epsilon)

    val assetList5 = listOf(
        Asset(
            symbol = "s1",
            quantity = 0.0,
            price = 0.0,
            date = 1
        ),
        Asset(
            symbol = "s1",
            quantity = -20.0,
            price = 20.0,
            date = 2
        ),
        Asset(
            symbol = "s1",
            quantity = -20.0,
            price = 20.0,
            date = 3
        )
    )
    val (totalShares5, totalPrice5) = getAssets(assetList5)
    assertEquals(0.0, totalShares5, epsilon)
    assertEquals(0.0, totalPrice5, epsilon)

    val assetList6 = listOf(
        Asset(
            symbol = "s1",
            quantity = 0.0,
            price = 0.0,
            date = 1
        ),
        Asset(
            symbol = "s1",
            quantity = 20.0,
            price = 20.0,
            date = 2
        ),
        Asset(
            symbol = "s1",
            quantity = -20.0,
            price = 20.0,
            date = 3
        )
    )
    val (totalShares6, totalPrice6) = getAssets(assetList6)
    assertEquals(0.0, totalShares6, epsilon)
    assertEquals(0.0, totalPrice6, epsilon)

    val assetList7 = listOf(
        Asset(
            symbol = "s1",
            quantity = 0.0,
            price = 0.0,
            date = 1
        ),
        Asset(
            symbol = "s1",
            quantity = 20.0,
            price = 10.0,
            date = 2
        ),
        Asset(
            symbol = "s1",
            quantity = 20.0,
            price = 5.0,
            date = 3
        )
    )
    val (totalShares7, totalPrice7) = getAssets(assetList7)
    assertEquals(40.0, totalShares7, epsilon)
    assertEquals(300.0, totalPrice7, epsilon)
  }

  @Test
  @Throws(Exception::class)
  fun assetCapitalGainTest1() {
    val assetList1 = listOf(
      Asset(
        symbol = "s1",
        quantity = 10.0,
        price = 5.0,
        date = 1
      ),
      Asset(
        symbol = "s1",
        quantity = -10.0,
        price = 10.0,
        date = 2
      ),
      Asset(
        symbol = "s1",
        quantity = 5.0,
        price = 1.0,
        date = 3
      ),
      Asset(
        symbol = "s1",
        quantity = -2.0,
        price = 2.0,
        date = 4
      ),
      Asset(
        symbol = "s1",
        quantity = -3.0,
        price = 10.0,
        date = 5
      ),
      Asset(
        symbol = "s1",
        quantity = 10.0,
        price = 1.0,
        date = 6
      ),
      Asset(
        symbol = "s1",
        quantity = -10.0,
        price = 0.10,
        date = 7
      )
    )
    val (capitalGain1, capitalLoss1, gainLossMap1) = getAssetsCapitalGain(assetList1)
    assertEquals(1800.0, capitalGain1 - capitalLoss1, epsilon)

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
    val (capitalGain2, capitalLoss2, gainLossMap2) = getAssetsCapitalGain(assetList2)
    assertEquals(0.0, capitalGain2 - capitalLoss2, epsilon)

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
    val (capitalGain3, capitalLoss3, gainLossMap3) = getAssetsCapitalGain(assetList3)
    assertEquals(-400.0 + 4.0, capitalGain3 - capitalLoss3, epsilon)
  }

  @Test
  @Throws(Exception::class)
  fun assetCapitalGainTest2() {
    val assetList1 = listOf(
      Asset(
        symbol = "s1",
        quantity = 1500.0,
        price = 6.6011,
        date = 1
      ),
      Asset(
        symbol = "s1",
        quantity = 2000.0,
        price = 7.105,
        date = 2
      ),
      Asset(
        symbol = "s1",
        quantity = -2683.0,
        price = 8.65,
        date = 3
      )
    )
    val (capitalGain1, capitalLoss1, gainLossMap1) = getAssetsCapitalGain(assetList1)
    assertEquals(7501.515, capitalGain1 - capitalLoss1, epsilon)
  }

  @Test
  @Throws(Exception::class)
  fun assetCapitalGainDateTest() {

    val localDateTime2020 = LocalDateTime.of(2020, 1, 1, 0, 0)
    val seconds2020 = localDateTime2020.toEpochSecond(ZoneOffset.UTC)

    val localDateTime = LocalDateTime.ofEpochSecond(seconds2020, 0, ZoneOffset.UTC)

    assertEquals(2020, localDateTime.year)
  }

  @Test
  @Throws(Exception::class)
  fun assetCapitalGainTotalGainsTest() {

    val totalGains: MutableMap<Int, GainLoss> = mutableMapOf()

    val localDateTime2020 = LocalDateTime.of(2020, 1, 1, 0, 0)
    val seconds2020 = localDateTime2020.toEpochSecond(ZoneOffset.UTC)
    val localDateTime = LocalDateTime.ofEpochSecond(seconds2020, 0, ZoneOffset.UTC)
    val year = localDateTime.year
    totalGains[year] = GainLoss(1.0, 2.0)
    totalGains[year]?.gain = totalGains[year]?.gain!! + 1.0
    totalGains[year]?.loss = totalGains[year]?.loss!! + 2.0

    val gains1 = totalGains[year]
    assertEquals(2.0, gains1?.gain)
    assertEquals(4.0, gains1?.loss)
  }

  @Test
  @Throws(Exception::class)
  fun assetCapitalGainDate1() {

    val localDateTime2020 = LocalDateTime.of(2022, 1, 1, 0, 0)
    val seconds2020 = localDateTime2020.toEpochSecond(ZoneOffset.UTC)

    val localDateTime2021 = LocalDateTime.of(2021, 1, 1, 0, 0)
    val seconds2021 = localDateTime2021.toEpochSecond(ZoneOffset.UTC)

    val assetList1 = listOf(
        Asset(
            symbol = "s1",
            quantity = 1.0,
            price = 20.0,
            date = seconds2020
        ),
        Asset(
            symbol = "s1",
            quantity = 1.0,
            price = 50.0,
            date = seconds2020
        ),
        Asset(
            symbol = "s1",
            quantity = -2.0,
            price = 70.0,
            date = seconds2020
        ),
        Asset(
            symbol = "s1",
            quantity = 1.0,
            price = 100.0,
            date = seconds2021
        ),
        Asset(
            symbol = "s1",
            quantity = -1.0,
            price = 10.0,
            date = seconds2021
        ),
        Asset(
            symbol = "s1",
            quantity = 1.0,
            price = 10.0,
            date = seconds2021
        ),
        Asset(
            symbol = "s1",
            quantity = -1.0,
            price = 1.0,
            date = seconds2021
        )
    )

    val (capitalGain1, capitalLoss1, totalGains) = getAssetsCapitalGain(assetList1)
    assertEquals(2, totalGains.size)
    assertEquals(0.0, totalGains[2021]?.gain)
    assertEquals(99.0, totalGains[2021]?.loss)
    assertEquals(70.0, totalGains[2022]?.gain)
    assertEquals(0.0, totalGains[2022]?.loss)
  }


  @Test
  @Throws(Exception::class)
  fun assetCapitalGainDate2() {

    val localDateTime2020 = LocalDateTime.of(2020, 1, 1, 0, 0)
    val seconds2020 = localDateTime2020.toEpochSecond(ZoneOffset.UTC)

    val localDateTime2021 = LocalDateTime.of(2021, 1, 1, 0, 0)
    val seconds2021 = localDateTime2021.toEpochSecond(ZoneOffset.UTC)

    val assetList1 = listOf(
        Asset(
            symbol = "s1",
            quantity = 1.0,
            price = 20.0,
            date = seconds2020
        ),
        Asset(
            symbol = "s1",
            quantity = 1.0,
            price = 50.0,
            date = seconds2020
        ),
        Asset(
            symbol = "s1",
            quantity = -2.0,
            price = 70.0,
            date = seconds2021
        ),
    )

    val (capitalGain1, capitalLoss1, totalGains) = getAssetsCapitalGain(assetList1)
    assertEquals(1, totalGains.size)
    assertEquals(70.0, totalGains[2021]?.gain)
    assertEquals(0.0, totalGains[2021]?.loss)
  }
}
