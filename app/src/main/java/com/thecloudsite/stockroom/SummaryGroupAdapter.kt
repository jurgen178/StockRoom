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
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.text.underline
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.database.Group
import com.thecloudsite.stockroom.utils.epsilon
import com.thecloudsite.stockroom.utils.getAssets
import com.thecloudsite.stockroom.utils.getAssetsCapitalGain
import com.thecloudsite.stockroom.utils.getCapitalGainLossText
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset

data class SummaryData(
  val desc: String,
  val text1: SpannableStringBuilder,
  val text2: SpannableStringBuilder,
  val color: Int
)

class SummaryGroupAdapter internal constructor(
  val context: Context
) : RecyclerView.Adapter<SummaryGroupAdapter.OnlineDataViewHolder>() {

  private val groupStandardName = context.getString(R.string.standard_group)
  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var data = mutableListOf<SummaryData>()
  private var stockItemsList: List<StockItem> = emptyList()
  private var groupList: List<Group> = emptyList()

  class OnlineDataViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val summaryItemDataDesc: TextView = itemView.findViewById(R.id.summaryItemDataDesc)
    val summaryItemData1: TextView = itemView.findViewById(R.id.summaryItemData1)
    val summaryItemData2: TextView = itemView.findViewById(R.id.summaryItemData2)
    val summaryItemGroup: TextView = itemView.findViewById(R.id.summaryItemGroup)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): OnlineDataViewHolder {
    val itemView = inflater.inflate(R.layout.summarygroup_item, parent, false)
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
    stockItemsList = stockItems
    updateData()
  }

  private fun updateData() {
    data.clear()
    val (text1, text2) = getTotal(0, true, stockItemsList)
    val portfolio = SharedRepository.selectedPortfolio.value ?: ""
    val overview = if (portfolio.isEmpty()) {
      context.getString(R.string.overview_headline_standard_portfolio)
    } else {
      context.getString(R.string.overview_headline_portfolio, portfolio)
    }
    data.add(SummaryData(overview, text1, text2, Color.WHITE))

    // Get all groups.
    val groupSet = HashSet<Int>()
    stockItemsList.forEach { stockItem ->
      groupSet.add(stockItem.stockDBdata.groupColor)
    }

    // Get all names assigned to each color.
    val groups = groupSet.map { color ->
      val name = groupList.find { group ->
        group.color == color
      }?.name
      if (name == null) {
        Group(color = 0, name = groupStandardName)
      } else {
        Group(color = color, name = name)
      }
    }

    // Display stats for each group.
    if (groups.size > 1) {
      groups.sortedBy { group ->
        group.name
      }
          .forEach { group ->
            val (text1, text2) = getTotal(group.color, false, stockItemsList)
            data.add(
                SummaryData(
                    context.getString(R.string.group_name, group.name), text1, text2, group.color
                )
            )
          }
    }

    notifyDataSetChanged()
  }

  // groups is the second data source and gets called after updateData(stockItems: List<StockItem>),
  // run updateData for the color assignment
  fun addGroups(groups: List<Group>) {
    groupList = groups

    updateData()
    notifyDataSetChanged()
  }

  override fun getItemCount() = data.size

  private fun getTotal(
    color: Int,
    all: Boolean,
    stockItems: List<StockItem>
  ): Pair<SpannableStringBuilder, SpannableStringBuilder> {

    var totalPurchasePrice = 0.0
    var totalAssets = 0.0
    var totalGain = 0.0
    //var totalLoss = 0.0
    var totalShares = 0.0
    var totalDividendAssets = 0.0
    var totalDividend = 0.0
    var totalDividendPayed = 0.0
    var totalDividendPayedYTD = 0.0
    var totalAlerts: Int = 0
    var totalNotes: Int = 0

    val datetimeYTD = LocalDateTime.of(LocalDateTime.now().year, 1, 1, 0, 0)
    val secondsYTD = datetimeYTD.toEpochSecond(ZoneOffset.UTC)

    val stockItemsSelected =
      stockItems.filter {
        all || it.stockDBdata.groupColor == color
      }

    var capitalGain = 0.0
    var capitalLoss = 0.0

    stockItemsSelected.forEach { stockItem ->
      val (shares, price) = getAssets(stockItem.assets)

      totalPurchasePrice += price
      totalShares += shares

      val capitalGainLoss = getAssetsCapitalGain(stockItem.assets)
      when {
        capitalGainLoss == Double.NEGATIVE_INFINITY -> {
          capitalGain = 0.0
          capitalLoss = 0.0
        }
        capitalGainLoss > 0.0 -> {
          capitalGain += capitalGainLoss
        }
        else -> {
          capitalLoss += -capitalGainLoss
        }
      }

      stockItem.dividends.forEach { dividend ->
        if (dividend.type == DividendType.Received.value) {
          totalDividendPayed += dividend.amount
          if (dividend.paydate >= secondsYTD) {
            totalDividendPayedYTD += dividend.amount
          }
        }
      }

      if (stockItem.stockDBdata.alertAbove > 0.0) {
        totalAlerts++
      }
      if (stockItem.stockDBdata.alertBelow > 0.0) {
        totalAlerts++
      }
      if (stockItem.stockDBdata.notes.isNotEmpty()) {
        totalNotes++
      }

      if (stockItem.onlineMarketData.marketPrice > 0.0) {
        val assetsPrice = shares * stockItem.onlineMarketData.marketPrice
        val gainLoss = assetsPrice - price
        if (gainLoss > 0.0) {
          totalGain += gainLoss
        } //else {
        //totalLoss -= gainLoss
        //}

        totalAssets += assetsPrice

        if (stockItem.onlineMarketData.annualDividendRate > 0.0) {
          totalDividendAssets += assetsPrice
          totalDividend += shares * stockItem.onlineMarketData.annualDividendRate
        }
      }
    }

    val capitalGainLoss = getCapitalGainLossText(capitalGain, capitalLoss, context)

    val stockAssets = stockItemsSelected.filter {
      it.assets.isNotEmpty()
    }
    val stockEvents = stockItemsSelected.filter {
      it.events.isNotEmpty()
    }
        .sumBy {
          it.events.size
        }

    val summaryGroup2 = SpannableStringBuilder()
        .append("${context.getString(R.string.summary_stocks)} ")
        .bold { append("${stockItemsSelected.size}\n") }
        .append("${context.getString(R.string.summary_stocks_with_assets)} ")
        .bold { append("${stockAssets.size}\n") }
        .append("${context.getString(R.string.summary_alerts)} ")
        .bold { append("${totalAlerts}\n") }
        .append("${context.getString(R.string.summary_events)} ")
        .bold { append("${stockEvents}\n") }
        .append("${context.getString(R.string.summary_note)} ")
        .bold { append("${totalNotes}\n") }
        .append("${context.getString(R.string.summary_number_of_stocks)} ")
        .bold { append("${DecimalFormat("0.####").format(totalShares)}\n\n") }
        .append("${context.getString(R.string.summary_capital_gain)} ")
        .append(capitalGainLoss)
        .append("\n${context.getString(R.string.summary_total_purchase_price)} ")
        .bold { append("${DecimalFormat("0.00").format(totalPurchasePrice)}\n") }
        .append("${context.getString(R.string.summary_total_assets)} ")
        .underline { bold { append(DecimalFormat("0.00").format(totalAssets)) } }

    /*
    val s = SpannableStringBuilder()
            .color(green, { append("Green text ") })
            .append("Normal text ")
            .scale(0.5, { append("Text at half size " })
            .backgroundColor(green, { append("Background green") })
     */

    val totalDividendChange: Double = if (totalDividendAssets > 0.0) {
      totalDividend / totalDividendAssets
    } else {
      0.0
    }

    val gain = if (totalGain > 0.0) {
      SpannableStringBuilder()
          .color(
              context.getColor(R.color.green)
          ) { bold { append("${DecimalFormat("0.00").format(totalGain)}\n") } }
    } else {
      SpannableStringBuilder().bold {
        append("${DecimalFormat("0.00").format(totalGain)}\n")
      }
    }

    // To minimize rounding errors
    val totalLoss = if (totalAssets > 0.0) {
      totalGain - (totalAssets - totalPurchasePrice)
    } else {
      0.0
    }

    // Possible rounding error
    val loss = if (totalLoss > epsilon) {
      SpannableStringBuilder()
          .color(
              context.getColor(R.color.red)
          ) { bold { append("${DecimalFormat("0.00").format(totalLoss)}\n") } }
    } else {
      SpannableStringBuilder().bold {
        append("0.00\n")
      }
    }

    val total = if (totalAssets > 0.0) {
      totalAssets - totalPurchasePrice
    } else {
      0.0
    }
    val gainloss = when {
      total > 0.0 -> {
        SpannableStringBuilder()
            .color(
                context.getColor(R.color.green)
            ) {
              bold {
                append(
                    "${DecimalFormat("0.00").format(total)}\n"
                )
              }
            }
      }
      total < 0.0 -> {
        SpannableStringBuilder().color(context.getColor(R.color.red)) {
          bold { append("${DecimalFormat("0.00").format(total)}\n") }
        }
      }
      else -> {
        SpannableStringBuilder().bold {
          append("0.00\n")
        }
      }
    }

    val summaryGroup1 = SpannableStringBuilder()
        .append("${context.getString(R.string.summary_gain)} ")
        .append(gain)
        //.color(Color.GREEN, { bold { append("${DecimalFormat("0.00").format(totalGain)}\n") } })
        .append("${context.getString(R.string.summary_loss)} ")
        .append(loss)
        .append("${context.getString(R.string.summary_gain_loss)} ")
        .append(gainloss)
        .append("\n")
        .append("${context.getString(R.string.summary_dividend_assets)} ")
        .bold {
          append(
              "${
                DecimalFormat("0.00")
                    .format(totalDividendAssets)
              }\n"
          )
        }
        .append("${context.getString(R.string.summary_no_dividend_assets)} ")
        .bold {
          append(
              "${
                DecimalFormat("0.00")
                    .format(totalAssets - totalDividendAssets)
              }\n"
          )
        }
        .append("${context.getString(R.string.summary_dividend_per_year)} ")
        .bold {
          append(
              "${
                DecimalFormat("0.00")
                    .format(totalDividend)
              } (${
                DecimalFormat("0.00")
                    .format(
                        totalDividendChange * 100.0
                    )
              }%)\n"
          )
        }
        .append("${context.getString(R.string.totaldividend_payed)} ")
        .bold {
          append(
              "${
                DecimalFormat("0.00")
                    .format(totalDividendPayed)
              }\n"
          )
        }
        .append("${context.getString(R.string.totaldividend_payedYTD)} ")
        .bold {
          append(
              DecimalFormat("0.00")
                  .format(totalDividendPayedYTD)
          )
        }

    // summaryGroup1: Gain, loss, dividend
    // summaryGroup2: Stock summary, properties, assets
    return Pair(summaryGroup1, summaryGroup2)
  }
}
