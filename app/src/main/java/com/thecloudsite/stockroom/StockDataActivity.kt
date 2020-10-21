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
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.thecloudsite.stockroom.news.NewsFragment
import kotlinx.android.synthetic.main.activity_stock.stockViewpager
import kotlinx.android.synthetic.main.activity_stock.tab_layout
import java.util.Locale

class StockDataActivity : AppCompatActivity() {

  private lateinit var symbol: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_stock)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    val symbolString = intent.getStringExtra("symbol")
    symbol = symbolString?.toUpperCase(Locale.ROOT) ?: ""

    // Query only this symbol when this activity is on.
    SharedRepository.selectedSymbol = symbol

    stockViewpager.adapter = object : FragmentStateAdapter(this) {
      override fun createFragment(position: Int): Fragment {
        return when (position) {
          0 -> {
            val instance = DividendFragment.newInstance()
            instance.arguments = Bundle().apply {
              putString("symbol", symbol)
            }
            instance
          }
          1 -> {
            val instance = StockDataFragment.newInstance()
            instance.arguments = Bundle().apply {
              putString("symbol", symbol)
            }
            instance
          }
          else -> {
            val instance = NewsFragment.newInstance()
            instance.arguments = Bundle().apply {
              putString("symbol", symbol)
            }
            instance
          }
        }
      }

      override fun getItemCount(): Int {
        return 3
      }
    }

    stockViewpager.setCurrentItem(1, false)

    TabLayoutMediator(tab_layout, stockViewpager) { tab, position ->
      tab.text = when (position) {
        0 -> getString(R.string.dividend_headline)
        1 -> getString(R.string.data_headline)
        else -> getString(R.string.news_headline, symbol)
      }
    }.attach()
  }

  override fun onPause() {
    SharedRepository.selectedSymbol = ""
    super.onPause()
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.stock_data_menu, menu)
    return true
  }

  fun onDelete(item: MenuItem) {
    AlertDialog.Builder(this)
        .setTitle(R.string.delete)
        .setMessage(getString(R.string.delete_stock, symbol))
        .setPositiveButton(R.string.delete) { dialog, _ ->
          SharedHandler.deleteStockHandler.postValue(symbol)

          dialog.dismiss()

          finish()
          //activity?.onBackPressed()
        }
        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .show()
//    Toast.makeText(
//        applicationContext,
//        "delete: ${symbol}",
//        Toast.LENGTH_LONG
//    )
//        .show()
  }
}