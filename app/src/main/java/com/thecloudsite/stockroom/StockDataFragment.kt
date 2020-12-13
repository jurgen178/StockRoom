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

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.DatePicker
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.text.italic
import androidx.core.text.underline
import androidx.core.view.MenuCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.DefaultValueFormatter
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.thecloudsite.stockroom.MainActivity.Companion.onlineDataTimerDelay
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.database.Assets
import com.thecloudsite.stockroom.database.Event
import com.thecloudsite.stockroom.database.Events
import com.thecloudsite.stockroom.database.Group
import com.thecloudsite.stockroom.database.StockDBdata
import com.thecloudsite.stockroom.utils.DecimalFormat0To4Digits
import com.thecloudsite.stockroom.utils.DecimalFormat0To6Digits
import com.thecloudsite.stockroom.utils.DecimalFormat2Digits
import com.thecloudsite.stockroom.utils.DecimalFormat2To4Digits
import com.thecloudsite.stockroom.utils.DecimalFormat2To6Digits
import com.thecloudsite.stockroom.utils.TextMarkerViewCandleChart
import com.thecloudsite.stockroom.utils.TextMarkerViewLineChart
import com.thecloudsite.stockroom.utils.getAssetChange
import com.thecloudsite.stockroom.utils.getAssets
import com.thecloudsite.stockroom.utils.getChangeColor
import com.thecloudsite.stockroom.utils.getMarketValues
import com.thecloudsite.stockroom.utils.openNewTabWindow
import kotlinx.android.synthetic.main.fragment_stockdata.addAssetsButton
import kotlinx.android.synthetic.main.fragment_stockdata.addEventsButton
import kotlinx.android.synthetic.main.fragment_stockdata.alertAboveInputEditText
import kotlinx.android.synthetic.main.fragment_stockdata.alertAboveInputLayout
import kotlinx.android.synthetic.main.fragment_stockdata.alertAboveNoteInputEditText
import kotlinx.android.synthetic.main.fragment_stockdata.alertBelowInputEditText
import kotlinx.android.synthetic.main.fragment_stockdata.alertBelowInputLayout
import kotlinx.android.synthetic.main.fragment_stockdata.alertBelowNoteInputEditText
import kotlinx.android.synthetic.main.fragment_stockdata.assetsView
import kotlinx.android.synthetic.main.fragment_stockdata.buttonFiveDays
import kotlinx.android.synthetic.main.fragment_stockdata.buttonFiveYears
import kotlinx.android.synthetic.main.fragment_stockdata.buttonMax
import kotlinx.android.synthetic.main.fragment_stockdata.buttonOneDay
import kotlinx.android.synthetic.main.fragment_stockdata.buttonOneMonth
import kotlinx.android.synthetic.main.fragment_stockdata.buttonOneYear
import kotlinx.android.synthetic.main.fragment_stockdata.buttonThreeMonth
import kotlinx.android.synthetic.main.fragment_stockdata.buttonYTD
import kotlinx.android.synthetic.main.fragment_stockdata.candleStickChart
import kotlinx.android.synthetic.main.fragment_stockdata.eventsView
import kotlinx.android.synthetic.main.fragment_stockdata.imageButtonIconCandle
import kotlinx.android.synthetic.main.fragment_stockdata.imageButtonIconLine
import kotlinx.android.synthetic.main.fragment_stockdata.lineChart
import kotlinx.android.synthetic.main.fragment_stockdata.stockgroupLayout
import kotlinx.android.synthetic.main.fragment_stockdata.new_stock_price
import kotlinx.android.synthetic.main.fragment_stockdata.new_total_asset
import kotlinx.android.synthetic.main.fragment_stockdata.noteTextView
import kotlinx.android.synthetic.main.fragment_stockdata.onlineDataView
import kotlinx.android.synthetic.main.fragment_stockdata.picker_knob
import kotlinx.android.synthetic.main.fragment_stockdata.price_preview_divider
import kotlinx.android.synthetic.main.fragment_stockdata.price_preview_layout
import kotlinx.android.synthetic.main.fragment_stockdata.price_preview_textview
import kotlinx.android.synthetic.main.fragment_stockdata.removeAssetButton
import kotlinx.android.synthetic.main.fragment_stockdata.splitAssetsButton
import kotlinx.android.synthetic.main.fragment_stockdata.textViewAssetChange
import kotlinx.android.synthetic.main.fragment_stockdata.textViewChange
import kotlinx.android.synthetic.main.fragment_stockdata.textViewGroup
import kotlinx.android.synthetic.main.fragment_stockdata.textViewGroupColor
import kotlinx.android.synthetic.main.fragment_stockdata.textViewMarketPrice
import kotlinx.android.synthetic.main.fragment_stockdata.textViewMarketPriceDelayed
import kotlinx.android.synthetic.main.fragment_stockdata.textViewName
import kotlinx.android.synthetic.main.fragment_stockdata.textViewPortfolio
import kotlinx.android.synthetic.main.fragment_stockdata.textViewPurchasePrice
import kotlinx.android.synthetic.main.fragment_stockdata.textViewRange
import kotlinx.android.synthetic.main.fragment_stockdata.textViewSymbol
import kotlinx.android.synthetic.main.fragment_stockdata.updateNoteButton
import okhttp3.internal.toHexString
import java.lang.Double.min
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.LONG
import java.time.format.FormatStyle.MEDIUM
import java.time.format.FormatStyle.SHORT
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

enum class StockViewRange(val value: Int) {
  OneDay(0),
  FiveDays(1),
  OneMonth(2),
  ThreeMonth(3),
  YTD(4),
  Max(5),
  OneYear(6),
  FiveYears(7),
}

enum class StockViewMode(val value: Int) {
  Line(0),
  Candle(1),
}

enum class LinkType(val value: Int) {
  HeadlineType(0),
  CommunityType(1),
  SearchType(2),
  WebsiteType(3),
}

data class StockAssetsLiveData(
  var assets: Assets? = null,
  var stockDBdata: StockDBdata? = null,
  var onlineMarketData: OnlineMarketData? = null
)

// Enable scrolling by disable parent scrolling
class CustomLineChart(
  context: Context?,
  attrs: AttributeSet?
) :
    LineChart(context, attrs) {
  override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
    if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
      parent?.requestDisallowInterceptTouchEvent(true)
    }
    return false
  }
}

class CustomCandleStickChart(
  context: Context?,
  attrs: AttributeSet?
) :
    CandleStickChart(context, attrs) {
  override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
    if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
      parent?.requestDisallowInterceptTouchEvent(true)
    }
    return false
  }
}

class CustomTimePicker(
  context: Context?,
  attrs: AttributeSet?
) :
    TimePicker(context, attrs) {
  override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
    if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
      parent?.requestDisallowInterceptTouchEvent(true)
    }
    return false
  }
}

class StockDataFragment : Fragment() {

  private lateinit var stockChartDataViewModel: StockChartDataViewModel
  private lateinit var stockRoomViewModel: StockRoomViewModel

  private val assetChange = StockAssetsLiveData()
  private val assetChangeLiveData = MediatorLiveData<StockAssetsLiveData>()

  companion object {
    fun newInstance() = StockDataFragment()
  }

  private var stockDBdata = StockDBdata(symbol = "")
  private var stockDataEntries: List<StockDataEntry>? = null
  private var symbol: String = ""

  private var alertAbove: Double = 0.0
  private var alertBelow: Double = 0.0

  private lateinit var standardPortfolio: String

  lateinit var onlineDataHandler: Handler

  // Settings.
  private val settingStockViewRange = "SettingStockViewRange"
  private var stockViewRange: StockViewRange
    get() {
      val sharedPref =
        PreferenceManager.getDefaultSharedPreferences(activity) ?: return StockViewRange.OneDay
      val index = sharedPref.getInt(settingStockViewRange, StockViewRange.OneDay.value)
      return if (index >= 0 && index < StockViewRange.values().size) {
        StockViewRange.values()[index]
      } else {
        StockViewRange.OneDay
      }
    }
    set(value) {
      val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity) ?: return
      with(sharedPref.edit()) {
        putInt(settingStockViewRange, value.value)
        commit()
      }
    }

  private val settingStockViewMode = "SettingStockViewMode"
  private var stockViewMode: StockViewMode
    get() {
      val sharedPref =
        PreferenceManager.getDefaultSharedPreferences(activity) ?: return StockViewMode.Line
      val index = sharedPref.getInt(settingStockViewMode, StockViewMode.Line.value)
      return if (index >= 0 && index < StockViewMode.values().size) {
        StockViewMode.values()[index]
      } else {
        StockViewMode.Line
      }
    }
    set(value) {
      val sharedPref = PreferenceManager.getDefaultSharedPreferences(activity) ?: return
      with(sharedPref.edit()) {
        putInt(settingStockViewMode, value.value)
        commit()
      }
    }

  private fun assetItemUpdateClicked(asset: Asset) {
    val builder = AlertDialog.Builder(requireContext())
    // Get the layout inflater
    val inflater = LayoutInflater.from(requireContext())

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    val dialogView = inflater.inflate(R.layout.dialog_add_asset, null)

    val addUpdateQuantityHeadlineView =
      dialogView.findViewById<TextView>(R.id.addUpdateQuantityHeadline)
    addUpdateQuantityHeadlineView.text = getString(R.string.update_asset)
    val addQuantityView = dialogView.findViewById<TextView>(R.id.addQuantity)
    addQuantityView.text = DecimalFormat(DecimalFormat0To6Digits).format(asset.quantity.absoluteValue)
//    if (asset.shares < 0) {
//      addSharesView.inputType = TYPE_CLASS_NUMBER or
//          TYPE_NUMBER_FLAG_DECIMAL or
//          TYPE_NUMBER_FLAG_SIGNED
//    }

    val addPriceView = dialogView.findViewById<TextView>(R.id.addPrice)
    addPriceView.text = DecimalFormat(DecimalFormat2To6Digits).format(asset.price)

    val addNoteView = dialogView.findViewById<TextView>(R.id.addNote)
    addNoteView.text = asset.note

    val localDateTime = if (asset.date == 0L) {
      LocalDateTime.now()
    } else {
      LocalDateTime.ofEpochSecond(asset.date, 0, ZoneOffset.UTC)
    }
    val datePickerAssetDateView = dialogView.findViewById<DatePicker>(R.id.datePickerAssetDate)
    // month is starting from zero
    datePickerAssetDateView.updateDate(
        localDateTime.year, localDateTime.month.value - 1, localDateTime.dayOfMonth
    )

    builder.setView(dialogView)
        // Add action buttons
        .setPositiveButton(
            R.string.update
        ) { _, _ ->
          val quantityText = (addQuantityView.text).toString()
              .trim()
          var quantity = 0.0

          try {
            val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
            quantity = numberFormat.parse(quantityText)!!
                .toDouble()
            if (asset.quantity < 0.0) {
              quantity = -quantity
            }
          } catch (e: Exception) {
            Toast.makeText(
                requireContext(), getString(R.string.asset_share_not_empty), Toast.LENGTH_LONG
            )
                .show()
            return@setPositiveButton
          }
          if (quantity == 0.0) {
            Toast.makeText(
                requireContext(), getString(R.string.quantity_not_zero), Toast.LENGTH_LONG
            )
                .show()
            return@setPositiveButton
          }

          val priceText = (addPriceView.text).toString()
              .trim()
          var price = 0.0
          try {
            val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
            price = numberFormat.parse(priceText)!!
                .toDouble()
          } catch (e: Exception) {
            Toast.makeText(
                requireContext(), getString(R.string.asset_price_not_empty), Toast.LENGTH_LONG
            )
                .show()
            return@setPositiveButton
          }
          if (price <= 0.0) {
            Toast.makeText(
                requireContext(), getString(R.string.price_not_zero), Toast.LENGTH_LONG
            )
                .show()
            return@setPositiveButton
          }

          // val date = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
          val localDateTimeNew: LocalDateTime = LocalDateTime.of(
              datePickerAssetDateView.year, datePickerAssetDateView.month + 1,
              datePickerAssetDateView.dayOfMonth, 0, 0
          )
          val date = localDateTimeNew.toEpochSecond(ZoneOffset.UTC)

          val noteText = (addNoteView.text).toString()
              .trim()

          val assetNew =
            Asset(symbol = symbol, quantity = quantity, price = price, date = date, note = noteText)

          if (asset.quantity != assetNew.quantity
              || asset.price != assetNew.price
              || asset.date != assetNew.date
              || asset.note != assetNew.note
          ) {
            // Each asset has an id. Delete the asset with that id and then add assetNew.
            stockRoomViewModel.updateAsset2(asset, assetNew)

            var pluralstr: String = ""
            val quantityAbs = quantity.absoluteValue
            val count: Int = when {
              quantityAbs == 1.0 -> {
                1
              }
              quantityAbs > 1.0 -> {
                quantityAbs.toInt() + 1
              }
              else -> {
                0
              }
            }

            pluralstr = if (asset.quantity > 0.0) {
              resources.getQuantityString(
                  R.plurals.asset_updated, count, DecimalFormat(DecimalFormat0To4Digits).format(quantityAbs),
                  DecimalFormat(DecimalFormat2To4Digits).format(price)
              )
            } else {
              resources.getQuantityString(
                  R.plurals.asset_removed_updated, count,
                  DecimalFormat(DecimalFormat2To4Digits).format(quantityAbs)
              )
            }

            Toast.makeText(
                requireContext(), pluralstr, Toast.LENGTH_LONG
            )
                .show()
          }

          // hideSoftInputFromWindow()
        }
        .setNegativeButton(
            R.string.cancel
        ) { _, _ ->
          //getDialog().cancel()
        }
    builder
        .create()
        .show()
  }

  private fun assetItemDeleteClicked(
    symbol: String?,
    asset: Asset?
  ) {
    // Summary tag?
    if (symbol != null && asset == null) {
      android.app.AlertDialog.Builder(requireContext())
          .setTitle(R.string.delete_all_assets)
          .setMessage(getString(R.string.delete_all_assets_confirm, symbol))
          .setPositiveButton(R.string.delete) { _, _ ->
            stockRoomViewModel.deleteAssets(symbol)
            Toast.makeText(
                requireContext(), getString(R.string.delete_all_assets_msg), Toast.LENGTH_LONG
            )
                .show()
          }
          .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
          .show()
    } else if (asset != null) {
      val count: Int = when {
        asset.quantity == 1.0 -> {
          1
        }
        asset.quantity > 1.0 -> {
          asset.quantity.toInt() + 1
        }
        else -> {
          0
        }
      }

      android.app.AlertDialog.Builder(requireContext())
          .setTitle(R.string.delete_asset)
          .setMessage(
              if (asset.quantity > 0) {
                resources.getQuantityString(
                    R.plurals.delete_asset_confirm, count,
                    DecimalFormat(DecimalFormat0To4Digits).format(asset.quantity),
                    DecimalFormat(DecimalFormat2To4Digits).format(asset.price)
                )
              } else {
                resources.getQuantityString(
                    R.plurals.delete_removed_asset_confirm, count,
                    DecimalFormat(DecimalFormat0To4Digits).format(asset.quantity.absoluteValue)
                )
              }
          )
          .setPositiveButton(R.string.delete) { _, _ ->
            stockRoomViewModel.deleteAsset(asset)
            val pluralstr = if (asset.quantity > 0) {
              resources.getQuantityString(
                  R.plurals.delete_asset_msg, count, DecimalFormat(DecimalFormat0To4Digits).format(asset.quantity),
                  DecimalFormat(DecimalFormat2To4Digits).format(asset.price)
              )
            } else {
              resources.getQuantityString(
                  R.plurals.delete_removed_asset_msg, count,
                  DecimalFormat(DecimalFormat0To4Digits).format(asset.quantity.absoluteValue)
              )
            }

            Toast.makeText(
                requireContext(), pluralstr, Toast.LENGTH_LONG
            )
                .show()
          }
          .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
          .show()
    }
  }

  private fun eventItemUpdateClicked(event: Event) {
    val builder = AlertDialog.Builder(requireContext())
    // Get the layout inflater
    val inflater = LayoutInflater.from(requireContext())

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    val dialogView = inflater.inflate(R.layout.dialog_add_event, null)

    val eventHeadlineView = dialogView.findViewById<TextView>(R.id.eventHeadline)
    eventHeadlineView.text = getString(R.string.update_event)
    val textInputEditEventTitleView =
      dialogView.findViewById<TextView>(R.id.textInputEditEventTitle)
    textInputEditEventTitleView.text = event.title
    val textInputEditEventNoteView = dialogView.findViewById<TextView>(R.id.textInputEditEventNote)
    textInputEditEventNoteView.text = event.note
    val localDateTime = LocalDateTime.ofEpochSecond(event.datetime, 0, ZoneOffset.UTC)
    val datePickerEventDateView = dialogView.findViewById<DatePicker>(R.id.datePickerEventDate)
    // month is starting from zero
    datePickerEventDateView.updateDate(
        localDateTime.year, localDateTime.month.value - 1, localDateTime.dayOfMonth
    )
    val datePickerEventTimeView = dialogView.findViewById<TimePicker>(R.id.datePickerEventTime)
    datePickerEventTimeView.hour = localDateTime.hour
    datePickerEventTimeView.minute = localDateTime.minute

    builder.setView(dialogView)
        // Add action buttons
        .setPositiveButton(
            R.string.update
        ) { _, _ ->
          // Add () to avoid cast exception.
          val title = (textInputEditEventTitleView.text).toString()
              .trim()
          if (title.isEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.event_empty), Toast.LENGTH_LONG)
                .show()
          } else {
            val note = (textInputEditEventNoteView.text).toString()

            val datetime: LocalDateTime = LocalDateTime.of(
                datePickerEventDateView.year, datePickerEventDateView.month + 1,
                datePickerEventDateView.dayOfMonth, datePickerEventTimeView.hour,
                datePickerEventTimeView.minute
            )
            val seconds = datetime.toEpochSecond(ZoneOffset.UTC)
            val eventNew =
              Event(symbol = symbol, type = 0, title = title, note = note, datetime = seconds)
            if (event.title != eventNew.title || event.note != eventNew.note || event.datetime != eventNew.datetime) {
              // Each event has an id. Delete the event with that id and then add eventNew.
              stockRoomViewModel.updateEvent2(event, eventNew)

              Toast.makeText(
                  requireContext(), getString(
                  R.string.event_updated, title, datetime.format(
                  DateTimeFormatter.ofLocalizedDateTime(
                      MEDIUM
                  )
              )
              ), Toast.LENGTH_LONG
              )
                  .show()
            }
          }

          //hideSoftInputFromWindow()
        }
        .setNegativeButton(
            R.string.cancel
        ) { _, _ ->
        }
    builder
        .create()
        .show()
  }

  private fun eventItemDeleteClicked(event: Event) {
    val localDateTime = LocalDateTime.ofEpochSecond(event.datetime, 0, ZoneOffset.UTC)
    val datetime = localDateTime.format(
        DateTimeFormatter.ofLocalizedDateTime(
            MEDIUM
        )
    )
    android.app.AlertDialog.Builder(requireContext())
        .setTitle(R.string.delete_event)
        .setMessage(
            getString(
                R.string.delete_event_confirm, event.title, datetime
            )
        )
        .setPositiveButton(R.string.delete) { _, _ ->
          stockRoomViewModel.deleteEvent(event)
          Toast.makeText(
              requireContext(), getString(
              R.string.delete_event_msg, event.title, datetime
          ), Toast.LENGTH_LONG
          )
              .show()
        }
        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .show()
  }

  private val onlineDataTask = object : Runnable {
    override fun run() {
      stockRoomViewModel.runOnlineTask()
      onlineDataHandler.postDelayed(this, onlineDataTimerDelay)
    }
  }

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

    // Setup online data every 2s for regular hours.
    onlineDataHandler = Handler(Looper.getMainLooper())
    standardPortfolio = getString(R.string.standard_portfolio)

    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_stockdata, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    val onlineDataAdapter = OnlineDataAdapter(requireContext())
    onlineDataView.adapter = onlineDataAdapter

    // Set column number depending on screen width.
    val scale = 299
    val spanCount =
      (resources.configuration.screenWidthDp / (scale * resources.configuration.fontScale) + 0.5).roundToInt()

    onlineDataView.layoutManager = GridLayoutManager(
        requireContext(),
        Integer.min(Integer.max(spanCount, 1), 10)
    )

    var timeInSeconds5minUpdate = LocalDateTime.now()
        .toEpochSecond(ZoneOffset.UTC)
    var timeInSeconds24hUpdate = timeInSeconds5minUpdate

    // use requireActivity() instead of this to have only one shared viewmodel
    stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)

    stockRoomViewModel.onlineMarketDataList.observe(viewLifecycleOwner, Observer { data ->
      data?.let { onlineMarketDataList ->
        val onlineMarketData = onlineMarketDataList.find { onlineMarketDataItem ->
          onlineMarketDataItem.symbol == symbol
        }
        if (onlineMarketData != null) {
          updateHeader(onlineMarketData)
          onlineDataAdapter.updateData(onlineMarketData)

          // Update charts
          val timeInSecondsNow = LocalDateTime.now()
              .toEpochSecond(ZoneOffset.UTC)

          // Update daily and 5-day chart every 5min
          if (stockViewRange == StockViewRange.OneDay
              || stockViewRange == StockViewRange.FiveDays
          ) {
            if (timeInSecondsNow > timeInSeconds5minUpdate + 5 * 60) {
              timeInSeconds5minUpdate = timeInSecondsNow
              getData(stockViewRange)
              loadStockView(stockViewRange, stockViewMode)
            }
          } else {
            // Update other charts every day
            if (timeInSecondsNow > timeInSeconds24hUpdate + 24 * 60 * 60) {
              timeInSeconds24hUpdate = timeInSecondsNow
              getData(stockViewRange)
              loadStockView(stockViewRange, stockViewMode)
            }
          }

          /*
          val dividendText =
            if (onlineMarketData.annualDividendRate > 0.0 && onlineMarketData.annualDividendYield > 0.0) {
              "${DecimalFormat(DecimalFormat2To4Digits).format(
                  onlineMarketData.annualDividendRate
              )} (${DecimalFormat(DecimalFormat2To4Digits).format(
                  onlineMarketData.annualDividendYield * 100.0
              )}%)"
            } else {
              ""
            }

          textViewDividend.text = dividendText
        */
        }
      }
    })

    stockChartDataViewModel = ViewModelProvider(this).get(StockChartDataViewModel::class.java)

    stockChartDataViewModel.chartData.observe(viewLifecycleOwner, Observer { data ->
      if (data != null) {
        stockDataEntries = data.stockDataEntries
        setupCharts(stockViewRange, stockViewMode)
        loadCharts(data.symbol, stockViewRange, stockViewMode)
      }
    })

    textViewSymbol.text =
      SpannableStringBuilder().underline { color(Color.BLUE) { append(symbol) } }

    // Setup community pages menu
    textViewSymbol.setOnClickListener { viewSymbol ->
      val popupMenu = PopupMenu(requireContext(), viewSymbol)

      data class LinkListEntry
      (
        val linkType: LinkType,
        val link: String,
      )

      val linkList: Map<String, LinkListEntry> = mapOf(
          "Yahoo community" to LinkListEntry(
              linkType = LinkType.CommunityType,
              link = "https://finance.yahoo.com/quote/$symbol/community"
          ),
          "Stocktwits" to LinkListEntry(
              linkType = LinkType.CommunityType,
              link = "https://stocktwits.com/symbol/$symbol"
          ),
          "Bing" to LinkListEntry(
              linkType = LinkType.SearchType,
              link = "https://www.bing.com/search?q=$symbol%20stock"
          ),
          "Google" to LinkListEntry(
              linkType = LinkType.SearchType,
              link = "https://www.google.com/finance?q=$symbol"
          ),
          "Yahoo" to LinkListEntry(
              linkType = LinkType.SearchType,
              link = "https://finance.yahoo.com/quote/$symbol"
          ),
          "CNBC" to LinkListEntry(
              linkType = LinkType.WebsiteType,
              link = "https://www.cnbc.com/quotes/?symbol=$symbol"
          ),
          "CNN Money" to LinkListEntry(
              linkType = LinkType.WebsiteType,
              link = "http://money.cnn.com/quote/quote.html?symb=$symbol"
          ),
          "FinViz" to LinkListEntry(
              linkType = LinkType.WebsiteType,
              link = "https://finviz.com/quote.ashx?t=$symbol"
          ),
          "MarketBeat" to LinkListEntry(
              linkType = LinkType.WebsiteType,
              link = "https://www.marketbeat.com/stocks/$symbol/"
          ),
          "MarketWatch" to LinkListEntry(
              linkType = LinkType.WebsiteType,
              link = "https://www.marketwatch.com/investing/stock/$symbol/"
          ),
          "Nasdaq" to LinkListEntry(
              linkType = LinkType.WebsiteType,
              link = "https://www.nasdaq.com/market-activity/stocks/$symbol"
          ),
          "OTC Markets" to LinkListEntry(
              linkType = LinkType.WebsiteType,
              link = "https://otcmarkets.com/stock/$symbol/overview"
          ),
          "TD Ameritrade" to LinkListEntry(
              linkType = LinkType.WebsiteType,
              link = "https://research.tdameritrade.com/grid/public/research/stocks/calendar?symbol=$symbol"
          ),
          "TheStreet" to LinkListEntry(
              linkType = LinkType.WebsiteType,
              link = "https://www.thestreet.com/quote/$symbol"
          ),
          "Zacks" to LinkListEntry(
              linkType = LinkType.WebsiteType,
              link = "https://www.zacks.com/stock/quote/$symbol"
          )
      )

      var menuIndex: Int = Menu.FIRST

      val menuHeadlineItem = SpannableStringBuilder()
          .color(context?.getColor(R.color.colorPrimary)!!) {
            bold { append(getString(R.string.community_search_links)) }
          }

      popupMenu.menu.add(LinkType.HeadlineType.value, menuIndex++, Menu.NONE, menuHeadlineItem)

      // One group for each link type.
      linkList.forEach { (name, entry) ->
        popupMenu.menu.add(entry.linkType.value, menuIndex++, Menu.NONE, name)
      }

      MenuCompat.setGroupDividerEnabled(popupMenu.menu, true)
      popupMenu.show()

      popupMenu.setOnMenuItemClickListener { menuitem ->
        linkList[menuitem.toString()]?.let { openNewTabWindow(it.link, requireContext()) }

        true
      }
    }

/*
    stockdataLinearLayout.setOnTouchListener(object : OnSwipeTouchListener(requireContext()){
      override fun onSwipeRight() {
        super.onSwipeRight()

      }

      override fun onSwipeLeft() {
        super.onSwipeLeft()
      }
    })
*/
    /*

    stockdataLinearLayout.setOnTouchListener(object : OnSwipeTouchListener(requireContext()) {
      override fun onSwipeLeft() {
        super.onSwipeLeft()
        Toast.makeText(requireContext(), "Swipe Left gesture detected",
            Toast.LENGTH_SHORT)
            .show()
      }
      override fun onSwipeRight() {
        super.onSwipeRight()
        Toast.makeText(
            requireContext(),
            "Swipe Right gesture detected",
            Toast.LENGTH_SHORT
        ).show()
      }
      override fun onSwipeUp() {
        super.onSwipeUp()
        Toast.makeText(requireContext(), "Swipe up gesture detected", Toast.LENGTH_SHORT)
            .show()
      }
      override fun onSwipeDown() {
        super.onSwipeDown()
        Toast.makeText(requireContext(), "Swipe down gesture detected", Toast.LENGTH_SHORT)
            .show()
      }
    })
     */

    updateStockViewRange(stockViewRange)

    // Assets
    val assetClickListenerUpdate =
      { asset: Asset -> assetItemUpdateClicked(asset) }
    val assetClickListenerDelete =
      { symbol: String?, asset: Asset? -> assetItemDeleteClicked(symbol, asset) }
    val assetAdapter =
      AssetListAdapter(requireContext(), assetClickListenerUpdate, assetClickListenerDelete)
    assetsView.adapter = assetAdapter
    assetsView.layoutManager = LinearLayoutManager(requireContext())

    // Events
    val eventClickListenerUpdate =
      { event: Event -> eventItemUpdateClicked(event) }
    val eventClickListenerDelete =
      { event: Event -> eventItemDeleteClicked(event) }
    val eventAdapter =
      EventListAdapter(requireContext(), eventClickListenerUpdate, eventClickListenerDelete)
    eventsView.adapter = eventAdapter
    eventsView.layoutManager = LinearLayoutManager(requireContext())

    val stockDBLiveData: LiveData<StockDBdata> = stockRoomViewModel.getStockDBLiveData(symbol)
    stockDBLiveData.observe(viewLifecycleOwner, Observer { data ->
      if (data != null) {
        stockDBdata = data
        noteTextView.text = stockDBdata.note

        // Portfolio
        textViewPortfolio.text = if (stockDBdata.portfolio.isEmpty()) {
          standardPortfolio
        } else {
          stockDBdata.portfolio
        }
      }

      // Group color
      // color = 0 is not valid and not stored in the DB
      var color = stockDBdata.groupColor
      if (color == 0) {
        color = context?.getColor(R.color.backgroundListColor)!!
      }
      setBackgroundColor(textViewGroupColor, color)

      textViewGroup.text = if (stockDBdata.groupColor == 0) {
        getString(R.string.standard_group)
      } else {
        val group = stockRoomViewModel.getGroupSync(stockDBdata.groupColor)
        if (group.name.isEmpty()) {
          "Color code=0x${color.toHexString()}"
        } else {
          group.name
        }
      }

      alertAbove = stockDBdata.alertAbove
      alertBelow = stockDBdata.alertBelow

      if (alertAbove > 0.0 && alertBelow > 0.0 && alertBelow >= alertAbove) {
        alertAbove = 0.0
        alertBelow = 0.0
      }

      alertAboveInputEditText.setText(
          if (alertAbove > 0.0) {
            DecimalFormat(DecimalFormat2To4Digits).format(alertAbove)
          } else {
            ""
          }
      )
      alertAboveNoteInputEditText.setText(stockDBdata.alertAboveNote)

      alertBelowInputEditText.setText(
          if (alertBelow > 0.0) {
            DecimalFormat(DecimalFormat2To4Digits).format(alertBelow)
          } else {
            ""
          }
      )
      alertBelowNoteInputEditText.setText(stockDBdata.alertBelowNote)
    })

    // Use MediatorLiveView to combine the assets, stockDB and online data changes.
    assetChangeLiveData.addSource(stockDBLiveData) { value ->
      if (value != null) {
        assetChange.stockDBdata = value
        assetChangeLiveData.postValue(assetChange)
      }
    }

    // Update the current asset list.
    val assetsLiveData: LiveData<Assets> = stockRoomViewModel.getAssetsLiveData(symbol)
    assetsLiveData.observe(viewLifecycleOwner, Observer { data ->
      if (data != null) {
        assetAdapter.updateAssets(data.assets)

        // Reset when assets are changed.
        picker_knob.setValue(0.0, 100.0, 0.0)

//        val (totalQuantity, totalPrice) = getAssets(data.assets)
//
//        val visibility = if (totalPrice != 0.0) {
//          View.VISIBLE
//        } else {
//          View.GONE
//        }
//
//        alertGainDivider.visibility = visibility
//        alertGainAboveLayout.visibility = visibility
//        alertGainBelowLayout.visibility = visibility
//
//        alertLossDivider.visibility = visibility
//        alertLossAboveLayout.visibility = visibility
//        alertLossBelowLayout.visibility = visibility
      }
    })

    // Update the current event list.
    val eventsLiveData: LiveData<Events> = stockRoomViewModel.getEventsLiveData(symbol)
    eventsLiveData.observe(viewLifecycleOwner, Observer { data ->
      if (data != null) {
        eventAdapter.updateEvents(data.events)
      }
    })

    // Use MediatorLiveView to combine the assets and online data changes.
    assetChangeLiveData.addSource(assetsLiveData) { value ->
      if (value != null) {
        assetChange.assets = value
        assetChangeLiveData.postValue(assetChange)
      }
    }

    // online data is not stored in the DB (there is no stockRoomViewModel.getOnlineMarketLiveData(symbol))
    assetChangeLiveData.addSource(stockRoomViewModel.onlineMarketDataList) { value ->
      if (value != null) {
        val onlineMarketData = value.find { data ->
          data.symbol == symbol
        }
        if (onlineMarketData != null) {
          assetChange.onlineMarketData = onlineMarketData
          assetChangeLiveData.postValue(assetChange)
        }
      }
    }

    assetChangeLiveData.observe(viewLifecycleOwner, Observer { item ->
      if (item != null) {
        updateAssetChange(item)
      }
    })

    // Setup portfolio menu
    textViewPortfolio.setOnClickListener { viewPortfolio ->
      val popupMenu = PopupMenu(requireContext(), viewPortfolio)

      var menuIndex: Int = Menu.FIRST

      SharedRepository.portfolios.value?.sortedBy {
        it.toLowerCase(Locale.ROOT)
      }
          ?.forEach { portfolio ->
            val name = if (portfolio.isEmpty()) {
              // first entry in bold
              SpannableStringBuilder()
                  .bold { append(standardPortfolio) }
            } else {
              portfolio
            }
            popupMenu.menu.add(0, menuIndex++, Menu.NONE, name)
          }

      // Last-1 item is to add a new portfolio
      // Last item is to rename the portfolio
      val addPortfolioItem = SpannableStringBuilder()
          .color(context?.getColor(R.color.colorAccent)!!) {
            bold { append(getString(R.string.add_portfolio)) }
          }
      popupMenu.menu.add(0, menuIndex++, Menu.CATEGORY_CONTAINER, addPortfolioItem)

      // Display 'Rename portfolio' only for other than the standard portfolio.
      if (stockDBdata.portfolio.isNotEmpty()) {
        val renamePortfolioItem = SpannableStringBuilder()
            .color(context?.getColor(R.color.colorAccent)!!) {
              bold { append(getString(R.string.rename_portfolio)) }
            }
        popupMenu.menu.add(0, menuIndex++, Menu.CATEGORY_CONTAINER, renamePortfolioItem)
      }

      popupMenu.show()

      popupMenu.setOnMenuItemClickListener { menuitem ->
        val i = if (stockDBdata.portfolio.isNotEmpty()) {
          2
        } else {
          1
        }

        val addSelected = menuIndex - i == menuitem.itemId
        val renameSelected = menuIndex - i + 1 == menuitem.itemId

        if (addSelected || renameSelected) {
          // Add/Rename portfolio
          val builder = android.app.AlertDialog.Builder(requireContext())
          // Get the layout inflater
          val inflater = LayoutInflater.from(requireContext())

          // Inflate and set the layout for the dialog
          // Pass null as the parent view because its going in the dialog layout
          val dialogView = inflater.inflate(R.layout.dialog_add_portfolio, null)

          val portfolioHeaderView =
            dialogView.findViewById<TextView>(R.id.portfolioHeader)
          val portfolioTextView =
            dialogView.findViewById<TextView>(R.id.portfolioTextView)

          val selectedPortfolio =
            SharedRepository.selectedPortfolio.value ?: if (stockDBdata.portfolio.isEmpty()) {
              standardPortfolio
            } else {
              stockDBdata.portfolio
            }

          if (addSelected) {
            portfolioHeaderView.text = getString(R.string.add_portfolio)
            portfolioTextView.text = getString(R.string.portfolio_name_text)
          } else {
            portfolioHeaderView.text =
              getString(R.string.rename_portfolio_header, selectedPortfolio)
            portfolioTextView.text = getString(R.string.portfolio_rename_text)
          }
          val addNameView = dialogView.findViewById<TextView>(R.id.addPortfolioName)
          builder.setView(dialogView)
              // Add action buttons
              .setPositiveButton(
                  if (addSelected) {
                    R.string.add
                  } else {
                    R.string.rename
                  }
              ) { _, _ ->
                // Add () to avoid cast exception.
                val portfolioText = (addNameView.text).toString()
                    .trim()
                if (portfolioText.isEmpty() || portfolioText.compareTo(
                        standardPortfolio, true
                    ) == 0
                ) {
                  Toast.makeText(
                      requireContext(), getString(R.string.portfolio_name_not_empty),
                      Toast.LENGTH_LONG
                  )
                      .show()
                  return@setPositiveButton
                }

                textViewPortfolio.text = portfolioText

//                val portfolios = SharedRepository.portfolios.value
//                if (portfolios?.find {
//                      it.isEmpty()
//                    } == null) {
//                  portfolios?.add("")
//                }

                if (addSelected) {
                  stockRoomViewModel.setPortfolio(symbol, portfolioText)
//                  if (portfolios != null) {
//                    portfolios.add(portfolioText)
//                  }
                } else {
                  stockRoomViewModel.updatePortfolio(selectedPortfolio, portfolioText)
//                  if (portfolios != null) {
//                    portfolios.remove(selectedPortfolio)
//                    portfolios.add(portfolioText)
//                  }
                }

                //SharedRepository.portfolios.value = portfolios
                SharedRepository.selectedPortfolio.value = portfolioText
              }
              .setNegativeButton(
                  R.string.cancel
              ) { _, _ ->
              }
          builder
              .create()
              .show()
        } else {
          var portfolio = menuitem.title.trim()
              .toString()
          textViewPortfolio.text = portfolio

          if (portfolio == standardPortfolio) {
            portfolio = ""
          }

          stockRoomViewModel.setPortfolio(symbol, portfolio)
          SharedRepository.selectedPortfolio.value = portfolio
        }
        true
      }
    }

    stockgroupLayout.setOnClickListener { viewLayout ->
      val popupMenu = PopupMenu(requireContext(), viewLayout)

      var menuIndex: Int = Menu.FIRST
      stockRoomViewModel.getGroupsMenuList(getString(R.string.standard_group))
          .forEach {
            popupMenu.menu.add(0, menuIndex++, Menu.NONE, it)
          }

      popupMenu.show()

      val groups: List<Group> = stockRoomViewModel.getGroupsSync()
      popupMenu.setOnMenuItemClickListener { menuitem ->
        val i: Int = menuitem.itemId - 1
        val clr: Int
        val clrDB: Int
        val name: String

        if (i >= groups.size) {
          clr = context?.getColor(R.color.backgroundListColor)!!
          clrDB = 0
          name = getString(R.string.standard_group)
        } else {
          clr = groups[i].color
          clrDB = clr
          name = groups[i].name
        }

        // Set the preview color in the activity.
        setBackgroundColor(textViewGroupColor, clr)
        textViewGroup.text = name

        // Store the selection.
        stockRoomViewModel.setGroup(symbol, name, clrDB)
        true
      }
    }

    buttonOneDay.setOnClickListener {
      updateStockViewRange(StockViewRange.OneDay)
    }
    buttonFiveDays.setOnClickListener {
      updateStockViewRange(StockViewRange.FiveDays)
    }
    buttonOneMonth.setOnClickListener {
      updateStockViewRange(StockViewRange.OneMonth)
    }
    buttonThreeMonth.setOnClickListener {
      updateStockViewRange(StockViewRange.ThreeMonth)
    }
    buttonYTD.setOnClickListener {
      updateStockViewRange(StockViewRange.YTD)
    }
    buttonOneYear.setOnClickListener {
      updateStockViewRange(StockViewRange.OneYear)
    }
    buttonFiveYears.setOnClickListener {
      updateStockViewRange(StockViewRange.FiveYears)
    }
    buttonMax.setOnClickListener {
      updateStockViewRange(StockViewRange.Max)
    }

    imageButtonIconLine.setOnClickListener {
      updateStockViewMode(StockViewMode.Candle)
    }
    imageButtonIconCandle.setOnClickListener {
      updateStockViewMode(StockViewMode.Line)
    }

    splitAssetsButton.setOnClickListener {
      val assets = stockRoomViewModel.getAssetsSync(symbol)
      val (totalQuantity, totalPrice) = getAssets(assets?.assets)

//      val totalQuantity = assets?.assets?.sumByDouble {
//        it.shares
//      }
//          ?: 0.0

      if (totalQuantity == 0.0) {
        Toast.makeText(
            requireContext(), getString(R.string.no_total_quantity), Toast.LENGTH_LONG
        )
            .show()
      } else {
        val builder = AlertDialog.Builder(requireContext())
        // Get the layout inflater
        val inflater = LayoutInflater.from(requireContext())

        val dialogView = inflater.inflate(R.layout.dialog_split_asset, null)
        val splitRatioViewZ = dialogView.findViewById<TextView>(R.id.splitRatioZ)
        splitRatioViewZ.text = "1"
        val splitRatioViewN = dialogView.findViewById<TextView>(R.id.splitRatioN)

        builder.setView(dialogView)
            // Add action buttons
            .setPositiveButton(
                R.string.split
            ) { _, _ ->
              // Add () to avoid cast exception.
              val splitRatioTextZ = (splitRatioViewZ.text).toString()
                  .trim()
              val splitRatioTextN = (splitRatioViewN.text).toString()
                  .trim()
              if (splitRatioTextZ.isNotEmpty() && splitRatioTextN.isNotEmpty()) {
                var valid = true
                var splitRatioZ = 0.0
                try {
                  val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                  splitRatioZ = numberFormat.parse(splitRatioTextZ)!!
                      .toDouble()

                  if (splitRatioZ <= 0.0 || splitRatioZ > 20.0) {
                    valid = false
                  }

                } catch (e: Exception) {
                  valid = false
                }

                var splitRatioN = 0.0
                try {
                  val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                  splitRatioN = numberFormat.parse(splitRatioTextN)!!
                      .toDouble()

                  if (splitRatioN <= 0.0 || splitRatioN > 20.0) {
                    valid = false
                  }

                } catch (e: Exception) {
                  valid = false
                }

                if (valid && assets?.assets != null) {
                  var minQuantity = Double.MAX_VALUE
                  var minPrice = Double.MAX_VALUE
                  // split = 1/(Z/N)
                  val splitRatio = splitRatioN / splitRatioZ
                  assets.assets.forEach { asset ->
                    asset.quantity *= splitRatio
                    if (asset.price > 0) {
                      asset.price /= splitRatio
                    }
                    if (asset.quantity > 0) {
                      minQuantity = min(asset.quantity, minQuantity)
                      minPrice = min(asset.price, minPrice)
                    }
                  }

                  if (minQuantity >= 0.1 && minPrice >= 0.01) {
                    stockRoomViewModel.updateAssets(
                        symbol = symbol, assets = assets.assets
                    )
                  } else {
                    Toast.makeText(
                        requireContext(), if (minQuantity >= 0.1) {
                      getString(R.string.split_min_price)
                    } else {
                      getString(R.string.split_min_quantity)
                    }, Toast.LENGTH_LONG
                    )
                        .show()
                  }

                  //hideSoftInputFromWindow()
                } else {
                  Toast.makeText(
                      requireContext(), getString(R.string.invalid_split_entry), Toast.LENGTH_LONG
                  )
                      .show()
                }
              }
            }
            .setNegativeButton(
                R.string.cancel
            ) { _, _ ->
            }
        builder
            .create()
            .show()
      }
    }

    addAssetsButton.setOnClickListener {
      val builder = AlertDialog.Builder(requireContext())
      // Get the layout inflater
      val inflater = LayoutInflater.from(requireContext())

      // Inflate and set the layout for the dialog
      // Pass null as the parent view because its going in the dialog layout
      val dialogView = inflater.inflate(R.layout.dialog_add_asset, null)
      val addUpdateQuantityHeadlineView =
        dialogView.findViewById<TextView>(R.id.addUpdateQuantityHeadline)
      addUpdateQuantityHeadlineView.text = getString(R.string.add_asset)
      val addQuantityView = dialogView.findViewById<TextView>(R.id.addQuantity)
      val addPriceView = dialogView.findViewById<TextView>(R.id.addPrice)
      val addNoteView = dialogView.findViewById<TextView>(R.id.addNote)
      val datePickerAssetDateView = dialogView.findViewById<DatePicker>(R.id.datePickerAssetDate)

      builder.setView(dialogView)
          // Add action buttons
          .setPositiveButton(
              R.string.add
          ) { _, _ ->
            // Add () to avoid cast exception.
            val quantitytText = (addQuantityView.text).toString()
                .trim()
            var quantity = 0.0

            try {
              val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
              quantity = numberFormat.parse(quantitytText)!!
                  .toDouble()
            } catch (e: Exception) {
              Toast.makeText(
                  requireContext(), getString(R.string.asset_share_not_empty), Toast.LENGTH_LONG
              )
                  .show()
              return@setPositiveButton
            }
            if (quantity <= 0.0) {
              Toast.makeText(
                  requireContext(), getString(R.string.quantity_not_zero), Toast.LENGTH_LONG
              )
                  .show()
              return@setPositiveButton
            }

            val priceText = (addPriceView.text).toString()
                .trim()
            var price = 0.0
            try {
              val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
              price = numberFormat.parse(priceText)!!
                  .toDouble()
            } catch (e: Exception) {
              Toast.makeText(
                  requireContext(), getString(R.string.asset_price_not_empty), Toast.LENGTH_LONG
              )
                  .show()
              return@setPositiveButton
            }
            if (price <= 0.0) {
              Toast.makeText(
                  requireContext(), getString(R.string.price_not_zero), Toast.LENGTH_LONG
              )
                  .show()
              return@setPositiveButton
            }

            val localDateTime: LocalDateTime = LocalDateTime.of(
                datePickerAssetDateView.year, datePickerAssetDateView.month + 1,
                datePickerAssetDateView.dayOfMonth, 0, 0
            )
            val date = localDateTime.toEpochSecond(ZoneOffset.UTC)

            val noteText = (addNoteView.text).toString()
                .trim()

            //val date = LocalDateTime.now()
            //    .toEpochSecond(ZoneOffset.UTC)

            stockRoomViewModel.addAsset(
                Asset(
                    symbol = symbol,
                    quantity = quantity,
                    price = price,
                    date = date,
                    note = noteText
                )
            )
            val count: Int = when {
              quantity == 1.0 -> {
                1
              }
              quantity > 1.0 -> {
                quantity.toInt() + 1
              }
              else -> {
                0
              }
            }

            val pluralstr = resources.getQuantityString(
                R.plurals.asset_added, count, DecimalFormat(DecimalFormat0To4Digits).format(quantity),
                DecimalFormat(DecimalFormat2To4Digits).format(price)
            )
            Toast.makeText(requireContext(), pluralstr, Toast.LENGTH_LONG)
                .show()

            //hideSoftInputFromWindow()
          }
          .setNegativeButton(
              R.string.cancel
          ) { _, _ ->
          }
      builder
          .create()
          .show()
    }

    removeAssetButton.setOnClickListener {
      val assets = stockRoomViewModel.getAssetsSync(symbol)
      val (totalQuantity, totalPrice) = getAssets(assets?.assets)

//      val totalQuantity = assets?.assets?.sumByDouble {
//        it.shares
//      }
//          ?: 0.0

      if (totalQuantity == 0.0) {
        Toast.makeText(
            requireContext(), getString(R.string.no_total_quantity), Toast.LENGTH_LONG
        )
            .show()
      } else {
        val builder = AlertDialog.Builder(requireContext())
        // Get the layout inflater
        val inflater = LayoutInflater.from(requireContext())

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        val dialogView = inflater.inflate(R.layout.dialog_remove_asset, null)
        val removeQuantityView = dialogView.findViewById<TextView>(R.id.removeQuantity)
        val removePriceView = dialogView.findViewById<TextView>(R.id.removePrice)
        val removeNoteView = dialogView.findViewById<TextView>(R.id.removeNote)
        val datePickerAssetDateView = dialogView.findViewById<DatePicker>(R.id.datePickerAssetDate)

        builder.setView(dialogView)
            // Add action buttons
            .setPositiveButton(
                R.string.delete
            ) { _, _ ->
              val quantityText = (removeQuantityView.text).toString()
                  .trim()
              var quantity = 0.0

              try {
                val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                quantity = numberFormat.parse(quantityText)!!
                    .toDouble()
              } catch (e: Exception) {
                Toast.makeText(
                    requireContext(), getString(R.string.asset_share_not_empty), Toast.LENGTH_LONG
                )
                    .show()
                return@setPositiveButton
              }
              if (quantity <= 0.0) {
                Toast.makeText(
                    requireContext(), getString(R.string.quantity_not_zero), Toast.LENGTH_LONG
                )
                    .show()
                return@setPositiveButton
              }

              val priceText = (removePriceView.text).toString()
                  .trim()
              var price = 0.0
              try {
                val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                price = numberFormat.parse(priceText)!!
                    .toDouble()
              } catch (e: Exception) {
                Toast.makeText(
                    requireContext(), getString(R.string.asset_price_not_empty), Toast.LENGTH_LONG
                )
                    .show()
                return@setPositiveButton
              }
              if (price <= 0.0) {
                Toast.makeText(
                    requireContext(), getString(R.string.price_not_zero), Toast.LENGTH_LONG
                )
                    .show()
                return@setPositiveButton
              }

              // Send msg and adjust if more shares than owned are removed.
              if (quantity > totalQuantity) {
                Toast.makeText(
                    requireContext(), getString(
                    R.string.removed_quantity_exceed_existing,
                    DecimalFormat(DecimalFormat0To4Digits).format(quantity),
                    DecimalFormat(DecimalFormat0To4Digits).format(totalQuantity)
                ), Toast.LENGTH_LONG
                )
                    .show()
                quantity = totalQuantity
              }

              val localDateTime: LocalDateTime = LocalDateTime.of(
                  datePickerAssetDateView.year, datePickerAssetDateView.month + 1,
                  datePickerAssetDateView.dayOfMonth, 0, 0
              )
              val date = localDateTime.toEpochSecond(ZoneOffset.UTC)

              val noteText = (removeNoteView.text).toString()
                  .trim()

              //val date = LocalDateTime.now()
              //    .toEpochSecond(ZoneOffset.UTC)

              // Add negative shares for removed asset.
              stockRoomViewModel.addAsset(
                  Asset(
                      symbol = symbol,
                      quantity = -quantity,
                      price = price,
                      date = date,
                      note = noteText
                  )
              )
              val count: Int = when {
                quantity == 1.0 -> {
                  1
                }
                quantity > 1.0 -> {
                  quantity.toInt() + 1
                }
                else -> {
                  0
                }
              }

              val pluralstr = resources.getQuantityString(
                  R.plurals.asset_removed, count, DecimalFormat(DecimalFormat0To4Digits).format(quantity)
              )
              Toast.makeText(requireContext(), pluralstr, Toast.LENGTH_LONG)
                  .show()
            }
            .setNegativeButton(
                R.string.cancel
            ) { _, _ ->
            }
        builder
            .create()
            .show()
      }
    }

/*
    removeAssetButton.setOnClickListener {
      val assets = stockRoomViewModel.getAssetsSync(symbol)
      val totalShares = assets?.assets?.sumByDouble {
        it.shares
      }
          ?: 0.0

      if (totalShares == 0.0) {
        Toast.makeText(
            requireContext(), getString(R.string.no_total_shares), Toast.LENGTH_LONG
        )
            .show()
      } else {
        val builder = AlertDialog.Builder(requireContext())
        // Get the layout inflater
        val inflater = LayoutInflater.from(requireContext())

        val dialogView = inflater.inflate(R.layout.dialog_remove_asset, null)
        val removeSharesView = dialogView.findViewById<TextView>(R.id.removeShares)

        builder.setView(dialogView)
            // Add action buttons
            .setPositiveButton(
                R.string.delete
            ) { _, _ ->
              // Add () to avoid cast exception.
              val removeSharesText = (removeSharesView.text).toString()
                  .trim()
              if (removeSharesText.isNotEmpty()) {
                var shares = 0.0
                var valid = true
                try {
                  val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                  shares = numberFormat.parse(removeSharesText)!!
                      .toDouble()
                } catch (e: Exception) {
                  valid = false
                }

                if (valid) {
                  // Avoid wrong data due to rounding errors.
                  val totalPaidPrice = assets?.assets?.sumByDouble {
                    it.shares * it.price
                  } ?: 0.0
                  val averagePrice = totalPaidPrice / totalShares

                  if (shares > (totalShares + epsilon)) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.remove_shares_exceeds_total_shares),
                        Toast.LENGTH_LONG
                    )
                        .show()
                  } else {
                    //assetSummary.removeAllViews()
                    val newTotal: Double = totalPaidPrice - shares * averagePrice
                    val shareAdjustment: Double = newTotal / totalPaidPrice

                    assets?.assets?.forEach { asset ->
                      asset.shares *= shareAdjustment
                    }
                    stockRoomViewModel.updateAssets(
                        symbol = symbol, assets = assets?.assets!!
                    )
                  }
                }

                //hideSoftInputFromWindow()
              } else {
                Toast.makeText(
                    requireContext(), getString(R.string.invalid_entry), Toast.LENGTH_LONG
                )
                    .show()
              }
            }
            .setNegativeButton(
                R.string.cancel
            ) { _, _ ->
            }
        builder
            .create()
            .show()
      }
    }
*/

    addEventsButton.setOnClickListener {
      val builder = AlertDialog.Builder(requireContext())
      // Get the layout inflater
      val inflater = LayoutInflater.from(requireContext())

      // Inflate and set the layout for the dialog
      // Pass null as the parent view because its going in the dialog layout
      val dialogView = inflater.inflate(R.layout.dialog_add_event, null)
      val eventHeadlineView = dialogView.findViewById<TextView>(R.id.eventHeadline)
      eventHeadlineView.text = getString(R.string.add_event)
      val textInputEditEventTitleView =
        dialogView.findViewById<TextView>(R.id.textInputEditEventTitle)
      val textInputEditEventNoteView =
        dialogView.findViewById<TextView>(R.id.textInputEditEventNote)
      val datePickerEventDateView = dialogView.findViewById<DatePicker>(R.id.datePickerEventDate)
      val datePickerEventTimeView = dialogView.findViewById<TimePicker>(R.id.datePickerEventTime)

      builder.setView(dialogView)
          // Add action buttons
          .setPositiveButton(
              R.string.add
          ) { _, _ ->
            // Add () to avoid cast exception.
            val title = (textInputEditEventTitleView.text).toString()
                .trim()
            // add new event
            if (title.isEmpty()) {
              Toast.makeText(requireContext(), getString(R.string.event_empty), Toast.LENGTH_LONG)
                  .show()
            } else {
              val note = (textInputEditEventNoteView.text).toString()
              val datetime: LocalDateTime = LocalDateTime.of(
                  datePickerEventDateView.year, datePickerEventDateView.month + 1,
                  datePickerEventDateView.dayOfMonth, datePickerEventTimeView.hour,
                  datePickerEventTimeView.minute
              )
              val seconds = datetime.toEpochSecond(ZoneOffset.UTC)
              stockRoomViewModel.addEvent(
                  Event(symbol = symbol, type = 0, title = title, note = note, datetime = seconds)
              )
              Toast.makeText(
                  requireContext(), getString(
                  R.string.event_added, title, datetime.format(
                  DateTimeFormatter.ofLocalizedDateTime(
                      MEDIUM
                  )
              )
              ), Toast.LENGTH_LONG
              )
                  .show()
            }
          }
          .setNegativeButton(
              R.string.cancel
          ) { _, _ ->
          }
      builder
          .create()
          .show()
    }

    updateNoteButton.setOnClickListener {
      updateNote()
    }
    noteTextView.setOnClickListener {
      updateNote()
    }

    alertAboveInputEditText.addTextChangedListener(
        object : TextWatcher {
          override fun afterTextChanged(s: Editable?) {
            alertAboveInputLayout.error = ""
            var valid: Boolean = true
            if (s != null) {
              try {
                val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                alertAbove = numberFormat.parse(s.toString())
                    .toDouble()
              } catch (e: NumberFormatException) {
                alertAboveInputLayout.error = getString(R.string.invalid_number)
                valid = false
              } catch (e: Exception) {
                valid = false
              }

              if (valid && alertAbove == 0.0) {
                alertAboveInputLayout.error = getString(R.string.invalid_number)
                valid = false
              }
              if (valid && alertAbove > 0.0 && alertBelow > 0.0) {
                if (valid && alertBelow >= alertAbove) {
                  alertAboveInputLayout.error = getString(R.string.alert_below_error)
                  valid = false
                }
              }

              if (!valid) {
                alertAbove = 0.0
              }
            }
          }

          override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int
          ) {
          }

          override fun onTextChanged(
            s: CharSequence?,
            start: Int,
            before: Int,
            count: Int
          ) {
          }
        })

    alertBelowInputEditText.addTextChangedListener(
        object : TextWatcher {
          override fun afterTextChanged(s: Editable?) {
            alertBelowInputLayout.error = ""
            var valid: Boolean = true
            if (s != null) {
              try {
                val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                alertBelow = numberFormat.parse(s.toString())
                    .toDouble()
              } catch (e: NumberFormatException) {
                alertBelowInputLayout.error = getString(R.string.invalid_number)
                valid = false
              } catch (e: Exception) {
                valid = false
              }
            }

            if (valid && alertBelow == 0.0) {
              alertBelowInputLayout.error = getString(R.string.invalid_number)
              valid = false
            }
            if (valid && alertAbove > 0.0 && alertBelow > 0.0) {
              if (valid && alertBelow >= alertAbove) {
                alertBelowInputLayout.error = getString(R.string.alert_above_error)
                valid = false
              }
            }

            if (!valid) {
              alertBelow = 0.0
            }
          }

          override fun beforeTextChanged(
            s: CharSequence?,
            start: Int,
            count: Int,
            after: Int
          ) {
          }

          override fun onTextChanged(
            s: CharSequence?,
            start: Int,
            before: Int,
            count: Int
          ) {
          }
        })
  }

  override fun onPause() {
    updateAlerts()
    onlineDataHandler.removeCallbacks(onlineDataTask)
    super.onPause()
  }

  override fun onResume() {
    super.onResume()
    onlineDataHandler.post(onlineDataTask)
    stockRoomViewModel.runOnlineTaskNow()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menu_sync -> {
        onSync()
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun onSync() {
    stockRoomViewModel.runOnlineTaskNow("Request to get online data manually.")
    updateStockViewRange(stockViewRange)
  }

//  private fun hideSoftInputFromWindow() {
//    val view = activity?.currentFocus
//    if (view is TextView) {
//      val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//      imm.hideSoftInputFromWindow(view.windowToken, 0)
//    }
//  }

  private fun updateNote() {
    val builder = AlertDialog.Builder(requireContext())
    // Get the layout inflater
    val inflater = LayoutInflater.from(requireContext())

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    val dialogView = inflater.inflate(R.layout.dialog_add_note, null)
    val textInputEditNoteView =
      dialogView.findViewById<TextView>(R.id.textInputEditNote)

    val note = noteTextView.text
    textInputEditNoteView.text = note

    builder.setView(dialogView)
        // Add action buttons
        .setPositiveButton(
            R.string.add
        ) { _, _ ->
          // Add () to avoid cast exception.
          val noteText = (textInputEditNoteView.text).toString()

          if (noteText != note) {
            noteTextView.text = noteText
            stockRoomViewModel.updateNote(symbol, noteText)

            if (noteText.isEmpty()) {
              Toast.makeText(
                  requireContext(), getString(R.string.note_deleted), Toast.LENGTH_LONG
              )
                  .show()
            } else {
              Toast.makeText(
                  requireContext(), getString(
                  R.string.note_added, noteText
              ), Toast.LENGTH_LONG
              )
                  .show()
            }
          }
        }
        .setNegativeButton(
            R.string.cancel
        ) { _, _ ->
        }
    builder
        .create()
        .show()
  }

  private fun updateHeader(onlineMarketData: OnlineMarketData?) {
    var name: String = ""
    val marketPrice = SpannableStringBuilder()
    val marketChange = SpannableStringBuilder()

    if (onlineMarketData != null) {
      name = getName(onlineMarketData)

      val marketValues = getMarketValues(onlineMarketData)

      val marketChangeStr = SpannableStringBuilder().color(
          getChangeColor(
              onlineMarketData.marketChange,
              onlineMarketData.postMarketData,
              Color.DKGRAY,
              requireContext()
          )
      ) { append("${marketValues.second} ${marketValues.third}") }

      if (onlineMarketData.postMarketData) {
        marketPrice.italic { append(marketValues.first) }
        marketChange.italic { append(marketChangeStr) }
      } else {
        marketPrice.append(marketValues.first)
        marketChange.append(marketChangeStr)
      }
    }

    textViewName.text = name
    textViewMarketPrice.text = marketPrice
    textViewChange.text = marketChange

    val quoteSourceName = onlineMarketData?.quoteSourceName ?: ""
    textViewMarketPriceDelayed.text = when (quoteSourceName) {
      "Delayed Quote" -> {
        getString(R.string.delayed_quote)
      }
      "Nasdaq Real Time Price" -> {
        getString(R.string.nasdaq_real_time_price)
      }
      else -> {
        ""
      }
    }
  }

  private fun updateAssetChange(data: StockAssetsLiveData) {
    if (data.assets != null && data.onlineMarketData != null) {

      val assets: List<Asset> = data.assets?.assets!!
      val (totalQuantity, totalPrice) = getAssets(assets)

      val marketPrice = data.onlineMarketData!!.marketPrice

      if (totalPrice > 0.0 && marketPrice > 0.0) {

        price_preview_divider.visibility = View.VISIBLE
        price_preview_textview.visibility = View.VISIBLE
        price_preview_layout.visibility = View.VISIBLE

        // Update the new price and asset.
        picker_knob.onValueChangeListener { value ->
          new_stock_price.text = if (marketPrice > 5.0) {
            DecimalFormat(DecimalFormat2Digits).format(value)
          } else {
            DecimalFormat(DecimalFormat2To4Digits).format(value)
          }

          val assetChange = getAssetChange(
              assets,
              value,
              data.onlineMarketData?.postMarketData!!,
              Color.DKGRAY,
              requireActivity()
          )

          val asset = SpannableStringBuilder()
              .append(assetChange.second)
              .append("\n")
              .bold {
                append(DecimalFormat(DecimalFormat2Digits).format(totalQuantity * value))
              }

          new_total_asset.text = asset
        }

        // min, max, start
        picker_knob.setValue(marketPrice / 10, 4 * marketPrice, marketPrice)
      } else {
        price_preview_divider.visibility = View.GONE
        price_preview_textview.visibility = View.GONE
        price_preview_layout.visibility = View.GONE
      }

      val purchasePrice = if (totalPrice > 0.0) {
        getString(
            R.string.bought_for,
            DecimalFormat(DecimalFormat0To4Digits).format(totalQuantity),
            DecimalFormat(DecimalFormat2To4Digits).format(totalPrice / totalQuantity)
        )
      } else {
        ""
      }

      textViewAssetChange.text = if (purchasePrice.isNotEmpty()) {
        getAssetChange(
            assets,
            data.onlineMarketData?.marketPrice!!,
            data.onlineMarketData?.postMarketData!!,
            Color.DKGRAY,
            requireActivity()
        ).second
      } else {
        ""
      }

      textViewPurchasePrice.text = purchasePrice
    }
  }

  private fun updateAlerts() {
    // If both are set, below value must be smaller than the above value.
    if (!(alertAbove > 0.0 && alertBelow > 0.0 && alertBelow >= alertAbove)) {

      // Add () to avoid cast exception.
      val alertAboveNote = (alertAboveNoteInputEditText.text).toString()

      if (stockDBdata.alertAbove != alertAbove || stockDBdata.alertAboveNote != alertAboveNote) {
        stockRoomViewModel.updateAlertAboveSync(symbol, alertAbove, alertAboveNote)
      }

      // Add () to avoid cast exception.
      val alertBelowNote = (alertBelowNoteInputEditText.text).toString()

      if (stockDBdata.alertBelow != alertBelow || stockDBdata.alertBelowNote != alertBelowNote) {
        stockRoomViewModel.updateAlertBelowSync(symbol, alertBelow, alertBelowNote)
      }
    }
  }

  private fun updateStockViewRange(_stockViewRange: StockViewRange) {
    stockDataEntries = null
    updateStockView(_stockViewRange, stockViewMode)
  }

  private fun updateStockViewMode(
    _stockViewMode: StockViewMode
  ) {
    updateStockView(stockViewRange, _stockViewMode)
  }

  private fun updateStockView(
    _stockViewRange: StockViewRange,
    _stockViewMode: StockViewMode
  ) {
    stockViewMode = _stockViewMode
    //AppPreferences.INSTANCE.stockViewMode = stockViewMode

    stockViewRange = _stockViewRange
    //AppPreferences.INSTANCE.stockViewRange = stockViewRange

    setupCharts(stockViewRange, stockViewMode)
    loadStockView(stockViewRange, stockViewMode)
  }

  private fun loadStockView(
    stockViewRange: StockViewRange,
    stockViewMode: StockViewMode
  ) {
    if (stockDataEntries == null) {
      getData(stockViewRange)
    } else {
      loadCharts(symbol, stockViewRange, stockViewMode)
    }
  }

  private fun getData(stockViewRange: StockViewRange) {
    stockChartDataViewModel.getChartData(symbol, stockViewRange)
  }

  private fun setupCharts(
    stockViewRange: StockViewRange,
    stockViewMode: StockViewMode
  ) {
    updateButtons(stockViewRange, stockViewMode)

    when (stockViewMode) {
      StockViewMode.Line -> {
        setupLineChart()
      }
      StockViewMode.Candle -> {
        setupCandleStickChart()
      }
    }
  }

  private fun loadCharts(
    symbol: String,
    stockViewRange: StockViewRange,
    stockViewMode: StockViewMode
  ) {
    when (stockViewMode) {
      StockViewMode.Line -> {
        loadLineChart(symbol, stockViewRange)
      }
      StockViewMode.Candle -> {
        loadCandleStickChart(symbol, stockViewRange)
      }
    }
  }

  private val rangeButtons: List<Button> by lazy {
    listOf<Button>(
        buttonOneDay,
        buttonFiveDays,
        buttonOneMonth,
        buttonThreeMonth,
        buttonYTD,
        buttonOneYear,
        buttonFiveYears,
        buttonMax
    )
  }

  // Setup formatter for X and Y Axis and data slider.
  private val axisTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedTime(SHORT)
  private val axisDateTimeFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDateTime(LONG, SHORT)
  private val xAxisDateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(SHORT)
  private val axisDateFormatter: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(LONG)

  private val xAxisFormatter: ValueFormatter
    get() =
      when (stockViewRange) {
        StockViewRange.OneDay -> {
          IndexAxisValueFormatter(stockDataEntries!!.map { stockDataEntry ->
            val date =
              LocalDateTime.ofEpochSecond(
                  stockDataEntry.dateTimePoint, 0, ZoneOffset.UTC
              )
            date.format(axisTimeFormatter)
          })
        }
        else -> {
          IndexAxisValueFormatter(stockDataEntries!!.map { stockDataEntry ->
            val date =
              LocalDateTime.ofEpochSecond(
                  stockDataEntry.dateTimePoint, 0, ZoneOffset.UTC
              )
            date.format(xAxisDateFormatter)
          })
        }
      }

  private fun updateButtons(
    stockViewRange: StockViewRange,
    stockViewMode: StockViewMode
  ) {
    rangeButtons.forEach { button ->
      button.isEnabled = true
    }

    when (stockViewRange) {
      StockViewRange.OneDay -> {
        buttonOneDay.isEnabled = false
        textViewRange.text = getString(R.string.one_day_range)
      }
      StockViewRange.FiveDays -> {
        buttonFiveDays.isEnabled = false
        textViewRange.text = getString(R.string.five_days_range)
      }
      StockViewRange.OneMonth -> {
        buttonOneMonth.isEnabled = false
        textViewRange.text = getString(R.string.one_month_range)
      }
      StockViewRange.ThreeMonth -> {
        buttonThreeMonth.isEnabled = false
        textViewRange.text = getString(R.string.three_month_range)
      }
      StockViewRange.YTD -> {
        buttonYTD.isEnabled = false
        textViewRange.text = getString(R.string.ytd_range)
      }
      StockViewRange.OneYear -> {
        buttonOneYear.isEnabled = false
        textViewRange.text = getString(R.string.one_year_range)
      }
      StockViewRange.FiveYears -> {
        buttonFiveYears.isEnabled = false
        textViewRange.text = getString(R.string.five_year_range)
      }
      StockViewRange.Max -> {
        buttonMax.isEnabled = false
        textViewRange.text = getString(R.string.max_range)
      }
    }

    when (stockViewMode) {
      StockViewMode.Line -> {
        lineChart.visibility = View.VISIBLE
        candleStickChart.visibility = View.GONE
        imageButtonIconLine.visibility = View.VISIBLE
        imageButtonIconCandle.visibility = View.GONE
      }
      StockViewMode.Candle -> {
        lineChart.visibility = View.GONE
        candleStickChart.visibility = View.VISIBLE
        imageButtonIconLine.visibility = View.GONE
        imageButtonIconCandle.visibility = View.VISIBLE
      }
    }
  }

  private fun setupCandleStickChart() {
    val candleStickChart
        : CandleStickChart = view?.findViewById(R.id.candleStickChart)!!
    candleStickChart.isDoubleTapToZoomEnabled = false

    candleStickChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
    candleStickChart.xAxis.setDrawAxisLine(true)
    candleStickChart.xAxis.setDrawGridLines(false)

    candleStickChart.axisRight.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
    candleStickChart.axisRight.setDrawAxisLine(true)
    candleStickChart.axisRight.setDrawGridLines(true)
    candleStickChart.axisRight.isEnabled = true

    candleStickChart.axisLeft.setDrawGridLines(false)
    candleStickChart.axisLeft.setDrawAxisLine(false)
    candleStickChart.axisLeft.isEnabled = false

    candleStickChart.extraBottomOffset = resources.getDimension(R.dimen.graph_bottom_offset)
    candleStickChart.legend.isEnabled = false
    candleStickChart.description = null
    candleStickChart.setNoDataText("")
  }

  private fun loadCandleStickChart(
    symbol: String,
    stockViewRange: StockViewRange
  ) {
    val candleStickChart: CandleStickChart = view?.findViewById(R.id.candleStickChart)!!
    candleStickChart.candleData?.clearValues()

    val candleEntries: MutableList<CandleEntry> = mutableListOf()

    if (stockDataEntries != null && stockDataEntries!!.isNotEmpty()) {
      stockDataEntries!!.forEach { stockDataEntry ->
        candleEntries.add(stockDataEntry.candleEntry)
      }

      val series = CandleDataSet(candleEntries, symbol)
      series.color = Color.rgb(0, 0, 255)
      series.shadowColor = Color.rgb(255, 255, 0)
      series.shadowWidth = 1f
      series.decreasingColor = Color.rgb(255, 0, 0)
      series.decreasingPaintStyle = Paint.Style.FILL
      series.increasingColor = Color.rgb(0, 255, 0)
      series.increasingPaintStyle = Paint.Style.FILL
      series.neutralColor = Color.LTGRAY
      series.setDrawValues(false)

      val candleData = CandleData(series)
      candleStickChart.data = candleData
      candleStickChart.xAxis.valueFormatter = xAxisFormatter
      val digits = if (candleData.yMax < 1.0) {
        4
      } else {
        2
      }
      candleStickChart.axisRight.valueFormatter = DefaultValueFormatter(digits)
    } else {
      // Unlike the line chart, the candle chart needs at least one entry.
      // Set the data to null for empty data set.
      candleStickChart.data = null
    }

    when (stockViewRange) {
      StockViewRange.OneDay -> {
        candleStickChart.marker =
          TextMarkerViewCandleChart(requireContext(), axisTimeFormatter, stockDataEntries)
      }
      StockViewRange.FiveDays, StockViewRange.OneMonth -> {
        candleStickChart.marker =
          TextMarkerViewCandleChart(requireContext(), axisDateTimeFormatter, stockDataEntries)
      }
      else -> {
        candleStickChart.marker =
          TextMarkerViewCandleChart(requireContext(), axisDateFormatter, stockDataEntries)
      }
    }

    candleStickChart.invalidate()
  }

  private fun setupLineChart() {
    val lineChart: LineChart = view?.findViewById(R.id.lineChart)!!
    lineChart.isDoubleTapToZoomEnabled = false

    lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
    lineChart.xAxis.setDrawAxisLine(true)
    lineChart.xAxis.setDrawGridLines(false)

    lineChart.axisRight.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
    lineChart.axisRight.setDrawAxisLine(true)
    lineChart.axisRight.setDrawGridLines(true)
    lineChart.axisRight.isEnabled = true

    lineChart.axisLeft.setDrawAxisLine(false)
    lineChart.axisLeft.isEnabled = false

    lineChart.extraBottomOffset = resources.getDimension(R.dimen.graph_bottom_offset)
    lineChart.legend.isEnabled = false
    lineChart.description = null
    lineChart.setNoDataText("")
  }

  private fun loadLineChart(
    symbol: String,
    stockViewRange: StockViewRange
  ) {
    val lineChart: LineChart = view?.findViewById(R.id.lineChart)!!
    lineChart.setNoDataText("")
    lineChart.lineData?.clearValues()

    val dataPoints = ArrayList<DataPoint>()
    if (stockDataEntries != null && stockDataEntries!!.isNotEmpty()) {
      stockDataEntries!!.forEach { stockDataEntry ->
        dataPoints.add(DataPoint(stockDataEntry.candleEntry.x, stockDataEntry.candleEntry.y))
      }
    }

    val series = LineDataSet(dataPoints as List<Entry>?, symbol)

    series.setDrawHorizontalHighlightIndicator(false)
    series.setDrawValues(false)
    series.setDrawFilled(true)
    series.setDrawCircles(false)

    val lineData = LineData(series)

    lineChart.data = lineData
    lineChart.xAxis.valueFormatter = xAxisFormatter
    val digits = if (lineData.yMax < 1.0) {
      4
    } else {
      2
    }
    lineChart.axisRight.valueFormatter = DefaultValueFormatter(digits)

    when (stockViewRange) {
      StockViewRange.OneDay -> {
        lineChart.marker =
          TextMarkerViewLineChart(requireContext(), axisTimeFormatter, stockDataEntries)
      }
      StockViewRange.FiveDays, StockViewRange.OneMonth -> {
        lineChart.marker =
          TextMarkerViewLineChart(requireContext(), axisDateTimeFormatter, stockDataEntries)
      }
      else -> {
        lineChart.marker =
          TextMarkerViewLineChart(requireContext(), axisDateFormatter, stockDataEntries)
      }
    }

    lineChart.invalidate()
  }
}
