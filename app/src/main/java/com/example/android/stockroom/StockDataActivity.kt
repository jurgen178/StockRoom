package com.example.android.stockroom

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.android.synthetic.main.activity_stock.stockViewpager
import kotlinx.android.synthetic.main.activity_stock.tab_layout
import java.util.Locale

class StockDataActivity : AppCompatActivity() {

  private lateinit var symbol: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_stock)
    //supportActionBar?.setDisplayHomeAsUpEnabled(true)

    symbol = intent.getStringExtra("symbol")
        .toUpperCase(Locale.ROOT)

    stockViewpager.adapter = object : FragmentStateAdapter(this) {
      override fun createFragment(position: Int): Fragment {
        return when (position) {
          0 -> {
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
        return 2
      }
    }

    TabLayoutMediator(tab_layout, stockViewpager) { tab, position ->
      tab.text = when(position) {
        0 -> getString(R.string.data_headline)
        else -> getString(R.string.news_headline, symbol)
      }
    }.attach()
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
          Storage.deleteStockHandler.postValue(symbol)
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