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

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.databinding.TimelineAssetItemBinding
import com.thecloudsite.stockroom.utils.DecimalFormatQuantityDigits
import com.thecloudsite.stockroom.utils.DecimalFormat2Digits
import com.thecloudsite.stockroom.utils.DecimalFormat2To4Digits
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

data class AssetTimelineElement(
  val date: String,
  val symbol: String,
  val name: String,
  val assets: List<Asset>,
)

class AssetTimelineAdapter(
  private val context: Context,
  private val clickListenerCardItemLambda: (AssetTimelineElement) -> Unit
) : RecyclerView.Adapter<AssetTimelineAdapter.ViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var timelineElementList: List<AssetTimelineElement> = listOf()

  class ViewHolder(
    val binding: TimelineAssetItemBinding
  ) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
      timelineElement: AssetTimelineElement,
      clickListenerLambda: (AssetTimelineElement) -> Unit
    ) {
      binding.timelineCardView.setOnClickListener { clickListenerLambda(timelineElement) }
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

    holder.bind(timelineElement, clickListenerCardItemLambda)
    holder.binding.timelineHeader.text = timelineElement.name

    var stockTransactions = ""
    var skipFirstline = true

    timelineElement.assets.sortedBy { asset ->
      asset.date
    }
      .forEach { asset ->
        val date = if (asset.date > 0) {
          val localDateTime =
            ZonedDateTime.ofInstant(Instant.ofEpochSecond(asset.date), ZoneOffset.systemDefault())
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
          var price = DecimalFormat(DecimalFormat2To4Digits).format(asset.price)
          if (asset.fee > 0.0) {
            price += "+${DecimalFormat(DecimalFormat2To4Digits).format(asset.fee)}"
          }
          context.getString(
            R.string.timeline_asset_bought,
            date,
            DecimalFormat(DecimalFormatQuantityDigits).format(asset.quantity),
            price,
            DecimalFormat(DecimalFormat2Digits).format(asset.quantity * asset.price + asset.fee)
          )
        } else {
          var price = DecimalFormat(DecimalFormat2To4Digits).format(asset.price)
          if (asset.fee > 0.0) {
            price += "-${DecimalFormat(DecimalFormat2To4Digits).format(asset.fee)}"
          }
          context.getString(
            R.string.timeline_asset_sold,
            date,
            DecimalFormat(DecimalFormatQuantityDigits).format(-asset.quantity),
            price,
            DecimalFormat(DecimalFormat2Digits).format(-asset.quantity * asset.price - asset.fee)
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
