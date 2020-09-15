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
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_news.newsRecyclerview
import kotlinx.android.synthetic.main.fragment_news.swipeRefreshLayout
import java.util.Locale

class NewsFragment : Fragment() {

  private lateinit var stockRoomViewModel: StockRoomViewModel
  private lateinit var yahooNewsViewModel: YahooNewsViewModel
  private lateinit var googleNewsViewModel: GoogleNewsViewModel
  private lateinit var newsAdapter: NewsAdapter

  companion object {
    fun newInstance() = NewsFragment()
  }

  private var symbol: String = ""
  private var yahooNewsQuery = ""
  private var googleNewsQuery = ""

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {

    symbol = (arguments?.getString("symbol") ?: "").toUpperCase(Locale.ROOT)
    yahooNewsQuery = symbol
    googleNewsQuery = symbol

    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_news, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    newsAdapter = NewsAdapter(requireContext())
    newsRecyclerview.adapter = newsAdapter
    newsRecyclerview.layoutManager = LinearLayoutManager(requireContext())

    yahooNewsViewModel = ViewModelProvider(this).get(YahooNewsViewModel::class.java)
    googleNewsViewModel = ViewModelProvider(this).get(GoogleNewsViewModel::class.java)

    yahooNewsViewModel.data.observe(viewLifecycleOwner, Observer { data ->
      newsAdapter.updateData(data)

      // Stop observing now. News needs to be updated manually.
      yahooNewsViewModel.data.removeObservers(viewLifecycleOwner)
    })

    yahooNewsViewModel.getNewsData(yahooNewsQuery)

    // use requireActivity() instead of this to have only one shared viewmodel
    stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)

    // Get the online data and compose a news query for the symbol.
    stockRoomViewModel.onlineMarketDataList.observe(viewLifecycleOwner, Observer { data ->
      data?.let { onlineMarketDataList ->
        val onlineMarketData = onlineMarketDataList.find { onlineMarketDataItem ->
          onlineMarketDataItem.symbol == symbol
        }
        googleNewsQuery = onlineMarketData?.name ?: symbol
        //newsQuery = "${onlineMarketData.fullExchangeName}: ${onlineMarketData.symbol}, ${onlineMarketData.name}"
        //newsQuery = "${onlineMarketData.fullExchangeName}: ${onlineMarketData.symbol}"

        googleNewsViewModel.getNewsData(googleNewsQuery)

        // Stop observing now. News needs to be updated manually.
        stockRoomViewModel.onlineMarketDataList.removeObservers(viewLifecycleOwner)
      }
    })

    googleNewsViewModel.data.observe(viewLifecycleOwner, Observer { data ->
      newsAdapter.updateData(data)
    })

    swipeRefreshLayout.setOnRefreshListener {
      updateData()
      swipeRefreshLayout.isRefreshing = false
    }
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menu_sync -> {
        updateData()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun updateData() {
    yahooNewsViewModel.getNewsData(yahooNewsQuery)
    if (yahooNewsViewModel.data.value != null) {
      newsAdapter.updateData(yahooNewsViewModel.data.value!!)
    }

    googleNewsViewModel.getNewsData(googleNewsQuery)
    if (googleNewsViewModel.data.value != null) {
      newsAdapter.updateData(googleNewsViewModel.data.value!!)
    }
  }
}
