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
import com.thecloudsite.stockroom.database.Event
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

data class EventTimelineElement(
  val date: String,
  val symbol: String,
  val events: List<Event>,
)

class EventTimelineAdapter(
  private val context: Context
) : RecyclerView.Adapter<EventTimelineAdapter.ViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var timelineElementList: List<EventTimelineElement> = listOf()

  class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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

    holder.header.text = timelineElement.symbol

    var events = ""
    var skipFirstline = true

    timelineElement.events.sortedBy { event ->
      event.datetime
    }
        .forEach { event ->
          if (event.datetime > 0) {
            val localDateTime = LocalDateTime.ofEpochSecond(event.datetime, 0, ZoneOffset.UTC)
            val timeStr = localDateTime.format(DateTimeFormatter.ofLocalizedTime(MEDIUM))

            if (!skipFirstline) {
              events += "\n"
            } else {
              skipFirstline = false
            }

            events += context.getString(
                R.string.timeline_event,
                event.title,
                event.symbol,
                timeStr
            )
          }
        }

    holder.details.text = events
  }

  fun updateData(timeline: List<EventTimelineElement>) {
    timelineElementList = timeline
    notifyDataSetChanged()
  }

  override fun getItemCount(): Int = timelineElementList.size
}
