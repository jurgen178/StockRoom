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

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.databinding.FragmentTimelineBinding
import com.thecloudsite.stockroom.timeline.TimeLineRecyclerView
import com.thecloudsite.stockroom.timeline.callback.SectionCallback
import com.thecloudsite.stockroom.timeline.model.SectionInfo
import com.thecloudsite.stockroom.utils.getSymbolDisplayName
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

// https://androidexample365.com/stickytimeline-is-timeline-view-for-android/

open class AssetBaseTimelineFragment : Fragment() {

    private var _binding: FragmentTimelineBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    lateinit var stockRoomViewModel: StockRoomViewModel
    private lateinit var assetTimelineAdapter: AssetTimelineAdapter
    lateinit var recyclerView: TimeLineRecyclerView

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

        recyclerView = binding.timelineRecyclerView

        // Currently only LinearLayoutManager is supported.
        recyclerView.layoutManager = LinearLayoutManager(
            requireContext(),
            LinearLayoutManager.VERTICAL,
            false
        )

        // Set Adapter
        val clickListenerCardItem =
            { timelineElement: AssetTimelineElement -> clickListenerCardItem(timelineElement) }
        assetTimelineAdapter = AssetTimelineAdapter(
            requireContext(),
            clickListenerCardItem
        )

        recyclerView.adapter = assetTimelineAdapter

        // use requireActivity() instead of this to have only one shared viewmodel
        stockRoomViewModel =
            ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)
    }

    fun updateAssets(assets: List<Asset>) {
        val hashMap: HashMap<String, HashMap<TimelineHeader, MutableList<Asset>>> =
            hashMapOf()
        val unknownDate = getString(R.string.timeline_unknown_date)

        // map the list of assets to date map that maps to a symbol map with each matching asset
        assets.forEach { asset ->
            val date = if (asset.date > 0) {
                val localDateTime = ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(asset.date),
                    ZoneOffset.systemDefault()
                )
                val yearMonth: YearMonth = YearMonth.from(localDateTime)
                yearMonth.format(DateTimeFormatter.ofPattern("u.MM"))
            } else {
                unknownDate
            }

            if (hashMap[date] == null) {
                hashMap[date] = hashMapOf()
            }

            val header = TimelineHeader(
                symbol = asset.symbol,
                name = getSymbolDisplayName(asset.symbol)
            )

            if (hashMap[date]?.get(header) == null) {
                hashMap[date]?.set(header, mutableListOf())
            }

            hashMap[date]?.get(header)
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
                    .forEach { (header, list) ->
                        assetList.add(
                            AssetTimelineElement(
                                date,
                                header,
                                list
                            )
                        )
                    }
            }

        assetTimelineAdapter.updateData(assetList)

        for (i in 0 until recyclerView.itemDecorationCount) {
            recyclerView.removeItemDecorationAt(0)
        }

        recyclerView.addItemDecoration(getSectionCallback(assetList))
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
        intent.putExtra(EXTRA_SYMBOL, timelineElement.header.symbol)
        //stockRoomViewModel.runOnlineTaskNow()
        startActivity(intent)
    }
}
