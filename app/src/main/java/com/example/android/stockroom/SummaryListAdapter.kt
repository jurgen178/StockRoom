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

package com.example.android.stockroom

import android.content.Context
import android.graphics.Color
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.bold
import androidx.recyclerview.widget.RecyclerView
import java.text.DecimalFormat

data class SummaryData(
  val desc: String,
  val text1: SpannableStringBuilder,
  val text2: SpannableStringBuilder,
  val color: Int
)

class SummaryListAdapter internal constructor(
  val context: Context,
  private val groupList: List<Group>
) : RecyclerView.Adapter<SummaryListAdapter.OnlineDataViewHolder>() {

  private val groupStandardName = context.getString(R.string.standard_group)
  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var data = mutableListOf<SummaryData>()

  inner class OnlineDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val summaryItemDataDesc: TextView = itemView.findViewById(R.id.summaryItemDataDesc)
    val summaryItemData1: TextView = itemView.findViewById(R.id.summaryItemData1)
    val summaryItemData2: TextView = itemView.findViewById(R.id.summaryItemData2)
    val summaryItemGroup: TextView = itemView.findViewById(R.id.summaryItemGroup)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): OnlineDataViewHolder {
    val itemView = inflater.inflate(R.layout.summary_item, parent, false)
    return OnlineDataViewHolder(itemView)
  }

  override fun onBindViewHolder(
    holder: OnlineDataViewHolder,
    position: Int
  ) {
    val current: SummaryData = data[position]

    var color = current.color
    if (color == 0) {
      color = context.getColor(R.color.backgroundListColor)
    }
    setBackgroundColor(holder.summaryItemGroup, color)

    holder.summaryItemDataDesc.text = current.desc
    holder.summaryItemData1.text = current.text1
    holder.summaryItemData2.text = current.text2
  }

  fun updateData(stockItems: List<StockItem>) {
    data.clear()
    val (text1, text2) = getTotal(0, true, stockItems)
    data.add(SummaryData(context.getString(R.string.overview), text1, text2, Color.WHITE))

    // Get all groups.
    val groupSet = HashSet<Int>()
    stockItems.forEach { stockItem ->
      groupSet.add(stockItem.stockDBdata.groupColor)
    }

    // Get all names assigned to each color.
    val groups = groupSet.map { color ->
      val name = groupList.find { group ->
        group.color == color
      }?.name
      if (name == null) {
        Group(color = context.getColor(R.color.backgroundListColor), name = groupStandardName)
      } else {
        Group(color = color, name = name)
      }
    }

    // Display stats for each group.
    groups.sortedBy { group ->
      group.name
    }
        .forEach { group ->
          val (text1, text2) = getTotal(group.color, false, stockItems)
          data.add(
              SummaryData(
                  context.getString(R.string.group_name, group.name), text1, text2, group.color
              )
          )
        }

    notifyDataSetChanged()
  }

  override fun getItemCount() = data.size

  private fun getTotal(
    color: Int,
    all: Boolean,
    stockItems: List<StockItem>
  ): Pair<SpannableStringBuilder, SpannableStringBuilder> {
    var totalPurchasePrice = 0f
    var totalAssets = 0f
    var totalGain = 0f
    var totalLoss = 0f
    var totalShares = 0f
    var totalDividendAssets = 0f
    var totalDividend = 0f
    var totalAlerts: Int = 0
    var totalNotes: Int = 0

    val stockItemsSelected =
      stockItems.filter {
        all || it.stockDBdata.groupColor == color
      }

    stockItemsSelected.forEach {
      var shares = 0f
      var price = 0f
      it.assets.forEach { asset ->
        price += asset.shares * asset.price
        shares += asset.shares
      }

      val assetsPrice = shares * it.onlineMarketData.marketPrice
      val gainLoss = assetsPrice - price
      if (gainLoss > 0f) {
        totalGain += gainLoss
      } else {
        totalLoss -= gainLoss
      }

      totalPurchasePrice += price
      totalAssets += assetsPrice
      totalShares += shares

      if (it.onlineMarketData.annualDividendRate > 0f) {
        totalDividendAssets += assetsPrice
        totalDividend += shares * it.onlineMarketData.annualDividendRate
      }

      if (it.stockDBdata.alertAbove > 0f) {
        totalAlerts++
      }
      if (it.stockDBdata.alertBelow > 0f) {
        totalAlerts++
      }
      if (it.stockDBdata.notes.isNotEmpty()) {
        totalNotes++
      }
    }

    val stockAssets = stockItemsSelected.filter {
      it.assets.isNotEmpty()
    }
    val stockEvents = stockItemsSelected.filter {
      it.assets.isNotEmpty()
    }

    val totals1 = SpannableStringBuilder()
        .append("Aktien ")
        .bold { append("${stockItemsSelected.size}\n") }
        .append("Aktien mit Bestand ")
        .bold { append("${stockAssets.size}\n") }
        .append("Benachrichtigungen ")
        .bold { append("${totalAlerts}\n") }
        .append("Ereignisse ")
        .bold { append("${stockEvents.size}\n") }
        .append("Anmerkungen ")
        .bold { append("${totalNotes}\n") }
        .append("Anzahl Aktien ")
        .bold { append("${totalShares}\n") }
        .append("Gesamtkaufpreis ")
        .bold { append("${DecimalFormat("0.00").format(totalPurchasePrice)}\n") }
        .append("Gesamtbestand ")
        .bold { append("${DecimalFormat("0.00").format(totalAssets)}") }

    val totalDividendChange: Float = if (totalDividendAssets > 0f) {
      totalDividend / totalDividendAssets
    } else {
      0f
    }
    val totals2 = SpannableStringBuilder()
        .append("Gewinn ")
        .bold { append("${DecimalFormat("0.00").format(totalGain)}\n") }
        .append("Verlust ")
        .bold {
          append(
              "${DecimalFormat("0.00")
                  .format(totalLoss)}\n"
          )
        }
        .append("Gewinn-Verlust ")
        .bold {
          append(
              "${DecimalFormat("0.00")
                  .format(totalAssets - totalPurchasePrice)}\n"
          )
        }
        .append("Bestand mit Dividende ")
        .bold {
          append(
              "${DecimalFormat("0.00")
                  .format(totalDividendAssets)}\n"
          )
        }
        .append("Bestand ohne Dividende ")
        .bold {
          append(
              "${DecimalFormat("0.00")
                  .format(totalAssets - totalDividendAssets)}\n"
          )
        }
        .append("Dividende pro Jahr ")
        .bold {
          append(
              "${DecimalFormat("0.00")
                  .format(totalDividend)} (${DecimalFormat("0.00")
                  .format(
                      totalDividendChange * 100
                  )}%)"
          )
        }

    return Pair(totals1, totals2)
  }
}
