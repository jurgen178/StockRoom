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
import com.thecloudsite.stockroom.database.Dividend
import com.thecloudsite.stockroom.databinding.TimelineDividendItemBinding
import com.thecloudsite.stockroom.utils.DecimalFormat2Digits
import com.thecloudsite.stockroom.utils.dividendCycleStr
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
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

  class ViewHolder(
    val binding: TimelineDividendItemBinding
  ) : RecyclerView.ViewHolder(binding.root) {
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {

    val binding = TimelineDividendItemBinding.inflate(inflater, parent, false)
    return ViewHolder(binding)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {
    val timelineElement = timelineElementList[position]

    holder.binding.timelineHeader.text = timelineElement.symbol

    var dividends = ""
    var skipFirstline = true

    timelineElement.dividends.sortedBy { dividend ->
      dividend.paydate
    }
        .forEach { dividend ->
          val localDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(dividend.paydate), ZoneOffset.systemDefault())
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
              DecimalFormat(DecimalFormat2Digits).format(dividend.amount),
              dividend.symbol
          )
        }

    holder.binding.timelineDetails.text = dividends
  }

  fun updateData(timeline: List<DividendTimelineElement>) {
    timelineElementList = timeline
    notifyDataSetChanged()
  }

  override fun getItemCount(): Int = timelineElementList.size
}
