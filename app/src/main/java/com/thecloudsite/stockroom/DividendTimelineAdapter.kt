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

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.database.Dividend
import com.thecloudsite.stockroom.utils.dividendCycleStr
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

data class DividendTimelineElement(
  val date: String,
  val symbol: String,
  val dividends: List<Dividend>,
)

class DividendTimelineAdapter(
  private val context: Context
) : RecyclerView.Adapter<DividendTimelineAdapter.ViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var timelineElementList: List<DividendTimelineElement> = listOf()

  class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val header: TextView = view.findViewById<View>(R.id.timeline_header) as TextView
    val details: TextView = view.findViewById<View>(R.id.timeline_details) as TextView
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {
    val itemView = inflater.inflate(R.layout.timeline_dividend_item, parent, false)
    return ViewHolder(itemView)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {
    val timelineElement = timelineElementList[position]

    holder.header.text = timelineElement.symbol

    var dividends = ""
    var skipFirstline = true

    timelineElement.dividends.sortedBy { dividend ->
      dividend.paydate
    }
        .forEach { dividend ->
          val localDateTime = LocalDateTime.ofEpochSecond(dividend.paydate, 0, ZoneOffset.UTC)
          val timeStr = localDateTime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))

          if (!skipFirstline) {
            dividends += "\n"
          } else {
            skipFirstline = false
          }

          dividends += context.getString(
              R.string.timeline_dividend,
              timeStr,
              dividendCycleStr(dividend.cycle, context),
              DecimalFormat("0.00").format(dividend.amount),
              dividend.symbol
          )
        }

    holder.details.text = dividends
  }

  fun updateData(timeline: List<DividendTimelineElement>) {
    timelineElementList = timeline
    notifyDataSetChanged()
  }

  override fun getItemCount(): Int = timelineElementList.size
}
