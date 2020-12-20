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
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.database.Event
import com.thecloudsite.stockroom.databinding.AssetviewItemBinding
import com.thecloudsite.stockroom.databinding.TimelineEventItemBinding
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.SHORT

data class EventTimelineElement(
  val date: String,
  val symbol: String,
  val events: List<Event>,
)

class EventTimelineAdapter(
  private val context: Context
) : RecyclerView.Adapter<EventTimelineAdapter.ViewHolder>() {

  private lateinit var binding: TimelineEventItemBinding
  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var timelineElementList: List<EventTimelineElement> = listOf()

  class ViewHolder(
    val binding: TimelineEventItemBinding
  ) : RecyclerView.ViewHolder(binding.root) {
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): ViewHolder {

    binding = TimelineEventItemBinding.inflate(inflater, parent, false)
    return ViewHolder(binding)
  }

  override fun onBindViewHolder(
    holder: ViewHolder,
    position: Int
  ) {
    val timelineElement = timelineElementList[position]

    holder.binding.timelineHeader.text = timelineElement.symbol

    var events = ""
    var skipFirstline = true

    timelineElement.events.sortedBy { event ->
      event.datetime
    }
        .forEach { event ->
          val localDateTime = LocalDateTime.ofEpochSecond(event.datetime, 0, ZoneOffset.UTC)
          val timeStr = localDateTime.format(DateTimeFormatter.ofLocalizedTime(SHORT))

          if (!skipFirstline) {
            events += "\n"
          } else {
            skipFirstline = false
          }

          events += context.getString(
              R.string.timeline_event,
              timeStr,
              event.title,
              event.symbol
          )
        }

    holder.binding.timelineDetails.text = events
  }

  fun updateData(timeline: List<EventTimelineElement>) {
    timelineElementList = timeline
    notifyDataSetChanged()
  }

  override fun getItemCount(): Int = timelineElementList.size
}
