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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thecloudsite.stockroom.utils.frac
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.sqrt

@RunWith(AndroidJUnit4::class)
class CalcTest {

  @Test
  @Throws(Exception::class)
  fun codeParse() {

    var code =
      "\nover - swap //* comment */100 * \n// comment1\n/* comment */\n\"∆% \"\n// comment2"

    // Remove comments
    code = code
      .replace("/[*].*?[*]/".toRegex(), " ")
      .replace("//.*?(\n|$)".toRegex(), " ")

    // Split by spaces not followed by even amount of quotes. Spaces need to be outside of quotes.
    val symbols1 = code.split("\\s+(?=([^\"']*[\"'][^\"']*[\"'])*[^\"']*$)".toRegex())
    val symbols2 =
      code.split("\\s(?=(?:[^\"'\\\\]*(?:\\\\.|[\"'](?:[^\"'\\\\]*\\\\.)*[^\"'\\\\]*[\"']))*[^\"']*$)".toRegex())

    assertEquals(9, symbols1.size)
    assertEquals("\"∆% \"", symbols1[7])
    assertEquals(14, symbols2.size)
    assertEquals("\"∆% \"", symbols2[11])

    val isComment = symbols1[7].matches("[\"'](.*?)[\"']".toRegex())
    val match = "[\"'](.*?)[\"']".toRegex()
      .matchEntire(symbols1[7])

    var sym = ""
    if (match != null && match.groups.size == 2) {
      sym = match.groups[1]?.value.toString()
    }
    assertEquals("∆% ", sym)
  }

  @Test
  @Throws(Exception::class)
  fun fracTest() {

    assertEquals(Pair(5, 19), frac(5.0 / 19.0))
    assertEquals(Pair(104348, 33215), frac(3.14159265359))
    assertEquals(Pair(37, 61), frac(0.606557377049))
    assertEquals(Pair(2, 1), frac(2.0))
    assertEquals(Pair(1, 3), frac(0.33333333))

    assertEquals(Pair(0, 1), frac(0.0))

    assertEquals(Pair(-2, 1), frac(-2.0))
    assertEquals(Pair(-5, 19), frac(-5.0 / 19.0))
    assertEquals(Pair(-104348, 33215), frac(-3.14159265359))
    assertEquals(Pair(-37, 61), frac(-0.606557377049))
    assertEquals(Pair(-1, 3), frac(-0.33333333))

    // null result

    // infinity
    assertEquals(Pair(null, 0), frac(1 / 0.0))
    // NaN
    assertEquals(Pair(null, 0), frac(sqrt(-1.0)))
    // large number
    assertEquals(Pair(null, 0), frac(100000000000000.0))
  }
}
