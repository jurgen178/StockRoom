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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.thecloudsite.stockroom.databinding.FragmentTimelineBinding
import com.thecloudsite.stockroom.timeline.TimeLineRecyclerView
import com.thecloudsite.stockroom.timeline.callback.SectionCallback
import com.thecloudsite.stockroom.timeline.model.SectionInfo
import com.thecloudsite.stockroom.utils.epsilon
import com.thecloudsite.stockroom.utils.getAssetsCapitalGain
import com.thecloudsite.stockroom.utils.getCapitalGainLossText
import kotlin.math.absoluteValue

// https://androidexample365.com/stickytimeline-is-timeline-view-for-android/

data class GainLoss2(
    var gain: Double = 0.0,
    var loss: Double = 0.0,
    var stockList: MutableList<GainLossStockItem> = mutableListOf()
)

data class GainLossStockItem(
    var date: Long = 0L,
    var symbol: String = "",
    var name: String = "",
    var text: SpannableStringBuilder = SpannableStringBuilder()
)

open class GainLossBaseTimelineFragment : Fragment() {

    private var _binding: FragmentTimelineBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    lateinit var stockRoomViewModel: StockRoomViewModel
    private lateinit var gainLossTimelineAdapter: GainLossTimelineAdapter
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
        gainLossTimelineAdapter = GainLossTimelineAdapter(requireContext())

        recyclerView.adapter = gainLossTimelineAdapter

        // use requireActivity() instead of this to have only one shared viewmodel
        stockRoomViewModel =
            ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)
    }

    fun updateList(stockItemsList: List<StockItem>) {
        val capitalGainLossMap: MutableMap<Int, MutableMap<String, GainLoss2>> = mutableMapOf()

        // Add Summary for all accounts.
        updateMap(stockItemsList, "", capitalGainLossMap)

        // Add Summary for each Portfolio.
        val portfolioMap: java.util.HashSet<String> = hashSetOf()

        stockItemsList.forEach { stockItem ->
            if (stockItem.assets.isNotEmpty()) {
                portfolioMap.add(stockItem.stockDBdata.portfolio)
            }
        }

        val assetsPortfolio =
            portfolioMap.map { portfolio ->
                portfolio
            }

        if (assetsPortfolio.size > 1) {
            assetsPortfolio.sorted().forEach { portfolio ->

                // Deep copy of the list because content gets removed.
                var stockItemsListCopy = stockItemsList.map { it.copy() }

                // Filter for stockitems matching the portfolio.
                stockItemsListCopy = stockItemsListCopy.filter { stockItem ->
                    stockItem.stockDBdata.portfolio == portfolio && stockItem.assets.isNotEmpty()
                }

                val portfolioStr = if (portfolio.isEmpty()) {
                    getString(R.string.standard_portfolio)
                } else {
                    getString(R.string.portfolio_overview_headline, portfolio)
                }

                updateMap(stockItemsListCopy, portfolioStr, capitalGainLossMap)
            }
        }

        // Add Summary for each Account.
        val accountMap: java.util.HashSet<String> = hashSetOf()

        stockItemsList.forEach { stockItem ->
            stockItem.assets.forEach { asset ->
                accountMap.add(asset.account)
            }
        }

        val assetsAccounts =
            accountMap.map { account ->
                account
            }

        if (assetsAccounts.size > 1) {
            assetsAccounts.sorted().forEach { account ->

                // Deep copy of the list because content gets removed.
                var stockItemsListCopy = stockItemsList.map { it.copy() }

                // Filter for stockitems matching the account.
                stockItemsListCopy.forEach { stockItem ->
                    stockItem.assets = stockItem.assets.filter { asset ->
                        asset.account == account
                    }
                }

                stockItemsListCopy = stockItemsListCopy.filter { stockItem ->
                    stockItem.assets.isNotEmpty()
                }

                val accountStr = if (account.isEmpty()) {
                    getString(R.string.standard_account)
                } else {
                    getString(R.string.account_overview_headline, account)
                }

                updateMap(stockItemsListCopy, accountStr, capitalGainLossMap)
            }
        }

        // Timeline data.
        val gainLossList: MutableList<GainLossTimelineElement> = mutableListOf()

        // Map gainloss to timeline data.
        capitalGainLossMap.toSortedMap()
            .forEach { (year, map) ->
                // If map.size == 2 other accounts did not contribute.
                // The map contains only all item and standard account items, which are the same.
                val map1 = if (map.size == 2 && map.containsKey("")) {
                    mapOf<String, GainLoss2>(Pair("", map[""]!!)) // return only one entry
                } else {
                    map
                }

                map1.forEach { (headline, gainloss) ->

                    // Get gain/loss for the header.
                    val text = getCapitalGainLossText(
                        requireContext(),
                        gainloss.gain,
                        gainloss.loss,
                        0.0,
                        "-",
                        "\n"
                    )

                    // Add item with header (summary gain/loss) and stock list with each individual gain/loss.
                    gainLossList.add(
                        GainLossTimelineElement(
                            date = "$year $headline",
                            totalGainLoss = text,
                            gainloss.stockList
                        )
                    )
                }
            }

        gainLossTimelineAdapter.updateData(gainLossList)

        for (i in 0 until recyclerView.itemDecorationCount) {
            recyclerView.removeItemDecorationAt(0)
        }

        recyclerView.addItemDecoration(getSectionCallback(gainLossList))
    }

    private fun updateMap(
        stockItems: List<StockItem>,
        headline: String,
        capitalGainLossMap: MutableMap<Int, MutableMap<String, GainLoss2>>
    ) {
        var capitalGain = 0.0
        var capitalLoss = 0.0

        stockItems.forEach { stockItem ->
            val (gain, loss, gainLossMap) = getAssetsCapitalGain(stockItem.assets)
            // Merge gain and loss of the individual stock to one gain/loss to prevent
            // having individual loss/gain reported in the summary.
            val capitalGainLoss = gain - loss

            if (capitalGainLoss.absoluteValue >= epsilon) {
                if (capitalGainLoss > 0.0) {
                    capitalGain += capitalGainLoss
                } else {
                    capitalLoss += -capitalGainLoss
                }

                gainLossMap.forEach { (year, map) ->

                    if (!capitalGainLossMap.containsKey(year)) {
                        capitalGainLossMap[year] = mutableMapOf()
                    }

                    if (capitalGainLossMap[year]?.containsKey(headline) == false) {
                        capitalGainLossMap[year]?.put(headline, GainLoss2())
                    }

                    val gainloss = map.gain - map.loss

                    if (gainloss >= 0.0) {
                        capitalGainLossMap[year]?.get(headline)?.gain =
                            capitalGainLossMap[year]?.get(headline)?.gain!! + gainloss
                    } else {
                        capitalGainLossMap[year]?.get(headline)?.loss =
                            capitalGainLossMap[year]?.get(headline)?.loss!! - gainloss
                    }

                    // Add the gain/loss of the stock per year
                    val capitalGainLossText = getCapitalGainLossText(
                        requireContext(),
                        map.gain,
                        map.loss,
                        0.0,
                        "-",
                        "\n"
                    )

                    capitalGainLossMap[year]?.get(headline)?.stockList?.add(
                        GainLossStockItem(
                            date = map.lastTransactionDate,
                            symbol = stockItem.stockDBdata.symbol,
                            name = stockItem.stockDBdata.name.ifEmpty { stockItem.stockDBdata.symbol },
                            text = capitalGainLossText
                        )
                    )
                }
            }
        }
    }
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
