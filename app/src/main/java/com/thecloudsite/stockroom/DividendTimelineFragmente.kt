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
import com.thecloudsite.stockroom.database.Dividend
import com.thecloudsite.stockroom.utils.dividendCycleStr
import xyz.sangcomz.stickytimelineview.TimeLineRecyclerView
import xyz.sangcomz.stickytimelineview.callback.SectionCallback
import xyz.sangcomz.stickytimelineview.model.SectionInfo
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// https://androidexample365.com/stickytimeline-is-timeline-view-for-android/
// Bug in xyz.sangcomz.stickytimelineview.TimeLineRecyclerView
// updating the data shifts the cardview to the right

class DividendTimelineFragment : Fragment() {

  private lateinit var stockRoomViewModel: StockRoomViewModel

  companion object {
    fun newInstance() = DividendTimelineFragment()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {

    return inflater.inflate(R.layout.fragment_timeline, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    val recyclerView: TimeLineRecyclerView = view.findViewById(R.id.timeline_recycler_view)

    // Currently only LinearLayoutManager is supported.
    recyclerView.layoutManager = LinearLayoutManager(
        requireContext(),
        LinearLayoutManager.VERTICAL,
        false
    )

    // Set Adapter
    val dividendTimelineAdapter = DividendTimelineAdapter(requireContext())

    recyclerView.adapter = dividendTimelineAdapter

    // use requireActivity() instead of this to have only one shared viewmodel
    stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)

    stockRoomViewModel.allDividendTable.observe(viewLifecycleOwner, Observer { dividends ->
      if (dividends != null) {
        val hashMap: HashMap<String, HashMap<String, MutableList<Dividend>>> = hashMapOf()

        dividends.forEach { dividend ->
          if (dividend.type == DividendType.Received.value && dividend.paydate > 0) {

            val localDateTime = LocalDateTime.ofEpochSecond(dividend.paydate, 0, ZoneOffset.UTC)
            val yearMonth: YearMonth = YearMonth.from(localDateTime)
            val dateYM = yearMonth.format(DateTimeFormatter.ofPattern("u.MM"))
            val dateYMlong = context?.getString(
                R.string.timeline_dividend_headline,
                localDateTime.format(DateTimeFormatter.ofPattern("MMMM u"))
            )!!

            if (hashMap[dateYM] == null) {
              hashMap[dateYM] = hashMapOf()
            }

            if (hashMap[dateYM]?.get(dateYMlong) == null) {
              hashMap[dateYM]?.set(dateYMlong, mutableListOf())
            }

            hashMap[dateYM]?.get(dateYMlong)
                ?.add(dividend)
          }
        }

        val dividendList: MutableList<DividendTimelineElement> = mutableListOf()

        // Copy the new structured data-date map to timeline elements.
        hashMap.toSortedMap()
            .forEach { (date, symbolMap) ->
              // sort by first date entry in the dividend list
              val dividendlist = symbolMap.toList()
                  .sortedBy {
                    if (it.second.isNotEmpty()) {
                      // sort the date list
                      it.second.minByOrNull { dividend ->
                        dividend.paydate
                      }!!.paydate
                    } else {
                      0
                    }
                  }

              // get dividend total of the month
              val dividendTotal =
                DecimalFormat("0.00").format(dividendlist.sumByDouble { (dividenddate, list) ->
                  list.sumByDouble { dividend ->
                    if (dividend.type == DividendType.Received.value) {
                      dividend.amount
                    } else {
                      0.0
                    }
                  }
                })

              // add dividend total to the date header for each item
              dividendlist
                  .forEach { (dividenddate, list) ->
                    dividendList.add(
                        DividendTimelineElement(date, "$dividenddate ($dividendTotal)", list)
                    )
                  }
            }

        dividendTimelineAdapter.updateData(dividendList)
        recyclerView.addItemDecoration(getSectionCallback(dividendList))
      }
    })
  }

  private fun getSectionCallback(timelineElementList: List<DividendTimelineElement>): SectionCallback {
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
