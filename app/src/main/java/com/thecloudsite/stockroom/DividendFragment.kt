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
import android.widget.DatePicker
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.text.bold
import androidx.core.text.underline
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
import com.thecloudsite.stockroom.database.StockDBdata
import com.thecloudsite.stockroom.databinding.DialogAddDividendBinding
import com.thecloudsite.stockroom.databinding.DialogAddNoteBinding
import com.thecloudsite.stockroom.databinding.DialogSetAnnualDividendBinding
import com.thecloudsite.stockroom.databinding.FragmentDividendBinding
import com.thecloudsite.stockroom.utils.DecimalFormat0To2Digits
import com.thecloudsite.stockroom.utils.DecimalFormat0To6Digits
import com.thecloudsite.stockroom.utils.DecimalFormat2Digits
import com.thecloudsite.stockroom.utils.DecimalFormat2To4Digits
import com.thecloudsite.stockroom.utils.dividendCycleToSelection
import com.thecloudsite.stockroom.utils.dividendSelectionToCycle
import com.thecloudsite.stockroom.utils.getAssets
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
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

  private var _binding: FragmentDividendBinding? = null

  // This property is only valid between onCreateView and
  // onDestroyView.
  private val binding get() = _binding!!

  private lateinit var stockRoomViewModel: StockRoomViewModel

  private val assetChange = StockAssetsLiveData()
  private val assetChangeLiveData = MediatorLiveData<StockAssetsLiveData>()

  companion object {
    fun newInstance() = DividendFragment()
  }

  lateinit var onlineDataHandler: Handler

  private var symbol: String = ""
  private var annualDividend: Double = 0.0

  private fun dividendReceivedItemUpdateClicked(dividend: Dividend) {
    val builder = AlertDialog.Builder(requireContext())
    // Get the layout inflater
    val inflater = LayoutInflater.from(requireContext())

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    val dialogBinding = DialogAddDividendBinding.inflate(inflater)

    dialogBinding.textViewDividendExDate.visibility = View.GONE
    dialogBinding.datePickerDividendExDate.visibility = View.GONE
    dialogBinding.addDividend.setText(
      DecimalFormat(DecimalFormat0To6Digits).format(dividend.amount)
    )
    dialogBinding.addNote.setText(dividend.note)
    val localDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(dividend.paydate), ZoneOffset.systemDefault())
    // month is starting from zero
    dialogBinding.datePickerDividendDate.updateDate(
      localDateTime.year, localDateTime.month.value - 1, localDateTime.dayOfMonth
    )

    dialogBinding.textViewDividendCycleSpinner.setSelection(
      dividendCycleToSelection(dividend.cycle)
    )

    builder.setView(dialogBinding.root)
      .setTitle(R.string.update_dividend)
      // Add action buttons
      .setPositiveButton(
        R.string.update
      ) { _, _ ->
        // Add () to avoid cast exception.
        val addDividendText = (dialogBinding.addDividend.text).toString()
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
            val datetime: ZonedDateTime = ZonedDateTime.of(
              dialogBinding.datePickerDividendDate.year,
              dialogBinding.datePickerDividendDate.month + 1,
              dialogBinding.datePickerDividendDate.dayOfMonth,
              localDateTime.hour,
              localDateTime.minute,
              localDateTime.second,
              0,
              ZoneOffset.systemDefault()
            )
            val seconds = datetime.toEpochSecond()

            val noteText = (dialogBinding.addNote.text).toString()
              .trim()

            val cycle =
              dividendSelectionToCycle(
                dialogBinding.textViewDividendCycleSpinner.selectedItemPosition
              )

            if (dividend.amount != dividendAmount
              || dividend.note != noteText
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
                  note = noteText
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
      val localDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(dividend.paydate), ZoneOffset.systemDefault())
      android.app.AlertDialog.Builder(requireContext())
        .setTitle(R.string.delete_dividend)
        .setMessage(
          getString(
            R.string.delete_dividend_confirm,
            DecimalFormat(DecimalFormat0To2Digits).format(dividend.amount),
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
    val dialogBinding = DialogAddDividendBinding.inflate(inflater)

    dialogBinding.addDividend.setText(
      DecimalFormat(DecimalFormat0To6Digits).format(dividend.amount)
    )
    dialogBinding.addNote.setText(dividend.note)

    val localDateTime =
      ZonedDateTime.ofInstant(Instant.ofEpochSecond(dividend.paydate), ZoneOffset.systemDefault())
    // month is starting from zero
    dialogBinding.datePickerDividendDate.updateDate(
      localDateTime.year, localDateTime.month.value - 1, localDateTime.dayOfMonth
    )

    val localDateTimeEx =
      ZonedDateTime.ofInstant(Instant.ofEpochSecond(dividend.exdate), ZoneOffset.systemDefault())
    // month is starting from zero
    dialogBinding.datePickerDividendExDate.updateDate(
      localDateTimeEx.year, localDateTimeEx.month.value - 1, localDateTimeEx.dayOfMonth
    )

    dialogBinding.textViewDividendCycleSpinner.setSelection(
      dividendCycleToSelection(dividend.cycle)
    )

    builder.setView(dialogBinding.root)
      .setTitle(R.string.update_dividend)
      // Add action buttons
      .setPositiveButton(
        R.string.update
      ) { _, _ ->
        // Add () to avoid cast exception.
        val addDividendText = (dialogBinding.addDividend.text).toString()
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

        val datetime: ZonedDateTime = ZonedDateTime.of(
          dialogBinding.datePickerDividendDate.year,
          dialogBinding.datePickerDividendDate.month + 1,
          dialogBinding.datePickerDividendDate.dayOfMonth,
          localDateTime.hour,
          localDateTime.minute,
          localDateTime.second,
          0,
          ZoneOffset.systemDefault()
        )
        val seconds = datetime.toEpochSecond()

        val datetimeEx: ZonedDateTime = ZonedDateTime.of(
          dialogBinding.datePickerDividendExDate.year,
          dialogBinding.datePickerDividendExDate.month + 1,
          dialogBinding.datePickerDividendExDate.dayOfMonth,
          localDateTimeEx.hour,
          localDateTimeEx.minute,
          localDateTimeEx.second,
          0,
          ZoneOffset.systemDefault()
        )
        val secondsEx = datetimeEx.toEpochSecond()

        val noteText = (dialogBinding.addNote.text).toString()
          .trim()

        val cycle = dividendSelectionToCycle(
          dialogBinding.textViewDividendCycleSpinner.selectedItemPosition
        )

        if (dividend.amount != dividendAmount
          || dividend.note != noteText
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
              note = noteText
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
      val localDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(dividend.paydate), ZoneOffset.systemDefault())
      android.app.AlertDialog.Builder(requireContext())
        .setTitle(R.string.delete_dividend)
        .setMessage(
          getString(
            R.string.delete_dividend_confirm,
            DecimalFormat(DecimalFormat0To2Digits).format(dividend.amount),
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
  ): View {

    symbol = (arguments?.getString("symbol") ?: "").toUpperCase(Locale.ROOT)

    // Setup online data every 2s for regular hours.
    onlineDataHandler = Handler(Looper.getMainLooper())

    // Inflate the layout for this fragment
    _binding = FragmentDividendBinding.inflate(inflater, container, false)
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
    binding.dividendsReceivedView.adapter = dividendReceivedListAdapter
    binding.dividendsReceivedView.layoutManager = LinearLayoutManager(requireContext())

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
    binding.dividendsAnnouncedView.adapter = dividendAnnouncedListAdapter
    binding.dividendsAnnouncedView.layoutManager = LinearLayoutManager(requireContext())

    // Update the dividend list.
    val dividendsLiveData: LiveData<Dividends> = stockRoomViewModel.getDividendsLiveData(symbol)
    dividendsLiveData.observe(viewLifecycleOwner, Observer { data ->
      if (data != null) {
        dividendReceivedListAdapter.updateDividends(data)
        dividendAnnouncedListAdapter.updateDividends(data)
        binding.dividendNoteTextView.text = data.stockDBdata.dividendNote
      }
    })

    val stockDBLiveData: LiveData<StockDBdata> = stockRoomViewModel.getStockDBLiveData(symbol)
    stockDBLiveData.observe(viewLifecycleOwner, Observer { data ->
      if (data != null) {
        annualDividend = data.annualDividendRate
      }
    })

    // Use MediatorLiveView to combine the assets, stockDB and online data changes.
    assetChangeLiveData.addSource(stockDBLiveData) { value ->
      if (value != null) {
        assetChange.stockDBdata = value
        assetChangeLiveData.postValue(assetChange)
      }
    }

    val assetsLiveData: LiveData<Assets> = stockRoomViewModel.getAssetsLiveData(symbol)
    assetsLiveData.observe(viewLifecycleOwner, Observer { data ->
    })

    // Use MediatorLiveView to combine the assets, stockDB and online data changes.
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
        dividendReceivedListAdapter.updateAssetData(item)
      }
    })

    binding.addDividendReceivedButton.setOnClickListener {
      val builder = AlertDialog.Builder(requireContext())
      // Get the layout inflater
      val inflater = LayoutInflater.from(requireContext())

      // Inflate and set the layout for the dialog
      // Pass null as the parent view because its going in the dialog layout
      val dialogBinding = DialogAddDividendBinding.inflate(inflater)

      dialogBinding.textViewDividendExDate.visibility = View.GONE
      dialogBinding.datePickerDividendExDate.visibility = View.GONE
      dialogBinding.textViewDividendCycleSpinner.setSelection(
        dividendCycleToSelection(Quarterly.value)
      )

      builder.setView(dialogBinding.root)
        .setTitle(R.string.add_dividend)
        // Add action buttons
        .setPositiveButton(
          R.string.add
        ) { _, _ ->
          // Add () to avoid cast exception.
          val addDividendText = (dialogBinding.addDividend.text).toString()
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
              val localDateTimeNow = ZonedDateTime.now()
              val datetime: ZonedDateTime = ZonedDateTime.of(
                dialogBinding.datePickerDividendDate.year,
                dialogBinding.datePickerDividendDate.month + 1,
                dialogBinding.datePickerDividendDate.dayOfMonth,
                localDateTimeNow.hour,
                localDateTimeNow.minute,
                localDateTimeNow.second,
                0,
                ZoneOffset.systemDefault()
              )
              val seconds = datetime.toEpochSecond()

              val noteText = (dialogBinding.addNote.text).toString()
                .trim()

              val cycle =
                dividendSelectionToCycle(
                  dialogBinding.textViewDividendCycleSpinner.selectedItemPosition
                )

              val dividend = Dividend(
                symbol = symbol,
                amount = dividendAmount,
                type = Received.value,
                cycle = cycle,
                paydate = seconds,
                exdate = 0L,
                note = noteText
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

    binding.textViewAddDividendLayout.setOnClickListener {
      val builder = AlertDialog.Builder(requireContext())
      // Get the layout inflater
      val inflater = LayoutInflater.from(requireContext())

      // Inflate and set the layout for the dialog
      // Pass null as the parent view because its going in the dialog layout
      val dialogBinding = DialogSetAnnualDividendBinding.inflate(inflater)

      if (annualDividend >= 0.0) {
        dialogBinding.setAnnualDividend.setText(
          DecimalFormat(DecimalFormat2To4Digits).format(annualDividend)
        )
      }

      builder.setView(dialogBinding.root)
        .setTitle(R.string.annual_dividend_headline)
        // Add action buttons
        .setPositiveButton(
          R.string.add
        ) { _, _ ->
          // Add () to avoid cast exception.
          val annualDividendText = (dialogBinding.setAnnualDividend.text).toString()
            .trim()
          // 0.0 is a valid value
          var dividendAmount = -1.0
          try {
            val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
            dividendAmount = numberFormat.parse(annualDividendText)!!
              .toDouble()
          } catch (e: Exception) {
          }
          stockRoomViewModel.updateAnnualDividendRate(symbol, dividendAmount)
        }
        .setNegativeButton(
          R.string.cancel
        ) { _, _ ->
        }
      builder
        .create()
        .show()
    }

    binding.addDividendAnnouncedButton.setOnClickListener {
      val builder = AlertDialog.Builder(requireContext())
      // Get the layout inflater
      val inflater = LayoutInflater.from(requireContext())

      // Inflate and set the layout for the dialog
      // Pass null as the parent view because its going in the dialog layout
      val dialogBinding = DialogAddDividendBinding.inflate(inflater)
      dialogBinding.textViewDividendCycleSpinner.setSelection(
        dividendCycleToSelection(Quarterly.value)
      )

      builder.setView(dialogBinding.root)
        .setTitle(R.string.add_dividend)
        // Add action buttons
        .setPositiveButton(
          R.string.add
        ) { _, _ ->
          // Add () to avoid cast exception.
          val addDividendText = (dialogBinding.addDividend.text).toString()
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

          val localDateTimeNow = ZonedDateTime.now()

          val datetime: ZonedDateTime = ZonedDateTime.of(
            dialogBinding.datePickerDividendDate.year,
            dialogBinding.datePickerDividendDate.month + 1,
            dialogBinding.datePickerDividendDate.dayOfMonth,
            localDateTimeNow.hour,
            localDateTimeNow.minute,
            localDateTimeNow.second,
            0,
            ZoneOffset.systemDefault()
          )
          val seconds = datetime.toEpochSecond()

          val datetimeEx: ZonedDateTime = ZonedDateTime.of(
            dialogBinding.datePickerDividendExDate.year,
            dialogBinding.datePickerDividendExDate.month + 1,
            dialogBinding.datePickerDividendExDate.dayOfMonth,
            localDateTimeNow.hour,
            localDateTimeNow.minute,
            localDateTimeNow.second,
            0,
            ZoneOffset.systemDefault()
          )
          val secondsEx = datetimeEx.toEpochSecond()

          val noteText = (dialogBinding.addNote.text).toString()
            .trim()

          val cycle =
            dividendSelectionToCycle(
              dialogBinding.textViewDividendCycleSpinner.selectedItemPosition
            )

          val dividend = Dividend(
            symbol = symbol,
            amount = dividendAmount,
            type = Announced.value,
            cycle = cycle,
            paydate = seconds,
            exdate = secondsEx,
            note = noteText
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

    binding.updateDividendNoteButton.setOnClickListener {
      updateDividendNote()
    }

    binding.dividendNoteTextView.setOnClickListener {
      updateDividendNote()
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

  private fun updateDividendNote() {
    val builder = AlertDialog.Builder(requireContext())
    // Get the layout inflater
    val inflater = LayoutInflater.from(requireContext())

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    val dialogBinding = DialogAddNoteBinding.inflate(inflater)

    val note = binding.dividendNoteTextView.text
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
          binding.dividendNoteTextView.text = noteText
          stockRoomViewModel.updateDividendNote(symbol, noteText)

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

  private fun updateAssetChange(data: StockAssetsLiveData) {

    binding.textViewSetAnnualDividend.text =
      if (data.stockDBdata != null && data.stockDBdata?.annualDividendRate!! >= 0.0) {
        DecimalFormat(DecimalFormat2To4Digits).format(data.stockDBdata?.annualDividendRate)
      } else {
        ""
      }

    val isOnlineDividendData =
      data.onlineMarketData != null && data.onlineMarketData?.annualDividendRate!! > 0.0

    binding.textViewDividendOnlineData.text = if (isOnlineDividendData) {
      getDividendOnlineData(data)
    } else {
      getString(R.string.no_dividend_data)
    }

    binding.textViewSetAnnualDividendPercent.text =
      if (annualDividend > 0.0 && data.onlineMarketData != null && data.onlineMarketData?.marketPrice!! > 0.0) {
        "(${
          DecimalFormat(DecimalFormat2To4Digits).format(
            annualDividend / data.onlineMarketData?.marketPrice!! * 100
          )
        }%)"
      } else {
        ""
      }

    binding.textViewDividendPayout.text = if (annualDividend >= 0.0 || isOnlineDividendData) {
      getDividendPayout(annualDividend, data)
    } else {
      ""
    }
  }

  private fun getDividendOnlineData(data: StockAssetsLiveData): SpannableStringBuilder {

    val dividend = SpannableStringBuilder()
      .underline {
        append(getString(R.string.onlineAnnualDividendData))
      }
      .append("\n")
      .append(getString(R.string.annualDividendRate))
      .bold {
        append(
          " ${
            DecimalFormat(DecimalFormat2To4Digits).format(
              data.onlineMarketData?.annualDividendRate
            )
          }\n"
        )
      }
      .append(getString(R.string.annualDividendYield))
      .bold {
        append(
          " ${
            DecimalFormat(DecimalFormat2To4Digits).format(
              data.onlineMarketData?.annualDividendYield?.times(100)
            )
          }%"
        )
      }

    val dividendDate = data.onlineMarketData?.dividendDate!!
    val dateTimeNow = ZonedDateTime.now()
      .toEpochSecond() // in GMT
    if (dividendDate > 0 && dividendDate > dateTimeNow) {
      val datetime: ZonedDateTime =
        ZonedDateTime.ofInstant(Instant.ofEpochSecond(data.onlineMarketData?.dividendDate!!), ZoneOffset.systemDefault())
      dividend
        .append(
          "\n${getString(R.string.dividend_pay_date)}"
        )
        .bold {
          append(" ${datetime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))}")
        }
    }

    return dividend
  }

  private fun getDividendPayout(
    dividend: Double,
    data: StockAssetsLiveData
  ): SpannableStringBuilder {

    val dividendStr = SpannableStringBuilder()

    if (data.assets != null) {
      val (totalQuantity, totalPrice, totalCommission) = getAssets(data.assets?.assets)

//      val totalQuantity = data.assets?.assets?.sumByDouble {
//        it.totalQuantity
//      } ?: 0.0

      val dividendRate = if (dividend >= 0.0) {
        dividend
      } else {
        data.onlineMarketData?.annualDividendRate!!
      }

      if (dividendRate > 0.0 && totalQuantity > 0.0) {
        val totalDividend = totalQuantity * dividendRate
        dividendStr
          .append("\n${getString(R.string.quarterlyDividend)}")
          .bold {
            append(" ${DecimalFormat(DecimalFormat2Digits).format(totalDividend / 4.0)}")
          }
          .append("\n(${getString(R.string.monthly)}")
          .bold {
            append(" ${DecimalFormat(DecimalFormat2Digits).format(totalDividend / 12.0)}")
          }
          .append(", ${getString(R.string.annual)}")
          .bold {
            append(" ${DecimalFormat(DecimalFormat2Digits).format(totalDividend)}")
          }
          .append(")")
      }
    }

    return dividendStr
  }
}
