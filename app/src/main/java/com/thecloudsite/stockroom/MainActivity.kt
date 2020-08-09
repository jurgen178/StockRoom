/*
 * Copyright (C) 2017 Google Inc.
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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.MenuItem.OnMenuItemClickListener
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.thecloudsite.stockroom.StockRoomViewModel.AlertData
import kotlinx.android.synthetic.main.activity_main.recyclerViewDebug
import kotlinx.android.synthetic.main.activity_main.viewpager
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale

// App constants.
// https://stackoverflow.com/questions/9947156/what-are-the-default-color-values-for-the-holo-theme-on-android-4-0
// @android:color/holo_orange_light = 0xffffbb33
//val backgroundListColor = 0xffffbb33.toInt()
//val backgroundListColor = Color.rgb(240, 240, 255)

class MainActivity : AppCompatActivity() {

  private val newSymbolActivityRequestCode = 1

  private lateinit var stockRoomViewModel: StockRoomViewModel
  private var eventList: MutableList<Events> = mutableListOf()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // Setup the notification channel.
    NotificationChannelFactory(this)

    val debugListAdapter = DebugListAdapter(this)
    val debugList = findViewById<RecyclerView>(R.id.recyclerViewDebug)
    debugList.adapter = debugListAdapter
    val linearLayoutManager = LinearLayoutManager(this)
    linearLayoutManager.stackFromEnd = true
    debugList.layoutManager = linearLayoutManager

    // Get a new or existing ViewModel from the ViewModelProvider.
    stockRoomViewModel = ViewModelProvider(this).get(StockRoomViewModel::class.java)
    stockRoomViewModel.logDebug("Main activity started.")

    SharedRepository.alerts.observe(this, Observer { alerts ->
      showAlerts(alerts)
    })

    stockRoomViewModel.allEvents.observe(this, Observer { events ->
      events?.let {
        eventList = events.toMutableList()
        checkEvents()
      }
    })

    SharedRepository.debugData.observe(this, Observer { debugdatalist ->
      val s = recyclerViewDebug.canScrollVertically(1)
      debugListAdapter.updateData(debugdatalist)
      // Scroll only if last item at the bottom to allow scrolling up without jumping
      // to the bottom for each update.
      if (!s) {
        recyclerViewDebug.adapter?.itemCount?.minus(1)
            ?.let { recyclerViewDebug.scrollToPosition(it) }
      }
    })

    Storage.deleteStockHandler.observe(this, Observer { symbol ->
      stockRoomViewModel.delete(symbol)
    })

    viewpager.adapter = object : FragmentStateAdapter(this) {
      override fun createFragment(position: Int): Fragment {
        return when (position) {
          0 -> {
            ListFragment.newInstance()
          }
          1 -> {
            SummaryListFragment.newInstance()
          }
          else -> {
            SummaryGroupFragment.newInstance()
          }
        }
      }

      override fun getItemCount(): Int {
        return 3
      }
    }

    // Update the menu when portfolio data changed.
    stockRoomViewModel.portfoliosLiveData.observe(this, Observer {
      invalidateOptionsMenu()
    })

    // Check event every 5s
    val eventHandle: Handler = Handler()
    val eventDelay: Long = 5000L
    val eventRunnableCode = object : Runnable {
      override fun run() {
        checkEvents()
        eventHandle.postDelayed(this, eventDelay)
      }
    }
    eventHandle.post(eventRunnableCode)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.main_menu, menu)
    return true
  }

  override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
    val portfolioMenuItem = menu?.findItem(R.id.menu_portfolio)
    portfolioMenuItem?.isVisible = false

    if (stockRoomViewModel.portfoliosLiveData.value != null) {
      val portfolios = stockRoomViewModel.portfoliosLiveData.value!!
      if (portfolios.size > 1) {
        portfolioMenuItem?.isVisible = true
        val submenu = portfolioMenuItem?.subMenu
        submenu?.clear()

        // Add portfolios as submenu items.
        portfolios.forEach { portfolio ->
          val standardPortfolio = getString(R.string.standard_portfolio)
          val portfolioName = if (portfolio.isEmpty()) {
            standardPortfolio
          } else {
            portfolio
          }
          val subMenuItem = submenu?.add(portfolioName)
          subMenuItem?.setOnMenuItemClickListener { item ->
            if (item != null) {
              var itemText = item.toString()
              if (itemText == standardPortfolio) {
                itemText = ""
              }
              stockRoomViewModel.selectedPortfolio.postValue(itemText)
            }
            true
          }
        }
      }
    }

    return super.onPrepareOptionsMenu(menu)
  }

  /*
  override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
    val item = menu!!.add("Clear Array")
    item.setOnMenuItemClickListener(object : OnMenuItemClickListener {
      override fun onMenuItemClick(item: MenuItem?): Boolean {
        //clearArray()
        return true
      }
    })
    return super.onPrepareOptionsMenu(menu)
  }
   */

  fun onSettings(item: MenuItem) {
    val intent = Intent(this@MainActivity, SettingsActivity::class.java)
    startActivity(intent)
  }

  fun onAdd(item: MenuItem) {
    val intent = Intent(this@MainActivity, AddActivity::class.java)
    startActivityForResult(intent, newSymbolActivityRequestCode)
  }

  override fun onResume() {
    super.onResume()

    val sharedPreferences =
      PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
    val debug: Boolean = sharedPreferences.getBoolean("list", false)
    if (debug) {
      recyclerViewDebug.visibility = View.VISIBLE
    } else {
      recyclerViewDebug.visibility = View.GONE
    }
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    intentData: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, intentData)

    if (requestCode == newSymbolActivityRequestCode && resultCode == Activity.RESULT_OK) {
      intentData?.let { data ->
        val symbolText = data.getStringExtra(AddActivity.EXTRA_REPLY)
        if (symbolText != null) {
          val symbols = symbolText.split("[ ,;\r\n\t]".toRegex())

          val symbolList: List<String> = symbols.map { symbol ->
            symbol.replace("\"", "")
                .toUpperCase(Locale.ROOT)
          }
              .distinct()
              .filter { symbol ->
                isValidSymbol(symbol)
              }

          symbolList.forEach { symbol ->
            stockRoomViewModel.insert(symbol)

            Toast.makeText(
                applicationContext,
                getString(R.string.add_stock, symbol),
                Toast.LENGTH_LONG
            )
                .show()
          }
        }
      }
    }

    /*
    else {
      Toast.makeText(
          applicationContext,
          R.string.empty_not_saved,
          Toast.LENGTH_LONG
      )
          .show()
    }
  */
  }

  private fun showAlerts(alerts: List<AlertData>) {
    alerts.forEach { alert ->
      if (alert.alertAbove > 0f) {
        val title = getString(
            R.string.alert_above_notification_title, alert.symbol,
            DecimalFormat("0.00##").format(alert.alertAbove),
            DecimalFormat("0.00##").format(alert.marketPrice)
        )
        val text = getString(
            R.string.alert_above_notification, alert.symbol, alert.name,
            DecimalFormat("0.00##").format(alert.alertAbove),
            DecimalFormat("0.00##").format(alert.marketPrice)
        )
        val notification = NotificationFactory(this, title, text, alert.symbol)
        notification.sendNotification()
        // Alert is shown, remove alert.
        stockRoomViewModel.updateAlertAbove(alert.symbol, 0f)
      } else
        if (alert.alertBelow > 0f) {
          val title = getString(
              R.string.alert_below_notification_title, alert.symbol,
              DecimalFormat("0.00##").format(alert.alertBelow),
              DecimalFormat("0.00##").format(alert.marketPrice)
          )
          val text = getString(
              R.string.alert_below_notification, alert.symbol, alert.name,
              DecimalFormat("0.00##").format(alert.alertBelow),
              DecimalFormat("0.00##").format(alert.marketPrice)
          )
          val notification = NotificationFactory(this, title, text, alert.symbol)
          notification.sendNotification()
          // Alert is shown, remove alert.
          stockRoomViewModel.updateAlertBelow(alert.symbol, 0f)
        }
    }
  }

  private fun checkEvents() {
    if (SharedRepository.notifications) {
      val datetimeNow = LocalDateTime.now()
          .toEpochSecond(ZoneOffset.UTC)

      // Each stock item
      eventList.forEach { events ->
        // Each event item per stock
        events.events.forEach { event ->
          if (datetimeNow >= event.datetime) {
            val title =
              getString(R.string.alert_event_notification_title, event.title, event.symbol)
            val text =
              getString(R.string.alert_event_notification, event.symbol, event.title, event.note)
            val notification = NotificationFactory(this, title, text, event.symbol)
            notification.sendNotification()

            // Delete event from the DB. This is turn causes an update of the list
            // and runs checkEvents() again.
            stockRoomViewModel.deleteEvent(event)

            // process only one entry
            return
          }
        }
      }
    }
  }
}
