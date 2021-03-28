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

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.thecloudsite.stockroom.databinding.FragmentTimelineBinding
import com.thecloudsite.stockroom.timeline.TimeLineRecyclerView
import com.thecloudsite.stockroom.timeline.callback.SectionCallback
import com.thecloudsite.stockroom.timeline.model.SectionInfo
import com.thecloudsite.stockroom.utils.getAssetsCapitalGain
import com.thecloudsite.stockroom.utils.getCapitalGainLossText

// https://androidexample365.com/stickytimeline-is-timeline-view-for-android/

data class GainLoss2(
  var gain: Double = 0.0,
  var loss: Double = 0.0,
  var stockList: MutableList<GainLossStockItem> = mutableListOf()
)

data class GainLossStockItem(
  var date: Long = 0L,
  var symbol: String = "",
  var text: SpannableStringBuilder = SpannableStringBuilder()
)

class GainLossTimelineFragment : Fragment() {

  private var _binding: FragmentTimelineBinding? = null

  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  private lateinit var stockRoomViewModel: StockRoomViewModel

  companion object {
    fun newInstance() = GainLossTimelineFragment()
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
    val gainLossTimelineAdapter = GainLossTimelineAdapter(requireContext())

    recyclerView.adapter = gainLossTimelineAdapter

    // use requireActivity() instead of this to have only one shared viewmodel
    stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)

    stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { items ->
      items?.let { stockItems ->

        var capitalGain = 0.0
        var capitalLoss = 0.0
        val capitalGainLossMap: MutableMap<Int, GainLoss2> = mutableMapOf()

        stockItems.forEach { stockItem ->
          val (gain, loss, gainLossMap) = getAssetsCapitalGain(stockItem.assets)
          // Merge gain and loss of the individual stock to one gain/loss to prevent
          // having individual loss/gain reported in the summary.
          val capitalGainLoss = gain - loss

          // Add the gain/loss of the stock
          val capitalGainLossText = getCapitalGainLossText(
            requireContext(),
            gain,
            loss,
            0.0,
            "-",
            "\n"
          )

          when {
            capitalGainLoss > 0.0 -> {
              capitalGain += capitalGainLoss
            }
            capitalGainLoss < 0.0 -> {
              capitalLoss += -capitalGainLoss
            }
            else -> {
            }
          }

          gainLossMap.forEach { (year, map) ->
            if (!capitalGainLossMap.containsKey(year)) {
              capitalGainLossMap[year] = GainLoss2()
            }
            val gainloss = map.gain - map.loss
            if (gainloss >= 0.0) {
              capitalGainLossMap[year]?.gain = capitalGainLossMap[year]?.gain!! + gainloss
            } else {
              capitalGainLossMap[year]?.loss = capitalGainLossMap[year]?.loss!! - gainloss
            }

            capitalGainLossMap[year]?.stockList?.add(
              GainLossStockItem(
                date = map.lastTransactionDate,
                symbol = stockItem.stockDBdata.symbol,
                text = capitalGainLossText
              )
            )
          }
        }

        val gainLossList: MutableList<GainLossTimelineElement> = mutableListOf()

        capitalGainLossMap.toSortedMap()
          .forEach { (year, map) ->
            val text = getCapitalGainLossText(requireContext(), map.gain, map.loss, 0.0, "-", "\n")
            gainLossList.add(
              GainLossTimelineElement(
                date = "$year",
                totalGainLoss = text,
                map.stockList
              )
            )
          }

        gainLossTimelineAdapter.updateData(gainLossList)

        for (i in 0 until recyclerView.itemDecorationCount) {
          recyclerView.removeItemDecorationAt(0)
        }

        recyclerView.addItemDecoration(getSectionCallback(gainLossList))
      }
    })
  }

  private fun getSectionCallback(timelineElementList: List<GainLossTimelineElement>): SectionCallback {
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
