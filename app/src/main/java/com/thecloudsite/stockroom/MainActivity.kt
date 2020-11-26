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

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.text.italic
import androidx.core.view.MenuCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.ktx.get
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.thecloudsite.stockroom.R.array
import com.thecloudsite.stockroom.R.string
import com.thecloudsite.stockroom.StockRoomViewModel.AlertData
import com.thecloudsite.stockroom.database.Events
import com.thecloudsite.stockroom.list.ListLogAdapter
import com.thecloudsite.stockroom.news.AllNewsFragment
import com.thecloudsite.stockroom.notification.NotificationChannelFactory
import com.thecloudsite.stockroom.notification.NotificationFactory
import com.thecloudsite.stockroom.utils.isOnline
import com.thecloudsite.stockroom.utils.isValidSymbol
import kotlinx.android.synthetic.main.activity_main.main_tab_layout
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

  private lateinit var remoteConfig: FirebaseRemoteConfig

  private lateinit var filterDataViewModel: FilterDataViewModel

  private lateinit var stockRoomViewModel: StockRoomViewModel
  private var eventList: MutableList<Events> = mutableListOf()

  lateinit var onlineDataHandler: Handler

  lateinit var eventHandler: Handler
  val eventDelay: Long = 5000L

  companion object {

    // Remote Config keys
    private const val STOCKMARKETDATA_URL = "stockMarketDataUrl"
    private const val STOCKCHARTDATA_URL = "stockChartDataUrl"

    //    private const val YAHOONEWS_URL = "yahooNewsUrl"
    //    private const val YAHOOALLNEWS_URL = "yahooAllNewsUrl"
    //    private const val GOOGLENEWS_URL = "googleNewsUrl"
    //    private const val GOOGLEALLNEWS_URL = "googleAllNewsUrl"
    private const val USER_MSG_TITLE = "userMsgTitle"
    private const val USER_MSG = "userMsg"

    const val onlineDataTimerDelay: Long = 2000L
    var realtimeOverride: Boolean = false
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    // Setup the notification channel.
    NotificationChannelFactory(this)

    val debugListAdapter = ListLogAdapter(this)
    val debugList = findViewById<RecyclerView>(R.id.recyclerViewDebug)
    debugList.adapter = debugListAdapter
    val linearLayoutManager = LinearLayoutManager(this)
    linearLayoutManager.stackFromEnd = true
    debugList.layoutManager = linearLayoutManager

    // Get a new or existing ViewModel from the ViewModelProvider.
    stockRoomViewModel = ViewModelProvider(this).get(StockRoomViewModel::class.java)
    stockRoomViewModel.logDebug("Main activity started.")

    if (!isOnline(applicationContext)) {
      stockRoomViewModel.logDebug("Network is offline.")
    }

    filterDataViewModel = ViewModelProvider(this).get(FilterDataViewModel::class.java)

/*
    // When you enable disk persistence, your app writes the data locally
    // to the device so your app can maintain state while offline, even
    // if the user or operating system restarts the app.
    //Firebase.database.setPersistenceEnabled(true)

    // Write a message to the database
    val database = Firebase.database
    val myRef = database.getReference("message")

    myRef.setValue("Hello, World!A")


    // Read from the database
    myRef.addListenerForSingleValueEvent(object : ValueEventListener {
      override fun onDataChange(dataSnapshot: DataSnapshot) {
        // This method is called once with the initial value and again
        // whenever data at this location is updated.
        val value = dataSnapshot.getValue<String>()
        val a = value
        //Log.d(TAG, "Value is: $value")
      }

      override fun onCancelled(error: DatabaseError) {
        // Failed to read value
        //Log.w(TAG, "Failed to read value.", error.toException())
      }
    })
*/

    updateRemoteConfig()

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
      if (debugdatalist != null) {
        val s = recyclerViewDebug.canScrollVertically(1)
        debugListAdapter.updateData(debugdatalist)
        // Scroll only if last item at the bottom to allow scrolling up without jumping
        // to the bottom for each update.
        if (!s) {
          recyclerViewDebug.adapter?.itemCount?.minus(1)
              ?.let { recyclerViewDebug.scrollToPosition(it) }
        }
      }
    })

    SharedHandler.deleteStockHandler.observe(this, Observer { symbol ->
      if (symbol != null) {
        stockRoomViewModel.deleteStock(symbol)
      }
    })

    viewpager.adapter = object : FragmentStateAdapter(this) {
      override fun createFragment(position: Int): Fragment {

        return when (SharedRepository.displayedViewsList[position]) {
          "00_StockRoomChartFragment" -> {
            StockRoomChartFragment.newInstance()
          }
          "01_StockRoomListFragment" -> {
            StockRoomListFragment.newInstance()
          }
          "02_StockRoomTileFragment" -> {
            StockRoomTileFragment.newInstance()
          }
          "03_StockRoomSmallListFragment" -> {
            StockRoomSmallListFragment.newInstance()
          }
          "04_StockRoomSmallTileFragment" -> {
            StockRoomSmallTileFragment.newInstance()
          }
          "05_StockRoomTreemapFragment" -> {
            StockRoomTreemapFragment.newInstance()
          }
          "06_SummaryGroupFragment" -> {
            SummaryGroupFragment.newInstance()
          }
          "07_AllNewsFragment" -> {
            AllNewsFragment.newInstance()
          }
          "08_AssetTimelineFragment" -> {
            AssetTimelineFragment.newInstance()
          }
          "09_EventTimelineFragment" -> {
            EventTimelineFragment.newInstance()
          }
          "10_DividendTimelineFragment" -> {
            DividendTimelineFragment.newInstance()
          }
          else -> {
            StockRoomListFragment.newInstance()
          }
        }
      }

      override fun getItemCount(): Int {
        return SharedRepository.displayedViewsList.size
      }
    }

    // Display the tab layout headers.
    TabLayoutMediator(main_tab_layout, viewpager) { tab, position ->

      if (position >= 0 && position < SharedRepository.displayedViewsList.size) {
        // 00_StockRoomChartFragment, 01_StockRoomListFragment, ... 10_DividendTimelineFragment
        val viewlistEntry = SharedRepository.displayedViewsList[position].subSequence(0, 2)
            .toString()
        val index = viewlistEntry.toInt()
        val headers = this.resources.getStringArray(array.displayed_views_headers)
        if (index >= 0 && index < headers.size) {
          tab.text = headers[index]
        }
      }
    }.attach()

    viewpager.setCurrentItem(
        if (SharedRepository.displayedViewsList.contains("01_StockRoomListFragment")) {
          SharedRepository.displayedViewsList.indexOf("01_StockRoomListFragment")
        } else {
          0
        }, false
    )

    // Update the menu when filter data changed.
    filterDataViewModel.data.observe(this, Observer {
      invalidateOptionsMenu()
    })

    // Update the menu when portfolio data changed.
    SharedRepository.portfoliosLiveData.observe(this, Observer {
      invalidateOptionsMenu()
    })

    // Update the menu check marks when sorting is changed.
    stockRoomViewModel.sortingLiveData.observe(this, Observer {
      invalidateOptionsMenu()
    })

    /*
    val connectivityManager =
      application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    connectivityManager.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        //take action when network connection is gained
      }

      override fun onLost(network: Network) {
        //take action when network connection is lost
      }
    })
    */

    // Setup online data every 2s for regular hours.
    onlineDataHandler = Handler(Looper.getMainLooper())

    // Setup event handler every 5s.
    eventHandler = Handler(Looper.getMainLooper())
  }

  private fun updateRemoteConfig() {
    // Get Remote Config instance.
    // [START get_remote_config_instance]
    remoteConfig = Firebase.remoteConfig
    // [END get_remote_config_instance]

    // Create a Remote Config Setting to enable developer mode, which you can use to increase
    // the number of fetches available per hour during development. Also use Remote Config
    // Setting to set the minimum fetch interval.
    // [START enable_dev_mode]

    val configSettings = remoteConfigSettings {
      minimumFetchIntervalInSeconds = if (BuildConfig.DEBUG) {
        1
      } else {
        3600
      }
    }
    remoteConfig.setConfigSettingsAsync(configSettings)
    // [END enable_dev_mode]

    // Set default Remote Config parameter values. An app uses the in-app default values, and
    // when you need to adjust those defaults, you set an updated value for only the values you
    // want to change in the Firebase console. See Best Practices in the README for more
    // information.
    // [START set_default_values]
    remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
    // [END set_default_values]

    // [START fetch_config_with_callback]
    remoteConfig.fetchAndActivate()
        .addOnCompleteListener(this) { task ->
          if (task.isSuccessful) {
            //val updated = task.result
            stockRoomViewModel.logDebug("Config activated.")

            // Update configuration
            val marketDataUrl = remoteConfig[STOCKMARKETDATA_URL].asString()
            StockMarketDataApiFactory.update(marketDataUrl)
            //stockRoomViewModel.logDebug("Remote Config [url=$marketDataUrl]")

            val chartDataUrl = remoteConfig[STOCKCHARTDATA_URL].asString()
            StockChartDataApiFactory.update(chartDataUrl)
            //stockRoomViewModel.logDebug("Remote Config [url=$chartDataUrl]")

//            val yahooNewsUrl = remoteConfig[YAHOONEWS_URL].asString()
//            YahooNewsApiFactory.update(yahooNewsUrl)
//            //stockRoomViewModel.logDebug("Remote Config [url=$yahooNewsUrl]")
//
//            val yahooAllNewsUrl = remoteConfig[YAHOOALLNEWS_URL].asString()
//            YahooAllNewsApiFactory.update(yahooAllNewsUrl)
//            //stockRoomViewModel.logDebug("Remote Config [url=$yahooNewsUrl]")
//
//            val googleNewsUrl = remoteConfig[GOOGLENEWS_URL].asString()
//            GoogleNewsApiFactory.update(googleNewsUrl)
//            //stockRoomViewModel.logDebug("Remote Config [url=$googleNewsUrl]")
//
//            val googleAllNewsUrl = remoteConfig[GOOGLEALLNEWS_URL].asString()
//            GoogleAllNewsApiFactory.update(googleAllNewsUrl)
//            //stockRoomViewModel.logDebug("Remote Config [url=$googleNewsUrl]")

            val userMsgTitle = remoteConfig[USER_MSG_TITLE].asString()
            val userMsg = remoteConfig[USER_MSG].asString()
            if (userMsgTitle.isNotEmpty() && userMsg.isNotEmpty()) {
              AlertDialog.Builder(this)
                  .setTitle(userMsgTitle)
                  .setMessage(userMsg)
                  .setPositiveButton(R.string.ok) { dialog, _ -> dialog.dismiss() }
                  .show()
            }
          } else {
            stockRoomViewModel.logDebug("Config failed, using defaults.")
          }
        }
    // [END fetch_config_with_callback]
  }

  private val onlineDataTask = object : Runnable {
    override fun run() {
      stockRoomViewModel.runOnlineTask()
      onlineDataHandler.postDelayed(this, onlineDataTimerDelay)
    }
  }

  private val eventTask = object : Runnable {
    override fun run() {
      checkEvents()
      //stockRoomViewModel.logDebug("Check events.")
      eventHandler.postDelayed(this, eventDelay)
    }
  }

  override fun onPause() {
    super.onPause()

    onlineDataHandler.removeCallbacks(onlineDataTask)
    eventHandler.removeCallbacks(eventTask)
  }

  override fun onResume() {
    super.onResume()

    onlineDataHandler.post(onlineDataTask)
    eventHandler.post(eventTask)

    val sharedPreferences =
      PreferenceManager.getDefaultSharedPreferences(this /* Activity context */)
    val debug: Boolean = sharedPreferences.getBoolean("list", false)
    recyclerViewDebug.visibility = if (debug) {
      View.VISIBLE
    } else {
      View.GONE
    }

    val tabHeadlines: Boolean = sharedPreferences.getBoolean("tab_headlines", true)
    main_tab_layout.visibility = if (SharedRepository.displayedViewsList.size > 1 && tabHeadlines) {
      View.VISIBLE
    } else {
      View.GONE
    }

    val selectedFilter = sharedPreferences.getString("selectedFilter", "")
    val filterActive = sharedPreferences.getBoolean("filterActive", false)
    val filterData = sharedPreferences.getString("filterSetting", "")
    if (filterData != null) {
      filterDataViewModel.setSerializedStr(filterData, selectedFilter, filterActive)
    }
  }

  private var filterMenuIdMap: MutableMap<Int, String> = mutableMapOf()

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    // Inflate the menu; this adds items to the action bar if it is present.
    menuInflater.inflate(R.menu.main_menu, menu)
    MenuCompat.setGroupDividerEnabled(menu, true)

    // Remove previous filter items.
    filterMenuIdMap.forEach { (id, _) ->
      menu.removeItem(id)
    }
    filterMenuIdMap.clear()

    // Change the Filter menu item to bold style.
    val menuFilterItem = menu.findItem(R.id.menu_filter)
    menuFilterItem.title = SpannableStringBuilder()
        .bold { append(getString(R.string.menu_filter)) }

    val menuItem = menu.findItem(R.id.menu_sort)
    val filterActive = filterDataViewModel.filterActive
    val selectedFilter = filterDataViewModel.selectedFilter

    // Add the new filter items.
    var id: Int = 1
    filterDataViewModel.filterNameList.forEach { filterSet ->

      val filterSetMenuName = SpannableStringBuilder()
          .italic { append(filterSet) }

      // Add the menu item to the group R.id.filter
      val newMenuFilterItem = menuItem.subMenu.add(R.id.filter, id, Menu.NONE, filterSetMenuName)
      if (filterActive) {
        newMenuFilterItem.isCheckable = true
        newMenuFilterItem.isChecked = selectedFilter == filterSet
      }

      filterMenuIdMap[id] = filterSet
      id++
    }

    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menu_sort_change_percentage -> {
        stockRoomViewModel.updateSortMode(SortMode.ByChangePercentage)
        true
      }
      R.id.menu_sort_name -> {
        stockRoomViewModel.updateSortMode(SortMode.ByName)
        true
      }
      R.id.menu_sort_assets -> {
        stockRoomViewModel.updateSortMode(SortMode.ByAssets)
        true
      }
      R.id.menu_sort_profit -> {
        stockRoomViewModel.updateSortMode(SortMode.ByProfit)
        true
      }
      R.id.menu_sort_profit_percentage -> {
        stockRoomViewModel.updateSortMode(SortMode.ByProfitPercentage)
        true
      }
      R.id.menu_sort_dividend_percentage -> {
        stockRoomViewModel.updateSortMode(SortMode.ByDividendPercentage)
        true
      }
      R.id.menu_sort_group -> {
        stockRoomViewModel.updateSortMode(SortMode.ByGroup)
        true
      }
//      R.id.menu_sort_unsorted -> {
//        stockRoomViewModel.updateSortMode(SortMode.ByUnsorted)
//        true
//      }
      R.id.menu_sync -> {
        stockRoomViewModel.runOnlineTaskNow("Request to get online data manually.")
        true
      }
      R.id.menu_filter -> {
        val intent = Intent(this, FilterActivity::class.java)
        startActivity(intent)
        true
      }
      else -> {
        if (filterMenuIdMap.contains(item.itemId)) {
          filterDataViewModel.filterActive = true
          filterDataViewModel.selectedFilter = filterMenuIdMap[item.itemId].toString()

          true
        } else {
          super.onOptionsItemSelected(item)
        }
      }
    }
  }

  override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
    val portfolioMenuItem = menu?.findItem(R.id.menu_portfolio)
    portfolioMenuItem?.isVisible = false

    val sortMode = stockRoomViewModel.sortMode()
    menu?.findItem(R.id.menu_sort_change_percentage)?.isChecked =
      sortMode == SortMode.ByChangePercentage
    menu?.findItem(R.id.menu_sort_name)?.isChecked = sortMode == SortMode.ByName
    menu?.findItem(R.id.menu_sort_assets)?.isChecked = sortMode == SortMode.ByAssets
    menu?.findItem(R.id.menu_sort_profit)?.isChecked = sortMode == SortMode.ByProfit
    menu?.findItem(R.id.menu_sort_profit_percentage)?.isChecked =
      sortMode == SortMode.ByProfitPercentage
    menu?.findItem(R.id.menu_sort_dividend_percentage)?.isChecked =
      sortMode == SortMode.ByDividendPercentage
    menu?.findItem(R.id.menu_sort_group)?.isChecked = sortMode == SortMode.ByGroup
    //menu?.findItem(R.id.menu_sort_unsorted)?.isChecked = sortMode == SortMode.ByUnsorted

    menu?.findItem(R.id.menu_filter)?.isChecked =
      filterDataViewModel.filterActive == true

    if (SharedRepository.portfolios.value != null) {

      val portfolios = SharedRepository.portfolios.value!!
      if (portfolios.size > 1) {
        portfolioMenuItem?.isVisible = true
        val submenu = portfolioMenuItem?.subMenu
        submenu?.clear()

        // Add portfolios as submenu items.
        portfolios.sortedBy {
          it
        }
            .forEach { portfolio ->
              val standardPortfolio = getString(R.string.standard_portfolio)
              val portfolioName = if (portfolio.isEmpty()) {
                standardPortfolio
              } else {
                portfolio
              }
              val subMenuItem = submenu?.add(portfolioName)

              subMenuItem?.isCheckable = true
              subMenuItem?.isChecked = portfolio == SharedRepository.selectedPortfolio.value

              subMenuItem?.setOnMenuItemClickListener { item ->
                if (item != null) {
                  var itemText = item.toString()
                  stockRoomViewModel.logDebug("Selected portfolio '$itemText'")
                  if (itemText == standardPortfolio) {
                    itemText = ""
                  }
                  SharedRepository.selectedPortfolio.value = itemText
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

          val portfolio = SharedRepository.selectedPortfolio.value ?: ""
          symbolList.forEach { symbol ->
            stockRoomViewModel.insert(symbol = symbol, portfolio = portfolio)

            val msg = getString(R.string.add_stock, symbol)
            stockRoomViewModel.logDebug("AddActivity '$msg'")

            Toast.makeText(
                applicationContext,
                msg,
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
      if (alert.alertAbove > 0.0) {
        val title = getString(
            R.string.alert_above_notification_title, alert.symbol,
            DecimalFormat("0.00##").format(alert.alertAbove),
            DecimalFormat("0.00##").format(alert.marketPrice)
        )

        var text = getString(
            R.string.alert_above_notification, alert.symbol, alert.name,
            DecimalFormat("0.00##").format(alert.alertAbove),
            DecimalFormat("0.00##").format(alert.marketPrice)
        )
        if (alert.alertAboveNote.isNotEmpty()) {
          text += "\n───\n${alert.alertAboveNote}"  // '─' = \u2500
        }

        val notification = NotificationFactory(this, title, text, alert.symbol)
        notification.sendNotification()
        // Alert is shown, remove alert.
        stockRoomViewModel.updateAlertAboveSync(alert.symbol, 0.0, "")

        stockRoomViewModel.logDebug("Alert '$title'")
        stockRoomViewModel.logDebug("Alert '$text'")
      } else
        if (alert.alertBelow > 0.0) {
          val title = getString(
              R.string.alert_below_notification_title, alert.symbol,
              DecimalFormat("0.00##").format(alert.alertBelow),
              DecimalFormat("0.00##").format(alert.marketPrice)
          )

          var text = getString(
              R.string.alert_below_notification, alert.symbol, alert.name,
              DecimalFormat("0.00##").format(alert.alertBelow),
              DecimalFormat("0.00##").format(alert.marketPrice)
          )
          if (alert.alertBelowNote.isNotEmpty()) {
            text += "\n───\n${alert.alertBelowNote}"
          }

          val notification = NotificationFactory(this, title, text, alert.symbol)
          notification.sendNotification()
          // Alert is shown, remove alert.
          stockRoomViewModel.updateAlertBelowSync(alert.symbol, 0.0, "")

          stockRoomViewModel.logDebug("Alert '$title'")
          stockRoomViewModel.logDebug("Alert '$text'")
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

            stockRoomViewModel.logDebug("Event '$title'")
            stockRoomViewModel.logDebug("Event '$text'")

            // process only one entry
            return
          }
        }
      }
    }
  }
}
