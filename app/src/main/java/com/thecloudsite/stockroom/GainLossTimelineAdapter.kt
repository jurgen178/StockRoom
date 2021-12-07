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
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.scale
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.databinding.TimelineGainlossItemBinding
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

data class GainLossTimelineElement(
  val date: String,
  val totalGainLoss: SpannableStringBuilder,
  val stockItemGainLossList: MutableList<GainLossStockItem>,
)

class GainLossTimelineAdapter(
  private val context: Context
) : RecyclerView.Adapter<GainLossTimelineAdapter.ViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var timelineElementList: List<GainLossTimelineElement> = listOf()

  class ViewHolder(
    val binding: TimelineGainlossItemBinding
  ) : RecyclerView.ViewHolder(binding.root) {
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {

    val binding = TimelineGainlossItemBinding.inflate(inflater, parent, false)
    return ViewHolder(binding)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {
    val timelineElement = timelineElementList[position]

    holder.binding.timelineHeader.text = timelineElement.totalGainLoss

    val gainlossStr = SpannableStringBuilder()

    if (timelineElement.stockItemGainLossList.size > 3) {
      gainlossStr.append("${context.getString(R.string.summary_stocks)} ")
        .bold { append("${timelineElement.stockItemGainLossList.size}\n\n") }
    }

    timelineElement.stockItemGainLossList.sortedBy { gainloss ->
      gainloss.date
    }.forEach { gainloss ->

      gainlossStr.bold { append(gainloss.name) }
      gainlossStr.append(" ")

      if (gainloss.date > 0L) {
        val datetime: ZonedDateTime =
          ZonedDateTime.ofInstant(Instant.ofEpochSecond(gainloss.date), ZoneOffset.systemDefault())
        val dateMedium = datetime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))

        gainlossStr.scale(0.8f) {
          append("(")
            .append(dateMedium)
            .append(") ")
        }
      }

      gainlossStr.append(gainloss.text)
    }

    holder.binding.timelineDetails.text = gainlossStr
  }

  fun updateData(timeline: List<GainLossTimelineElement>) {
    timelineElementList = timeline
    notifyDataSetChanged()
  }

  override fun getItemCount(): Int = timelineElementList.size
}
