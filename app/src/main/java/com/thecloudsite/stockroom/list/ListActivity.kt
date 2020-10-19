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

package com.thecloudsite.stockroom.list

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.thecloudsite.stockroom.MainActivity.Companion
import com.thecloudsite.stockroom.R.layout
import com.thecloudsite.stockroom.StockRoomViewModel
import kotlinx.android.synthetic.main.activity_list.debugswitch
import kotlinx.android.synthetic.main.activity_list.listDBRecyclerview
import kotlinx.android.synthetic.main.activity_list.realtimeswitch

class ListActivity : AppCompatActivity() {

  private lateinit var stockRoomViewModel: StockRoomViewModel
  private lateinit var listDBAdapter: ListDBAdapter

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(layout.activity_list)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    val sharedPreferences =
      PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
    debugswitch.isChecked = sharedPreferences.getBoolean("list", false)

    debugswitch.setOnCheckedChangeListener { _, isChecked ->
      sharedPreferences
          .edit()
          .putBoolean("list", isChecked)
          .apply()
    }

    // Only valid for the current App run. Will be reset the next time the app starts.
    realtimeswitch.isChecked = Companion.realtimeOverride
    realtimeswitch.setOnCheckedChangeListener { _, isChecked ->
      Companion.realtimeOverride = isChecked
    }

    stockRoomViewModel = ViewModelProvider(this).get(StockRoomViewModel::class.java)

    listDBAdapter = ListDBAdapter(this)
    listDBRecyclerview.adapter = listDBAdapter
    listDBRecyclerview.layoutManager = LinearLayoutManager(this)

/*
    stockRoomViewModel.allStockItems.observe(this, Observer { items ->
      items?.let { stockItemSet ->
        val htmlText = updateHtmlText(stockItemSet)
        val mimeType: String = "text/html"
        val utfType: String = "UTF-8"
        webview.loadDataWithBaseURL(null, htmlText, mimeType, utfType, null);
      }
    })
  */

    stockRoomViewModel.allProperties.observe(this, Observer { items ->

      listDBAdapter.updateStockDBdata(items)

      stockRoomViewModel.allProperties.removeObservers(this)
    })

    stockRoomViewModel.allGroupTable.observe(this, Observer { items ->

      listDBAdapter.updateGroup(items)

      stockRoomViewModel.allGroupTable.removeObservers(this)
    })

    stockRoomViewModel.allAssetTable.observe(this, Observer { items ->

      listDBAdapter.updateAsset(items)

      stockRoomViewModel.allAssetTable.removeObservers(this)
    })

    stockRoomViewModel.allEventTable.observe(this, Observer { items ->

      listDBAdapter.updateEvent(items)

      stockRoomViewModel.allEventTable.removeObservers(this)
    })

    stockRoomViewModel.allDividendTable.observe(this, Observer { items ->

      listDBAdapter.updateDividend(items)

      stockRoomViewModel.allDividendTable.removeObservers(this)
    })
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }

//  private fun updateHtmlText() {
//    var htmlText = resources.getRawTextFile(raw.list)
//
//    htmlText = htmlText.replace("<!-- stock_table_name -->", "stock_table ($stockTableRowsCount)")
//    htmlText = htmlText.replace("<!-- stock_table -->", stockTableRows.toString())
//
//    htmlText = htmlText.replace("<!-- group_table_name -->", "group_table ($groupTableRowsCount)")
//    htmlText = htmlText.replace("<!-- group_table -->", groupTableRows.toString())
//
//    htmlText = htmlText.replace("<!-- asset_table_name -->", "asset_table ($assetTableRowsCount)")
//    htmlText = htmlText.replace("<!-- asset_table -->", assetTableRows.toString())
//
//    htmlText = htmlText.replace("<!-- event_table_name -->", "event_table ($eventTableRowsCount)")
//    htmlText = htmlText.replace("<!-- event_table -->", eventTableRows.toString())
//
//    htmlText =
//      htmlText.replace("<!-- dividend_table_name -->", "dividend_table ($dividendTableRowsCount)")
//    htmlText = htmlText.replace("<!-- dividend_table -->", dividendTableRows.toString())
//
//    val mimeType: String = "text/html"
//    val utfType: String = "UTF-8"
//    webview.loadDataWithBaseURL(null, htmlText, mimeType, utfType, null)
//  }

//  private fun Resources.getRawTextFile(@RawRes id: Int) =
//    openRawResource(id).bufferedReader()
//        .use { it.readText() }
}