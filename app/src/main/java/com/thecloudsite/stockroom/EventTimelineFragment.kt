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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.thecloudsite.stockroom.database.Event
import com.thecloudsite.stockroom.databinding.FragmentTimelineBinding
import xyz.sangcomz.stickytimelineview.TimeLineRecyclerView
import xyz.sangcomz.stickytimelineview.callback.SectionCallback
import xyz.sangcomz.stickytimelineview.model.SectionInfo
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.FULL

// https://androidexample365.com/stickytimeline-is-timeline-view-for-android/
// Bug in xyz.sangcomz.stickytimelineview.TimeLineRecyclerView
// updating the data shifts the cardview to the right

class EventTimelineFragment : Fragment() {

  private var _binding: FragmentTimelineBinding? = null

  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  private lateinit var stockRoomViewModel: StockRoomViewModel

  companion object {
    fun newInstance() = EventTimelineFragment()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {

    // Inflate the layout for this fragment
    _binding = FragmentTimelineBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    val recyclerView: TimeLineRecyclerView = binding.timelineRecyclerView

    // Currently only LinearLayoutManager is supported.
    recyclerView.layoutManager = LinearLayoutManager(
        requireContext(),
        LinearLayoutManager.VERTICAL,
        false
    )

    // Set Adapter
    val eventTimelineAdapter = EventTimelineAdapter(requireContext())

    recyclerView.adapter = eventTimelineAdapter

    // use requireActivity() instead of this to have only one shared viewmodel
    stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)

    stockRoomViewModel.allEventTable.observe(viewLifecycleOwner, Observer { events ->
      if (events != null) {
        val hashMap: HashMap<String, HashMap<String, MutableList<Event>>> = hashMapOf()

        events.forEach { event ->
          if (event.datetime > 0) {

            val localDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(event.datetime), ZoneOffset.systemDefault())
            val yearMonth: YearMonth = YearMonth.from(localDateTime)
            val dateYM = yearMonth.format(DateTimeFormatter.ofPattern("u.MM"))
            val dateFull = localDateTime.format(DateTimeFormatter.ofLocalizedDate(FULL))

            if (hashMap[dateYM] == null) {
              hashMap[dateYM] = hashMapOf()
            }

            if (hashMap[dateYM]?.get(dateFull) == null) {
              hashMap[dateYM]?.set(dateFull, mutableListOf())
            }

            hashMap[dateYM]?.get(dateFull)
                ?.add(event)
          }
        }

        val eventList: MutableList<EventTimelineElement> = mutableListOf()

        // Copy the new structured data-date map to timeline elements.
        hashMap.toSortedMap()
            .forEach { (date, symbolMap) ->
              // sort by first date entry in the event list
              symbolMap.toList()
                  .sortedBy {
                    if (it.second.isNotEmpty()) {
                      // sort the date list
                      it.second.minByOrNull { event ->
                        event.datetime
                      }!!.datetime
                    } else {
                      0
                    }
                  }
                  .forEach { (eventdate, list) ->
                    eventList.add(EventTimelineElement(date, eventdate, list))
                  }
            }

        eventTimelineAdapter.updateData(eventList)
        recyclerView.addItemDecoration(getSectionCallback(eventList))
      }
    })
  }

  private fun getSectionCallback(timelineElementList: List<EventTimelineElement>): SectionCallback {
    return object : SectionCallback {
      // In your data, implement a method to determine if this is a section.
      override fun isSection(position: Int): Boolean =
        if (position > 0 && position < timelineElementList.size) {
          timelineElementList[position].date != timelineElementList[position - 1].date
        } else {
          false
        }

      // Implement a method that returns a SectionHeader.
      override fun getSectionHeader(position: Int): SectionInfo? =
        if (position >= 0 && position < timelineElementList.size) {
          SectionInfo(timelineElementList[position].date, "")
        } else {
          null
        }
    }
  }
}
