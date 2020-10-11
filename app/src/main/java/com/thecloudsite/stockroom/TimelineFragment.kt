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
import com.thecloudsite.stockroom.database.Asset
import xyz.sangcomz.stickytimelineview.RecyclerSectionItemDecoration
import xyz.sangcomz.stickytimelineview.TimeLineRecyclerView
import xyz.sangcomz.stickytimelineview.model.SectionInfo
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// https://androidexample365.com/stickytimeline-is-timeline-view-for-android/
class TimelineFragment : Fragment() {

  private lateinit var stockRoomViewModel: StockRoomViewModel

  companion object {
    fun newInstance() = TimelineFragment()
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(false)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {

    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_timeline, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    val recyclerView: TimeLineRecyclerView = view.findViewById(R.id.recycler_view)

    // Currently only LinearLayoutManager is supported.
    recyclerView.layoutManager = LinearLayoutManager(
        requireContext(),
        LinearLayoutManager.VERTICAL,
        false
    )

    // use requireActivity() instead of this to have only one shared viewmodel
    stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)

    stockRoomViewModel.allAssetTable.observe(viewLifecycleOwner, Observer { assets ->

      val hashMap: HashMap<String, HashMap<String, MutableList<Asset>>> = hashMapOf()
      val unknownDate = getString(R.string.timeline_unknown_date)

      assets.forEach { asset ->
        val date = if (asset.date > 0) {
          val localDateTime = LocalDateTime.ofEpochSecond(asset.date, 0, ZoneOffset.UTC)
          val yearMonth: YearMonth = YearMonth.from(localDateTime)
          yearMonth.format(DateTimeFormatter.ofPattern("u.MM"))
        } else {
          unknownDate
        }

        if (hashMap[date] == null) {
          hashMap[date] = hashMapOf()
        }

        if (hashMap[date]?.get(asset.symbol) == null) {
          hashMap[date]?.set(asset.symbol, mutableListOf())
        }

        hashMap[date]?.get(asset.symbol)
            ?.add(asset)
      }

      val assetList: MutableList<TimelineElement> = mutableListOf()

      hashMap.toSortedMap()
          .forEach { (date, symbolMap) ->
            symbolMap.forEach { (symbol, list) ->
              assetList.add(TimelineElement(date, symbol, list))
            }
          }

      recyclerView.addItemDecoration(getSectionCallback(assetList))

      //Set Adapter
      recyclerView.adapter = TimelineAdapter(
          requireContext(),
          layoutInflater,
          assetList,
          R.layout.timeline_item
      )

      stockRoomViewModel.allAssetTable.removeObservers(viewLifecycleOwner)
    })
  }

  private fun getSectionCallback(timelineElementList: List<TimelineElement>): RecyclerSectionItemDecoration.SectionCallback {
    return object : RecyclerSectionItemDecoration.SectionCallback {
      //In your data, implement a method to determine if this is a section.
      override fun isSection(position: Int): Boolean =
        timelineElementList[position].date != timelineElementList[position - 1].date

      //Implement a method that returns a SectionHeader.
      override fun getSectionHeader(position: Int): SectionInfo? =
        SectionInfo(timelineElementList[position].date, "")
    }
  }
}
