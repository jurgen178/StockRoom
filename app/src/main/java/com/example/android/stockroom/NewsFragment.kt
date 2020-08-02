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
import kotlinx.android.synthetic.main.fragment_yahoonews.yahoonewsRecyclerview
import java.util.Locale

class YahooNewsFragment : Fragment() {

  private lateinit var newsViewModel: YahooNewsViewModel

  companion object {
    fun newInstance() = YahooNewsFragment()
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
    return inflater.inflate(R.layout.fragment_yahoonews, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    val newsAdapter = NewsAdapter(requireContext())
    yahoonewsRecyclerview.adapter = newsAdapter
    yahoonewsRecyclerview.layoutManager = LinearLayoutManager(requireContext())

    newsViewModel = ViewModelProvider(this).get(YahooNewsViewModel::class.java)

    newsViewModel.data.observe(viewLifecycleOwner, Observer { data ->
      newsAdapter.updateData(data)
    })

    getNewsData()
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
