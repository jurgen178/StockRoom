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

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
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
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.net.toUri
import androidx.core.text.*
import androidx.core.view.MenuCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.github.mikephil.charting.charts.CandleStickChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.CandleData
import com.github.mikephil.charting.data.CandleDataSet
import com.github.mikephil.charting.data.CandleEntry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ICandleDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.thecloudsite.stockroom.MainActivity.Companion.onlineDataTimerDelay
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.database.Assets
import com.thecloudsite.stockroom.database.Event
import com.thecloudsite.stockroom.database.Events
import com.thecloudsite.stockroom.database.Group
import com.thecloudsite.stockroom.database.StockDBdata
import com.thecloudsite.stockroom.databinding.*
import com.thecloudsite.stockroom.utils.*
import okhttp3.internal.toHexString
import java.lang.Double.min
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.LONG
import java.time.format.FormatStyle.MEDIUM
import java.time.format.FormatStyle.SHORT
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

const val EXTRA_SYMBOL = "com.thecloudsite.stockroom.STOCKSYMBOL"
const val EXTRA_TYPE = "com.thecloudsite.stockroom.STOCKTYPE"
const val EXTRA_SETSTARTFRAGMENT = "com.thecloudsite.stockroom.SETSTARTFRAGMENT"

// Enabled/Disabled overlay symbols not per instance
var chartOverlaySymbolsEnableList: MutableList<Boolean> =
    MutableList(MaxChartOverlays) { true }

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
    WebsiteTypeCrypto(3),
    WebsiteTypeStock(4),
    WebsiteGeneralType(5),
}

data class StockAssetsLiveData(
    var assets: Assets? = null,
    var stockDBdata: StockDBdata? = null,
    var onlineMarketData: OnlineMarketData? = null
)

data class AssetsTimeData(
    var date: Long = 0L,
    var price: Double = 0.0,
    var quantity: Double = 0.0,
    var account: String = ""
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

    // https://github.com/PhilJay/MPAndroidChart/issues/2917
    override fun drawMarkers(canvas: Canvas?) {
        try {
            super.drawMarkers(canvas)
        } catch (e: Exception) {
        }
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

    // https://github.com/PhilJay/MPAndroidChart/issues/2917
    override fun drawMarkers(canvas: Canvas?) {
        try {
            super.drawMarkers(canvas)
        } catch (e: Exception) {
        }
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

    private var _binding: FragmentStockdataBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var stockChartDataViewModel: StockChartDataViewModel
    private lateinit var stockRoomViewModel: StockRoomViewModel
    private var chartDataItems: HashMap<String, List<StockDataEntry>?> = hashMapOf()
    private val symbolTypesMap = HashMap<String, DataProvider>()

    private val assetChange = StockAssetsLiveData()
    private val assetChangeLiveData = MediatorLiveData<StockAssetsLiveData>()

    companion object {
        fun newInstance() = StockDataFragment()
    }

    private var stockDBdata = StockDBdata(symbol = "")
    private var stockDataEntries: List<StockDataEntry>? = null
    private var assetTimeEntries: MutableList<AssetsTimeData> = mutableListOf()
    private var symbol: String = ""
    private var type: DataProvider = DataProvider.Standard

    private var purchasePriceValue = 0.0
    private var purchaseQuantity = 0.0

    private var isOnline: Boolean = false

    private var alertAbove: Double = 0.0
    private var alertBelow: Double = 0.0
    private var marketPriceForDefaultAlertValue: Double = 0.0

    private lateinit var standardPortfolio: String

    lateinit var onlineDataHandler: Handler

    // Settings.
    private val settingStockViewRange = "SettingStockViewRange"
    private var stockViewRange: StockViewRange
        get() {
            val sharedPref =
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    ?: return StockViewRange.OneDay
            val index = sharedPref.getInt(settingStockViewRange, StockViewRange.OneDay.value)
            return if (index >= 0 && index < StockViewRange.values().size) {
                StockViewRange.values()[index]
            } else {
                StockViewRange.OneDay
            }
        }
        set(value) {
            val sharedPref =
                PreferenceManager.getDefaultSharedPreferences(requireContext()) ?: return
            with(sharedPref.edit()) {
                putInt(settingStockViewRange, value.value)
                commit()
            }
        }

    private val settingStockViewMode = "SettingStockViewMode"
    private var stockViewMode: StockViewMode
        get() {
            val sharedPref =
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    ?: return StockViewMode.Line
            val index = sharedPref.getInt(settingStockViewMode, StockViewMode.Line.value)
            return if (index >= 0 && index < StockViewMode.values().size) {
                StockViewMode.values()[index]
            } else {
                StockViewMode.Line
            }
        }
        set(value) {
            val sharedPref =
                PreferenceManager.getDefaultSharedPreferences(requireContext()) ?: return
            with(sharedPref.edit()) {
                putInt(settingStockViewMode, value.value)
                commit()
            }
        }

    private val settingOverlaySymbols = "chart_overlay_symbols"
    private var chartOverlaySymbols: String
        get() {
            val sharedPref =
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    ?: return settingChartOverlaySymbolsDefault
            return sharedPref.getString(settingOverlaySymbols, settingChartOverlaySymbolsDefault)
                ?: return settingChartOverlaySymbolsDefault
        }
        set(value) {
        }

    private val settingUseChartOverlaySymbols = "use_chart_overlay_symbols"
    private var useChartOverlaySymbols: Boolean
        get() {
            val sharedPref =
                PreferenceManager.getDefaultSharedPreferences(requireContext())
                    ?: return false
            return sharedPref.getBoolean(settingUseChartOverlaySymbols, false)
        }
        set(value) {
        }

    private fun assetItemUpdateClicked(asset: Asset) {
        val builder = AlertDialog.Builder(requireContext())
        // Get the layout inflater
        val inflater = LayoutInflater.from(requireContext())

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        val dialogBinding = DialogAddAssetBinding.inflate(inflater)

        dialogBinding.addQuantity.setText(
            DecimalFormat(DecimalFormatQuantityDigits).format(asset.quantity.absoluteValue)
        )
//    if (asset.shares < 0) {
//      addSharesView.inputType = TYPE_CLASS_NUMBER or
//          TYPE_NUMBER_FLAG_DECIMAL or
//          TYPE_NUMBER_FLAG_SIGNED
//    }

        dialogBinding.addPrice.setText(DecimalFormat(DecimalFormat2To8Digits).format(asset.price))
        if (asset.fee > 0.0) {
            dialogBinding.addFee.setText(DecimalFormat(DecimalFormat2To8Digits).format(asset.fee))
        }

        val standardAccount = getString(R.string.standard_account)

        dialogBinding.textViewAssetAccount.text =
            asset.account.ifEmpty {
                standardAccount
            }

        dialogBinding.textViewAssetAccount.setOnClickListener { view ->
            val popupMenu = PopupMenu(requireContext(), view)

            var menuIndex: Int = Menu.FIRST

            SharedAccountList.accounts.sortedBy {
                it.lowercase(Locale.ROOT)
            }
                .forEach { account ->
                    val name = account.ifEmpty {
                        // first entry in bold
                        SpannableStringBuilder()
                            .bold { append(standardAccount) }
                    }
                    popupMenu.menu.add(0, menuIndex++, Menu.NONE, name)
                }

            // Last item is to add a new account
            val addAccountItem = SpannableStringBuilder()
                .color(context?.getColor(R.color.colorAccent)!!) {
                    bold { append(getString(R.string.add_account)) }
                }
            popupMenu.menu.add(0, menuIndex++, Menu.CATEGORY_CONTAINER, addAccountItem)

            popupMenu.show()

            popupMenu.setOnMenuItemClickListener { menuitem ->
                val addSelected = menuIndex - 1 == menuitem.itemId

                if (addSelected) {
                    // Add/Rename account
                    val builderAdd = android.app.AlertDialog.Builder(requireContext())
                    // Get the layout account
                    val inflaterAdd = LayoutInflater.from(requireContext())

                    // Inflate and set the layout for the dialog
                    // Pass null as the parent view because its going in the dialog layout
                    val addDialogBinding = DialogAddAccountBinding.inflate(inflaterAdd)

                    builderAdd.setView(addDialogBinding.root)
                        .setTitle(getString(R.string.add_account))
                        // Add action buttons
                        .setPositiveButton(R.string.add) { _, _ ->
                            // Add () to avoid cast exception.
                            val accountText = (addDialogBinding.addAccount.text).toString()
                                .trim()

                            dialogBinding.textViewAssetAccount.text = accountText
                            SharedAccountList.accounts = SharedAccountList.accounts + accountText
                        }
                        .setNegativeButton(
                            R.string.cancel
                        ) { _, _ ->
                        }
                    builderAdd
                        .create()
                        .show()
                } else {
                    val account = menuitem.title?.trim()
                        .toString()
                    dialogBinding.textViewAssetAccount.text = account
                }
                true
            }
        }

        dialogBinding.addNote.setText(asset.note)

        val localDateTime = if (asset.date == 0L) {
            ZonedDateTime.now()
        } else {
            ZonedDateTime.ofInstant(Instant.ofEpochSecond(asset.date), ZoneOffset.systemDefault())
        }
        // month is starting from zero
        dialogBinding.datePickerAssetDate.updateDate(
            localDateTime.year, localDateTime.month.value - 1, localDateTime.dayOfMonth
        )
        dialogBinding.datePickerAssetTime.hour = localDateTime.hour
        dialogBinding.datePickerAssetTime.minute = localDateTime.minute

        builder.setView(dialogBinding.root)
            .setTitle(R.string.update_asset)
            // Add action buttons
            .setPositiveButton(
                R.string.update
            )
            { _, _ ->
                val quantityText = (dialogBinding.addQuantity.text).toString()
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
                        requireContext(),
                        getString(R.string.asset_share_not_empty),
                        Toast.LENGTH_LONG
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

                val priceText = (dialogBinding.addPrice.text).toString()
                    .trim()
                var price = 0.0
                try {
                    val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                    price = numberFormat.parse(priceText)!!
                        .toDouble()
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.asset_price_not_empty),
                        Toast.LENGTH_LONG
                    )
                        .show()
                    return@setPositiveButton
                }

                if (price < 0.0) {
                    Toast.makeText(
                        requireContext(), getString(R.string.price_not_negative), Toast.LENGTH_LONG
                    )
                        .show()
                    return@setPositiveButton
                }

                val feeText = (dialogBinding.addFee.text).toString()
                    .trim()
                var fee = 0.0
                if (feeText.isNotEmpty()) {
                    try {
                        val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                        fee = numberFormat.parse(feeText)!!
                            .toDouble()
                    } catch (e: Exception) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.asset_fee_not_valid),
                            Toast.LENGTH_LONG
                        )
                            .show()
                        return@setPositiveButton
                    }

                    if (fee < 0.0) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.fee_not_negative),
                            Toast.LENGTH_LONG
                        )
                            .show()
                        return@setPositiveButton
                    }
                }

                var accountText = (dialogBinding.textViewAssetAccount.text).toString()
                    .trim()
                if (accountText == standardAccount) {
                    accountText = ""
                }

                // val date = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
                val localDateTimeNew: ZonedDateTime = ZonedDateTime.of(
                    dialogBinding.datePickerAssetDate.year,
                    dialogBinding.datePickerAssetDate.month + 1,
                    dialogBinding.datePickerAssetDate.dayOfMonth,
                    dialogBinding.datePickerAssetTime.hour,
                    dialogBinding.datePickerAssetTime.minute,
                    localDateTime.second,   // preserve the seconds to not change the entry position
                    0,
                    ZoneOffset.systemDefault()
                )
                val date = localDateTimeNew.toEpochSecond() // in GMT

                val noteText = (dialogBinding.addNote.text).toString()
                    .trim()

                val assetNew =
                    Asset(
                        symbol = symbol,
                        quantity = quantity,
                        price = price,
                        account = accountText,
                        fee = fee,
                        date = date,
                        note = noteText
                    )

                if (asset.quantity != assetNew.quantity
                    || asset.price != assetNew.price
                    || asset.fee != assetNew.fee
                    || asset.account != assetNew.account
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
                            R.plurals.asset_updated, count,
                            DecimalFormat(DecimalFormatQuantityDigits).format(quantityAbs),
                            to2To8Digits(price)
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
            }
            .setNegativeButton(
                R.string.cancel
            )
            { _, _ ->
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
                        requireContext(),
                        getString(R.string.delete_all_assets_msg),
                        Toast.LENGTH_LONG
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
                .setTitle(R.string.sold_asset)
                .setMessage(
                    if (asset.quantity > 0) {
                        resources.getQuantityString(
                            R.plurals.delete_asset_confirm, count,
                            DecimalFormat(DecimalFormatQuantityDigits).format(asset.quantity),
                            to2To8Digits(asset.price)
                        )
                    } else {
                        resources.getQuantityString(
                            R.plurals.delete_removed_asset_confirm, count,
                            DecimalFormat(DecimalFormatQuantityDigits).format(asset.quantity.absoluteValue)
                        )
                    }
                )
                .setPositiveButton(R.string.delete) { _, _ ->
                    stockRoomViewModel.deleteAsset(asset)
                    val pluralstr = if (asset.quantity > 0) {
                        resources.getQuantityString(
                            R.plurals.delete_asset_msg, count,
                            DecimalFormat(DecimalFormatQuantityDigits).format(asset.quantity),
                            to2To8Digits(asset.price)
                        )
                    } else {
                        resources.getQuantityString(
                            R.plurals.delete_removed_asset_msg, count,
                            DecimalFormat(DecimalFormatQuantityDigits).format(asset.quantity.absoluteValue)
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
        val dialogBinding = DialogAddEventBinding.inflate(inflater)
        dialogBinding.textInputEditEventTitle.setText(event.title)
        dialogBinding.textInputEditEventNote.setText(event.note)
        val localDateTime =
            ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(event.datetime),
                ZoneOffset.systemDefault()
            )
        // month is starting from zero
        dialogBinding.datePickerEventDate.updateDate(
            localDateTime.year, localDateTime.month.value - 1, localDateTime.dayOfMonth
        )
        dialogBinding.datePickerEventTime.hour = localDateTime.hour
        dialogBinding.datePickerEventTime.minute = localDateTime.minute

        builder.setView(dialogBinding.root)
            .setTitle(R.string.update_event)
            // Add action buttons
            .setPositiveButton(
                R.string.update
            ) { _, _ ->
                // Add () to avoid cast exception.
                val title = (dialogBinding.textInputEditEventTitle.text).toString()
                    .trim()
                if (title.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.event_empty),
                        Toast.LENGTH_LONG
                    )
                        .show()
                } else {
                    val note = (dialogBinding.textInputEditEventNote.text).toString()

                    val datetime: ZonedDateTime = ZonedDateTime.of(
                        dialogBinding.datePickerEventDate.year,
                        dialogBinding.datePickerEventDate.month + 1,
                        dialogBinding.datePickerEventDate.dayOfMonth,
                        dialogBinding.datePickerEventTime.hour,
                        dialogBinding.datePickerEventTime.minute,
                        localDateTime.second,   // preserve the seconds to not change the entry position
                        0,
                        ZoneOffset.systemDefault()
                    )
                    val seconds = datetime.toEpochSecond() // in GMT
                    val eventNew =
                        Event(
                            symbol = symbol,
                            type = 0,
                            title = title,
                            note = note,
                            datetime = seconds
                        )
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
        val localDateTime =
            ZonedDateTime.ofInstant(
                Instant.ofEpochSecond(event.datetime),
                ZoneOffset.systemDefault()
            )
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
    ): View {

        symbol = (arguments?.getString(EXTRA_SYMBOL) ?: "").uppercase(Locale.ROOT)
        type = dataProviderFromInt(arguments?.getInt(EXTRA_TYPE, 0) ?: 0)

        // Setup online data every 2s for regular hours.
        onlineDataHandler = Handler(Looper.getMainLooper())
        standardPortfolio = getString(R.string.standard_portfolio)

        // Inflate the layout for this fragment
        _binding = FragmentStockdataBinding.inflate(inflater, container, false)
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

        val onlineDataAdapter = OnlineDataAdapter(requireContext())
        binding.onlineDataView.adapter = onlineDataAdapter

        // Coingecko has no candle data for crypto.
        if (type == DataProvider.Coingecko) {
            stockViewMode = StockViewMode.Line
        }

        // Set column number depending on screen width.
        val scale = 299
        val spanCount =
            (resources.configuration.screenWidthDp / (scale * resources.configuration.fontScale) + 0.5).roundToInt()

        binding.onlineDataView.layoutManager = GridLayoutManager(
            requireContext(),
            Integer.min(Integer.max(spanCount, 1), 10)
        )

        var timeInSeconds5minUpdate = ZonedDateTime.now()
            .toEpochSecond() // in GMT
        var timeInSeconds24hUpdate = timeInSeconds5minUpdate

        // use requireActivity() instead of this to have only one shared viewmodel
        stockRoomViewModel =
            ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)

        stockRoomViewModel.onlineMarketDataList.observe(viewLifecycleOwner, Observer { data ->
            data?.let { onlineMarketDataList ->
                val onlineMarketData = onlineMarketDataList.find { onlineMarketDataItem ->
                    onlineMarketDataItem.symbol.equals(symbol, true)
                }
                if (onlineMarketData != null) {
                    updateHeader(onlineMarketData)
                    onlineDataAdapter.updateData(
                        StockSymbol(
                            symbol = symbol,
                            type = type
                        ),
                        onlineMarketData
                    )

                    marketPriceForDefaultAlertValue = onlineMarketData.marketPrice

                    // Update charts
                    val timeInSecondsNow = ZonedDateTime.now()
                        .toEpochSecond() // in GMT

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

        //type = StockTypeFromInt(stockRoomViewModel.getTypeSync(symbol))

        stockChartDataViewModel = ViewModelProvider(this).get(StockChartDataViewModel::class.java)

//    stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { items ->
//      items?.let { stockItems ->
//
//        stockItems.forEach { stockItem ->
//          // Cache the type for each symbol used by the ref symbols.
//          val stocktype = StockTypeFromInt(stockItem.stockDBdata.type)
//          symbolTypesMap[stockItem.stockDBdata.symbol] = stocktype
//        }
//      }
//    })

        stockChartDataViewModel.chartData.observe(viewLifecycleOwner, Observer { stockChartData ->
            if (stockChartData != null) {
                if (stockChartData.stockDataEntries?.isNotEmpty() == true) {
                    chartDataItems[stockChartData.symbol] = stockChartData.stockDataEntries

                    stockDataEntries = stockChartData.stockDataEntries
                    setupCharts(stockViewRange, stockViewMode)
                    loadCharts(stockViewRange, stockViewMode)
                }
            }
        })

        val textViewStockLegends: List<TextView> = listOf(
            binding.textViewStockLegend1,
            binding.textViewStockLegend2,
            binding.textViewStockLegend3,
            binding.textViewStockLegend4,
        )

        textViewStockLegends.forEach { textView ->
            textView.visibility = View.GONE
            textView.text = ""
        }

        if (useChartOverlaySymbols) {

            var chartOverlayColorIndex = 0

            chartOverlaySymbols.split(",").take(MaxChartOverlays).forEach { symbolRef ->

                if (symbolRef.isNotEmpty()) {

                    val color = chartOverlayColors[chartOverlayColorIndex % chartOverlayColors.size]
                    val textView = textViewStockLegends[chartOverlayColorIndex]
                    textView.text = SpannableStringBuilder().color(color) { append(symbolRef) }
                    textView.visibility = View.VISIBLE

                    chartOverlayColorIndex++

                    // Listeners cannot be assigned to the list items, needs to be done explicitly.
                    when (chartOverlayColorIndex) {
                        1 -> {
                            binding.textViewStockLegend1.setOnClickListener {
                                updateLegend(1)
                            }
                        }
                        2 -> {
                            binding.textViewStockLegend2.setOnClickListener {
                                updateLegend(2)
                            }
                        }
                        3 -> {
                            binding.textViewStockLegend3.setOnClickListener {
                                updateLegend(3)
                            }
                        }
                        4 -> {
                            binding.textViewStockLegend4.setOnClickListener {
                                updateLegend(4)
                            }
                        }
                    }
                }
            }
        }

        // Setup community pages menu
        binding.textViewSymbol.setOnClickListener { viewSymbol ->
            val popupMenu = PopupMenu(requireContext(), viewSymbol)

            data class LinkListEntry
                (
                val linkType: LinkType,
                val link: String,
            )

            val linkList: Map<String, LinkListEntry> = mapOf(
                getString(R.string.community_links) to LinkListEntry(
                    linkType = LinkType.HeadlineType,
                    link = ""
                ),
                "Yahoo community" to LinkListEntry(
                    linkType = LinkType.CommunityType,
                    link = "https://finance.yahoo.com/quote/$symbol/community"
                ),
                "Stocktwits" to LinkListEntry(
                    linkType = LinkType.CommunityType,
                    link = "https://stocktwits.com/symbol/$symbol"
                ),
                //--------------------------------
                getString(R.string.search_links) to LinkListEntry(
                    linkType = LinkType.HeadlineType,
                    link = ""
                ),
                "Bing" to LinkListEntry(
                    linkType = LinkType.SearchType,
                    link = "https://www.bing.com/search?q=$symbol%20stock"
                ),
                "Google" to LinkListEntry(
                    linkType = LinkType.SearchType,
                    link = "https://www.google.com/finance?q=$symbol stock"
                ),
                "Yahoo" to LinkListEntry(
                    linkType = LinkType.SearchType,
                    link = "https://finance.yahoo.com/quote/$symbol"
                ),
                //--------------------------------
                getString(R.string.crypto_links) to LinkListEntry(
                    linkType = LinkType.HeadlineType,
                    link = ""
                ),
                "CoinMarketCap" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeCrypto,
                    link = "https://coinmarketcap.com/currencies/$symbol"
                ),
                "TradingView" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeCrypto,
                    link = "https://www.tradingview.com/symbols/$symbol"
                ),
                "CoinBase" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeCrypto,
                    link = "https://www.coinbase.com/de/price/$symbol"
                ),
                "CoinGecko" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeCrypto,
                    link = "https://www.coingecko.com/en/coins/$symbol"
                ),
                "Gemini" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeCrypto,
                    link = "https://www.gemini.com/prices/$symbol"
                ),
                "crypto.com" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeCrypto,
                    link = "https://crypto.com/price/$symbol"
                ),
                "CoinCodex" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeCrypto,
                    link = "https://coincodex.com/crypto/$symbol"
                ),
                "WorldCoinIndex" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeCrypto,
                    link = "https://www.worldcoinindex.com/coin/$symbol"
                ),
                "MarketWatch Crypto" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeCrypto,
                    link = "https://www.marketwatch.com/investing/cryptocurrency/$symbol"
                ),
                //--------------------------------
                getString(R.string.stock_links) to LinkListEntry(
                    linkType = LinkType.HeadlineType,
                    link = ""
                ),
                "CNBC" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeStock,
                    link = "https://www.cnbc.com/quotes/?symbol=$symbol"
                ),
                "CNN Money" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeStock,
                    link = "http://money.cnn.com/quote/quote.html?symb=$symbol"
                ),
                "ETF.COM" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeStock,
                    link = "https://www.etf.com/stock/$symbol"
                ),
                "ETF.COM (ETF)" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeStock,
                    link = "https://www.etf.com/$symbol"
                ),
                "Fidelity" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeStock,
                    link = "https://quotes.fidelity.com/webxpress/get_quote?QUOTE_TYPE=D&SID_VALUE_ID=$symbol"
                ),
                "FinViz" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeStock,
                    link = "https://finviz.com/quote.ashx?t=$symbol"
                ),
                "MarketBeat" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeStock,
                    link = "https://www.marketbeat.com/stocks/$symbol/"
                ),
                "MarketWatch" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeStock,
                    link = "https://www.marketwatch.com/investing/stock/$symbol/"
                ),
                "Nasdaq" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeStock,
                    link = "https://www.nasdaq.com/market-activity/stocks/$symbol"
                ),
                "OTC Markets" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeStock,
                    link = "https://www.otcmarkets.com/stock/$symbol/overview"
                ),
                "TD Ameritrade" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeStock,
                    link = "https://research.tdameritrade.com/grid/public/research/stocks/summary?symbol=$symbol"
                ),
                "TheStreet" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeStock,
                    link = "https://www.thestreet.com/quote/$symbol"
                ),
                "Real Money (TheStreet)" to LinkListEntry(
                    linkType = LinkType.WebsiteTypeStock,
                    link = "https://realmoney.thestreet.com/quote/$symbol"
                ),
                //--------------------------------
                getString(R.string.general_links) to LinkListEntry(
                    linkType = LinkType.HeadlineType,
                    link = ""
                ),
                "Yahoo Daily Gainers" to LinkListEntry(
                    linkType = LinkType.WebsiteGeneralType,
                    link = "https://finance.yahoo.com/gainers"
                ),
                "Yahoo Cryptocurrencies" to LinkListEntry(
                    linkType = LinkType.WebsiteGeneralType,
                    link = "https://finance.yahoo.com/cryptocurrencies"
                )
            )

            var menuIndex: Int = Menu.FIRST

            // One group for each link type.
            linkList.forEach { (name, entry) ->
                val displayName = if (entry.linkType == LinkType.HeadlineType) {
                    SpannableStringBuilder()
                        .color(context?.getColor(R.color.colorPrimary)!!) {
                            bold { append(name) }
                        }
                } else {
                    // Intend by two spaces.
                    "  $name"
                }
                popupMenu.menu.add(entry.linkType.value, menuIndex++, Menu.NONE, displayName)
            }

            MenuCompat.setGroupDividerEnabled(popupMenu.menu, true)
            popupMenu.show()

            popupMenu.setOnMenuItemClickListener { menuitem ->
                val menuText = menuitem.toString()

                // menuitem is intended by two spaces
                if (menuText.startsWith("  ")) {
                    linkList[menuText.substring(2)]?.let {
                        openNewTabWindow(
                            it.link,
                            requireContext()
                        )
                    }
                }

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
        binding.assetsView.adapter = assetAdapter
        binding.assetsView.layoutManager = LinearLayoutManager(requireContext())

        // Events
        val eventClickListenerUpdate =
            { event: Event -> eventItemUpdateClicked(event) }
        val eventClickListenerDelete =
            { event: Event -> eventItemDeleteClicked(event) }
        val eventAdapter =
            EventListAdapter(requireContext(), eventClickListenerUpdate, eventClickListenerDelete)
        binding.eventsView.adapter = eventAdapter
        binding.eventsView.layoutManager = LinearLayoutManager(requireContext())

        val stockDBLiveData: LiveData<StockDBdata> = stockRoomViewModel.getStockDBLiveData(symbol)
        stockDBLiveData.observe(viewLifecycleOwner, Observer { data ->
            if (data != null) {
                stockDBdata = data

                val displayName =
                    stockDBdata.name.ifEmpty { stockDBdata.symbol }

                binding.textViewSymbol.text =
                    SpannableStringBuilder().underline { color(Color.BLUE) { append(displayName) } }
                binding.buttonSetName.text = displayName

                binding.noteTextView.text = stockDBdata.note

                // Portfolio
                binding.textViewPortfolio.text = stockDBdata.portfolio.ifEmpty {
                    standardPortfolio
                }
            }

            // Setup type selection
            binding.dataproviderSpinner.setSelection(
                stockDBdata.type
            )

            // Group color
            // color = 0 is not valid and not stored in the DB
            var color = stockDBdata.groupColor
            if (color == 0) {
                color = context?.getColor(R.color.backgroundListColor)!!
            }
            setBackgroundColor(binding.textViewGroupColor, color)

            binding.textViewGroup.text = if (stockDBdata.groupColor == 0) {
                getString(R.string.standard_group)
            } else {
                val group = stockRoomViewModel.getGroupSync(stockDBdata.groupColor)
                group.name.ifEmpty {
                    "Color code=0x${color.toHexString()}"
                }
            }

            // Set Marker
            val markerColor =
                if (stockDBdata.marker == 0) {
                    context?.getColor(R.color.backgroundListColor)!!
                } else {
                    context?.let { getMarkerColor(it, stockDBdata.marker) }
                }
            if (markerColor != null) {
                setBackgroundColor(binding.textViewMarkerColor, markerColor)
            }
            binding.textViewMarkerText.text =
                context?.let { "${getMarkerText(it, stockDBdata.marker)}" }

            alertAbove = stockDBdata.alertAbove
            alertBelow = stockDBdata.alertBelow

            if (alertAbove > 0.0 && alertBelow > 0.0 && alertBelow >= alertAbove) {
                alertAbove = 0.0
                alertBelow = 0.0
            }

            binding.alertAboveInputEditText.setText(
                if (alertAbove > 0.0) {
                    to2To8Digits(alertAbove)
                } else {
                    ""
                }
            )
            binding.alertAboveNoteInputEditText.setText(stockDBdata.alertAboveNote)

            binding.alertBelowInputEditText.setText(
                if (alertBelow > 0.0) {
                    to2To8Digits(alertBelow)
                } else {
                    ""
                }
            )
            binding.alertBelowNoteInputEditText.setText(stockDBdata.alertBelowNote)
        })

        binding.buttonSetAlertDefault.setOnClickListener {
            if (marketPriceForDefaultAlertValue > 0.000001) {
                binding.alertAboveInputEditText.setText(
                    to2To8Digits(marketPriceForDefaultAlertValue * 1.05)
                )
                binding.alertBelowInputEditText.setText(
                    to2To8Digits(marketPriceForDefaultAlertValue * 0.95)
                )
            }
        }

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
                // assetAdapter.updateAssets(data.assets)

                // Get the time stamps for the assets.
                assetTimeEntries =
                    data.assets.filter { assetItem ->
                        assetItem.price != 0.0 && assetItem.quantity != 0.0
                    }
                        .map { asset ->
                            AssetsTimeData(
                                date = asset.date,
                                price = asset.price,
                                quantity = asset.quantity,
                                account = asset.account,
                            )
                        }.toMutableList()

                // Remove item pairs for moved content.
                var j: Int = 0
                while (j < assetTimeEntries.size) {

                    if (j < assetTimeEntries.size - 1) {

                        // same date and price, but quantity have different sign
                        if ((assetTimeEntries[j].date - assetTimeEntries[j + 1].date).absoluteValue <= 1
                            && (assetTimeEntries[j].price - assetTimeEntries[j + 1].price).absoluteValue < epsilon
                            && (assetTimeEntries[j].quantity + assetTimeEntries[j + 1].quantity).absoluteValue < epsilon
                            && (assetTimeEntries[j].account != assetTimeEntries[j + 1].account ||
                                    assetTimeEntries[j + 1].account != assetTimeEntries[j].account)
                        ) {
                            assetTimeEntries.removeAt(j)
                            assetTimeEntries.removeAt(j)
                            continue
                        }
                    }

                    j++
                }

                // Reload view with updated asset time data.
                loadStockView(stockViewRange, stockViewMode)

                // Reset when assets are changed.
                binding.pickerKnob.setValue(0.0, 100.0, 0.0)

                // Connect the fine-tune rotary to the picker knob.
                binding.rotaryControl.onValueChangeListener { value ->
                    binding.pickerKnob.incValue(value)
                }

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
                    data.symbol.equals(symbol, true)
                }
                if (onlineMarketData != null) {
                    assetChange.onlineMarketData = onlineMarketData
                    assetChangeLiveData.postValue(assetChange)
                }
            }
        }

        assetChangeLiveData.observe(viewLifecycleOwner, Observer { stockAssetsLiveData ->
            if (stockAssetsLiveData != null) {
                assetAdapter.updateAssets(stockAssetsLiveData)
                updateAssetChange(stockAssetsLiveData)
            }
        })

        // Setup portfolio menu
        binding.textViewPortfolio.setOnClickListener { viewPortfolio ->
            val popupMenu = PopupMenu(requireContext(), viewPortfolio)

            var menuIndex: Int = Menu.FIRST

            SharedRepository.portfolios.value?.sortedBy {
                it.lowercase(Locale.ROOT)
            }
                ?.forEach { portfolio ->
                    val name = portfolio.ifEmpty {
                        // first entry in bold
                        SpannableStringBuilder()
                            .bold { append(standardPortfolio) }
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
                    val dialogBinding = DialogAddPortfolioBinding.inflate(inflater)
                    //val dialogView = inflater.inflate(R.layout.dialog_add_portfolio, null)

                    val selectedPortfolio =
                        SharedRepository.selectedPortfolio.value
                            ?: stockDBdata.portfolio.ifEmpty {
                                standardPortfolio
                            }

                    if (addSelected) {
                        dialogBinding.portfolioTextView.text =
                            getString(R.string.portfolio_name_text)
                    } else {
                        dialogBinding.portfolioTextView.text =
                            getString(R.string.portfolio_rename_text)
                        // set the current portfolio name as default
                        dialogBinding.addPortfolioName.setText(selectedPortfolio)
                    }
                    builder.setView(dialogBinding.root)
                        .setTitle(
                            if (addSelected) {
                                getString(R.string.add_portfolio)
                            } else {
                                getString(R.string.rename_portfolio_header, selectedPortfolio)
                            }
                        )
                        // Add action buttons
                        .setPositiveButton(
                            if (addSelected) {
                                R.string.add
                            } else {
                                R.string.rename
                            }
                        ) { _, _ ->
                            // Add () to avoid cast exception.
                            val portfolioText = (dialogBinding.addPortfolioName.text).toString()
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

                            binding.textViewPortfolio.text = portfolioText

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
                    var portfolio = menuitem.title?.trim()
                        .toString()
                    binding.textViewPortfolio.text = portfolio

                    if (portfolio == standardPortfolio) {
                        portfolio = ""
                    }

                    stockRoomViewModel.setPortfolio(symbol, portfolio)
                    SharedRepository.selectedPortfolio.value = portfolio
                }
                true
            }
        }

        binding.dataproviderSpinner.onItemSelectedListener = object : OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                //stockRoomViewModel.setType(symbol, position)
            }
        }

        binding.stockgroupLayout.setOnClickListener { viewLayout ->
            val popupMenu = PopupMenu(requireContext(), viewLayout)

            var menuIndex: Int = Menu.FIRST
            stockRoomViewModel.getGroupsMenuList(
                getString(R.string.standard_group),
                context?.getColor(R.color.black) ?: 0
            )
                .forEach {
                    popupMenu.menu.add(0, menuIndex++, Menu.NONE, it)
                }

            popupMenu.show()

            val groups: List<Group> = stockRoomViewModel.getGroupsSync()
            popupMenu.setOnMenuItemClickListener { menuitem ->
                val i: Int = menuitem.itemId - 1
                val clrDB: Int
                val name: String

                if (i >= groups.size) {
                    clrDB = 0
                    name = getString(R.string.standard_group)
                } else {
                    clrDB = groups[i].color
                    name = groups[i].name
                }

                // Store the selection.
                stockRoomViewModel.setGroup(symbol, name, clrDB)
                true
            }
        }

        binding.markerLayout.setOnClickListener { viewLayout ->
            val popupMenu = PopupMenu(requireContext(), viewLayout)

            for (index in 0..10) {
                val text = context?.let { getMarkerText(it, index) }
                popupMenu.menu.add(0, Menu.FIRST + index, Menu.NONE, text)
            }

            popupMenu.show()

            popupMenu.setOnMenuItemClickListener { menuitem ->
                val marker: Int = menuitem.itemId - 1

                // Store the selected marker.
                stockRoomViewModel.setMarker(symbol, marker)
                true
            }
        }

        binding.buttonOneDay.setOnClickListener {
            updateStockViewRange(StockViewRange.OneDay)
        }
        binding.buttonFiveDays.setOnClickListener {
            updateStockViewRange(StockViewRange.FiveDays)
        }
        binding.buttonOneMonth.setOnClickListener {
            updateStockViewRange(StockViewRange.OneMonth)
        }
        binding.buttonThreeMonth.setOnClickListener {
            updateStockViewRange(StockViewRange.ThreeMonth)
        }
        binding.buttonYTD.setOnClickListener {
            updateStockViewRange(StockViewRange.YTD)
        }
        binding.buttonOneYear.setOnClickListener {
            updateStockViewRange(StockViewRange.OneYear)
        }
        binding.buttonFiveYears.setOnClickListener {
            updateStockViewRange(StockViewRange.FiveYears)
        }
        binding.buttonMax.setOnClickListener {
            updateStockViewRange(StockViewRange.Max)
        }

        binding.imageButtonIconLine.setOnClickListener {
            updateStockViewMode(StockViewMode.Candle)
        }
        binding.imageButtonIconCandle.setOnClickListener {
            updateStockViewMode(StockViewMode.Line)
        }

        binding.splitAssetsButton.setOnClickListener {
            val assets = stockRoomViewModel.getAssetsSync(symbol)
//      val (totalQuantity, totalPrice) = getAssets(assets?.assets)

//      val totalQuantity = assets?.assets?.sumOf {
//        it.shares
//      }
//          ?: 0.0

//    if (totalQuantity == 0.0) {
            if (assets?.assets?.size == 0) {
                Toast.makeText(
                    requireContext(), getString(R.string.no_total_quantity), Toast.LENGTH_LONG
                )
                    .show()
            } else {
                val builder = AlertDialog.Builder(requireContext())
                // Get the layout inflater
                val inflater = LayoutInflater.from(requireContext())

                val dialogBinding = DialogSplitAssetBinding.inflate(inflater)

                dialogBinding.splitRatioZ.setText("1")

                builder.setView(dialogBinding.root)
                    .setTitle(R.string.split_asset)
                    // Add action buttons
                    .setPositiveButton(
                        R.string.split
                    ) { _, _ ->
                        // Add () to avoid cast exception.
                        val splitRatioTextZ = (dialogBinding.splitRatioZ.text).toString()
                            .trim()
                        val splitRatioTextN = (dialogBinding.splitRatioN.text).toString()
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
                                    if (asset.price > 0.0) {
                                        asset.price /= splitRatio
                                    }
                                    if (asset.quantity > 0.0) {
                                        minQuantity = min(asset.quantity, minQuantity)
                                        minPrice = min(asset.price, minPrice)
                                    }
                                }

                                if (minQuantity >= 0.1 && minPrice >= 0.01) {
                                    stockRoomViewModel.updateAssets(
                                        symbol = symbol, assets = assets.assets
                                    )

                                    // Update below/above alerts.
                                    val stockDBdata = stockRoomViewModel.getStockDBdataSync(symbol)

                                    if (stockDBdata.alertBelow > 0.0) {
//                                        stockRoomViewModel.updateAlertBelowSync(
//                                            symbol,
//                                            stockDBdata.alertBelow / splitRatio,
//                                            stockDBdata.alertBelowNote
//                                        )

                                        binding.alertBelowInputEditText.setText(
                                            to2To8Digits(stockDBdata.alertBelow / splitRatio)
                                        )
                                    }

                                    if (stockDBdata.alertAbove > 0.0) {
//                                        stockRoomViewModel.updateAlertAboveSync(
//                                            symbol,
//                                            stockDBdata.alertAbove / splitRatio,
//                                            stockDBdata.alertAboveNote
//                                        )

                                        binding.alertAboveInputEditText.setText(
                                            to2To8Digits(stockDBdata.alertAbove / splitRatio)
                                        )
                                    }

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
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.invalid_split_entry),
                                    Toast.LENGTH_LONG
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

        binding.addAssetsButton.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            // Get the layout inflater
            val inflater = LayoutInflater.from(requireContext())

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val dialogBinding = DialogAddAssetBinding.inflate(inflater)

            val standardAccount = getString(R.string.standard_account)
            dialogBinding.textViewAssetAccount.text = standardAccount

            dialogBinding.textViewAssetAccount.setOnClickListener { view ->
                val popupMenu = PopupMenu(requireContext(), view)

                var menuIndex: Int = Menu.FIRST

                SharedAccountList.accounts.sortedBy {
                    it.lowercase(Locale.ROOT)
                }
                    .forEach { account ->
                        val name = account.ifEmpty {
                            // first entry in bold
                            SpannableStringBuilder()
                                .bold { append(standardAccount) }
                        }
                        popupMenu.menu.add(0, menuIndex++, Menu.NONE, name)
                    }

                // Last item is to add a new account
                val addAccountItem = SpannableStringBuilder()
                    .color(context?.getColor(R.color.colorAccent)!!) {
                        bold { append(getString(R.string.add_account)) }
                    }
                popupMenu.menu.add(0, menuIndex++, Menu.CATEGORY_CONTAINER, addAccountItem)

                popupMenu.show()

                popupMenu.setOnMenuItemClickListener { menuitem ->
                    val addSelected = menuIndex - 1 == menuitem.itemId

                    if (addSelected) {
                        // Add/Rename account
                        val builderAdd = android.app.AlertDialog.Builder(requireContext())
                        // Get the layout account
                        val inflaterAdd = LayoutInflater.from(requireContext())

                        // Inflate and set the layout for the dialog
                        // Pass null as the parent view because its going in the dialog layout
                        val addDialogBinding = DialogAddAccountBinding.inflate(inflaterAdd)

                        builderAdd.setView(addDialogBinding.root)
                            .setTitle(getString(R.string.add_account))
                            // Add action buttons
                            .setPositiveButton(R.string.add) { _, _ ->
                                // Add () to avoid cast exception.
                                val accountText = (addDialogBinding.addAccount.text).toString()
                                    .trim()

                                dialogBinding.textViewAssetAccount.text = accountText
                                SharedAccountList.accounts =
                                    SharedAccountList.accounts + accountText
                            }
                            .setNegativeButton(
                                R.string.cancel
                            ) { _, _ ->
                            }
                        builderAdd
                            .create()
                            .show()
                    } else {
                        val account = menuitem.title?.trim()
                            .toString()
                        dialogBinding.textViewAssetAccount.text = account
                    }
                    true
                }
            }

            builder.setView(dialogBinding.root)
                .setTitle(R.string.bought_asset)
                // Add action buttons
                .setPositiveButton(
                    R.string.bought
                ) { _, _ ->
                    // Add () to avoid cast exception.
                    val quantityText = (dialogBinding.addQuantity.text).toString()
                        .trim()
                    var quantity = 0.0

                    try {
                        val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                        quantity = numberFormat.parse(quantityText)!!
                            .toDouble()
                    } catch (e: Exception) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.asset_share_not_empty),
                            Toast.LENGTH_LONG
                        )
                            .show()
                        return@setPositiveButton
                    }

                    if (quantity <= 0.0) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.quantity_not_zero),
                            Toast.LENGTH_LONG
                        )
                            .show()
                        return@setPositiveButton
                    }

                    val priceText = (dialogBinding.addPrice.text).toString()
                        .trim()
                    var price = 0.0
                    try {
                        val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                        price = numberFormat.parse(priceText)!!
                            .toDouble()
                    } catch (e: Exception) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.asset_price_not_empty),
                            Toast.LENGTH_LONG
                        )
                            .show()
                        return@setPositiveButton
                    }

                    if (price < 0.0) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.price_not_negative),
                            Toast.LENGTH_LONG
                        )
                            .show()
                        return@setPositiveButton
                    }

                    val feeText = (dialogBinding.addFee.text).toString()
                        .trim()
                    var fee = 0.0
                    if (feeText.isNotEmpty()) {
                        try {
                            val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                            fee = numberFormat.parse(feeText)!!
                                .toDouble()
                        } catch (e: Exception) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.asset_fee_not_valid),
                                Toast.LENGTH_LONG
                            )
                                .show()
                            return@setPositiveButton
                        }

                        if (fee < 0.0) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.fee_not_negative),
                                Toast.LENGTH_LONG
                            )
                                .show()
                            return@setPositiveButton
                        }
                    }

                    val localDateTime: ZonedDateTime = ZonedDateTime.of(
                        dialogBinding.datePickerAssetDate.year,
                        dialogBinding.datePickerAssetDate.month + 1,
                        dialogBinding.datePickerAssetDate.dayOfMonth,
                        dialogBinding.datePickerAssetTime.hour,
                        dialogBinding.datePickerAssetTime.minute,
                        0,
                        0,
                        ZoneOffset.systemDefault()
                    )
                    val date = localDateTime.toEpochSecond() // in GMT

                    var accountText = (dialogBinding.textViewAssetAccount.text).toString()
                        .trim()
                    if (accountText == standardAccount) {
                        accountText = ""
                    }

                    val noteText = (dialogBinding.addNote.text).toString()
                        .trim()

                    //val date = LocalDateTime.now()
                    //    .toEpochSecond(ZoneOffset.UTC)

                    stockRoomViewModel.addAsset(
                        Asset(
                            symbol = symbol,
                            quantity = quantity,
                            price = price,
                            account = accountText,
                            fee = fee,
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
                        R.plurals.asset_added, count,
                        DecimalFormat(DecimalFormatQuantityDigits).format(quantity),
                        to2To8Digits(price)
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

        binding.moveAssetsButton.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            // Get the layout inflater
            val inflater = LayoutInflater.from(requireContext())

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val dialogBinding = DialogMoveAssetBinding.inflate(inflater)

            if (purchaseQuantity > 0.0) {
                dialogBinding.addQuantity.setText(
                    DecimalFormat(DecimalFormat2To8Digits).format(
                        purchaseQuantity
                    )
                )
            } else {
                Toast.makeText(
                    requireContext(),
                    getString(R.string.add_assets_first),
                    Toast.LENGTH_LONG
                )
                    .show()
                return@setOnClickListener
            }

            val standardAccount = getString(R.string.standard_account)
            dialogBinding.textViewFromAssetAccount.text = standardAccount
            dialogBinding.textViewToAssetAccount.text = standardAccount

            dialogBinding.textViewFromAssetAccount.setOnClickListener { view ->
                val popupMenu = PopupMenu(requireContext(), view)

                var menuIndex: Int = Menu.FIRST

                SharedAccountList.accounts.sortedBy {
                    it.lowercase(Locale.ROOT)
                }
                    .forEach { account ->
                        val name = account.ifEmpty {
                            // first entry in bold
                            SpannableStringBuilder()
                                .bold { append(standardAccount) }
                        }
                        popupMenu.menu.add(0, menuIndex++, Menu.NONE, name)
                    }

                // Last item is to add a new account
                val addAccountItem = SpannableStringBuilder()
                    .color(context?.getColor(R.color.colorAccent)!!) {
                        bold { append(getString(R.string.add_account)) }
                    }
                popupMenu.menu.add(0, menuIndex++, Menu.CATEGORY_CONTAINER, addAccountItem)

                popupMenu.show()

                popupMenu.setOnMenuItemClickListener { menuitem ->
                    val addSelected = menuIndex - 1 == menuitem.itemId

                    if (addSelected) {
                        // Add/Rename account
                        val builderAdd = android.app.AlertDialog.Builder(requireContext())
                        // Get the layout account
                        val inflaterAdd = LayoutInflater.from(requireContext())

                        // Inflate and set the layout for the dialog
                        // Pass null as the parent view because its going in the dialog layout
                        val addDialogBinding = DialogAddAccountBinding.inflate(inflaterAdd)

                        builderAdd.setView(addDialogBinding.root)
                            .setTitle(getString(R.string.add_account))
                            // Add action buttons
                            .setPositiveButton(R.string.add) { _, _ ->
                                // Add () to avoid cast exception.
                                val accountText = (addDialogBinding.addAccount.text).toString()
                                    .trim()

                                dialogBinding.textViewFromAssetAccount.text = accountText
                                SharedAccountList.accounts =
                                    SharedAccountList.accounts + accountText
                            }
                            .setNegativeButton(
                                R.string.cancel
                            ) { _, _ ->
                            }
                        builderAdd
                            .create()
                            .show()
                    } else {
                        val account = menuitem.title?.trim()
                            .toString()
                        dialogBinding.textViewFromAssetAccount.text = account
                    }
                    true
                }
            }

            dialogBinding.textViewToAssetAccount.setOnClickListener { view ->
                val popupMenu = PopupMenu(requireContext(), view)

                var menuIndex: Int = Menu.FIRST

                SharedAccountList.accounts.sortedBy {
                    it.lowercase(Locale.ROOT)
                }
                    .forEach { account ->
                        val name = account.ifEmpty {
                            // first entry in bold
                            SpannableStringBuilder()
                                .bold { append(standardAccount) }
                        }
                        popupMenu.menu.add(0, menuIndex++, Menu.NONE, name)
                    }

                // Last item is to add a new account
                val addAccountItem = SpannableStringBuilder()
                    .color(context?.getColor(R.color.colorAccent)!!) {
                        bold { append(getString(R.string.add_account)) }
                    }
                popupMenu.menu.add(0, menuIndex++, Menu.CATEGORY_CONTAINER, addAccountItem)

                popupMenu.show()

                popupMenu.setOnMenuItemClickListener { menuitem ->
                    val addSelected = menuIndex - 1 == menuitem.itemId

                    if (addSelected) {
                        val builderAdd = android.app.AlertDialog.Builder(requireContext())
                        // Get the layout account
                        val inflaterAdd = LayoutInflater.from(requireContext())

                        // Inflate and set the layout for the dialog
                        // Pass null as the parent view because its going in the dialog layout
                        val addDialogBinding = DialogAddAccountBinding.inflate(inflaterAdd)

                        builderAdd.setView(addDialogBinding.root)
                            .setTitle(getString(R.string.add_account))
                            // Add action buttons
                            .setPositiveButton(R.string.add) { _, _ ->
                                // Add () to avoid cast exception.
                                val accountText = (addDialogBinding.addAccount.text).toString()
                                    .trim()

                                dialogBinding.textViewToAssetAccount.text = accountText
                                SharedAccountList.accounts =
                                    SharedAccountList.accounts + accountText
                            }
                            .setNegativeButton(
                                R.string.cancel
                            ) { _, _ ->
                            }
                        builderAdd
                            .create()
                            .show()
                    } else {
                        val account = menuitem.title?.trim()
                            .toString()
                        dialogBinding.textViewToAssetAccount.text = account
                    }
                    true
                }
            }

            builder.setView(dialogBinding.root)
                .setTitle(R.string.move_asset)
                // Add action buttons
                .setPositiveButton(
                    R.string.move
                ) { _, _ ->
                    // Add () to avoid cast exception.
                    val quantityText = (dialogBinding.addQuantity.text).toString()
                        .trim()
                    var quantity = 0.0

                    try {
                        val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                        quantity = numberFormat.parse(quantityText)!!
                            .toDouble()
                    } catch (e: Exception) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.asset_share_not_empty),
                            Toast.LENGTH_LONG
                        )
                            .show()
                        return@setPositiveButton
                    }

                    if (quantity <= 0.0) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.quantity_not_zero),
                            Toast.LENGTH_LONG
                        )
                            .show()
                        return@setPositiveButton
                    }

                    val feeText = (dialogBinding.addFee.text).toString()
                        .trim()
                    var fee = 0.0
                    if (feeText.isNotEmpty()) {
                        try {
                            val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                            fee = numberFormat.parse(feeText)!!
                                .toDouble()
                        } catch (e: Exception) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.asset_fee_not_valid),
                                Toast.LENGTH_LONG
                            )
                                .show()
                            return@setPositiveButton
                        }

                        if (fee < 0.0) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.fee_not_negative),
                                Toast.LENGTH_LONG
                            )
                                .show()
                            return@setPositiveButton
                        }
                    }

                    val localDateTime: ZonedDateTime = ZonedDateTime.of(
                        dialogBinding.datePickerAssetDate.year,
                        dialogBinding.datePickerAssetDate.month + 1,
                        dialogBinding.datePickerAssetDate.dayOfMonth,
                        dialogBinding.datePickerAssetTime.hour,
                        dialogBinding.datePickerAssetTime.minute,
                        0,
                        0,
                        ZoneOffset.systemDefault()
                    )
                    val date = localDateTime.toEpochSecond() // in GMT

                    var fromAccountText = (dialogBinding.textViewFromAssetAccount.text).toString()
                        .trim()
                    if (fromAccountText == standardAccount) {
                        fromAccountText = ""
                    }

                    var toAccountText = (dialogBinding.textViewToAssetAccount.text).toString()
                        .trim()
                    if (toAccountText == standardAccount) {
                        toAccountText = ""
                    }

                    val noteText = (dialogBinding.addNote.text).toString()
                        .trim()

                    if (fromAccountText != toAccountText) {
                        stockRoomViewModel.moveAsset(
                            // From
                            Asset(
                                symbol = symbol,
                                quantity = -quantity,
                                price = purchasePriceValue,
                                account = fromAccountText,
                                fee = fee,
                                date = date,
                                note = noteText
                            ),
                            // To
                            Asset(
                                symbol = symbol,
                                quantity = quantity,
                                price = purchasePriceValue,
                                account = toAccountText,
                                fee = 0.0,
                                date = date + 1,
                                note = noteText
                            )
                        )

                        Toast.makeText(
                            requireContext(),
                            getString(R.string.asset_moved, toAccountText),
                            Toast.LENGTH_LONG
                        )
                            .show()
                    } else {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.asset_same_account),
                            Toast.LENGTH_LONG
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

        binding.removeAssetButton.setOnClickListener {
            val assets = stockRoomViewModel.getAssetsSync(symbol)
            val (totalQuantity, totalPrice, totalFee) = getAssets(assets?.assets)

//      val totalQuantity = assets?.assets?.sumOf {
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
                val dialogBinding = DialogRemoveAssetBinding.inflate(inflater)

                val standardAccount = getString(R.string.standard_account)
                dialogBinding.textViewAssetAccount.text = standardAccount

                dialogBinding.textViewAssetAccount.setOnClickListener { view ->
                    val popupMenu = PopupMenu(requireContext(), view)

                    var menuIndex: Int = Menu.FIRST

                    SharedAccountList.accounts.sortedBy {
                        it.lowercase(Locale.ROOT)
                    }
                        .forEach { account ->
                            val name = account.ifEmpty {
                                // first entry in bold
                                SpannableStringBuilder()
                                    .bold { append(standardAccount) }
                            }
                            popupMenu.menu.add(0, menuIndex++, Menu.NONE, name)
                        }

                    // Last item is to add a new account
                    val addAccountItem = SpannableStringBuilder()
                        .color(context?.getColor(R.color.colorAccent)!!) {
                            bold { append(getString(R.string.add_account)) }
                        }
                    popupMenu.menu.add(0, menuIndex++, Menu.CATEGORY_CONTAINER, addAccountItem)

                    popupMenu.show()

                    popupMenu.setOnMenuItemClickListener { menuitem ->
                        val addSelected = menuIndex - 1 == menuitem.itemId

                        if (addSelected) {
                            // Add/Rename account
                            val builderAdd = android.app.AlertDialog.Builder(requireContext())
                            // Get the layout account
                            val inflaterAdd = LayoutInflater.from(requireContext())

                            // Inflate and set the layout for the dialog
                            // Pass null as the parent view because its going in the dialog layout
                            val addDialogBinding = DialogAddAccountBinding.inflate(inflaterAdd)

                            builderAdd.setView(addDialogBinding.root)
                                .setTitle(getString(R.string.add_account))
                                // Add action buttons
                                .setPositiveButton(R.string.add) { _, _ ->
                                    // Add () to avoid cast exception.
                                    val accountText = (addDialogBinding.addAccount.text).toString()
                                        .trim()

                                    dialogBinding.textViewAssetAccount.text = accountText
                                    SharedAccountList.accounts =
                                        SharedAccountList.accounts + accountText
                                }
                                .setNegativeButton(
                                    R.string.cancel
                                ) { _, _ ->
                                }
                            builderAdd
                                .create()
                                .show()
                        } else {
                            val account = menuitem.title?.trim()
                                .toString()
                            dialogBinding.textViewAssetAccount.text = account
                        }
                        true
                    }
                }

                builder.setView(dialogBinding.root)
                    .setTitle(R.string.sold_asset)
                    // Add action buttons
                    .setPositiveButton(
                        R.string.sold
                    ) { _, _ ->
                        val quantityText = (dialogBinding.removeQuantity.text).toString()
                            .trim()
                        var quantity = 0.0

                        try {
                            val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                            quantity = numberFormat.parse(quantityText)!!
                                .toDouble()
                        } catch (e: Exception) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.asset_share_not_empty),
                                Toast.LENGTH_LONG
                            )
                                .show()
                            return@setPositiveButton
                        }
                        if (quantity <= 0.0) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.quantity_not_zero),
                                Toast.LENGTH_LONG
                            )
                                .show()
                            return@setPositiveButton
                        }

                        val priceText = (dialogBinding.removePrice.text).toString()
                            .trim()
                        var price = 0.0
                        try {
                            val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                            price = numberFormat.parse(priceText)!!
                                .toDouble()
                        } catch (e: Exception) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.asset_price_not_empty),
                                Toast.LENGTH_LONG
                            )
                                .show()
                            return@setPositiveButton
                        }
                        if (price < 0.0) {
                            Toast.makeText(
                                requireContext(),
                                getString(R.string.price_not_negative),
                                Toast.LENGTH_LONG
                            )
                                .show()
                            return@setPositiveButton
                        }

                        // Send msg and adjust if more shares than owned are removed.
                        if (quantity > totalQuantity) {
                            Toast.makeText(
                                requireContext(), getString(
                                    R.string.removed_quantity_exceed_existing,
                                    DecimalFormat(DecimalFormatQuantityDigits).format(quantity),
                                    DecimalFormat(DecimalFormatQuantityDigits).format(totalQuantity)
                                ), Toast.LENGTH_LONG
                            )
                                .show()
                            quantity = totalQuantity
                        }

                        val feeText = (dialogBinding.addFee.text).toString()
                            .trim()
                        var fee = 0.0
                        if (feeText.isNotEmpty()) {
                            try {
                                val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                                fee = numberFormat.parse(feeText)!!
                                    .toDouble()
                            } catch (e: Exception) {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.asset_fee_not_valid),
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                                return@setPositiveButton
                            }

                            if (fee < 0.0) {
                                Toast.makeText(
                                    requireContext(),
                                    getString(R.string.fee_not_negative),
                                    Toast.LENGTH_LONG
                                )
                                    .show()
                                return@setPositiveButton
                            }
                        }

                        val localDateTime: ZonedDateTime = ZonedDateTime.of(
                            dialogBinding.datePickerAssetDate.year,
                            dialogBinding.datePickerAssetDate.month + 1,
                            dialogBinding.datePickerAssetDate.dayOfMonth,
                            dialogBinding.datePickerAssetTime.hour,
                            dialogBinding.datePickerAssetTime.minute,
                            0,
                            0,
                            ZoneOffset.systemDefault()
                        )
                        val date = localDateTime.toEpochSecond() // in GMT

                        var accountText = (dialogBinding.textViewAssetAccount.text).toString()
                            .trim()
                        if (accountText == standardAccount) {
                            accountText = ""
                        }

                        val noteText = (dialogBinding.removeNote.text).toString()
                            .trim()

                        //val date = LocalDateTime.now()
                        //    .toEpochSecond(ZoneOffset.UTC)

                        // Add negative shares for removed asset.
                        stockRoomViewModel.addAsset(
                            Asset(
                                symbol = symbol,
                                quantity = -quantity,
                                price = price,
                                account = accountText,
                                fee = fee,
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
                            R.plurals.asset_removed, count,
                            DecimalFormat(DecimalFormatQuantityDigits).format(quantity)
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
      val totalShares = assets?.assets?.sumOf {
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
                  val totalPaidPrice = assets?.assets?.sumOf {
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

        binding.addEventsButton.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            // Get the layout inflater
            val inflater = LayoutInflater.from(requireContext())

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val dialogBinding = DialogAddEventBinding.inflate(inflater)
            builder.setView(dialogBinding.root)
                .setTitle(R.string.add_event)
                // Add action buttons
                .setPositiveButton(
                    R.string.add
                ) { _, _ ->
                    // Add () to avoid cast exception.
                    val title = (dialogBinding.textInputEditEventTitle.text).toString()
                        .trim()
                    // add new event
                    if (title.isEmpty()) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.event_empty),
                            Toast.LENGTH_LONG
                        )
                            .show()
                    } else {
                        val note = (dialogBinding.textInputEditEventNote.text).toString()
                        val datetime: ZonedDateTime = ZonedDateTime.of(
                            dialogBinding.datePickerEventDate.year,
                            dialogBinding.datePickerEventDate.month + 1,
                            dialogBinding.datePickerEventDate.dayOfMonth,
                            dialogBinding.datePickerEventTime.hour,
                            dialogBinding.datePickerEventTime.minute,
                            0,
                            0,
                            ZoneOffset.systemDefault()
                        )
                        val seconds = datetime.toEpochSecond() // in GMT
                        stockRoomViewModel.addEvent(
                            Event(
                                symbol = symbol,
                                type = 0,
                                title = title,
                                note = note,
                                datetime = seconds
                            )
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

        binding.updateNoteButton.setOnClickListener {
            updateNote()
        }
        binding.noteTextView.setOnClickListener {
            updateNote()
        }

        binding.buttonSetName.setOnClickListener {
            val builder = AlertDialog.Builder(requireContext())
            // Get the layout inflater
            val inflater = LayoutInflater.from(requireContext())

            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val dialogBinding = DialogAddNameBinding.inflate(inflater)

            val name = stockDBdata.name.ifEmpty { stockDBdata.symbol }
            dialogBinding.newName.setText(name)

            builder.setView(dialogBinding.root)
                .setTitle(R.string.display_name)
                .setMessage(
                    if (stockDBdata.name.isNotEmpty()) getString(
                        R.string.display_name_desc,
                        stockDBdata.symbol
                    ) else
                        ""
                )
                // Add action buttons
                .setPositiveButton(R.string.change)
                { _, _ ->
                    // Add () to avoid cast exception.
                    val newName = (dialogBinding.newName.text).toString()

                    // Name changed, but not an empty name if name was the symbol name.
                    if (newName != name && !(stockDBdata.symbol == name && newName.isEmpty())) {

                        // Empty name resets to symbol name.
                        if (stockDBdata.symbol == newName || newName.isEmpty()) {

                            stockRoomViewModel.setName(stockDBdata.symbol, "")

                            Toast.makeText(
                                requireContext(), getString(
                                    R.string.name_reset, symbol
                                ), Toast.LENGTH_LONG
                            )
                                .show()

                        } else {

                            stockRoomViewModel.setName(stockDBdata.symbol, newName)

                            if (newName != name) {
                                Toast.makeText(
                                    requireContext(), getString(
                                        R.string.name_added, name, newName
                                    ), Toast.LENGTH_LONG
                                )
                                    .show()
                            }
                        }
                    }
                }
                .setNeutralButton(R.string.reset)
                { _, _ ->

                    if (stockDBdata.name.isNotEmpty()) {

                        // Reset display name.
                        stockRoomViewModel.setName(stockDBdata.symbol, "")

                        Toast.makeText(
                            requireContext(), getString(
                                R.string.name_reset, stockDBdata.symbol
                            ), Toast.LENGTH_LONG
                        )
                            .show()
                    }
                }
                .setNegativeButton(R.string.cancel)
                { _, _ ->
                }
            builder
                .create()
                .show()
        }

        binding.alertAboveInputEditText.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    binding.alertAboveInputLayout.error = ""
                    var valid: Boolean = true
                    if (s != null) {
                        try {
                            val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                            alertAbove = numberFormat.parse(s.toString())
                                .toDouble()
                        } catch (e: NumberFormatException) {
                            binding.alertAboveInputLayout.error = getString(R.string.invalid_number)
                            valid = false
                        } catch (e: Exception) {
                            valid = false
                        }

                        if (valid && alertAbove == 0.0) {
                            binding.alertAboveInputLayout.error = getString(R.string.invalid_number)
                            valid = false
                        }
                        if (valid && alertAbove > 0.0 && alertBelow > 0.0) {
                            if (valid && alertBelow >= alertAbove) {
                                binding.alertAboveInputLayout.error =
                                    getString(R.string.alert_below_error)
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

        binding.alertBelowInputEditText.addTextChangedListener(
            object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    binding.alertBelowInputLayout.error = ""
                    var valid: Boolean = true
                    if (s != null) {
                        try {
                            val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                            alertBelow = numberFormat.parse(s.toString())
                                .toDouble()
                        } catch (e: NumberFormatException) {
                            binding.alertBelowInputLayout.error = getString(R.string.invalid_number)
                            valid = false
                        } catch (e: Exception) {
                            valid = false
                        }
                    }

                    if (valid && alertBelow == 0.0) {
                        binding.alertBelowInputLayout.error = getString(R.string.invalid_number)
                        valid = false
                    }
                    if (valid && alertAbove > 0.0 && alertBelow > 0.0) {
                        if (valid && alertBelow >= alertAbove) {
                            binding.alertBelowInputLayout.error =
                                getString(R.string.alert_above_error)
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
        updateType()

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

    private fun updateNote() {
        val builder = AlertDialog.Builder(requireContext())
        // Get the layout inflater
        val inflater = LayoutInflater.from(requireContext())

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        val dialogBinding = DialogAddNoteBinding.inflate(inflater)

        val note = binding.noteTextView.text
        dialogBinding.textInputEditNote.setText(note)

        builder.setView(dialogBinding.root)
            .setTitle(R.string.note)
            // Add action buttons
            .setPositiveButton(
                R.string.add
            ) { _, _ ->
                // Add () to avoid cast exception.
                val noteText = (dialogBinding.textInputEditNote.text).toString()

                if (noteText != note) {
                    binding.noteTextView.text = noteText
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
        val marketCurrency = SpannableStringBuilder()
        val marketChange = SpannableStringBuilder()

        binding.imageViewSymbol.visibility = View.GONE

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

            val marketCurrencyValue = getCurrency(onlineMarketData)

            if (onlineMarketData.postMarketData) {
                marketPrice.italic { append(marketValues.first) }
                marketChange.italic { append(marketChangeStr) }
                marketCurrency.italic { append(marketCurrencyValue) }
            } else {
                marketPrice.append(marketValues.first)
                marketChange.append(marketChangeStr)
                marketCurrency.append(marketCurrencyValue)
            }

            //binding.imageViewSymbol.visibility = View.GONE
            // val imgUrl = "https://s.yimg.com/uc/fin/img/reports-thumbnails/1.png"
            val imgUrl = onlineMarketData.coinImageUrl
            if (imgUrl.isNotEmpty()) {
                val imgView: ImageView = binding.imageViewSymbol
                val imgUri = imgUrl.toUri()
                // use imgUrl as it is, no need to build upon the https scheme (https://...)
                //.buildUpon()
                //.scheme("https")
                //.build()

                Glide.with(imgView.context)
                    .load(imgUri)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<Drawable?>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            binding.imageViewSymbol.visibility = View.VISIBLE
                            return false
                        }
                    })
                    .into(imgView)
            }
        }

        binding.textViewName.text = name
        binding.textViewMarketPrice.text = marketPrice
        binding.textViewChange.text = marketChange
        binding.textViewMarketCurrency.text = marketCurrency

        val quoteSourceName = onlineMarketData?.quoteSourceName ?: ""
        binding.textViewMarketPriceDelayed.text = when (quoteSourceName) {
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
        if (data.assets != null) {

            val assets: List<Asset> = data.assets?.assets!!
            val (totalQuantity, totalPrice, totalFee) = getAssets(assets)

            val marketPrice = data.onlineMarketData?.marketPrice ?: 0.0
            purchasePriceValue = 0.0
            purchaseQuantity = 0.0

            // Display the asset even if the purchase price is 0.0
            if (data.onlineMarketData != null && totalQuantity > 0.0 && marketPrice > 0.0) {
                val assetChange = getAssetChange(
                    assets,
                    data.onlineMarketData?.marketPrice!!,
                    data.onlineMarketData?.postMarketData!!,
                    Color.DKGRAY,
                    requireActivity()
                ).displayColorStr

                val asset = SpannableStringBuilder()
                    .append(assetChange)
                    .append("\n")
                    .bold {
                        append(DecimalFormat(DecimalFormat2Digits).format(totalQuantity * marketPrice))
                    }

                binding.textViewAssetChange.visibility = View.VISIBLE
                binding.textViewAssetChange.text = asset
            } else {
                binding.textViewAssetChange.visibility = View.GONE
            }

            if (totalQuantity > 0.0 && totalPrice + totalFee > 0.0) {

                binding.pricePreviewDivider.visibility = View.VISIBLE
                binding.pricePreviewTextview.visibility = View.VISIBLE
                binding.pricePreviewLayout.visibility = View.VISIBLE

                binding.textViewPurchasePrice.visibility = View.VISIBLE

                purchasePriceValue = totalPrice / totalQuantity
                purchaseQuantity = totalQuantity

                // Update the new price and asset.
                binding.pickerKnob.onValueChangeListener { value ->
                    binding.newStockPrice.text =
                        when {
                            marketPrice > 5.0 || purchasePriceValue > 5.0 -> {
                                DecimalFormat(DecimalFormat2Digits).format(value)
                            }
                            marketPrice < 0.0001 || purchasePriceValue < 0.0001 -> {
                                DecimalFormat(DecimalFormat2To8Digits).format(value)
                            }
                            else -> {
                                DecimalFormat(DecimalFormat2To4Digits).format(value)
                            }
                        }

                    val assetChange = getAssetChange(
                        totalQuantity,
                        totalPrice,
                        value,
                        data.onlineMarketData?.postMarketData ?: false,
                        Color.DKGRAY,
                        requireActivity()
                    )

                    val asset = SpannableStringBuilder()
                        .append(assetChange.displayColorStr)
                        .append("\n")
                        .bold {
                            append(DecimalFormat(DecimalFormat2Digits).format(totalQuantity * value))
                        }

                    binding.newTotalAsset.text = asset
                }

                val purchasePrice = SpannableStringBuilder()
                if (totalPrice >= 0.0001) {
                    // Bought for %1$s\n%2$s@%1$s%3$s = %4$s
                    purchasePrice.append(
                        getString(
                            R.string.bought_for
                        )
                    )
                    // %1$s
                    purchasePrice.append(" ")
                    // Display 'bought price' exact as possible. Do not use to2To8Digits()
                    purchasePrice.append(
                        DecimalFormat(DecimalFormat2To8Digits).format(
                            purchasePriceValue
                        )
                    )
                    purchasePrice.append("\n")
                    // %2$s
                    purchasePrice.append(
                        DecimalFormat(DecimalFormatQuantityDigits).format(
                            totalQuantity
                        )
                    )
                    purchasePrice.append("@")
                    // Display 'bought price' rounded.
                    purchasePrice.append(
                        DecimalFormat(DecimalFormat2To4Digits).format(
                            purchasePriceValue
                        )
                    )
                    // %3$s
                    if (totalFee > 0.0) {
                        purchasePrice.scale(feeScale) {
                            append("+${DecimalFormat(DecimalFormat2To4Digits).format(totalFee)}")
                        }
                    }
                    // %4$s
                    purchasePrice.append(" = ")
                    purchasePrice.append(DecimalFormat(DecimalFormat2Digits).format(totalPrice + totalFee))
                }

                binding.textViewPurchasePrice.text = purchasePrice

                val currentPrice = if (data.onlineMarketData != null && marketPrice > 0.0) {
                    if (!isOnline) {
                        isOnline = true
                        // Reset when the first online data is ready.
                        binding.pickerKnob.setValue(0.0, 0.0, 0.0)
                    }
                    marketPrice
                } else {
                    purchasePriceValue
                }

                // min, max, start
                binding.pickerKnob.setValue(currentPrice / 10, 4 * currentPrice, currentPrice)
                return
            }
        } else {
            binding.textViewAssetChange.visibility = View.GONE
        }

        binding.pricePreviewDivider.visibility = View.GONE
        binding.pricePreviewTextview.visibility = View.GONE
        binding.pricePreviewLayout.visibility = View.GONE

        binding.textViewPurchasePrice.visibility = View.GONE
    }

    private fun updateAlerts() {
        // If both are set, below value must be smaller than the above value.
        if (!(alertAbove > 0.0 && alertBelow > 0.0 && alertBelow >= alertAbove)) {

            // Add () to avoid cast exception.
            val alertAboveNote = (binding.alertAboveNoteInputEditText.text).toString()

            if (stockDBdata.alertAbove != alertAbove || stockDBdata.alertAboveNote != alertAboveNote) {
                stockRoomViewModel.updateAlertAboveSync(symbol, alertAbove, alertAboveNote)
            }

            // Add () to avoid cast exception.
            val alertBelowNote = (binding.alertBelowNoteInputEditText.text).toString()

            if (stockDBdata.alertBelow != alertBelow || stockDBdata.alertBelowNote != alertBelowNote) {
                stockRoomViewModel.updateAlertBelowSync(symbol, alertBelow, alertBelowNote)
            }
        }
    }

    private fun updateType() {
        val selectedType = binding.dataproviderSpinner.selectedItemPosition
        if (type.value != selectedType) {
            stockRoomViewModel.setType(symbol, selectedType)
        }
    }

    private fun updateStockViewRange(_stockViewRange: StockViewRange) {
        chartDataItems.clear()
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
        if (chartDataItems.isEmpty()) {
            getData(stockViewRange)
        } else {
            loadCharts(stockViewRange, stockViewMode)
        }
    }

    private fun getData(stockViewRange: StockViewRange) {

        val stockSymbol = StockSymbol(
            symbol = symbol,
            type = type
        )
        stockChartDataViewModel.getChartData(stockSymbol, stockViewRange)

        if (useChartOverlaySymbols) {
            chartOverlaySymbols.split(",").take(MaxChartOverlays).forEach { symbolRef ->

                // Cache the type for each symbol used by the ref symbols.
                // stockRoomViewModel.allStockItems.observe is not ready yet.
                if (!symbolTypesMap.containsKey(symbolRef)) {
                    symbolTypesMap[symbolRef] =
                        dataProviderFromInt(stockRoomViewModel.getTypeSync(symbolRef))
                }
                val stockSymbolRef = StockSymbol(
                    symbol = symbolRef,
                    type = symbolTypesMap[symbolRef]!!
                )
                stockChartDataViewModel.getChartData(stockSymbolRef, stockViewRange)
            }
        }
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
        stockViewRange: StockViewRange,
        stockViewMode: StockViewMode
    ) {
        when (stockViewMode) {
            StockViewMode.Line -> {
                loadLineChart(stockViewRange)
            }
            StockViewMode.Candle -> {
                loadCandleStickChart(stockViewRange)
            }
        }
    }

    private val rangeButtons: List<Button> by lazy {
        listOf<Button>(
            binding.buttonOneDay,
            binding.buttonFiveDays,
            binding.buttonOneMonth,
            binding.buttonThreeMonth,
            binding.buttonYTD,
            binding.buttonOneYear,
            binding.buttonFiveYears,
            binding.buttonMax
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
                    val stockDataEntries = chartDataItems[symbol]
                    if (stockDataEntries != null) {
                        IndexAxisValueFormatter(stockDataEntries.map { stockDataEntry ->
                            val date =
                                ZonedDateTime.ofInstant(
                                    Instant.ofEpochSecond(stockDataEntry.dateTimePoint),
                                    ZoneOffset.systemDefault()
                                )
                            date.format(axisTimeFormatter)
                        })
                    } else {
                        IndexAxisValueFormatter()
                    }
                }
                else -> {
                    val stockDataEntries = chartDataItems[symbol]
                    if (stockDataEntries != null) {
                        IndexAxisValueFormatter(stockDataEntries.map { stockDataEntry ->
                            val date =
                                ZonedDateTime.ofInstant(
                                    Instant.ofEpochSecond(stockDataEntry.dateTimePoint),
                                    ZoneOffset.systemDefault()
                                )
                            date.format(xAxisDateFormatter)
                        })
                    } else {
                        IndexAxisValueFormatter()
                    }
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
                binding.buttonOneDay.isEnabled = false
                binding.textViewRange.text = getString(R.string.one_day_range)
            }
            StockViewRange.FiveDays -> {
                binding.buttonFiveDays.isEnabled = false
                binding.textViewRange.text = getString(R.string.five_days_range)
            }
            StockViewRange.OneMonth -> {
                binding.buttonOneMonth.isEnabled = false
                binding.textViewRange.text = getString(R.string.one_month_range)
            }
            StockViewRange.ThreeMonth -> {
                binding.buttonThreeMonth.isEnabled = false
                binding.textViewRange.text = getString(R.string.three_month_range)
            }
            StockViewRange.YTD -> {
                binding.buttonYTD.isEnabled = false
                binding.textViewRange.text = getString(R.string.ytd_range)
            }
            StockViewRange.OneYear -> {
                binding.buttonOneYear.isEnabled = false
                binding.textViewRange.text = getString(R.string.one_year_range)
            }
            StockViewRange.FiveYears -> {
                binding.buttonFiveYears.isEnabled = false
                binding.textViewRange.text = getString(R.string.five_year_range)
            }
            StockViewRange.Max -> {
                binding.buttonMax.isEnabled = false
                binding.textViewRange.text = getString(R.string.max_range)
            }
        }

        when (stockViewMode) {
            StockViewMode.Line -> {
                binding.lineChart.visibility = View.VISIBLE
                binding.candleStickChart.visibility = View.GONE
                binding.imageButtonIconLine.visibility = View.VISIBLE
                binding.imageButtonIconCandle.visibility = View.GONE
            }
            StockViewMode.Candle -> {
                binding.lineChart.visibility = View.GONE
                binding.candleStickChart.visibility = View.VISIBLE
                binding.imageButtonIconLine.visibility = View.GONE
                binding.imageButtonIconCandle.visibility = View.VISIBLE
            }
        }

        if (type == DataProvider.Coingecko) {
            binding.imageButtonIconLine.visibility = View.GONE
            binding.imageButtonIconCandle.visibility = View.GONE
        }
    }

    private fun setupCandleStickChart() {
        val candleStickChart
                : CandleStickChart = binding.candleStickChart
        candleStickChart.isDoubleTapToZoomEnabled = false

        candleStickChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        candleStickChart.xAxis.setDrawAxisLine(true)
        candleStickChart.xAxis.setDrawGridLines(false)
        candleStickChart.xAxis.textColor = context?.getColor(R.color.black)!!

        candleStickChart.axisRight.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        candleStickChart.axisRight.setDrawAxisLine(true)
        candleStickChart.axisRight.setDrawGridLines(true)
        candleStickChart.axisRight.textColor = context?.getColor(R.color.black)!!
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
        stockViewRange: StockViewRange
    ) {
        val candleStickChart: CandleStickChart = binding.candleStickChart
        candleStickChart.candleData?.clearValues()

        val candleEntries: MutableList<CandleEntry> = mutableListOf()
        val seriesList: MutableList<ICandleDataSet> = mutableListOf()

        val stockDataEntries = chartDataItems[symbol]
        var minY = Float.MAX_VALUE
        var maxY = 0f
        if (stockDataEntries != null && stockDataEntries.isNotEmpty()) {
            stockDataEntries.forEach { stockDataEntry ->
                minY = minOf(minY, stockDataEntry.candleEntry.y)
                maxY = maxOf(maxY, stockDataEntry.candleEntry.y)
                candleEntries.add(stockDataEntry.candleEntry)
            }

            // Chart data is constant, add a zero point for correct scaling of the control.
            if (minY == maxY) {
                candleEntries.add(CandleEntry(0f, 0f, 0f, 0f, 0f))
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

            // Get the ref chart data.
            if (useChartOverlaySymbols && chartOverlaySymbols.isNotEmpty()) {
                chartOverlaySymbols.split(",").take(MaxChartOverlays)
                    .forEachIndexed { index, symbolRef ->
                        val stockDataEntriesRef = chartDataItems[symbolRef]
                        if (chartOverlaySymbolsEnableList[index] && stockDataEntriesRef != null && stockDataEntriesRef.size > 1) {

                            var minRefY = Float.MAX_VALUE
                            var maxRefY = 0f

                            val refList = ArrayList<StockDataEntry>()

                            // Reference charts might not cover the same time line.
                            // Map the time points from the stockDataEntries to the stockDataEntriesRef points.
                            var indexRef = 0
                            var entriesRef1 = stockDataEntriesRef[indexRef]
                            var entriesRef2 = stockDataEntriesRef[indexRef + 1]

                            // Align the date points on stockDataEntries.
                            stockDataEntries.forEach { stockDataEntry ->

                                // Match stockDataEntriesRef to the stockDataEntry point.
                                while (stockDataEntry.dateTimePoint >= entriesRef2.dateTimePoint
                                ) {
                                    // Check the next point to match.
                                    if (indexRef < stockDataEntriesRef.size - 1) {
                                        indexRef++
                                        entriesRef1 = stockDataEntriesRef[indexRef]
                                        if (indexRef < stockDataEntriesRef.size - 2) {
                                            entriesRef2 = stockDataEntriesRef[indexRef + 1]
                                        } else {
                                            break
                                        }
                                    } else {
                                        break
                                    }
                                }

                                minRefY = minOf(minRefY, entriesRef1.candleEntry.y)
                                maxRefY = maxOf(maxRefY, entriesRef1.candleEntry.y)

                                // clone entry and update the x value to match the original stock chart
                                val refEntry = StockDataEntry(
                                    dateTimePoint = entriesRef1.dateTimePoint,
                                    x = stockDataEntry.candleEntry.x.toDouble(),
                                    high = entriesRef1.candleEntry.high.toDouble(),
                                    low = entriesRef1.candleEntry.low.toDouble(),
                                    open = entriesRef1.candleEntry.open.toDouble(),
                                    close = entriesRef1.candleEntry.close.toDouble(),
                                )
                                refList.add(refEntry)
                            }

                            // Include the right side value of the last entry.
                            minRefY = minOf(minRefY, entriesRef2.candleEntry.y)
                            maxRefY = maxOf(maxRefY, entriesRef2.candleEntry.y)

                            // Scale ref data to stock data so that the ref stock data will always look the same in each stock chart.
                            if (refList.isNotEmpty() && maxRefY > minRefY && maxRefY > 0f && maxY > minY && maxY > 0f) {
                                val scale = (maxY - minY) / (maxRefY - minRefY)

                                val candleEntriesRef = refList.map { stockDataEntry ->
                                    CandleEntryRef(
                                        stockDataEntry.candleEntry.x,
                                        shadowH = (stockDataEntry.candleEntry.high - minRefY) * scale + minY,
                                        shadowL = (stockDataEntry.candleEntry.low - minRefY) * scale + minY,
                                        open = (stockDataEntry.candleEntry.open - minRefY) * scale + minY,
                                        close = (stockDataEntry.candleEntry.close - minRefY) * scale + minY,
                                        refCandleEntry = stockDataEntry.candleEntry // original data for the marker display
                                    )
                                }

                                val seriesRef: CandleDataSet =
                                    CandleDataSet(candleEntriesRef, symbolRef)
                                val color = chartOverlayColors[index % chartOverlayColors.size]

                                seriesRef.color = color
                                seriesRef.shadowColor = color
                                seriesRef.shadowWidth = 1f
                                seriesRef.decreasingColor = Color.rgb(255, 204, 204)
                                seriesRef.decreasingPaintStyle = Paint.Style.FILL
                                seriesRef.increasingColor = Color.rgb(204, 255, 204)
                                seriesRef.increasingPaintStyle = Paint.Style.FILL
                                seriesRef.neutralColor = color
                                seriesRef.setDrawValues(false)

                                seriesList.add(seriesRef)
                            }
                        }
                    }
            }

            seriesList.add(series)

            val candleData = CandleData(seriesList)
            candleStickChart.data = candleData
            candleStickChart.xAxis.valueFormatter = xAxisFormatter

//            val digits = if (candleData.yMax < 1.0) {
//                4
//            } else {
//                2
//            }
//            candleStickChart.axisRight.valueFormatter = DefaultValueFormatter(digits)
            candleStickChart.axisRight.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) =
                    to2To8Digits(value)
            }

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
                    TextMarkerViewCandleChart(
                        requireContext(),
                        axisDateTimeFormatter,
                        stockDataEntries
                    )
            }
            else -> {
                candleStickChart.marker =
                    TextMarkerViewCandleChart(requireContext(), axisDateFormatter, stockDataEntries)
            }
        }

        candleStickChart.invalidate()
    }

    private fun setupLineChart() {
        val lineChart: LineChart = binding.lineChart
        lineChart.isDoubleTapToZoomEnabled = false

        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.setDrawAxisLine(true)
        lineChart.xAxis.setDrawGridLines(false)
        lineChart.xAxis.textColor = context?.getColor(R.color.black)!!

        lineChart.axisRight.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART)
        lineChart.axisRight.setDrawAxisLine(true)
        lineChart.axisRight.setDrawGridLines(true)
        lineChart.axisRight.textColor = context?.getColor(R.color.black)!!
        lineChart.axisRight.isEnabled = true

        lineChart.axisLeft.setDrawAxisLine(false)
        lineChart.axisLeft.isEnabled = false

        lineChart.extraBottomOffset = resources.getDimension(R.dimen.graph_bottom_offset)
        lineChart.legend.isEnabled = false
        lineChart.description = null
        lineChart.setNoDataText("")
    }

    private fun loadLineChart(
        stockViewRange: StockViewRange
    ) {
        val lineChart: LineChart = binding.lineChart
        lineChart.setNoDataText("")
        lineChart.lineData?.clearValues()

        val seriesList: MutableList<ILineDataSet> = mutableListOf()

        val dataPoints = ArrayList<DataPoint>()
        var minY = Float.MAX_VALUE
        var maxY = 0f
        val stockDataEntries = chartDataItems[symbol]
        if (stockDataEntries != null && stockDataEntries.isNotEmpty()) {
            stockDataEntries.forEach { stockDataEntry ->
                minY = minOf(minY, stockDataEntry.candleEntry.y)
                maxY = maxOf(maxY, stockDataEntry.candleEntry.y)
                dataPoints.add(
                    DataPoint(
                        stockDataEntry.candleEntry.x,
                        stockDataEntry.candleEntry.y
                    )
                )
            }

            // Chart data is constant, add a zero point for correct scaling of the control.
            if (minY == maxY) {
                dataPoints.add(DataPoint(0f, 0f))
            }

            val series = LineDataSet(dataPoints as List<DataPoint>, symbol)

            series.setDrawHorizontalHighlightIndicator(false)
            series.setDrawValues(false)
            series.setDrawFilled(true)
            series.setDrawCircles(false)
            series.color = context?.getColor(R.color.chartLine)!!
            series.fillColor = context?.getColor(R.color.chartLine)!!

            // Get the ref chart data.
            if (useChartOverlaySymbols && chartOverlaySymbols.isNotEmpty()) {
                chartOverlaySymbols.split(",").take(MaxChartOverlays)
                    .forEachIndexed { index, symbolRef ->
                        val stockDataEntriesRef = chartDataItems[symbolRef]
                        if (chartOverlaySymbolsEnableList[index] && stockDataEntriesRef != null && stockDataEntriesRef.size > 1) {

                            var minRefY = Float.MAX_VALUE
                            var maxRefY = 0f

                            val refList = ArrayList<StockDataEntry>()

                            // Reference charts might not cover the same time line.
                            // Map the time points from the stockDataEntries to the stockDataEntriesRef points.
                            var indexRef = 0
                            var entriesRef1 = stockDataEntriesRef[indexRef]
                            var entriesRef2 = stockDataEntriesRef[indexRef + 1]

                            // Align the date points on stockDataEntries.
                            stockDataEntries.forEach { stockDataEntry ->

                                // Match stockDataEntriesRef to the stockDataEntry point.
                                while (stockDataEntry.dateTimePoint >= entriesRef2.dateTimePoint
                                ) {
                                    // Check the next point to match.
                                    if (indexRef < stockDataEntriesRef.size - 1) {
                                        indexRef++
                                        entriesRef1 = stockDataEntriesRef[indexRef]
                                        if (indexRef < stockDataEntriesRef.size - 2) {
                                            entriesRef2 = stockDataEntriesRef[indexRef + 1]
                                        } else {
                                            break
                                        }
                                    } else {
                                        break
                                    }
                                }

                                minRefY = minOf(minRefY, entriesRef1.candleEntry.y)
                                maxRefY = maxOf(maxRefY, entriesRef1.candleEntry.y)

                                // clone entry and update the x value to match the original stock chart
                                val refEntry = StockDataEntry(
                                    dateTimePoint = entriesRef1.dateTimePoint,
                                    x = stockDataEntry.candleEntry.x.toDouble(),
                                    high = entriesRef1.candleEntry.high.toDouble(),
                                    low = entriesRef1.candleEntry.low.toDouble(),
                                    open = entriesRef1.candleEntry.open.toDouble(),
                                    close = entriesRef1.candleEntry.close.toDouble(),
                                )
                                refList.add(refEntry)
                            }

                            // Include the right side value of the last entry.
                            minRefY = minOf(minRefY, entriesRef2.candleEntry.y)
                            maxRefY = maxOf(maxRefY, entriesRef2.candleEntry.y)

                            // Scale ref data to stock data so that the ref stock data will always look the same in each stock chart.
                            if (refList.isNotEmpty() && maxRefY > minRefY && maxRefY > 0f && maxY > minY && maxY > 0f) {
                                val scale = (maxY - minY) / (maxRefY - minRefY)

                                val dataPointsRef = refList.map { stockDataEntry ->
                                    DataPointRef(
                                        x = stockDataEntry.candleEntry.x,
                                        y = (stockDataEntry.candleEntry.y - minRefY) // shift down ref data
                                                * scale                              // scale ref to match stock data range
                                                + minY,                              // shift up to min stock data
                                        refY = stockDataEntry.candleEntry.y          // original y for the marker display
                                    )
                                }

                                val seriesRef =
                                    LineDataSet(dataPointsRef as List<DataPointRef>, symbolRef)
                                val color = chartOverlayColors[index % chartOverlayColors.size]

                                seriesRef.setDrawHorizontalHighlightIndicator(false)
                                seriesRef.setDrawValues(false)
                                seriesRef.setDrawCircles(false)
                                seriesRef.color = color

                                // No filling for overlay graphs.
                                seriesRef.setDrawFilled(false)
                                //seriesRef.fillColor = color

                                seriesList.add(seriesRef)
                            }
                        }
                    }
            }

            seriesList.add(series)

//    // FFT
//    val dataPointsFFT = GoertzelFFT(dataPoints)
//    val seriesFFT = LineDataSet(dataPointsFFT as List<DataPoint>, symbol)
//
//    seriesFFT.setDrawHorizontalHighlightIndicator(false)
//    seriesFFT.setDrawValues(false)
//    seriesFFT.setDrawFilled(true)
//    seriesFFT.setDrawCircles(false)
//    seriesFFT.color = Color.RED
//    seriesFFT.fillColor = Color.MAGENTA
//
//    seriesList.add(seriesFFT)

            // Add line points (circles) for transaction dates.
            val assetTimeEntriesCopy: MutableList<AssetsTimeData> = mutableListOf()
            assetTimeEntriesCopy.addAll(assetTimeEntries)

            if (assetTimeEntriesCopy.isNotEmpty()) {
                var i: Int = 0

                val lastIndex = stockDataEntries.size - 2
                while (i < stockDataEntries.size - 1) {

                    if (assetTimeEntriesCopy.isEmpty()) {
                        break
                    }

                    for (j in assetTimeEntriesCopy.indices) {

                        val t: Long = assetTimeEntriesCopy[j].date
                        val a = stockDataEntries[i].dateTimePoint
                        val b = stockDataEntries[i + 1].dateTimePoint
                        // In the time interval a..b or just bought in after hours >= b
                        if ((a <= t && t < b) || (i == lastIndex && t >= b)) {
                            // use the index where the value is closest to t
                            // a <= t < b
                            // k=i: if t is closer to a
                            // k=i+1: if t is closer to b
                            val k = if (t <= (a + b) / 2) i else i + 1

                            // Data points > 0.0 are blue, and Data points = 0.0 are yellow using the current value.
                            // For example rewarded stocks have bought=0.0, but would distort the diagram.
                            val isDataPoint = assetTimeEntriesCopy[j].price > 0.0
                            val value = if (isDataPoint) {
                                assetTimeEntriesCopy[j].price.toFloat()
                            } else {
                                stockDataEntries[i].candleEntry.y
                            }

                            val transactionPoints = listOf(
                                DataPoint(
                                    stockDataEntries[k].candleEntry.x,
                                    value
                                )
                            )

                            val transactionSeries =
                                LineDataSet(transactionPoints as List<DataPoint>, symbol)

                            transactionSeries.setCircleColor(
                                if (assetTimeEntriesCopy[j].quantity > 0.0) {
                                    Color.BLUE  // bought
                                } else {
                                    0xffFF6A00.toInt() // sold, FF6A00=Orange
                                }
                            )

//                            val color = if (assetTimeEntriesCopy[j].bought) {
//                                if (isDataPoint) {
//                                    Color.BLUE  // data point > 0.0
//                                } else {
//                                    0xffC23FFF.toInt()  // data point = 0.0, C23FF=Violet
//                                }
//                            } else {
//                                0xffFF6A00.toInt()  // FF6A00=Orange
//                            }

                            //transactionSeries.setDrawCircleHole(false)

                            seriesList.add(transactionSeries)

                            // Item is painted and can be removed. No need to keep this item for the remainder of the loop.
                            assetTimeEntriesCopy.removeAt(j)
                            break
                        }
                    }

                    i++
                }
            }

            val lineData = LineData(seriesList)

            lineChart.data = lineData
            lineChart.xAxis.valueFormatter = xAxisFormatter

//            val digits = if (lineData.yMax < 1.0) {
//                4
//            } else {
//                2
//            }
//            lineChart.axisRight.valueFormatter = DefaultValueFormatter(digits)
            lineChart.axisRight.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) =
                    to2To8Digits(value)
            }

            when (stockViewRange) {
                StockViewRange.OneDay -> {
                    lineChart.marker =
                        TextMarkerViewLineChart(
                            requireContext(),
                            axisTimeFormatter,
                            stockDataEntries
                        )
                }
                StockViewRange.FiveDays, StockViewRange.OneMonth -> {
                    lineChart.marker =
                        TextMarkerViewLineChart(
                            requireContext(),
                            axisDateTimeFormatter,
                            stockDataEntries
                        )
                }
                else -> {
                    lineChart.marker =
                        TextMarkerViewLineChart(
                            requireContext(),
                            axisDateFormatter,
                            stockDataEntries
                        )
                }
            }
        }

        lineChart.invalidate()
    }

    private fun updateLegend(index: Int) {
        chartOverlaySymbolsEnableList[index - 1] = !chartOverlaySymbolsEnableList[index - 1]

        loadCharts(stockViewRange, stockViewMode)
    }
}