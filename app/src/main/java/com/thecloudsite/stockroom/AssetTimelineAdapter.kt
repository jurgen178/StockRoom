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
import android.view.ViewGroup
import android.widget.TextView
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.databinding.TimelineAssetItemBinding
import com.thecloudsite.stockroom.utils.DecimalFormat0To4Digits
import com.thecloudsite.stockroom.utils.DecimalFormat2Digits
import com.thecloudsite.stockroom.utils.DecimalFormat2To4Digits
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

  class ViewHolder(
    val binding: TimelineAssetItemBinding
  ) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
      timelineElement: AssetTimelineElement,
      clickListener: (AssetTimelineElement) -> Unit
    ) {
      binding.timelineCardView.setOnClickListener { clickListener(timelineElement) }
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {

    val binding = TimelineAssetItemBinding.inflate(inflater, parent, false)
    return ViewHolder(binding)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {
    val timelineElement = timelineElementList[position]

    holder.bind(timelineElement, clickListenerCardItem)
    holder.binding.timelineHeader.text = timelineElement.symbol

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
                DecimalFormat(DecimalFormat0To4Digits).format(asset.quantity),
                DecimalFormat(DecimalFormat2To4Digits).format(asset.price),
                DecimalFormat(DecimalFormat2Digits).format(asset.quantity * asset.price)
            )
          } else {
            context.getString(
                R.string.timeline_asset_sold,
                date,
                DecimalFormat(DecimalFormat0To4Digits).format(-asset.quantity),
                DecimalFormat(DecimalFormat2To4Digits).format(asset.price),
                DecimalFormat(DecimalFormat2Digits).format(-asset.quantity * asset.price)
            )
          }
        }

    holder.binding.timelineDetails.text = stockTransactions
  }

  fun updateData(timeline: List<AssetTimelineElement>) {
    timelineElementList = timeline
    notifyDataSetChanged()
  }

  override fun getItemCount(): Int = timelineElementList.size
}
