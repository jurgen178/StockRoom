package com.thecloudsite.stockroom

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
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
import com.thecloudsite.stockroom.StockDataFragment.Companion
import kotlinx.android.synthetic.main.fragment_dividend.addDividendReceivedButton
import kotlinx.android.synthetic.main.fragment_dividend.dividendNotesTextView
import kotlinx.android.synthetic.main.fragment_dividend.dividendsReceivedView
import kotlinx.android.synthetic.main.fragment_dividend.updateDividendNotesButton
import kotlinx.android.synthetic.main.fragment_dividend.dividendLinearLayout
import kotlinx.android.synthetic.main.fragment_dividend.textViewDividend
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM
import java.util.Locale

class DividendFragment : Fragment() {

  private lateinit var stockRoomViewModel: StockRoomViewModel

  private val dividendChange = AssetsLiveData()

  private val assetChange = AssetsLiveData()
  private val assetChangeLiveData = MediatorLiveData<AssetsLiveData>()

  companion object {
    fun newInstance() = DividendFragment()
    const val onlineDataTimerDelay: Long = 2000L
  }

  lateinit var onlineDataHandler: Handler

  private lateinit var stockDBdata: StockDBdata
  private var stockDataEntries: List<StockDataEntry>? = null
  private var symbol: String = ""

  private fun dividendReceivedItemUpdateClicked(dividend: Dividend) {
    val builder = AlertDialog.Builder(requireContext())
    // Get the layout inflater
    val inflater = LayoutInflater.from(requireContext())

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    val dialogView = inflater.inflate(R.layout.add_dividend, null)

    val addUpdateSharesHeadlineView =
      dialogView.findViewById<TextView>(R.id.addUpdateDividendHeadline)
    addUpdateSharesHeadlineView.text = getString(R.string.update_dividend)
    val addSharesView = dialogView.findViewById<TextView>(R.id.addShares)
    addSharesView.text = DecimalFormat("0.######").format(dividend.amount)
    val addPriceView = dialogView.findViewById<TextView>(R.id.addPrice)
    addPriceView.text = DecimalFormat("0.######").format(dividend.paydate)
    builder.setView(dialogView)
        // Add action buttons
        .setPositiveButton(
            R.string.update
        ) { _, _ ->
          val sharesText = addSharesView.text.toString()
              .trim()
          val priceText = addPriceView.text.toString()
              .trim()
          if (priceText.isNotEmpty() && sharesText.isNotEmpty()) {
            var price = 0f
            var shares = 0f
            var valid = true
            try {
              val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
              price = numberFormat.parse(priceText)!!
                  .toFloat()
            } catch (e: Exception) {
              valid = false
            }
            if (price <= 0f) {
              Toast.makeText(
                  requireContext(), getString(R.string.price_not_zero), Toast.LENGTH_LONG
              )
                  .show()
              valid = false
            }
            try {
              val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
              shares = numberFormat.parse(sharesText)!!
                  .toFloat()
            } catch (e: Exception) {
              valid = false
            }
            if (shares <= 0f) {
              Toast.makeText(
                  requireContext(), getString(R.string.shares_not_zero), Toast.LENGTH_LONG
              )
                  .show()
              valid = false
            }
            if (valid) {
/*
              val assetnew = Asset(symbol = symbol, shares = shares, price = price)
              if (asset.shares != assetnew.shares || asset.price != assetnew.price) {
                // delete old asset
                stockRoomViewModel.deleteAsset(asset)
                // add new asset
                stockRoomViewModel.addAsset(assetnew)
                val count: Int = when {
                  shares == 1f -> {
                    1
                  }
                  shares > 1f -> {
                    shares.toInt() + 1
                  }
                  else -> {
                    0
                  }
                }

                val pluralstr = resources.getQuantityString(
                    R.plurals.asset_updated, count, DecimalFormat("0.##").format(shares),
                    DecimalFormat("0.##").format(price)
                )

                Toast.makeText(
                    requireContext(), pluralstr, Toast.LENGTH_LONG
                )
                    .show()
              }
              */
            }
            hideSoftInputFromWindow()
          } else {
            Toast.makeText(requireContext(), getString(R.string.invalid_entry), Toast.LENGTH_LONG)
                .show()
          }
        }
        .setNegativeButton(R.string.cancel,
            DialogInterface.OnClickListener { _, _ ->
              //getDialog().cancel()
            })
    builder
        .create()
        .show()
  }

  private fun dividendReceivedItemDeleteClicked(
    symbol: String?,
    dividend: Dividend?
  ) {
    // Summary tag?
    if (symbol != null && dividend == null) {
      android.app.AlertDialog.Builder(requireContext())
          .setTitle(R.string.delete_all_assets)
          .setMessage(getString(R.string.delete_all_assets_confirm, symbol))
          .setPositiveButton(R.string.delete) { _, _ ->
            //stockRoomViewModel.deleteDividends(symbol)
            Toast.makeText(
                requireContext(), getString(R.string.delete_all_assets_msg), Toast.LENGTH_LONG
            )
                .show()
          }
          .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
          .show()
    } else if (dividend != null) {
      android.app.AlertDialog.Builder(requireContext())
          .setTitle(R.string.delete_asset)
          .setMessage("test"
          )
          .setPositiveButton(R.string.delete) { _, _ ->
            //stockRoomViewModel.deleteAsset(asset)
            Toast.makeText(
                requireContext(), "test", Toast.LENGTH_LONG
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
      { symbol: String?, dividend: Dividend? -> dividendReceivedItemDeleteClicked(symbol, dividend) }
    val dividendReceivedListAdapter =
      DividendReceivedListAdapter(requireContext(), dividendReceivedClickListenerUpdate, dividendReceivedClickListenerDelete)
    dividendsReceivedView.adapter = dividendReceivedListAdapter
    dividendsReceivedView.layoutManager = LinearLayoutManager(requireContext())

    // Update the dividend list.
    val dividendsLiveData: LiveData<Dividends> = stockRoomViewModel.getDividendsLiveData(symbol)
    dividendsLiveData.observe(viewLifecycleOwner, Observer { data ->
      if (data != null) {
        dividendReceivedListAdapter.updateDividends(data)
        dividendNotesTextView.text = data.stockDBdata.dividendNotes
      }
    })

    val assetsLiveData: LiveData<Assets> = stockRoomViewModel.getAssetsLiveData(symbol)
    assetsLiveData.observe(viewLifecycleOwner, Observer { data ->
      if (data != null) {
        //assetAdapter.updateAssets(data.assets)
      }
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
    })

    stockDBdata = stockRoomViewModel.getStockDBdataSync(symbol)
    //val notes = stockDBdata.notes

    //notesTextView.text = stockDBdata.notes

    addDividendReceivedButton.setOnClickListener {
      val builder = AlertDialog.Builder(requireContext())
      // Get the layout inflater
      val inflater = LayoutInflater.from(requireContext())

      // Inflate and set the layout for the dialog
      // Pass null as the parent view because its going in the dialog layout
      val dialogView = inflater.inflate(R.layout.add_dividend, null)
      val addUpdateSharesHeadlineView =
        dialogView.findViewById<TextView>(R.id.addUpdateDividendHeadline)
      addUpdateSharesHeadlineView.text = getString(R.string.add_dividend)
      val addSharesView = dialogView.findViewById<TextView>(R.id.addShares)
      val addPriceView = dialogView.findViewById<TextView>(R.id.addPrice)
      builder.setView(dialogView)
          // Add action buttons
          .setPositiveButton(
              R.string.add
          ) { _, _ ->
            val sharesText = addSharesView.text.toString()
                .trim()
            val priceText = addPriceView.text.toString()
                .trim()
            if (priceText.isNotEmpty() && sharesText.isNotEmpty()) {
              var price = 0f
              var shares = 0f
              var valid = true
              try {
                val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                price = numberFormat.parse(priceText)!!
                    .toFloat()
              } catch (e: Exception) {
                valid = false
              }
              if (price <= 0f) {
                Toast.makeText(
                    requireContext(), getString(R.string.price_not_zero), Toast.LENGTH_LONG
                )
                    .show()
                valid = false
              }
              try {
                val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
                shares = numberFormat.parse(sharesText)!!
                    .toFloat()
              } catch (e: Exception) {
                valid = false
              }
              if (shares <= 0f) {
                Toast.makeText(
                    requireContext(), getString(R.string.shares_not_zero), Toast.LENGTH_LONG
                )
                    .show()
                valid = false
              }
              if (valid) {
                stockRoomViewModel.addDividend(Dividend(symbol = symbol, amount = shares, exdate = 0L, paydate = 0L, type = 0))

                Toast.makeText(requireContext(), getString(R.string.dividend_added), Toast.LENGTH_LONG)
                    .show()
              }
              hideSoftInputFromWindow()
            } else {
              Toast.makeText(
                  requireContext(), getString(R.string.invalid_entry), Toast.LENGTH_LONG
              )
                  .show()
            }
          }
          .setNegativeButton(R.string.cancel,
              DialogInterface.OnClickListener { _, _ ->
              })
      builder
          .create()
          .show()
    }

    updateDividendNotesButton.setOnClickListener {
      updateNotes()
    }

    dividendNotesTextView.setOnClickListener {
      updateNotes()
    }
  }

  override fun onPause() {
    onlineDataHandler.removeCallbacks(onlineDataTask)
    super.onPause()
  }

  override fun onResume() {
    super.onResume()
    onlineDataHandler.post(onlineDataTask)
    stockRoomViewModel.updateOnlineDataManually()
  }

  private fun hideSoftInputFromWindow() {
    val view = activity?.currentFocus
    if (view is TextView) {
      val imm = activity?.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
      imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
  }

  private fun updateNotes() {
    val builder = AlertDialog.Builder(requireContext())
    // Get the layout inflater
    val inflater = LayoutInflater.from(requireContext())

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    val dialogView = inflater.inflate(R.layout.add_note, null)
    val textInputEditNoteView =
      dialogView.findViewById<TextView>(R.id.textInputEditEventNote)

    val note = dividendNotesTextView.text
    textInputEditNoteView.text = note

    builder.setView(dialogView)
        // Add action buttons
        .setPositiveButton(
            R.string.add
        ) { _, _ ->
          val noteText = textInputEditNoteView.text.toString()

          if (noteText != note) {
            dividendNotesTextView.text = noteText
            stockRoomViewModel.updateNotes(symbol, noteText)

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
    if (data.assets != null && data.onlineMarketData != null) {
      if (data.onlineMarketData?.annualDividendRate!! > 0f) {
        dividendLinearLayout.visibility = View.VISIBLE
        textViewDividend.text = getDividend(data)
      } else {
        dividendLinearLayout.visibility = View.GONE
      }
    } else {
      dividendLinearLayout.visibility = View.GONE
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
              " ${DecimalFormat("0.00##").format(
                  data.onlineMarketData?.annualDividendYield?.times(100)
              )}%"
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
      val shares = data.assets?.assets?.sumByDouble {
        it.shares.toDouble()
      }
          ?.toFloat() ?: 0f

      if (shares > 0f) {
        val totalDividend = shares * data.onlineMarketData?.annualDividendRate!!
        dividend
            .append("\n${getString(R.string.quarterlyDividend)}")
            .bold {
              append(" ${DecimalFormat("0.00").format(totalDividend / 4f)}")
            }
      }
    }

    return dividend
  }
}
