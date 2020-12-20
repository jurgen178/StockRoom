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
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.database.Event
import com.thecloudsite.stockroom.databinding.EventviewItemBinding
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM
import java.time.format.FormatStyle.SHORT

// https://codelabs.developers.google.com/codelabs/kotlin-android-training-diffutil-databinding/#4

class EventListAdapter internal constructor(
  private val context: Context,
  private val clickListenerUpdate: (Event) -> Unit,
  private val clickListenerDelete: (Event) -> Unit
) : RecyclerView.Adapter<EventListAdapter.EventViewHolder>() {

  private lateinit var binding: EventviewItemBinding
  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var eventList = mutableListOf<Event>()

  class EventViewHolder(
    val binding: EventviewItemBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bindUpdate(
      event: Event,
      clickListenerUpdate: (Event) -> Unit
    ) {
      binding.textViewEventItemsLayout.setOnClickListener { clickListenerUpdate(event) }
    }

    fun bindDelete(
      event: Event,
      clickListenerDelete: (Event) -> Unit
    ) {
      binding.textViewEventDelete.setOnClickListener { clickListenerDelete(event) }
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): EventViewHolder {

    binding = EventviewItemBinding.inflate(inflater, parent, false)
    return EventViewHolder(binding)
  }

  override fun onBindViewHolder(
    holder: EventViewHolder,
    position: Int
  ) {
    val current: Event = eventList[position]

    // First entry is headline.
    if (position == 0) {
      holder.binding.textViewEventTitle.text = context.getString(R.string.event_title)
      holder.binding.textViewEventNote.text = context.getString(R.string.event_note)
      holder.binding.textViewEventDateTime.text = context.getString(R.string.event_datetime)
      holder.binding.textViewEventDelete.visibility = View.GONE
      holder.binding.textViewEventLayout.setBackgroundColor(context.getColor(R.color.backgroundListColor))

      val background = TypedValue()
      holder.binding.textViewEventItemsLayout.setBackgroundResource(background.resourceId)
    } else {
      holder.bindUpdate(current, clickListenerUpdate)
      holder.bindDelete(current, clickListenerDelete)

      holder.binding.textViewEventTitle.text = current.title
      holder.binding.textViewEventNote.text = current.note
      val datetime: LocalDateTime = LocalDateTime.ofEpochSecond(current.datetime, 0, ZoneOffset.UTC)
      holder.binding.textViewEventDateTime.text =
        "${datetime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
        }\n${datetime.format(DateTimeFormatter.ofLocalizedTime(SHORT))
        }"

      val background = TypedValue()
      context.theme.resolveAttribute(android.R.attr.selectableItemBackground, background, true)
      holder.binding.textViewEventItemsLayout.setBackgroundResource(background.resourceId)
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
