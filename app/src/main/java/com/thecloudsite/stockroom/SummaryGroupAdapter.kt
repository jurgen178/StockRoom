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
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.text.italic
import androidx.core.text.scale
import androidx.core.text.underline
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.database.Group
import com.thecloudsite.stockroom.databinding.SummarygroupAllItemsBinding
import com.thecloudsite.stockroom.databinding.SummarygroupItemBinding
import com.thecloudsite.stockroom.news.NewsAdapter.BaseViewHolder
import com.thecloudsite.stockroom.utils.DecimalFormat0To4Digits
import com.thecloudsite.stockroom.utils.DecimalFormat2Digits
import com.thecloudsite.stockroom.utils.GainLoss
import com.thecloudsite.stockroom.utils.epsilon
import com.thecloudsite.stockroom.utils.getAssets
import com.thecloudsite.stockroom.utils.getAssetsCapitalGain
import com.thecloudsite.stockroom.utils.getCapitalGainLossText
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.Locale

const val summarygroup_all_items: Int = 0
const val summarygroup_item: Int = 1

data class SummaryData(
  val desc: String,
  val subdesc: String,
  val text1: SpannableStringBuilder,
  val text2: SpannableStringBuilder,
  val color: Int,
  val type: Int
)

class SummaryGroupAdapter internal constructor(
  val context: Context
) : RecyclerView.Adapter<BaseViewHolder<*>>() {

  private lateinit var bindingAllItems: SummarygroupAllItemsBinding
  private lateinit var bindingItem: SummarygroupItemBinding
  private val groupStandardName = context.getString(R.string.standard_group)
  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var data = mutableListOf<SummaryData>()
  private var stockItemsList: List<StockItem> = emptyList()
  private var groupList: List<Group> = emptyList()

  class OnlineDataAllViewHolder(
    val binding: SummarygroupAllItemsBinding
  ) : BaseViewHolder<SummaryData>(binding.root) {
    override fun bind(item: SummaryData) {
    }
  }

  class OnlineDataViewHolder(
    val binding: SummarygroupItemBinding
  ) : BaseViewHolder<SummaryData>(binding.root) {
    override fun bind(item: SummaryData) {
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder<*> {

    return when (viewType) {
      summarygroup_all_items -> {
        bindingAllItems = SummarygroupAllItemsBinding.inflate(inflater, parent, false)
        OnlineDataAllViewHolder(bindingAllItems)
      }
      summarygroup_item -> {
        bindingItem = SummarygroupItemBinding.inflate(inflater, parent, false)
        OnlineDataViewHolder(bindingItem)
      }
      else -> throw IllegalArgumentException("Invalid view type")
    }
  }

  override fun onBindViewHolder(
    holder: BaseViewHolder<*>,
    position: Int
  ) {
    val current: SummaryData = data[position]

    when (holder) {

      is OnlineDataAllViewHolder -> {
        holder.bind(current)

        holder.binding.summaryItemDataDesc.text = current.desc
        holder.binding.summaryItemData.text = current.text1.append("\n")
          .append(current.text2)
      }

      is OnlineDataViewHolder -> {
        holder.bind(current)

        var color = current.color
        if (color == 0) {
          color = context.getColor(R.color.backgroundListColor)
        }
        setBackgroundColor(holder.binding.summaryItemGroup, color)

        holder.binding.summaryItemDataDesc.text = current.desc
        holder.binding.summaryItemDataSubDesc.text = current.subdesc
        holder.binding.summaryItemData1.text = current.text1
        holder.binding.summaryItemData2.text = current.text2
      }

      else -> {
        throw IllegalArgumentException()
      }
    }
  }

  fun updateData(stockItems: List<StockItem>) {
    stockItemsList = stockItems
    updateData()
  }

  private fun updateData() {
    data.clear()
    val (allText1, allText2) = getTotal(0, true, stockItemsList)
    val portfolio = SharedRepository.selectedPortfolio.value ?: ""
    val overview = if (portfolio.isEmpty()) {
      context.getString(R.string.overview_headline_standard_portfolio)
    } else {
      context.getString(R.string.overview_headline_portfolio, portfolio)
    }
    data.add(
      SummaryData(
        overview,
        "",
        allText1,
        allText2,
        context.getColor(R.color.white),
        summarygroup_all_items
      )
    )

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
        group.name.toLowerCase(Locale.ROOT)
      }
        .forEach { group ->
          val (text1, text2) = getTotal(group.color, false, stockItemsList)

          // Get all symbols in that group as a comma separated string.
          val symbolsList = stockItemsList.filter { stockItem ->
            stockItem.stockDBdata.groupColor == group.color
          }
            .map { stockItem ->
              stockItem.stockDBdata.symbol
            }
            .sorted()
            .joinToString(
              prefix = "(",
              separator = ",",
              postfix = ")"
            )

          data.add(
            SummaryData(
              context.getString(R.string.group_name, group.name),
              symbolsList,
              text1,
              text2,
              group.color,
              summarygroup_item
            )
          )
        }
    }

    notifyDataSetChanged()
  }

  override fun getItemViewType(position: Int): Int {
    val element: SummaryData = data[position]
    return element.type
  }

  override fun getItemCount() = data.size

  // groups is the second data source and gets called after updateData(stockItems: List<StockItem>),
  // run updateData for the color assignment
  fun addGroups(groups: List<Group>) {
    groupList = groups

    updateData()
    notifyDataSetChanged()
  }

  private fun getTotal(
    color: Int,
    all: Boolean,
    stockItems: List<StockItem>
  ): Pair<SpannableStringBuilder, SpannableStringBuilder> {

    var totalPurchasePrice = 0.0
    var totalCommission = 0.0
    var totalAssets = 0.0
    var totalGain = 0.0
    //var totalLoss = 0.0
    var totalQuantity = 0.0
    var totalDividendAssets = 0.0
    var totalDividend = 0.0
    // var totalDividendPaid = 0.0
    // var totalDividendPaidYTD = 0.0
    var totalAlerts: Int = 0
    var totalNotes: Int = 0

    // val datetimeYTD = LocalDateTime.of(LocalDateTime.now().year, 1, 1, 0, 0, 0)
    // val secondsYTD = datetimeYTD.toEpochSecond(ZoneOffset.UTC)

    val stockItemsSelected =
      stockItems.filter {
        all || it.stockDBdata.groupColor == color
      }

    var capitalGain = 0.0
    var capitalLoss = 0.0

    var boughtAssets: Int = 0
    var soldAssets: Int = 0

    //val totalGainLossMap: MutableMap<Int, GainLoss> = mutableMapOf()
    val capitalGainLossMap: MutableMap<Int, GainLoss> = mutableMapOf()
    val totalDividendPaidMap: MutableMap<Int, Double> = mutableMapOf()

    stockItemsSelected.forEach { stockItem ->
      val (quantity, price, commission) = getAssets(stockItem.assets)

      totalPurchasePrice += price + commission
      totalQuantity += quantity
      totalCommission += commission

      val (gain, loss, gainLossMap) = getAssetsCapitalGain(stockItem.assets)
      // Merge gain and loss of the individual stock to one gain/loss to prevent
      // having individual loss/gain reported in the summary.
      val capitalGainLoss = gain - loss
      when {
        capitalGainLoss > 0.0 -> {
          capitalGain += capitalGainLoss
        }
        capitalGainLoss < 0.0 -> {
          capitalLoss += -capitalGainLoss
        }
        else -> {
        }
      }

      gainLossMap.forEach { (year, map) ->
        if (!capitalGainLossMap.containsKey(year)) {
          capitalGainLossMap[year] = GainLoss()
        }
        val gainloss = map.gain - map.loss
        if (gainloss >= 0.0) {
          capitalGainLossMap[year]?.gain = capitalGainLossMap[year]?.gain!! + gainloss
        } else {
          capitalGainLossMap[year]?.loss = capitalGainLossMap[year]?.loss!! - gainloss
        }
      }

      boughtAssets += stockItem.assets.filter { asset ->
        asset.quantity > 0.0
      }.size

      soldAssets += stockItem.assets.filter { asset ->
        asset.quantity < 0.0
      }.size

      stockItem.dividends.filter { dividend ->
        dividend.type == DividendType.Received.value
      }
        .sortedBy { dividend ->
          dividend.paydate
        }
        .forEach { dividend ->

          val localDateTime = ZonedDateTime.ofInstant(Instant.ofEpochSecond(dividend.paydate), ZoneOffset.systemDefault())
          val year = localDateTime.year
          if (!totalDividendPaidMap.containsKey(year)) {
            totalDividendPaidMap[year] = 0.0
          }

          totalDividendPaidMap[year] = totalDividendPaidMap[year]!! + dividend.amount

//            totalDividendPaid += dividend.amount
//            if (dividend.paydate >= secondsYTD) {
//              totalDividendPaidYTD += dividend.amount
//            }
        }

      if (stockItem.stockDBdata.alertAbove > 0.0) {
        totalAlerts++
      }
      if (stockItem.stockDBdata.alertBelow > 0.0) {
        totalAlerts++
      }
      if (stockItem.stockDBdata.note.isNotEmpty()) {
        totalNotes++
      }

      if (stockItem.onlineMarketData.marketPrice > 0.0) {
        val assetsPrice = quantity * stockItem.onlineMarketData.marketPrice
        val gainLoss = assetsPrice - (price + commission)

//        val localDateTime = LocalDateTime.ofEpochSecond(asset.date, 0, ZoneOffset.UTC)
//        val year = localDateTime.year
//        if (!totalGainLossMap.containsKey(year)) {
//          totalGainLossMap[year] = GainLoss()
//        }
//        totalGainLossMap[year]?.gain = totalGainLossMap[year]?.gain!! + map.gain
//        totalGainLossMap[year]?.loss = totalGainLossMap[year]?.loss!! + map.loss

        if (gainLoss > 0.0) {
          totalGain += gainLoss
        } //else {
        //totalLoss -= gainLoss
        //}

        totalAssets += assetsPrice

        val annualDividendRate = if (stockItem.stockDBdata.annualDividendRate >= 0.0) {
          stockItem.stockDBdata.annualDividendRate
        } else {
          stockItem.onlineMarketData.annualDividendRate
        }

        if (annualDividendRate > 0.0) {
          totalDividendAssets += assetsPrice
          totalDividend += quantity * annualDividendRate
        }
      }
    }

    val capitalGainLossText = SpannableStringBuilder()

    // Add single year to the summary text.
    if (capitalGainLossMap.size == 1) {
      val year = capitalGainLossMap.keys.first()
      capitalGainLossText.italic { append("$year ") }
    }

    capitalGainLossText.append(
      getCapitalGainLossText(context, capitalGain, capitalLoss, 0.0, "-", "\n")
    )

    // Multiple years gets added to the summary.
    if (capitalGainLossMap.size > 1) {
      // Add yearly details.
      capitalGainLossMap.toSortedMap()
        .forEach { (year, map) ->
          capitalGainLossText.italic { append("$year: ") }
          capitalGainLossText.append(
            getCapitalGainLossText(context, map.gain, map.loss, 0.0, "-", "\n")
          )
        }
    }

    val boughtSoldText = "${boughtAssets}/${soldAssets}"

    val stockAssets = stockItemsSelected.filter {
      it.assets.isNotEmpty()
    }
    val stockEvents = stockItemsSelected.filter {
      it.events.isNotEmpty()
    }
      .sumBy {
        it.events.size
      }

    var totalAssetsStr =
      SpannableStringBuilder().append(
        "\n${context.getString(R.string.summary_total_purchase_price)} "
      )
        .bold { append(DecimalFormat(DecimalFormat2Digits).format(totalPurchasePrice)) }

    if (totalAssets > 0.0) {
      totalAssetsStr.append(
        "\n${context.getString(R.string.summary_total_assets)} "
      )
        .underline { bold { append(DecimalFormat(DecimalFormat2Digits).format(totalAssets)) } }
    }

    // Print the summary assets in larger font.
    if (all) {
      totalAssetsStr = SpannableStringBuilder().scale(1.4f) {
        append(totalAssetsStr)
      }
    }

    val summaryGroup2 = SpannableStringBuilder()
      .append("${context.getString(R.string.summary_stocks)} ")
      .bold { append("${stockItemsSelected.size}\n") }
      .append("${context.getString(R.string.summary_stocks_with_assets)} ")
      .bold { append("${stockAssets.size}\n") }
      .append("${context.getString(R.string.summary_alerts)} ")
      .bold { append("$totalAlerts\n") }
      .append("${context.getString(R.string.summary_bought_sold)} ")
      .bold { append("$boughtSoldText\n") }
      .append("${context.getString(R.string.summary_events)} ")
      .bold { append("$stockEvents\n") }
      .append("${context.getString(R.string.summary_notes)} ")
      .bold { append("$totalNotes\n") }
      .append("${context.getString(R.string.summary_number_of_stocks)} ")
      .bold { append("${DecimalFormat(DecimalFormat0To4Digits).format(totalQuantity)}\n\n") }
      .append("${context.getString(R.string.summary_capital_gain)} ")
      .append(capitalGainLossText)
      .append(totalAssetsStr)

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

    // Possible rounding error
    val gain = if (totalGain > 0.0) {
      totalGain
    } else {
      0.0
    }

    val totalLoss = if (totalAssets > 0.0) {
      totalGain - (totalAssets - totalPurchasePrice)
    } else {
      0.0
    }

    val loss = if (totalLoss > epsilon) {
      totalLoss
    } else {
      0.0
    }

    val total = if (totalAssets > 0.0) {
      totalAssets - totalPurchasePrice
    } else {
      0.0
    }

    var gainLossText = SpannableStringBuilder().append(
      "${context.getString(R.string.summary_gain_loss)}  "
    )
      .append(
        getCapitalGainLossText(context, gain, loss, total, "-", "\n")
      )

    // Print the summary gain in larger font.
    if (all) {
      gainLossText = SpannableStringBuilder().scale(1.4f) {
        append(gainLossText)
      }
    }

    val summaryGroup1 = SpannableStringBuilder()
      .append(gainLossText)
      .append("\n${context.getString(R.string.summary_commission)} ")
      .bold {
        append(
          "${
            DecimalFormat(DecimalFormat2Digits)
              .format(totalCommission)
          }\n"
        )
      }
      .append("\n${context.getString(R.string.summary_no_dividend_assets)} ")
      .bold {
        append(
          "${
            DecimalFormat(DecimalFormat2Digits)
              .format(totalAssets - totalDividendAssets)
          }\n"
        )
      }
      .append("${context.getString(R.string.summary_dividend_assets)} ")
      .bold {
        append(
          "${
            DecimalFormat(DecimalFormat2Digits)
              .format(totalDividendAssets)
          }\n"
        )
      }
      .append("${context.getString(R.string.summary_dividend_per_year)} ")
      .bold {
        append(
          "${
            DecimalFormat(DecimalFormat2Digits)
              .format(totalDividend)
          } (${
            DecimalFormat(DecimalFormat2Digits)
              .format(
                totalDividendChange * 100.0
              )
          }%)\n"
        )
      }
      .append("${context.getString(R.string.totaldividend_paid)} ")

    // Add single year to the summary text.
    if (totalDividendPaidMap.size == 1) {
      val year = totalDividendPaidMap.keys.first()
      summaryGroup1.italic { append("$year ") }
    }

    var totalDividendPaid = 0.0
    totalDividendPaidMap.forEach { (year, dividend) ->
      totalDividendPaid += dividend
    }

    summaryGroup1.bold {
      if (totalDividendPaid > 0.0) {
        color(context.getColor(R.color.green))
        {
          append(
            "${
              DecimalFormat(DecimalFormat2Digits)
                .format(totalDividendPaid)
            }\n"
          )
        }
      } else {
        append("${DecimalFormat(DecimalFormat2Digits).format(0.0)}\n")
      }
    }

    // Multiple years gets added to the summary.
    if (totalDividendPaidMap.size > 1) {
      // Add yearly details.
      totalDividendPaidMap.toSortedMap()
        .forEach { (year, dividend) ->
          summaryGroup1.italic { append(" $year: ") }
          if (dividend > 0.0) {
            summaryGroup1.color(context.getColor(R.color.green))
            {
              append(
                "${
                  DecimalFormat(DecimalFormat2Digits)
                    .format(dividend)
                }\n"
              )
            }
          } else {
            summaryGroup1.append("${DecimalFormat(DecimalFormat2Digits).format(0.0)}\n")
          }
        }
    }

    // summaryGroup1: Gain, loss, dividend
    // summaryGroup2: Stock summary, properties, assets
    return Pair(summaryGroup1, summaryGroup2)
  }
}
