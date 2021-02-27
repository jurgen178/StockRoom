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
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.thecloudsite.stockroom.R
import com.thecloudsite.stockroom.R.layout
import com.thecloudsite.stockroom.StockDataEntry
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TextMarkerViewCandleChart(
  context: Context,
  private val dateTimeFormatter: DateTimeFormatter,
  private val stockDataEntries: List<StockDataEntry>?
) : MarkerView(context, layout.text_marker_layout) {

  private var textmarker = findViewById<TextView>(R.id.textmarker)
  private val offsetPoint by lazy {
    MPPointF(-(width / 2).toFloat(), -height.toFloat())
  }

  override fun refreshContent(
    entry: Entry?,
    highlight: Highlight?
  ) {
    if (entry is CandleEntry && stockDataEntries != null && stockDataEntries.isNotEmpty()) {
      val index: Int = entry.x.toInt()
      if (index >= 0 && index < stockDataEntries.size) {
        val date =
          ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(stockDataEntries[index].dateTimePoint),
            ZoneOffset.systemDefault()
          )
            .format(dateTimeFormatter)

        if (entry.high != entry.low) {
          textmarker.text = "${DecimalFormat(DecimalFormat2To4Digits).format(entry.low)}-${
            DecimalFormat(DecimalFormat2To4Digits).format(
              entry.high
            )
          }\n$date"
        } else {
          textmarker.text = "${DecimalFormat(DecimalFormat2To4Digits).format(entry.high)}\n$date"
        }

      } else {
        textmarker.text = ""
      }
    }

    super.refreshContent(entry, highlight)
  }

  override fun getOffset(): MPPointF = offsetPoint
}

class TextMarkerViewLineChart(
  context: Context,
  private val dateTimeFormatter: DateTimeFormatter,
  private val stockDataEntries: List<StockDataEntry>?
) : MarkerView(context, layout.text_marker_layout) {

  private var textmarker = findViewById<TextView>(R.id.textmarker)
  private val offsetPoint by lazy {
    MPPointF(-(width / 2).toFloat(), -height.toFloat())
  }

  override fun refreshContent(
    entry: Entry?,
    highlight: Highlight?
  ) {
    if (entry is Entry && stockDataEntries != null && stockDataEntries.isNotEmpty()) {
      val index: Int = entry.x.toInt()
      if (index >= 0 && index < stockDataEntries.size) {
        val date =
          ZonedDateTime.ofInstant(
            Instant.ofEpochSecond(stockDataEntries[index].dateTimePoint),
            ZoneOffset.systemDefault()
          )
            .format(dateTimeFormatter)

        textmarker.text = "${DecimalFormat(DecimalFormat2To4Digits).format(entry.y)}\n$date"

      } else {
        textmarker.text = ""
      }
    }

    super.refreshContent(entry, highlight)
  }

  override fun getOffset(): MPPointF = offsetPoint
}

