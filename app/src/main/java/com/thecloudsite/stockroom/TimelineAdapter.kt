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
import com.thecloudsite.stockroom.database.StockDBdata
import kotlinx.android.synthetic.main.timeline_item.view.timelineCardView
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class TimelineElement(
  val date: String,
  val symbol: String,
  val assets: List<Asset>,
)

class TimelineAdapter(
  private val context: Context,
  private val clickListenerCardItem: (TimelineElement) -> Unit
) : RecyclerView.Adapter<TimelineAdapter.ViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var timelineElementList: List<TimelineElement> = listOf()

  class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    fun bind(
      timelineElement: TimelineElement,
      clickListener: (TimelineElement) -> Unit
    ) {
      itemView.timelineCardView.setOnClickListener { clickListener(timelineElement) }
    }

    val header: TextView = view.findViewById<View>(R.id.timeline_header) as TextView
    val details: TextView = view.findViewById<View>(R.id.timeline_details) as TextView
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {
    val itemView = inflater.inflate(R.layout.timeline_item, parent, false)
    return ViewHolder(itemView)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {
    val timelineElement = timelineElementList[position]

    holder.bind(timelineElement, clickListenerCardItem)
    holder.header.text = timelineElement.symbol

    var stockTransactions = ""
    var skipFirstline = true

    timelineElement.assets.sortedBy { asset ->
      asset.date
    }
        .forEach { asset ->
          val date = if (asset.date > 0) {
            val localDateTime = LocalDateTime.ofEpochSecond(asset.date, 0, ZoneOffset.UTC)
            localDateTime.format(DateTimeFormatter.ofPattern("d"))
          } else {
            "-"
          }

          if (!skipFirstline) {
            stockTransactions += "\n"
          } else {
            skipFirstline = false
          }

          stockTransactions += if (asset.shares > 0.0) {
            context.getString(
                R.string.timeline_bought,
                DecimalFormat("0.####").format(asset.shares),
                DecimalFormat("0.00##").format(asset.price),
                DecimalFormat("0.00").format(asset.shares * asset.price),
                date
            )
          } else {
            context.getString(
                R.string.timeline_sold,
                DecimalFormat("0.####").format(-asset.shares),
                DecimalFormat("0.00##").format(asset.price),
                DecimalFormat("0.00").format(-asset.shares * asset.price),
                date
            )
          }
        }

    holder.details.text = stockTransactions
  }

  fun updateData(timeline: List<TimelineElement>) {
    timelineElementList = timeline
    notifyDataSetChanged()
  }

  override fun getItemCount(): Int = timelineElementList.size
}
