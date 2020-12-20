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

package com.thecloudsite.stockroom.news

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.thecloudsite.stockroom.R
import com.thecloudsite.stockroom.databinding.FragmentNewsBinding

class AllNewsFragment : Fragment() {

  private var _binding: FragmentNewsBinding? = null

  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  private lateinit var yahooAllNewsViewModel: YahooAllNewsViewModel
  private lateinit var googleAllNewsViewModel: GoogleAllNewsViewModel
  private lateinit var nasdaqAllNewsViewModel: NasdaqAllNewsViewModel
  private lateinit var newsAdapter: NewsAdapter

  companion object {
    fun newInstance() = AllNewsFragment()
  }

  private var yahooAllNewsQuery = ""
  private var googleAllNewsQuery = ""
  private var nasdaqAllNewsQuery = ""

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setHasOptionsMenu(true)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {

    // Inflate the layout for this fragment
    _binding = FragmentNewsBinding.inflate(inflater, container, false)
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

    newsAdapter = NewsAdapter(requireContext(), getString(R.string.all_news_headline))
    binding.newsRecyclerview.adapter = newsAdapter
    binding.newsRecyclerview.layoutManager = LinearLayoutManager(requireContext())

    yahooAllNewsViewModel = ViewModelProvider(this).get(YahooAllNewsViewModel::class.java)
    googleAllNewsViewModel = ViewModelProvider(this).get(GoogleAllNewsViewModel::class.java)
    nasdaqAllNewsViewModel = ViewModelProvider(this).get(NasdaqAllNewsViewModel::class.java)

    yahooAllNewsViewModel.data.observe(viewLifecycleOwner, Observer { data ->
      if (data != null) {
        newsAdapter.updateData(data)

        // Stop observing now. News needs to be updated manually.
        yahooAllNewsViewModel.data.removeObservers(viewLifecycleOwner)
      }
    })

    googleAllNewsViewModel.data.observe(viewLifecycleOwner, Observer { data ->
      if (data != null) {
        newsAdapter.updateData(data)

        // Stop observing now. News needs to be updated manually.
        googleAllNewsViewModel.data.removeObservers(viewLifecycleOwner)
      }
    })

    nasdaqAllNewsViewModel.data.observe(viewLifecycleOwner, Observer { data ->
      if (data != null) {
        newsAdapter.updateData(data)

        // Stop observing now. News needs to be updated manually.
        nasdaqAllNewsViewModel.data.removeObservers(viewLifecycleOwner)
      }
    })

    // Get the news data.
    yahooAllNewsViewModel.getNewsData(yahooAllNewsQuery)
    googleAllNewsViewModel.getNewsData(googleAllNewsQuery)
    nasdaqAllNewsViewModel.getNewsData(nasdaqAllNewsQuery)

    binding.swipeRefreshLayout.setOnRefreshListener {
      updateData()
      binding.swipeRefreshLayout.isRefreshing = false
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
    yahooAllNewsViewModel.getNewsData(yahooAllNewsQuery)
    if (yahooAllNewsViewModel.data.value != null) {
      newsAdapter.updateData(yahooAllNewsViewModel.data.value!!)
    }

    googleAllNewsViewModel.getNewsData(googleAllNewsQuery)
    if (googleAllNewsViewModel.data.value != null) {
      newsAdapter.updateData(googleAllNewsViewModel.data.value!!)
    }

    nasdaqAllNewsViewModel.getNewsData(nasdaqAllNewsQuery)
    if (nasdaqAllNewsViewModel.data.value != null) {
      newsAdapter.updateData(nasdaqAllNewsViewModel.data.value!!)
    }
  }
}
