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
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.DatePicker
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.thecloudsite.stockroom.database.Asset
import kotlinx.android.synthetic.main.activity_filter.filterRecyclerView
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.absoluteValue

class FilterActivity : AppCompatActivity() {

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_filter)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    val filterClickListenerUpdate =
      { asset: Asset -> filterItemUpdateClicked(asset) }
    val filterClickListenerDelete =
      { symbol: String?, asset: Asset? -> filterItemDeleteClicked(symbol, asset) }
    val filterAdapter =
      FilterListAdapter(this, filterClickListenerUpdate, filterClickListenerDelete)

    filterRecyclerView.layoutManager = LinearLayoutManager(this)
    filterRecyclerView.adapter = filterAdapter

    filterAdapter.updateAssets(listOf(Asset(symbol = "a", price = 1.0, quantity = 2.0)))
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }

  override fun onActivityResult(
    requestCode: Int,
    resultCode: Int,
    resultData: Intent?
  ) {
    super.onActivityResult(requestCode, resultCode, resultData)

    if (resultCode == Activity.RESULT_OK) {
//      if (requestCode == importListActivityRequestCode) {
//        resultData?.data?.also { uri ->
//
//          // Perform operations on the document using its URI.
//          stockRoomViewModel.importList(applicationContext, uri)
//          finish()
//        }
//      }
      /*
      else
        if (requestCode == exportListActivityRequestCode) {
          if (data != null && data.data is Uri) {
            val exportListUri = data.data!!
            stockRoomViewModel.exportList(applicationContext, exportListUri)
            finish()
          }
        }
      */
    }
  }

  private fun filterItemUpdateClicked(asset: Asset) {
    val builder = AlertDialog.Builder(this)
    // Get the layout inflater
    val inflater = LayoutInflater.from(this)

    // Inflate and set the layout for the dialog
    // Pass null as the parent view because its going in the dialog layout
    val dialogView = inflater.inflate(R.layout.dialog_add_asset, null)

    val addUpdateQuantityHeadlineView =
      dialogView.findViewById<TextView>(R.id.addUpdateQuantityHeadline)
    addUpdateQuantityHeadlineView.text = getString(R.string.update_asset)
    val addQuantityView = dialogView.findViewById<TextView>(R.id.addQuantity)
    addQuantityView.text = DecimalFormat("0.######").format(asset.quantity.absoluteValue)
//    if (asset.shares < 0) {
//      addSharesView.inputType = TYPE_CLASS_NUMBER or
//          TYPE_NUMBER_FLAG_DECIMAL or
//          TYPE_NUMBER_FLAG_SIGNED
//    }

    val addPriceView = dialogView.findViewById<TextView>(R.id.addPrice)
    addPriceView.text = DecimalFormat("0.00####").format(asset.price)

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
                this, getString(R.string.asset_share_not_empty), Toast.LENGTH_LONG
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
                this, getString(R.string.asset_price_not_empty), Toast.LENGTH_LONG
            )
                .show()
            return@setPositiveButton
          }
          if (price <= 0.0) {
            Toast.makeText(
                this, getString(R.string.price_not_zero), Toast.LENGTH_LONG
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
            Asset(
                symbol = "symbol", quantity = quantity, price = price, date = date, note = noteText
            )

          if (asset.quantity != assetNew.quantity
              || asset.price != assetNew.price
              || asset.date != assetNew.date
              || asset.note != assetNew.note
          ) {
            // Each asset has an id. Delete the asset with that id and then add assetNew.
            //stockRoomViewModel.updateAsset2(asset, assetNew)

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
                  R.plurals.asset_updated, count, DecimalFormat("0.####").format(quantityAbs),
                  DecimalFormat("0.00##").format(price)
              )
            } else {
              resources.getQuantityString(
                  R.plurals.asset_removed_updated, count,
                  DecimalFormat("0.####").format(quantityAbs)
              )
            }

            Toast.makeText(
                this, pluralstr, Toast.LENGTH_LONG
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

  private fun filterItemDeleteClicked(
    symbol: String?,
    asset: Asset?
  ) {
    android.app.AlertDialog.Builder(this)
        .setTitle(R.string.delete_filter)
        .setMessage(
            getString(
                R.string.delete_filter_confirm
            )
        )
        .setPositiveButton(R.string.delete) { _, _ ->
          //stockRoomViewModel.deleteAsset(asset)
          Toast.makeText(
              this, getString(R.string.delete_filter_msg), Toast.LENGTH_LONG
          )
              .show()
        }
        .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
        .show()
  }
}

