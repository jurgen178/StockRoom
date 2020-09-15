/*
 * Copyright (C) 2017 Google Inc.
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
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.eventview_item.view.textViewEventDelete
import kotlinx.android.synthetic.main.eventview_item.view.textViewEventItemsLayout
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

// https://codelabs.developers.google.com/codelabs/kotlin-android-training-diffutil-databinding/#4

class EventListAdapter internal constructor(
  private val context: Context,
  private val clickListenerUpdate: (Event) -> Unit,
  private val clickListenerDelete: (Event) -> Unit
) : RecyclerView.Adapter<EventListAdapter.EventViewHolder>() {
  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var eventList = mutableListOf<Event>()

  class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bindUpdate(
      event: Event,
      clickListenerUpdate: (Event) -> Unit
    ) {
      itemView.textViewEventItemsLayout.setOnClickListener { clickListenerUpdate(event) }
    }

    fun bindDelete(
      event: Event,
      clickListenerDelete: (Event) -> Unit
    ) {
      itemView.textViewEventDelete.setOnClickListener { clickListenerDelete(event) }
    }

    val textViewEventTitle: TextView = itemView.findViewById(R.id.textViewEventTitle)
    val textViewEventNote: TextView = itemView.findViewById(R.id.textViewEventNote)
    val textViewEventDateTime: TextView = itemView.findViewById(R.id.textViewEventDateTime)
    val textViewEventDelete: TextView = itemView.findViewById(R.id.textViewEventDelete)
    val textViewEventLayout: ConstraintLayout = itemView.findViewById(R.id.textViewEventLayout)
    val textViewEventItemsLayout: LinearLayout =
      itemView.findViewById(R.id.textViewEventItemsLayout)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): EventViewHolder {
    val itemView = inflater.inflate(R.layout.eventview_item, parent, false)
    return EventViewHolder(itemView)
  }

  override fun onBindViewHolder(
    holder: EventViewHolder,
    position: Int
  ) {
    val current: Event = eventList[position]

    // First entry is headline.
    if (position == 0) {
      holder.textViewEventTitle.text = context.getString(R.string.event_title)
      holder.textViewEventNote.text = context.getString(R.string.event_note)
      holder.textViewEventDateTime.text = context.getString(R.string.event_datetime)
      holder.textViewEventDelete.visibility = View.GONE
      holder.textViewEventLayout.setBackgroundColor(context.getColor(R.color.backgroundListColor))

      val background = TypedValue()
      holder.textViewEventItemsLayout.setBackgroundResource(background.resourceId)
    } else {
      holder.bindUpdate(current, clickListenerUpdate)
      holder.bindDelete(current, clickListenerDelete)

      holder.textViewEventTitle.text = current.title
      holder.textViewEventNote.text = current.note
      val datetime: LocalDateTime = LocalDateTime.ofEpochSecond(current.datetime, 0, ZoneOffset.UTC)
      holder.textViewEventDateTime.text =
        "${datetime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
        }\n${datetime.format(DateTimeFormatter.ofLocalizedTime(MEDIUM))
        }"

      val background = TypedValue()
      context.theme.resolveAttribute(android.R.attr.selectableItemBackground, background, true)
      holder.textViewEventItemsLayout.setBackgroundResource(background.resourceId)
    }
  }

  internal fun updateEvents(events: List<Event>) {
    // Headline placeholder
    eventList = mutableListOf(Event(symbol = "", type = 0, title = "", note = "", datetime = 0L))
    eventList.addAll(events)

    notifyDataSetChanged()
  }

  override fun getItemCount() = eventList.size
}
