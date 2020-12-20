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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.databinding.FragmentTimelineBinding
import xyz.sangcomz.stickytimelineview.TimeLineRecyclerView
import xyz.sangcomz.stickytimelineview.callback.SectionCallback
import xyz.sangcomz.stickytimelineview.model.SectionInfo
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// https://androidexample365.com/stickytimeline-is-timeline-view-for-android/
// Bug in xyz.sangcomz.stickytimelineview.TimeLineRecyclerView
// updating the data shifts the cardview to the right

class AssetTimelineFragment : Fragment() {

  private var _binding: FragmentTimelineBinding? = null

  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  private lateinit var stockRoomViewModel: StockRoomViewModel

  companion object {
    fun newInstance() = AssetTimelineFragment()
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

    val recyclerView: TimeLineRecyclerView = view.findViewById(R.id.timeline_recycler_view)

    // Currently only LinearLayoutManager is supported.
    recyclerView.layoutManager = LinearLayoutManager(
        requireContext(),
        LinearLayoutManager.VERTICAL,
        false
    )

    // Set Adapter
    val clickListenerCardItem =
      { timelineElement: AssetTimelineElement -> clickListenerCardItem(timelineElement) }
    val assetTimelineAdapter = AssetTimelineAdapter(
        requireContext(),
        clickListenerCardItem
    )

    recyclerView.adapter = assetTimelineAdapter

    // use requireActivity() instead of this to have only one shared viewmodel
    stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)

    stockRoomViewModel.allAssetTable.observe(viewLifecycleOwner, Observer { assets ->
      if (assets != null) {
        val hashMap: HashMap<String, HashMap<String, MutableList<Asset>>> = hashMapOf()
        val unknownDate = getString(R.string.timeline_unknown_date)

        // map the list of assets to date map that maps to a symbol map with each matching asset
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

        val assetList: MutableList<AssetTimelineElement> = mutableListOf()

        // Copy the new structured data-symbol map to timeline elements.
        hashMap.toSortedMap()
            .forEach { (date, symbolMap) ->
              // sort by first date entry in the asset list
              symbolMap.toList()
                  .sortedBy {
                    if (it.second.isNotEmpty()) {
                      // sort the date list
                      it.second.minByOrNull { asset ->
                        asset.date
                      }!!.date
                    } else {
                      0
                    }
                  }
                  .forEach { (symbol, list) ->
                    assetList.add(AssetTimelineElement(date, symbol, list))
                  }
            }

        assetTimelineAdapter.updateData(assetList)
        recyclerView.addItemDecoration(getSectionCallback(assetList))
      }
    })
  }

  private fun getSectionCallback(timelineElementList: List<AssetTimelineElement>): SectionCallback {
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

  private fun clickListenerCardItem(timelineElement: AssetTimelineElement) {
    val intent = Intent(context, StockDataActivity::class.java)
    intent.putExtra("symbol", timelineElement.symbol)
    //stockRoomViewModel.runOnlineTaskNow()
    startActivity(intent)
  }
}
