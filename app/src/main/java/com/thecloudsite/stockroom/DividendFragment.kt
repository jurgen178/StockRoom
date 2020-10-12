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
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.DatePicker
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.text.bold
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.thecloudsite.stockroom.DividendCycle.Quarterly
import com.thecloudsite.stockroom.DividendType.Announced
import com.thecloudsite.stockroom.DividendType.Received
import com.thecloudsite.stockroom.MainActivity.Companion.onlineDataTimerDelay
import com.thecloudsite.stockroom.database.Assets
import com.thecloudsite.stockroom.database.Dividend
import com.thecloudsite.stockroom.database.Dividends
import com.thecloudsite.stockroom.utils.dividendCycleToSelection
import com.thecloudsite.stockroom.utils.dividendSelectionToCycle
import com.thecloudsite.stockroom.utils.getAssets
import kotlinx.android.synthetic.main.fragment_dividend.addDividendAnnouncedButton
import kotlinx.android.synthetic.main.fragment_dividend.addDividendReceivedButton
import kotlinx.android.synthetic.main.fragment_dividend.dividendNotesTextView
import kotlinx.android.synthetic.main.fragment_dividend.dividendsAnnouncedView
import kotlinx.android.synthetic.main.fragment_dividend.dividendsReceivedView
import kotlinx.android.synthetic.main.fragment_dividend.textViewDividend
import kotlinx.android.synthetic.main.fragment_dividend.updateDividendNotesButton
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM
import java.util.Locale

enum class DividendType(val value: Int) {
  Received(0),
  Announced(1),
}

enum class DividendCycle(val value: Int) {
  Monthly(12),
  Quarterly(4),
  SemiAnnual(2),
  Annual(1),
}

// Enable scrolling by disable parent scrolling
class CustomDatePicker(
  context: Context?,
  attrs: AttributeSet?
) :
    DatePicker(context, attrs) {
  override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
    if (ev.actionMasked == MotionEvent.ACTION_DOWN) {
      parent?.requestDisallowInterceptTouchEvent(true)
    }
    return false
  }
}

class DividendFragment : Fragment() {

  private lateinit var stockRoomViewModel: StockRoomViewModel

  private val assetChange = AssetsLiveData()
  private val assetChangeLiveData = MediatorLiveData<AssetsLiveData>()

  companion object {
    fun newInstance() = DividendFragment()
  }

  lateinit var onlineDataHandler: Handler

  private var symbol: String = ""

  private fun dividendReceivedItemUpdateClicked(dividend: Dividend) {
    val builder = AlertDialog.Builder(requireContext())
    // Get the layout inflater
    val inflater = LayoutInflater.from(requireContext())

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    val dialogView = inflater.inflate(R.layout.dialog_add_dividend, null)
    dialogView.findViewById<TextView>(R.id.textViewDividendExDate).visibility = View.GONE
    dialogView.findViewById<DatePicker>(R.id.datePickerDividendExDate).visibility = View.GONE
    val addUpdateDividendHeadlineView =
      dialogView.findViewById<TextView>(R.id.addUpdateDividendHeadline)
    addUpdateDividendHeadlineView.text = getString(R.string.update_dividend)
    val addDividendView = dialogView.findViewById<TextView>(R.id.addDividend)
    addDividendView.text = DecimalFormat("0.##").format(dividend.amount)
    val datePickerDividendDateView =
      dialogView.findViewById<DatePicker>(R.id.datePickerDividendDate)
    val localDateTime = LocalDateTime.ofEpochSecond(dividend.paydate, 0, ZoneOffset.UTC)
    // month is starting from zero
    datePickerDividendDateView.updateDate(
        localDateTime.year, localDateTime.month.value - 1, localDateTime.dayOfMonth
    )

    val textViewDividendCycleSpinner =
      dialogView.findViewById<Spinner>(R.id.textViewDividendCycleSpinner)
    textViewDividendCycleSpinner.setSelection(dividendCycleToSelection(dividend.cycle))

    builder.setView(dialogView)
        // Add action buttons
        .setPositiveButton(
            R.string.update
        ) { _, _ ->
          // Add () to avoid cast exception.
          val addDividendText = (addDividendView.text).toString()
              .trim()
          if (addDividendText.isNotEmpty()) {
            var dividendAmount = 0.0
            var valid = true
            try {
              val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
              dividendAmount = numberFormat.parse(addDividendText)!!
                  .toDouble()
            } catch (e: Exception) {
              valid = false
            }
            if (dividendAmount <= 0.0) {
              Toast.makeText(
                  requireContext(), getString(R.string.dividend_not_zero), Toast.LENGTH_LONG
              )
                  .show()
              valid = false
            }

            if (valid) {
              val datetime: LocalDateTime = LocalDateTime.of(
                  datePickerDividendDateView.year, datePickerDividendDateView.month + 1,
                  datePickerDividendDateView.dayOfMonth, 0, 0
              )
              val seconds = datetime.toEpochSecond(ZoneOffset.UTC)

              val cycle =
                dividendSelectionToCycle(textViewDividendCycleSpinner.selectedItemPosition)

              if (dividend.amount != dividendAmount
                  || dividend.cycle != cycle
                  || dividend.paydate != seconds
              ) {
                stockRoomViewModel.updateDividend2(
                    dividend,
                    Dividend(
                        symbol = symbol,
                        amount = dividendAmount,
                        type = Received.value,
                        cycle = cycle,
                        paydate = seconds,
                        exdate = 0L,
                        note = ""
                    )
                )

                Toast.makeText(
                    requireContext(), getString(R.string.dividend_updated), Toast.LENGTH_LONG
                )
                    .show()
              }
            }
          } else {
            Toast.makeText(requireContext(), getString(R.string.invalid_entry), Toast.LENGTH_LONG)
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

  private fun dividendReceivedItemDeleteClicked(
    symbol: String?,
    dividend: Dividend?,
    dividendList: List<Dividend>?
  ) {
    // Summary tag?
    if (symbol != null && dividend == null && dividendList != null) {
      android.app.AlertDialog.Builder(requireContext())
          .setTitle(R.string.delete_all_dividends)
          .setMessage(getString(R.string.delete_all_dividends_confirm))
          .setPositiveButton(R.string.delete) { _, _ ->
            dividendList.forEach { dividend ->
              stockRoomViewModel.deleteDividend(dividend)
            }

            Toast.makeText(
                requireContext(), getString(R.string.delete_all_dividends_msg), Toast.LENGTH_LONG
            )
                .show()
          }
          .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
          .show()
    } else if (dividend != null && dividendList == null) {
      val localDateTime = LocalDateTime.ofEpochSecond(dividend.paydate, 0, ZoneOffset.UTC)
      android.app.AlertDialog.Builder(requireContext())
          .setTitle(R.string.delete_dividend)
          .setMessage(
              getString(
                  R.string.delete_dividend_confirm, DecimalFormat("0.##").format(dividend.amount),
                  localDateTime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
              )
          )
          .setPositiveButton(R.string.delete) { _, _ ->
            stockRoomViewModel.deleteDividend(dividend)
            Toast.makeText(
                requireContext(), R.string.dividend_deleted, Toast.LENGTH_LONG
            )
                .show()
          }
          .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
          .show()
    }
  }

  private fun dividendAnnouncedItemUpdateClicked(dividend: Dividend) {
    val builder = AlertDialog.Builder(requireContext())
    // Get the layout inflater
    val inflater = LayoutInflater.from(requireContext())

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    val dialogView = inflater.inflate(R.layout.dialog_add_dividend, null)
    val addUpdateDividendHeadlineView =
      dialogView.findViewById<TextView>(R.id.addUpdateDividendHeadline)
    addUpdateDividendHeadlineView.text = getString(R.string.update_dividend)
    val addDividendView = dialogView.findViewById<TextView>(R.id.addDividend)
    addDividendView.text = DecimalFormat("0.##").format(dividend.amount)

    val datePickerDividendDateView =
      dialogView.findViewById<DatePicker>(R.id.datePickerDividendDate)
    val localDateTime = LocalDateTime.ofEpochSecond(dividend.paydate, 0, ZoneOffset.UTC)
    // month is starting from zero
    datePickerDividendDateView.updateDate(
        localDateTime.year, localDateTime.month.value - 1, localDateTime.dayOfMonth
    )

    val datePickerDividendExDateView =
      dialogView.findViewById<DatePicker>(R.id.datePickerDividendExDate)
    val localDateTimeEx = LocalDateTime.ofEpochSecond(dividend.exdate, 0, ZoneOffset.UTC)
    // month is starting from zero
    datePickerDividendExDateView.updateDate(
        localDateTimeEx.year, localDateTimeEx.month.value - 1, localDateTimeEx.dayOfMonth
    )

    val textViewDividendCycleSpinner =
      dialogView.findViewById<Spinner>(R.id.textViewDividendCycleSpinner)
    textViewDividendCycleSpinner.setSelection(dividendCycleToSelection(dividend.cycle))

    builder.setView(dialogView)
        // Add action buttons
        .setPositiveButton(
            R.string.update
        ) { _, _ ->
          // Add () to avoid cast exception.
          val addDividendText = (addDividendView.text).toString()
              .trim()
          var dividendAmount = 0.0
          if (addDividendText.isNotEmpty()) {
            try {
              val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
              dividendAmount = numberFormat.parse(addDividendText)!!
                  .toDouble()
            } catch (e: Exception) {
            }
          }

          val datetime: LocalDateTime = LocalDateTime.of(
              datePickerDividendDateView.year, datePickerDividendDateView.month + 1,
              datePickerDividendDateView.dayOfMonth, 0, 0
          )
          val seconds = datetime.toEpochSecond(ZoneOffset.UTC)

          val datetimeEx: LocalDateTime = LocalDateTime.of(
              datePickerDividendExDateView.year, datePickerDividendExDateView.month + 1,
              datePickerDividendExDateView.dayOfMonth, 0, 0
          )
          val secondsEx = datetimeEx.toEpochSecond(ZoneOffset.UTC)

          val cycle = dividendSelectionToCycle(textViewDividendCycleSpinner.selectedItemPosition)

          if (dividend.amount != dividendAmount
              || dividend.cycle != cycle
              || dividend.paydate != seconds
              || dividend.exdate != secondsEx
          ) {
            stockRoomViewModel.updateDividend2(
                dividend,
                Dividend(
                    symbol = symbol,
                    amount = dividendAmount,
                    type = Announced.value,
                    cycle = cycle,
                    paydate = seconds,
                    exdate = secondsEx,
                    note = ""
                )
            )

            Toast.makeText(
                requireContext(), getString(R.string.dividend_updated), Toast.LENGTH_LONG
            )
                .show()
          }
        }
        .setNegativeButton(
            R.string.cancel
        )
        { _, _ ->
        }
    builder
        .create()
        .show()
  }

  private fun dividendAnnouncedItemDeleteClicked(
    symbol: String?,
    dividend: Dividend?
  ) {
    // Summary tag?
    if (dividend != null) {
      val localDateTime = LocalDateTime.ofEpochSecond(dividend.paydate, 0, ZoneOffset.UTC)
      android.app.AlertDialog.Builder(requireContext())
          .setTitle(R.string.delete_dividend)
          .setMessage(
              getString(
                  R.string.delete_dividend_confirm, DecimalFormat("0.##").format(dividend.amount),
                  localDateTime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
              )
          )
          .setPositiveButton(R.string.delete) { _, _ ->
            stockRoomViewModel.deleteDividend(dividend)
            Toast.makeText(
                requireContext(), R.string.dividend_deleted, Toast.LENGTH_LONG
            )
                .show()
          }
          .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
          .show()
    }
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

    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_dividend, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    // use requireActivity() instead of this to have only one shared viewmodel
    stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)

    // received dividends
    val dividendReceivedClickListenerUpdate =
      { dividend: Dividend -> dividendReceivedItemUpdateClicked(dividend) }
    val dividendReceivedClickListenerDelete =
      { symbol: String?, dividend: Dividend?, dividendList: List<Dividend>? ->
        dividendReceivedItemDeleteClicked(
            symbol, dividend, dividendList
        )
      }
    val dividendReceivedListAdapter =
      DividendReceivedListAdapter(
          requireContext(), dividendReceivedClickListenerUpdate, dividendReceivedClickListenerDelete
      )
    dividendsReceivedView.adapter = dividendReceivedListAdapter
    dividendsReceivedView.layoutManager = LinearLayoutManager(requireContext())

    // announced dividends
    val dividendAnnouncedClickListenerUpdate =
      { dividend: Dividend -> dividendAnnouncedItemUpdateClicked(dividend) }
    val dividendAnnouncedClickListenerDelete =
      { symbol: String?, dividend: Dividend? ->
        dividendAnnouncedItemDeleteClicked(
            symbol, dividend
        )
      }
    val dividendAnnouncedListAdapter =
      DividendAnnouncedListAdapter(
          requireContext(), dividendAnnouncedClickListenerUpdate,
          dividendAnnouncedClickListenerDelete
      )
    dividendsAnnouncedView.adapter = dividendAnnouncedListAdapter
    dividendsAnnouncedView.layoutManager = LinearLayoutManager(requireContext())

    // Update the dividend list.
    val dividendsLiveData: LiveData<Dividends> = stockRoomViewModel.getDividendsLiveData(symbol)
    dividendsLiveData.observe(viewLifecycleOwner, Observer { data ->
      if (data != null) {
        dividendReceivedListAdapter.updateDividends(data)
        dividendAnnouncedListAdapter.updateDividends(data)
        dividendNotesTextView.text = data.stockDBdata.dividendNotes
      }
    })

    val assetsLiveData: LiveData<Assets> = stockRoomViewModel.getAssetsLiveData(symbol)
    assetsLiveData.observe(viewLifecycleOwner, Observer { data ->
    })

    // Use MediatorLiveView to combine the assets and online data changes.
    assetChangeLiveData.addSource(assetsLiveData) { value ->
      if (value != null) {
        assetChange.assets = value
        assetChangeLiveData.postValue(assetChange)
      }
    }

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
      updateAssetChange(item)
      dividendReceivedListAdapter.updateAssetData(item)
    })

    addDividendReceivedButton.setOnClickListener {
      val builder = AlertDialog.Builder(requireContext())
      // Get the layout inflater
      val inflater = LayoutInflater.from(requireContext())

      // Inflate and set the layout for the dialog
      // Pass null as the parent view because its going in the dialog layout
      val dialogView = inflater.inflate(R.layout.dialog_add_dividend, null)
      dialogView.findViewById<TextView>(R.id.textViewDividendExDate).visibility = View.GONE
      dialogView.findViewById<DatePicker>(R.id.datePickerDividendExDate).visibility = View.GONE
      val addUpdateDividendHeadlineView =
        dialogView.findViewById<TextView>(R.id.addUpdateDividendHeadline)
      addUpdateDividendHeadlineView.text = getString(R.string.add_dividend)
      val addDividendView = dialogView.findViewById<TextView>(R.id.addDividend)
      val datePickerDividendDateView =
        dialogView.findViewById<DatePicker>(R.id.datePickerDividendDate)

      val textViewDividendCycleSpinner =
        dialogView.findViewById<Spinner>(R.id.textViewDividendCycleSpinner)
      textViewDividendCycleSpinner.setSelection(
          dividendCycleToSelection(Quarterly.value)
      )

      builder.setView(dialogView)
          // Add action buttons
          .setPositiveButton(
              R.string.add
          ) { _, _ ->
            // Add () to avoid cast exception.
            val addDividendText = (addDividendView.text).toString()
                .trim()
            if (addDividendText.isNotEmpty()) {
              var dividendAmount = 0.0
              var valid = true
              try {
                val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                dividendAmount = numberFormat.parse(addDividendText)!!
                    .toDouble()
              } catch (e: Exception) {
                valid = false
              }
              if (dividendAmount <= 0.0) {
                Toast.makeText(
                    requireContext(), getString(R.string.dividend_not_zero), Toast.LENGTH_LONG
                )
                    .show()
                valid = false
              }

              if (valid) {
                val datetime: LocalDateTime = LocalDateTime.of(
                    datePickerDividendDateView.year, datePickerDividendDateView.month + 1,
                    datePickerDividendDateView.dayOfMonth, 0, 0
                )
                val seconds = datetime.toEpochSecond(ZoneOffset.UTC)

                val cycle =
                  dividendSelectionToCycle(textViewDividendCycleSpinner.selectedItemPosition)

                val dividend = Dividend(
                    symbol = symbol,
                    amount = dividendAmount,
                    type = Received.value,
                    cycle = cycle,
                    paydate = seconds,
                    exdate = 0L,
                    note  = ""
                )
                stockRoomViewModel.updateDividend(dividend)

                Toast.makeText(
                    requireContext(), getString(R.string.dividend_added), Toast.LENGTH_LONG
                )
                    .show()
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

    addDividendAnnouncedButton.setOnClickListener {
      val builder = AlertDialog.Builder(requireContext())
      // Get the layout inflater
      val inflater = LayoutInflater.from(requireContext())

      // Inflate and set the layout for the dialog
      // Pass null as the parent view because its going in the dialog layout
      val dialogView = inflater.inflate(R.layout.dialog_add_dividend, null)
      val addUpdateDividendHeadlineView =
        dialogView.findViewById<TextView>(R.id.addUpdateDividendHeadline)
      addUpdateDividendHeadlineView.text = getString(R.string.add_dividend)
      val addDividendView = dialogView.findViewById<TextView>(R.id.addDividend)

      val datePickerDividendDateView =
        dialogView.findViewById<DatePicker>(R.id.datePickerDividendDate)

      val datePickerDividendExDateView =
        dialogView.findViewById<DatePicker>(R.id.datePickerDividendExDate)

      val textViewDividendCycleSpinner =
        dialogView.findViewById<Spinner>(R.id.textViewDividendCycleSpinner)
      textViewDividendCycleSpinner.setSelection(
          dividendCycleToSelection(Quarterly.value)
      )

      builder.setView(dialogView)
          // Add action buttons
          .setPositiveButton(
              R.string.add
          ) { _, _ ->
            // Add () to avoid cast exception.
            val addDividendText = (addDividendView.text).toString()
                .trim()
            var dividendAmount = 0.0
            if (addDividendText.isNotEmpty()) {
              try {
                val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                dividendAmount = numberFormat.parse(addDividendText)!!
                    .toDouble()
              } catch (e: Exception) {
              }
            }

            val datetime: LocalDateTime = LocalDateTime.of(
                datePickerDividendDateView.year, datePickerDividendDateView.month + 1,
                datePickerDividendDateView.dayOfMonth, 0, 0
            )
            val seconds = datetime.toEpochSecond(ZoneOffset.UTC)

            val datetimeEx: LocalDateTime = LocalDateTime.of(
                datePickerDividendExDateView.year, datePickerDividendExDateView.month + 1,
                datePickerDividendExDateView.dayOfMonth, 0, 0
            )
            val secondsEx = datetimeEx.toEpochSecond(ZoneOffset.UTC)

            val cycle =
              dividendSelectionToCycle(textViewDividendCycleSpinner.selectedItemPosition)

            val dividend = Dividend(
                symbol = symbol,
                amount = dividendAmount,
                type = Announced.value,
                cycle = cycle,
                paydate = seconds,
                exdate = secondsEx,
                note = ""
            )
            stockRoomViewModel.updateDividend(dividend)

            Toast.makeText(
                requireContext(), getString(R.string.dividend_added), Toast.LENGTH_LONG
            )
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

    updateDividendNotesButton.setOnClickListener {
      updateDividendNotes()
    }

    dividendNotesTextView.setOnClickListener {
      updateDividendNotes()
    }
  }

  override fun onPause() {
    onlineDataHandler.removeCallbacks(onlineDataTask)
    super.onPause()
  }

  override fun onResume() {
    super.onResume()
    onlineDataHandler.post(onlineDataTask)
    stockRoomViewModel.runOnlineTaskNow()
  }

  private fun updateDividendNotes() {
    val builder = AlertDialog.Builder(requireContext())
    // Get the layout inflater
    val inflater = LayoutInflater.from(requireContext())

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    val dialogView = inflater.inflate(R.layout.dialog_add_note, null)
    val textInputEditNoteView =
      dialogView.findViewById<TextView>(R.id.textInputEditNote)

    val note = dividendNotesTextView.text
    textInputEditNoteView.text = note

    builder.setView(dialogView)
        // Add action buttons
        .setPositiveButton(
            R.string.add
        ) { _, _ ->
          // Add () to avoid cast exception.
          val noteText = (textInputEditNoteView.text).toString()

          if (noteText != note) {
            dividendNotesTextView.text = noteText
            stockRoomViewModel.updateDividendNotes(symbol, noteText)

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

  private fun updateAssetChange(data: AssetsLiveData) {
    if (data.assets != null && data.onlineMarketData != null && data.onlineMarketData?.annualDividendRate!! > 0.0) {
      textViewDividend.text = getDividend(data)
    } else {
      textViewDividend.text = getString(R.string.no_dividend_data)
    }
  }

  private fun getDividend(data: AssetsLiveData): SpannableStringBuilder {

    val dividend = SpannableStringBuilder()
        .append(getString(R.string.annualDividendRate))
        .bold {
          append(
              " ${DecimalFormat("0.00##").format(data.onlineMarketData?.annualDividendRate)}\n"
          )
        }
        .append(getString(R.string.annualDividendYield))
        .bold {
          append(
              " ${
                DecimalFormat("0.00##").format(
                    data.onlineMarketData?.annualDividendYield?.times(100)
                )
              }%"
          )
        }

    val dividendDate = data.onlineMarketData?.dividendDate!!
    val dateTimeNow = LocalDateTime.now()
        .toEpochSecond(ZoneOffset.UTC)
    if (dividendDate > 0 && dividendDate > dateTimeNow) {
      val datetime: LocalDateTime =
        LocalDateTime.ofEpochSecond(data.onlineMarketData?.dividendDate!!, 0, ZoneOffset.UTC)
      dividend
          .append(
              "\n${getString(R.string.dividend_pay_date)}"
          )
          .bold {
            append(" ${datetime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))}")
          }
    }

    if (data.assets != null) {
      val (shares, asset) = getAssets(data.assets?.assets)

//      val shares = data.assets?.assets?.sumByDouble {
//        it.shares
//      } ?: 0.0

      if (shares > 0.0) {
        val totalDividend = shares * data.onlineMarketData?.annualDividendRate!!
        dividend
            .append("\n${getString(R.string.quarterlyDividend)}")
            .bold {
              append(" ${DecimalFormat("0.00").format(totalDividend / 4.0)}")
            }
            .append("\n(${getString(R.string.monthly)}")
            .bold {
              append(" ${DecimalFormat("0.00").format(totalDividend / 12.0)}")
            }
            .append(", ${getString(R.string.annual)}")
            .bold {
              append(" ${DecimalFormat("0.00").format(totalDividend)}")
            }
            .append(")")
      }
    }

    return dividend
  }
}
