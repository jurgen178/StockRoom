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
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.thecloudsite.stockroom.database.Asset
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

data class TimelineElement(
  val date: String,
  val header: String,
  val assets: List<Asset>,
)

class TimelineAdapter(
  private val context: Context,
  private val layoutInflater: LayoutInflater,
  private val timelineElementList: List<TimelineElement>,
  @param:LayoutRes private val rowLayout: Int
) : RecyclerView.Adapter<TimelineAdapter.ViewHolder>() {

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {
    val v = layoutInflater.inflate(
        rowLayout,
        parent,
        false
    )
    return ViewHolder(v)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {
    val timelineElement = timelineElementList[position]

    holder.header.text = timelineElement.header

    var stockTransactions = ""
    var skipFirstline = true

    timelineElement.assets.sortedBy { asset ->
      asset.date
    }
        .forEach { asset ->
          val date = if (asset.date > 0) {
            val localDateTime = LocalDateTime.ofEpochSecond(asset.date, 0, ZoneOffset.UTC)
            localDateTime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
          } else {
            ""
          }

          if (!skipFirstline) {
            stockTransactions += "\n"
          } else {
            skipFirstline = false
          }

          stockTransactions += if (asset.shares > 0.0) {
            context.getString(
                R.string.timeline_bought,
                date,
                DecimalFormat("0.####").format(asset.shares),
                DecimalFormat("0.00##").format(asset.price),
                DecimalFormat("0.00").format(asset.shares * asset.price)
            )
          } else {
            context.getString(
                R.string.timeline_sold,
                date,
                DecimalFormat("0.####").format(-asset.shares),
                DecimalFormat("0.00##").format(asset.price),
                DecimalFormat("0.00").format(-asset.shares * asset.price)
            )
          }
        }

    holder.details.text = stockTransactions
  }

  override fun getItemCount(): Int = timelineElementList.size

  class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val header: TextView = view.findViewById<View>(R.id.timeline_header) as TextView
    val details: TextView = view.findViewById<View>(R.id.timeline_details) as TextView
  }
}
