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
import com.thecloudsite.stockroom.database.Dividend
import com.thecloudsite.stockroom.database.Event
import com.thecloudsite.stockroom.database.StockDBdata
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FilterTest {

  class FilterTest1Type : IFilterType {
    override fun filter(stockItem: StockItem): Boolean {
      return stockItem.stockDBdata.symbol == "s1"
    }

    override val typeId = FilterTypeEnum.FilterTestType
    override val displayName = "test1"
    override val dataType = FilterDataTypeEnum.NoType
    override var data = ""
    override var desc = ""
    override var date = 0L
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
}
