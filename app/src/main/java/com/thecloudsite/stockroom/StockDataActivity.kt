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
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.thecloudsite.stockroom.calc.CalcActivity
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.database.Dividend
import com.thecloudsite.stockroom.databinding.ActivityStockBinding
import com.thecloudsite.stockroom.news.NewsFragment
import java.util.Locale

data class AccountLiveData(
  var assets: List<Asset> = emptyList(),
  var dividends: List<Dividend> = emptyList(),
)

class StockDataActivity : AppCompatActivity() {

  private lateinit var binding: ActivityStockBinding
  private lateinit var stockSymbol: StockSymbol

  private lateinit var stockRoomViewModel: StockRoomViewModel

  private lateinit var filterDataViewModel: FilterDataViewModel

//  private val accountChange = AccountLiveData()
//  private val accountChangeLiveData = MediatorLiveData<AccountLiveData>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityStockBinding.inflate(layoutInflater)
    val view = binding.root
    setContentView(view)

    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    stockRoomViewModel = ViewModelProvider(this).get(StockRoomViewModel::class.java)

    filterDataViewModel = ViewModelProvider(this).get(FilterDataViewModel::class.java)

    // Query only this symbol when this activity is on.
    val symbolString = intent.getStringExtra(EXTRA_SYMBOL)
    var type = intent.getIntExtra(EXTRA_TYPE, -1)
    if (symbolString != null && type == -1) {
      type = stockRoomViewModel.getTypeSync(symbolString)
    }
    // Query only this symbol when this activity is on.
    stockSymbol = StockSymbol(
      symbol = symbolString?.uppercase(Locale.ROOT) ?: "",
      type = dataProviderFromInt(type)
    )

    SharedRepository.selectedSymbol = stockSymbol

    // Moved to MainActivity, baecuase otherwise a stock need to be selected to
    // update the SharedAccountList.accounts

//    // Use MediatorLiveView to combine the assets and dividend data changes.
//    val assetsLiveData: LiveData<List<Asset>> = stockRoomViewModel.allAssetTable
//    accountChangeLiveData.addSource(assetsLiveData) { value ->
//      if (value != null) {
//        accountChange.assets = value
//        accountChangeLiveData.postValue(accountChange)
//      }
//    }
//
//    val dividendsLiveData: LiveData<List<Dividend>> = stockRoomViewModel.allDividendTable
//    accountChangeLiveData.addSource(dividendsLiveData) { value ->
//      if (value != null) {
//        accountChange.dividends = value
//        accountChangeLiveData.postValue(accountChange)
//      }
//    }
//
//    // Observe asset or dividend changes.
//    accountChangeLiveData.observe(this, Observer { item ->
//      if (item != null) {
//        val map: HashSet<String> = hashSetOf()
//
//        item.assets.forEach { asset ->
//          map.add(asset.account)
//        }
//
//        item.dividends.forEach { dividend ->
//          map.add(dividend.account)
//        }
//
//        SharedAccountList.accounts =
//          map.map { account ->
//            account
//          }
//
//        // Account filters require assets and dividends.
//        // Update filters when assets or dividends change.
//        updateFilterList(this, filterDataViewModel)
//      }
//    })

    binding.stockViewpager.adapter = object : FragmentStateAdapter(this) {
      override fun createFragment(position: Int): Fragment {
        return when (position) {
          0 -> {
            val instance = DividendFragment.newInstance()
            instance.arguments = Bundle().apply {
              putString(EXTRA_SYMBOL, stockSymbol.symbol)
              putInt(EXTRA_TYPE, stockSymbol.type.value)
            }
            instance
          }
          1 -> {
            val instance = StockDataFragment.newInstance()
            instance.arguments = Bundle().apply {
              putString(EXTRA_SYMBOL, stockSymbol.symbol)
              putInt(EXTRA_TYPE, stockSymbol.type.value)
            }
            instance
          }
          else -> {
            val instance = NewsFragment.newInstance()
            instance.arguments = Bundle().apply {
              putString(EXTRA_SYMBOL, stockSymbol.symbol)
            }
            instance
          }
        }
      }

      override fun getItemCount(): Int {
        return 3
      }
    }

    binding.stockViewpager.setCurrentItem(1, false)

    TabLayoutMediator(binding.tabLayout, binding.stockViewpager) { tab, position ->
      tab.text = when (position) {
        0 -> getString(R.string.dividend_headline)
        1 -> getString(R.string.data_headline)
        else -> getString(R.string.news_headline, stockSymbol.symbol)
      }
    }.attach()
  }

  override fun onPause() {
    SharedRepository.selectedSymbol = StockSymbol()
    super.onPause()
  }

  override fun onResume() {
    SharedRepository.selectedSymbol = stockSymbol
    super.onResume()
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
      .setMessage(getString(R.string.delete_stock, stockSymbol.symbol))
      .setPositiveButton(R.string.delete) { dialog, _ ->
        SharedHandler.deleteStockHandler.postValue(stockSymbol.symbol)

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

  fun onCalc(item: MenuItem) {
    val intent = Intent(this@StockDataActivity, CalcActivity::class.java)
    intent.putExtra(EXTRA_SYMBOL, stockSymbol.symbol)
    startActivity(intent)
  }
}