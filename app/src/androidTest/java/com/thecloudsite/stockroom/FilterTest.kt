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

package com.thecloudsite.stockroom

import android.text.SpannableStringBuilder
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.database.Dividend
import com.thecloudsite.stockroom.database.Event
import com.thecloudsite.stockroom.database.StockDBdata
import com.thecloudsite.stockroom.utils.epsilon
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.IGNORE_CASE

@RunWith(AndroidJUnit4::class)
class FilterTest {

  class FilterTest1Type : IFilterType {
    override fun filter(stockItem: StockItem): Boolean {
      return stockItem.stockDBdata.symbol == "s1"
    }

    override fun dataReady() {
    }

    override val typeId = FilterTypeEnum.FilterNullType
    override val displayName = "test1"
    override val desc = ""
    override val dataType = FilterDataTypeEnum.NoType
    override val subTypeList: List<FilterSubTypeEnum> = listOf()
    override var subType: FilterSubTypeEnum = FilterSubTypeEnum.NoType
    override val selectionList = listOf<SpannableStringBuilder>()
    override var data = ""
    override val serializedData = ""
    override val displayData: SpannableStringBuilder = SpannableStringBuilder()
  }

  @Test
  @Throws(Exception::class)
  fun filterTest1() {
    val filter1 = FilterTest1Type()

    val stockItem1 = StockItem(
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

    assertEquals(true, filter1.filter(stockItem1))
    assertEquals("test1", filter1.displayName)
  }

  @Test
  @Throws(Exception::class)
  fun filterRegexMatch() {
    // match char in second line
    val regexOption = setOf(IGNORE_CASE, DOT_MATCHES_ALL)
    val regexStr = "b"
    val match = (regexStr.toRegex(regexOption)).containsMatchIn("a\nb")
    assertEquals(true, match)
  }

  @Test
  @Throws(Exception::class)
  fun filterStringMatch() {

    // empty
    val d1 = getLevenshteinDistance("", "")
    assertEquals(0.0, d1, epsilon)

    // identical
    val d2 = getLevenshteinDistance("a", "a")
    assertEquals(0.0, d2, epsilon)

    // totally different
    val d3 = getLevenshteinDistance("a", "bb")
    assertEquals(1.0, d3, epsilon)

    // 3 out of 4 chars different
    val d4 = getLevenshteinDistance("abcd", "aaaa")
    assertEquals(0.75, d4, epsilon)

    // 1 out of 4 chars different
    val d5 = getLevenshteinDistance("abcd", "abcc")
    assertEquals(0.25, d5, epsilon)

    // 1 out of 4 chars different with different casing
    // expect same result as d5
    val d6 = getLevenshteinDistance("ABCD", "abcc")
    assertEquals(0.25, d6, epsilon)
  }
}
