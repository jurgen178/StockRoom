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
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.database.Event
import com.thecloudsite.stockroom.databinding.EventviewItemBinding
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM
import java.time.format.FormatStyle.SHORT

// https://codelabs.developers.google.com/codelabs/kotlin-android-training-diffutil-databinding/#4

class EventListAdapter internal constructor(
  private val context: Context,
  private val clickListenerUpdateLambda: (Event) -> Unit,
  private val clickListenerDeleteLambda: (Event) -> Unit
) : RecyclerView.Adapter<EventListAdapter.EventViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var eventList = mutableListOf<Event>()

  class EventViewHolder(
    val binding: EventviewItemBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bindUpdate(
      event: Event,
      clickListenerUpdateLambda: (Event) -> Unit
    ) {
      binding.textViewEventItemsLayout.setOnClickListener { clickListenerUpdateLambda(event) }
    }

    fun bindDelete(
      event: Event,
      clickListenerDeleteLambda: (Event) -> Unit
    ) {
      binding.textViewEventDelete.setOnClickListener { clickListenerDeleteLambda(event) }
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): EventViewHolder {

    val binding = EventviewItemBinding.inflate(inflater, parent, false)
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
      holder.bindUpdate(current, clickListenerUpdateLambda)
      holder.bindDelete(current, clickListenerDeleteLambda)

      holder.binding.textViewEventTitle.text = current.title
      holder.binding.textViewEventNote.text = current.note
      val datetime: ZonedDateTime =
        ZonedDateTime.ofInstant(Instant.ofEpochSecond(current.datetime), ZoneOffset.systemDefault())
      holder.binding.textViewEventDateTime.text =
        "${
          datetime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
        }\n${
          datetime.format(DateTimeFormatter.ofLocalizedTime(SHORT))
        }"

      val background = TypedValue()
      context.theme.resolveAttribute(android.R.attr.selectableItemBackground, background, true)
      holder.binding.textViewEventItemsLayout.setBackgroundResource(background.resourceId)
    }
  }

  internal fun updateEvents(events: List<Event>) {

    // Headline
    eventList = mutableListOf(Event(symbol = "", type = 0, title = "", note = "", datetime = 0L))

    // Event items
    eventList.addAll(events.sortedBy { event ->
      event.datetime
    })

    notifyDataSetChanged()
  }

  override fun getItemCount() = eventList.size
}
