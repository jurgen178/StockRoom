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
import org.junit.Assert.assertNotEquals
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.junit.Test
import org.junit.runner.RunWith
import java.text.NumberFormat
import java.util.Locale
import com.google.gson.Gson
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.database.Dividend
import com.thecloudsite.stockroom.database.Event
import com.thecloudsite.stockroom.database.StockDBdata
import com.thecloudsite.stockroom.utils.validateDouble
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.FULL
import java.time.format.FormatStyle.LONG
import java.time.format.FormatStyle.MEDIUM
import java.time.format.FormatStyle.SHORT

@RunWith(AndroidJUnit4::class)
class StockRoomTest {

  @Test
  @Throws(Exception::class)
  fun timeTest() {
    val localDateTime = LocalDateTime.ofEpochSecond(12345, 0, ZoneOffset.UTC)

    val dateShort = localDateTime.format(DateTimeFormatter.ofLocalizedDate(SHORT))
    val dateMedium = localDateTime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
    val dateLong = localDateTime.format(DateTimeFormatter.ofLocalizedDate(LONG))
    val dateFull = localDateTime.format(DateTimeFormatter.ofLocalizedDate(FULL))
    val dateMY = localDateTime.format(DateTimeFormatter.ofPattern("MMMM u"))

    assertEquals("01.01.70", dateShort)
    assertEquals("01.01.1970", dateMedium)
    assertEquals("1. Januar 1970", dateLong)
    assertEquals("Donnerstag, 1. Januar 1970", dateFull)
    assertEquals("Januar 1970", dateMY)

    val timeShort = localDateTime.format(DateTimeFormatter.ofLocalizedTime(SHORT))
    val timeMedium = localDateTime.format(DateTimeFormatter.ofLocalizedTime(MEDIUM))
    // Not defined
    //val timeLong = localDateTime.format(DateTimeFormatter.ofLocalizedTime(LONG))
    //val timeFull = localDateTime.format(DateTimeFormatter.ofLocalizedTime(FULL))

    assertEquals("03:25", timeShort)
    assertEquals("03:25:45", timeMedium)

    val dateTimeShort = localDateTime.format(DateTimeFormatter.ofLocalizedDateTime(SHORT))
    val dateTimeMedium = localDateTime.format(DateTimeFormatter.ofLocalizedDateTime(MEDIUM))
    // Not defined
    //val dateTimeLong = localDateTime.format(DateTimeFormatter.ofLocalizedDateTime(LONG))
    //val dateTimeFull = localDateTime.format(DateTimeFormatter.ofLocalizedDateTime(FULL))

    assertEquals("01.01.70, 03:25", dateTimeShort)
    assertEquals("01.01.1970, 03:25:45", dateTimeMedium)
  }

  @Test
  @Throws(Exception::class)
  fun timeConvertTest() {
    val datetimeStr1 = "Fri, 31 Jul 2020 14:54:54 GMT"
    val localDateTime1 = LocalDateTime.parse(datetimeStr1, DateTimeFormatter.RFC_1123_DATE_TIME)
    val dateStr = localDateTime1.format(DateTimeFormatter.ofLocalizedDate(FULL))
    val timeStr = localDateTime1.format(DateTimeFormatter.ofLocalizedTime(MEDIUM))
    assertEquals("Freitag, 31. Juli 2020", dateStr)
    assertEquals("14:54:54", timeStr)

    val datetimeStr2 = "ABC, 31 Jul 2020 14:54:54 DEF"
    var localDateTime2: LocalDateTime? = null
    try {
      localDateTime2 = LocalDateTime.parse(datetimeStr2, DateTimeFormatter.RFC_1123_DATE_TIME)
    } catch (e: java.lang.Exception) {
    }
    assertEquals(null, localDateTime2)
  }

  @Test
  @Throws(Exception::class)
  fun accessCounterTest() {
    val lastStatsCounters = IntArray(5){ -1 }

    fun getCounts(): String {
      return lastStatsCounters.filter { it >= 0 }
          .joinToString(
              prefix = "[",
              separator = ",",
              postfix = "]"
          )
    }

    fun shiftRight() {
      lastStatsCounters.forEachIndexed { i, _ ->
        val reverseIndex = lastStatsCounters.size - i - 1
        if (reverseIndex > 0) {
          lastStatsCounters[reverseIndex] = lastStatsCounters[reverseIndex - 1]
        }
      }
    }

    var lastCounts = getCounts()
    assertEquals("[]", lastCounts)

    shiftRight()
    lastStatsCounters[0] = 1
    lastCounts = getCounts()
    assertEquals("[1]", lastCounts)

    shiftRight()
    lastStatsCounters[0] = 2
    lastCounts = getCounts()
    assertEquals("[2,1]", lastCounts)

    shiftRight()
    lastStatsCounters[0] = 0
    lastCounts = getCounts()
    assertEquals("[0,2,1]", lastCounts)

    shiftRight()
    lastStatsCounters[0] = 4
    lastCounts = getCounts()
    assertEquals("[4,0,2,1]", lastCounts)

    shiftRight()
    lastStatsCounters[0] = 5
    lastCounts = getCounts()
    assertEquals("[5,4,0,2,1]", lastCounts)

    shiftRight()
    lastStatsCounters[0] = 6
    lastCounts = getCounts()
    assertEquals("[6,5,4,0,2]", lastCounts)
  }

  @Test
  @Throws(Exception::class)
  fun addmap() {
    val assetList = listOf(
        Asset(
            symbol = "s1",
            quantity = 11.0,
            price = 12.0
        ), Asset(
        symbol = "s2",
        quantity = 21.0,
        price = 22.0
    ), Asset(
        symbol = "s2",
        quantity = 211.0,
        price = 222.0
    ), Asset(
        symbol = "s3",
        quantity = 21.0,
        price = 22.0
    )
    )

    val assetItems = HashMap<String, List<Asset>>()
    assetList.forEach { asset ->
      val symbol = asset.symbol

      if (assetItems.containsKey(symbol)) {
        val list = assetItems[symbol]!!.toMutableList()
        list.add(asset)
        assetItems[symbol] = list
      } else {
        assetItems[symbol] = listOf(asset)
      }
    }

    assertEquals(assetItems.size, 3)
    assertEquals(assetItems["s2"]?.size, 2)
  }

  private fun isValidSymbol(symbol: String): Boolean {
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

  @Test
  @Throws(Exception::class)
  fun isValidSymbolTest() {

    // valid symbols
    assertEquals(true, isValidSymbol("1"))
    assertEquals(true, isValidSymbol("AAPL"))
    assertEquals(true, isValidSymbol("^12"))
    assertEquals(true, isValidSymbol("MSFT200814C00165000"))
    assertEquals(true, isValidSymbol("a*b"))
    assertEquals(true, isValidSymbol("ab*"))
    assertEquals(true, isValidSymbol("a:b"))
    assertEquals(true, isValidSymbol("A=B"))
    assertEquals(true, isValidSymbol("A-B"))
    assertEquals(true, isValidSymbol(".^*:A=-"))

    // invalid symbols
    assertEquals(false, isValidSymbol("."))
    assertEquals(false, isValidSymbol(".^*:=-"))
    assertEquals(false, isValidSymbol("a)"))
    assertEquals(false, isValidSymbol(""))
    assertEquals(false, isValidSymbol("    "))
    assertEquals(false, isValidSymbol("ABC**"))
    assertEquals(false, isValidSymbol("\u0001"))
    assertEquals(false, isValidSymbol("(a)"))
    assertEquals(false, isValidSymbol("@symbol"))
    assertEquals(false, isValidSymbol("a#"))
  }

  @Test
  @Throws(Exception::class)
  fun importTextTest() {
    val text: String = "abc,abc,,\"def\";AZN\nline\ttab\rret space,non_A-Z,\t亜,toLongToBeAStock"

    val symbols = text.split("[ ,;\r\n\t]".toRegex())

    // only a-z from 1..7 chars in length
    val symbolList: List<String> = symbols.map { symbol ->
      symbol.replace("\"", "")
          .toUpperCase(Locale.ROOT)
    }
        .filter { symbol ->
          symbol.matches("[A-Z]{1,7}".toRegex())
        }
        .distinct()

    assertEquals(symbolList.size, 7)
  }

  private fun csvStrToDouble(str: String): Double {
    val s = str.replace("$", "").replace(",", "")
    var value: Double
    try {
      value = s.toDouble()
      if (value == 0.0) {
        val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
        value = numberFormat.parse(s)!!
            .toDouble()
      }
    } catch (e: Exception) {
      value = 0.0
    }

    return value
  }

  @Test
  @Throws(Exception::class)
  fun importCSVtest() {
    val csvData: String =
      "\"Account Name/Number\",\"Symbol\",\"Description\",\"Quantity\",\"Last Price\",\"Last Price Change\",\"Current Value\",\"Today's Gain/Loss Dollar\",\"Today's Gain/Loss Percent\",\"Total Gain/Loss Dollar\",\"Total Gain/Loss Percent\",\"Cost Basis Per Share\",\"Cost Basis Total\",\"Type\",\"Dividend Quantity\",\"Grant ID\",\" Grant Price\",\"Gain Per TSRU\",\"Offering Period Ends\",\"Offering Period Begins\",\"Total Balances\"\n" +
          "\"X1\",\"FCASH**\",\"Cash\",70.120,\$1.00,\$0.00,\$70.12,n/a,n/a,n/a,n/a,n/a,n/a,\"Cash\",\"n/a\",\"n/a\",\"n/a\",\"n/a\",\"n/a\",\"n/a\",\"n/a\"\n" +
          "\"X1\",\"ABBV\",\"ABBVIE INC COM USD0.01\",35.000,\$98.560,+\$0.86,\$34496.00,+\$301.00,+0.88%,+\$5764.52,+20.06%,\$82.09,\$28731.48,\"Cash\",\"n/a\",\"n/a\",\"n/a\",\"n/a\",\"n/a\",\"n/a\",\"n/a\"\n" +
          "\"X1\",\"ACRX\",\"ACELRX PHARMACEUTICALS INC COM\",200.000,\$1.145,+\$0.085,\$2290.00,+\$170.00,+8.02%,-\$125.60,-5.20%,\$1.21,\$2415.60,\"Cash\",\"n/a\",\"n/a\",\"n/a\",\"n/a\",\"n/a\",\"n/a\",\"n/a\"\n" +
          "\"X1\",\"TEST\",\"test\",0,\$1.145,+\$0.085,\$2290.00,+\$170.00,+8.02%,-\$125.60,-5.20%,\$0,\$2415.60,\"Cash\",\"n/a\",\"n/a\",\"n/a\",\"n/a\",\"n/a\",\"n/a\",\"n/a\"\n" +
          "\"X1\",\"TEST\",\"test\",0,\$1.145,+\$0.085,\$2290.00,+\$170.00,+8.02%,-\$125.60,-5.20%,\$0,\$2415.60,\"Cash\",\"n/a\",\"n/a\",\"n/a\",\"n/a\",\"n/a\",\"n/a\",\"n/a\"\n" +
          "\"X1\",\"1-TEST\",\"test\",0,\$1.145,+\$0.085,\$2290.00,+\$170.00,+8.02%,-\$125.60,-5.20%,\$0,\$2415.60,\"Cash\",\"n/a\",\"n/a\",\"n/a\",\"n/a\",\"n/a\",\"n/a\",\"n/a\"\n" +
          "\n" +
          "Brokerage services are provided by Fidelity Brokerage Services LLC, 900 Salem Street, Smithfield, RI 02917. Custody and other services provided by National Financial Services LLC. Both are Fidelity Investment companies and members SIPC, NYSE.\n"

    val reader = csvReader {
      skipEmptyLine = true
      skipMissMatchedRow = true
    }
    val rows: List<List<String>> = reader.readAll(csvData)
    assertEquals(rows.size, 7)

    val headerRow = rows.first()
    val symbolColumn = headerRow.indexOfFirst {
      it.compareTo(other = "Symbol", ignoreCase = true) == 0
    }
    assertNotEquals(symbolColumn, -1)

    val sharesColumn = headerRow.indexOfFirst {
      it.compareTo(other = "Quantity", ignoreCase = true) == 0
    }
    assertNotEquals(sharesColumn, -1)

    val priceColumn = headerRow.indexOfFirst {
      it.compareTo(other = "Cost Basis Per Share", ignoreCase = true) == 0
    }
    assertNotEquals(priceColumn, -1)

    val assetItems = HashMap<String, List<Asset>>()

    rows.drop(1)
        .forEach { row ->
          val symbol = row[symbolColumn].toUpperCase()
          val amount = csvStrToDouble(row[sharesColumn])
          val price = csvStrToDouble(row[priceColumn])

          if (symbol.isNotEmpty()) {
            val asset = Asset(
                symbol = symbol,
                quantity = amount,
                price = price
            )

            if (assetItems.containsKey(symbol)) {
              val list = assetItems[symbol]!!.toMutableList()
              list.add(asset)
              assetItems[symbol] = list
            } else {
              assetItems[symbol] = listOf(asset)
            }
          }
        }

    // only a-z from 1..7 chars in length
    val assetList: Map<String, List<Asset>> = assetItems.filter { map ->
      map.key.matches("[A-Z]{1,7}".toRegex())
    }

    assertEquals(assetItems.size, 5)
    assertEquals(assetList.size, 3)
  }

  /*
data class StockItem
(
var onlineMarketData: OnlineMarketData,
var stockDBdata: StockDBdata,
var assets: List<Asset>,
var events: List<Event>
)


[
  {
    "annualDividendRate": 4.5,
    "annualDividendYield": 0.04551431,
    "change": -0.1800003,
    "changeInPercent": -0.17914042,
    "currency": "USD",
    "isPostMarket": true,
    "lastTradePrice": 100.3,
    "name": "AbbVie Inc.",
    "position": {
      "holdings": [
        {
          "id": 5,
          "price": 82.09,
          "shares": 350.0,
          "symbol": "ABBV"
        }
      ],
      "symbol": "ABBV"
    },
    "stockExchange": "NYQ",
    "symbol": "ABBV"
  },
  {
    "annualDividendRate": 0.0,
    "annualDividendYield": 0.0,
    "change": -0.010099411,
    "changeInPercent": -0.107899696,
    "currency": "USD",
    "isPostMarket": true,
    "lastTradePrice": 9.3499,
    "name": "Dynavax Technologies Corporatio",
    "position": {
      "holdings": [
        {
          "id": 209,
          "price": 5.17,
          "shares": 3600.0002,
          "symbol": "DVAX"
        }
      ],
      "symbol": "DVAX"
    },
    "properties": {
      "alertAbove": 0.0,
      "alertBelow": 8.0,
      "notes": "Verkauft 3000@9,24 am 26.6.2020",
      "symbol": "DVAX"
    },
    "stockExchange": "NMS",
    "symbol": "DVAX"
  },
  {

 */

  data class StockItemJson
  (
    var symbol: String,
    val portfolio: String,
    val data: String?,
    val groupColor: Int?,
    val groupName: String?,
    val note: String?,
    var dividendNote: String?,
    val annualDividendRate: Double?,
    val alertAbove: Double?,
    val alertAboveNote: String?,
    val alertBelow: Double?,
    val alertBelowNote: String?,
    var assets: List<AssetJson>?,
    var events: List<EventJson>?,
    var dividends: List<DividendJson>?
  )

  data class AssetJson(
    var quantity: Double,
    val price: Double,
    val type: Int?,
    var note: String?,
    var date: Long?,
    var sharesPerQuantity: Int?,
    var expirationDate: Long?,
    var premium: Double?,
    var commission: Double?
  )

  data class EventJson(
    val title: String,
    val datetime: Long,
    val note: String?,
    val type: Int?
  )

  data class DividendJson(
    var amount: Double,
    val cycle: Int,
    val paydate: Long,
    val exdate: Long?,
    val type: Int?,
    val note: String?
  )

  @Test
  @Throws(Exception::class)
  fun writeJSON() {
    val stockItems: MutableList<StockItem> = mutableListOf()

    stockItems.add(
        StockItem(
            OnlineMarketData(symbol = "s1"),
            StockDBdata(
                symbol = "s1", groupColor = 123, alertAbove = 11.0, alertBelow = 12.0,
                note = "note1"
            ),
            listOf(Asset(symbol = "s1", quantity = 1.0, price = 2.0)),
            listOf(Event(symbol = "s1", type = 1, title = "ti1", note = "te1", datetime = 1L)),
            listOf(
                Dividend(
                    symbol = "s1", amount = 0.0, type = 0, cycle = 0, exdate = 0L, paydate = 0L
                )
            )
        )
    )
    stockItems.add(
        StockItem(
            OnlineMarketData(symbol = "s2"),
            StockDBdata(
                symbol = "s2", groupColor = 223, alertAbove = 21.0, alertBelow = 22.0,
                note = "note2"
            ),
            listOf(Asset(symbol = "s2", quantity = 3.0, price = 4.0)),
            listOf(Event(symbol = "s2", type = 2, title = "ti2", note = "te2", datetime = 2L)),
            listOf(
                Dividend(
                    symbol = "s1", amount = 0.0, type = 0, cycle = 0, exdate = 0L, paydate = 0L
                )
            )
        )
    )

    val stockItemsJson = stockItems.map { stockItem ->
      StockItemJson(symbol = stockItem.stockDBdata.symbol,
          groupColor = stockItem.stockDBdata.groupColor,
          groupName = "a",
          portfolio = stockItem.stockDBdata.portfolio,
          data = stockItem.stockDBdata.data,
          alertAbove = stockItem.stockDBdata.alertAbove,
          alertAboveNote = stockItem.stockDBdata.alertAboveNote,
          alertBelow = stockItem.stockDBdata.alertBelow,
          alertBelowNote = stockItem.stockDBdata.alertBelowNote,
          note = stockItem.stockDBdata.note,
          dividendNote = stockItem.stockDBdata.dividendNote,
          annualDividendRate = stockItem.stockDBdata.annualDividendRate,
          assets = stockItem.assets.map { asset ->
            AssetJson(quantity = asset.quantity,
                price = asset.price,
                note = asset.note,
                date = asset.date,
                type = asset.type,
                sharesPerQuantity = asset.sharesPerQuantity,
                expirationDate = asset.expirationDate,
                premium = asset.premium,
                commission = asset.commission
            )
          },
          events = stockItem.events.map { event ->
            EventJson(
                type = event.type, title = event.title, note = event.note, datetime = event.datetime
            )
          },
          dividends = stockItem.dividends.map { dividend ->
            DividendJson(
                amount = validateDouble(dividend.amount),
                exdate = dividend.exdate,
                paydate = dividend.paydate,
                type = dividend.type,
                cycle = dividend.cycle,
                note = ""
            )
          }
      )
    }

    val jsonString = Gson().toJson(stockItemsJson)

    /*
[{
    "alertAbove": 11.0,
    "alertBelow": 12.0,
    "assets": [{
        "price": 2.0,
        "shares": 1.0
    }],
    "events": [{
        "datetime": 1,
        "type": "t1"
    }],
    "groupColor": 123,
    "groupName": "a",
    "notes": "notes1",
    "symbol": "s1"
}, {
    "alertAbove": 21.0,
    "alertBelow": 22.0,
    "assets": [{
        "price": 4.0,
        "shares": 3.0
    }],
    "events": [{
        "datetime": 2,
        "type": "t2"
    }],
    "groupColor": 223,
    "groupName": "a",
    "notes": "notes2",
    "symbol": "s2"
}]
     */
    assertEquals(true, jsonString.isNotEmpty())
  }
}
