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
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.IGNORE_CASE

@RunWith(AndroidJUnit4::class)
class CalcTest {

  // https://begriffs.com/pdf/dec2frac.pdf
  fun frac(x: Double): Pair<Int, Int> {
    val eps = 0.00000001
    var z = x
    var n = x.toInt()
    var d0: Int = 0
    var d1: Int = 1
    var x0 = 1.0
    var x1 = 0.0

    while ((z - z.toInt().toDouble()) > eps && (x0 - x1).absoluteValue > eps) {
      z = 1 / (z - z.toInt().toDouble())
      val d = d1 * z.toInt() + d0
      n = (x * d).roundToInt()

      x0 = x1;
      x1 = n.toDouble() / d.toDouble()

      d0 = d1;
      d1 = d;
    }

    return Pair(n, d1)
  }

  @Test
  @Throws(Exception::class)
  fun fracTest() {

    assertEquals(Pair(5, 19), frac(5.0 / 19.0))
    assertEquals(Pair(2, 1), frac(3.14159265359))
    assertEquals(Pair(37, 61), frac(0.606557377049))
    assertEquals(Pair(2, 1), frac(2.0))
  }
}
