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
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.thecloudsite.stockroom.database.Asset
import kotlinx.android.synthetic.main.timeline_asset_item.view.timelineCardView
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

data class AssetTimelineElement(
  val date: String,
  val symbol: String,
  val assets: List<Asset>,
)

class AssetTimelineAdapter(
  private val context: Context,
  private val clickListenerCardItem: (AssetTimelineElement) -> Unit
) : RecyclerView.Adapter<AssetTimelineAdapter.ViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var timelineElementList: List<AssetTimelineElement> = listOf()

  class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    fun bind(
      timelineElement: AssetTimelineElement,
      clickListener: (AssetTimelineElement) -> Unit
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
    val itemView = inflater.inflate(R.layout.timeline_asset_item, parent, false)
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

          stockTransactions += if (asset.quantity > 0.0) {
            context.getString(
                R.string.timeline_asset_bought,
                date,
                DecimalFormat("0.####").format(asset.quantity),
                DecimalFormat("0.00##").format(asset.price),
                DecimalFormat("0.00").format(asset.quantity * asset.price)
            )
          } else {
            context.getString(
                R.string.timeline_asset_sold,
                date,
                DecimalFormat("0.####").format(-asset.quantity),
                DecimalFormat("0.00##").format(asset.price),
                DecimalFormat("0.00").format(-asset.quantity * asset.price)
            )
          }
        }

    holder.details.text = stockTransactions
  }

  fun updateData(timeline: List<AssetTimelineElement>) {
    timelineElementList = timeline
    notifyDataSetChanged()
  }

  override fun getItemCount(): Int = timelineElementList.size
}
