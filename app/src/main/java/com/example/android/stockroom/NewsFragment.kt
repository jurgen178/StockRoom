package com.example.android.stockroom

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
import java.util.Locale

class NewsFragment : Fragment() {

  private lateinit var newsViewModel: NewsViewModel
  private lateinit var stockRoomViewModel: StockRoomViewModel

  companion object {
    fun newInstance() = NewsFragment()
  }

  private var symbol: String = ""
  private var newsQuery = ""

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
    newsQuery = symbol

    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_news, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    val newsAdapter = NewsAdapter(requireContext())
    newsRecyclerview.adapter = newsAdapter
    newsRecyclerview.layoutManager = LinearLayoutManager(requireContext())

    newsViewModel = ViewModelProvider(this).get(NewsViewModel::class.java)

    newsViewModel.data.observe(viewLifecycleOwner, Observer { data ->
      newsAdapter.updateData(data)
    })

    // use requireActivity() instead of this to have only one shared viewmodel
    stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)
    stockRoomViewModel.logDebug("News fragment started.")

    // Get the online data and compose a news query for the symbol.
    stockRoomViewModel.onlineMarketDataList.observe(viewLifecycleOwner, Observer { data ->
      data?.let { onlineMarketDataList ->
        val onlineMarketData = onlineMarketDataList.find { onlineMarketDataItem ->
          onlineMarketDataItem.symbol == symbol
        }
        if (onlineMarketData != null) {
          newsQuery = onlineMarketData.name
          //newsQuery = "${onlineMarketData.fullExchangeName}: ${onlineMarketData.symbol}, ${onlineMarketData.name}"
          //newsQuery = "${onlineMarketData.fullExchangeName}: ${onlineMarketData.symbol}"
          getNewsData()
        }
      }
    })
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menu_sync -> {
        getNewsData()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun getNewsData() {
    newsViewModel.getNewsData(newsQuery)
  }
}
