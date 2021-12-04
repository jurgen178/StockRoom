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
class SymbollistTest {

    fun IdToName(symbol: String): String {
        return symbol.replace(Regex("(\\w+)(\\w{3})"), "$1-$2")
    }

    @Test
    @Throws(Exception::class)
    fun IdToNameTest() {
        assertEquals("abc-usd", IdToName("abcusd"))
        assertEquals("btc-usd", IdToName("btcusd"))
        assertEquals("btcg-usd", IdToName("btcgusd"))
        assertEquals("btc-dai", IdToName("btcdai"))
        assertEquals("btc-gbp", IdToName("btcgbp"))
        assertEquals("btc-eur", IdToName("btceur"))
        assertEquals("btc-sgd", IdToName("btcsgd"))
        assertEquals("eth-btc", IdToName("ethbtc"))
    }
}
