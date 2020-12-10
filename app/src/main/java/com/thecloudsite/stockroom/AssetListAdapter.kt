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
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.text.italic
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.utils.getAssets
import com.thecloudsite.stockroom.utils.getAssetsCapitalGain
import com.thecloudsite.stockroom.utils.getCapitalGainLossText
import com.thecloudsite.stockroom.utils.obsoleteAssetType
import kotlinx.android.synthetic.main.assetview_item.view.textViewAssetDelete
import kotlinx.android.synthetic.main.assetview_item.view.textViewAssetItemsLayout
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM
import kotlin.math.absoluteValue

// https://codelabs.developers.google.com/codelabs/kotlin-android-training-diffutil-databinding/#4

class AssetListAdapter internal constructor(
  private val context: Context,
  private val clickListenerUpdate: (Asset) -> Unit,
  private val clickListenerDelete: (String?, Asset?) -> Unit
) : RecyclerView.Adapter<AssetListAdapter.AssetViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var assetList = mutableListOf<Asset>()
  private var assetsCopy = listOf<Asset>()
  private var defaultTextColor: Int? = null

  class AssetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    fun bindUpdate(
      asset: Asset,
      clickListenerUpdate: (Asset) -> Unit
    ) {
      itemView.textViewAssetItemsLayout.setOnClickListener { clickListenerUpdate(asset) }
    }

    fun bindDelete(
      symbol: String?,
      asset: Asset?,
      clickListenerDelete: (String?, Asset?) -> Unit
    ) {
      itemView.textViewAssetDelete.setOnClickListener { clickListenerDelete(symbol, asset) }
    }

    val itemViewQuantity: TextView = itemView.findViewById(R.id.textViewAssetQuantity)
    val itemViewPrice: TextView = itemView.findViewById(R.id.textViewAssetPrice)
    val itemViewTotal: TextView = itemView.findViewById(R.id.textViewAssetTotal)
    val itemViewDate: TextView = itemView.findViewById(R.id.textViewAssetDate)
    val itemViewNote: TextView = itemView.findViewById(R.id.textViewAssetNote)
    val itemViewDelete: TextView = itemView.findViewById(R.id.textViewAssetDelete)
    val assetSummaryView: LinearLayout = itemView.findViewById(R.id.assetSummaryView)
    val assetSummaryTextView: TextView = itemView.findViewById(R.id.assetSummaryTextView)
    val itemViewLayout: ConstraintLayout = itemView.findViewById(R.id.textViewAssetLayout)
    val textViewAssetItemsLayout: LinearLayout =
      itemView.findViewById(R.id.textViewAssetItemsLayout)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): AssetViewHolder {
    val itemView = inflater.inflate(R.layout.assetview_item, parent, false)
    return AssetViewHolder(itemView)
  }

  override fun onBindViewHolder(
    holder: AssetViewHolder,
    position: Int
  ) {
    val current: Asset = assetList[position]

    if (defaultTextColor == null) {
      defaultTextColor = holder.itemViewQuantity.currentTextColor
    }

    // First entry is headline.
    if (position == 0) {
      holder.itemViewQuantity.text = context.getString(R.string.quantity)
      holder.itemViewPrice.text = context.getString(R.string.price)
      holder.itemViewTotal.text = context.getString(R.string.value)
      holder.itemViewDate.text = context.getString(R.string.date)
      holder.itemViewNote.text = context.getString(R.string.note)
      holder.itemViewDelete.visibility = View.GONE
      holder.assetSummaryView.visibility = View.GONE
      holder.itemViewLayout.setBackgroundColor(context.getColor(R.color.backgroundListColor))

      val background = TypedValue()
      holder.textViewAssetItemsLayout.setBackgroundResource(background.resourceId)
    } else {
      // Last entry is summary.
      if (position == assetList.size - 1) {
        // handler for delete all
        holder.bindDelete(current.symbol, null, clickListenerDelete)

        val isSum = current.quantity > 0.0 && current.price > 0.0

        holder.itemViewQuantity.text = if (isSum) {
          DecimalFormat("0.####").format(current.quantity)
        } else {
          ""
        }

        holder.itemViewPrice.text = if (isSum) {
          DecimalFormat("0.00##").format(current.price / current.quantity)
        } else {
          ""
        }
        holder.itemViewTotal.text = DecimalFormat("0.00").format(current.price)
        holder.itemViewDate.text = ""
        holder.itemViewNote.text = ""

        // no delete icon for empty list, headline + summaryline = 2
        if (assetList.size <= 2) {
          holder.itemViewDelete.visibility = View.GONE
        } else {
          holder.itemViewDelete.visibility = View.VISIBLE
        }

        holder.assetSummaryView.visibility = View.VISIBLE

        if (assetsCopy.isNotEmpty()) {
          val (capitalGain, capitalLoss) = getAssetsCapitalGain(assetsCopy)
          val capitalGainLossText = getCapitalGainLossText(context, capitalGain, capitalLoss)
          holder.assetSummaryTextView.text = SpannableStringBuilder()
              .append("${context.getString(R.string.summary_capital_gain)} ")
              .append(capitalGainLossText)
              .append("\n${context.getString(R.string.asset_summary_text)}")
        } else {
          holder.assetSummaryTextView.text = context.getString(R.string.asset_summary_text)
        }

        holder.itemViewLayout.setBackgroundColor(Color.YELLOW)

        val background = TypedValue()
        holder.textViewAssetItemsLayout.setBackgroundResource(background.resourceId)
      } else {
        // Asset items
        holder.bindUpdate(current, clickListenerUpdate)
        holder.bindDelete(null, current, clickListenerDelete)

        val colorNegativeAsset = context.getColor(R.color.negativeAsset)
        val colorObsoleteAsset = context.getColor(R.color.obsoleteAsset)

        // Removed and obsolete entries are colored gray.
        when {
          current.quantity < 0.0 -> {
            holder.itemViewQuantity.setTextColor(colorNegativeAsset)
            holder.itemViewPrice.setTextColor(colorNegativeAsset)
            holder.itemViewTotal.setTextColor(colorNegativeAsset)
            holder.itemViewDate.setTextColor(colorNegativeAsset)
            holder.itemViewNote.setTextColor(colorNegativeAsset)
          }
          current.type and obsoleteAssetType != 0 -> {
            holder.itemViewQuantity.setTextColor(colorObsoleteAsset)
            holder.itemViewPrice.setTextColor(colorObsoleteAsset)
            holder.itemViewTotal.setTextColor(colorObsoleteAsset)
            holder.itemViewDate.setTextColor(colorObsoleteAsset)
            holder.itemViewNote.setTextColor(colorObsoleteAsset)
          }
          defaultTextColor != null -> {
            holder.itemViewQuantity.setTextColor(defaultTextColor!!)
            holder.itemViewPrice.setTextColor(defaultTextColor!!)
            holder.itemViewTotal.setTextColor(defaultTextColor!!)
            holder.itemViewDate.setTextColor(defaultTextColor!!)
            holder.itemViewNote.setTextColor(defaultTextColor!!)
          }
        }

        val itemViewQuantityText = DecimalFormat("0.####").format(current.quantity)
        val itemViewPriceText = if (current.price > 0.0) {
          DecimalFormat("0.00##").format(current.price)
        } else {
          ""
        }
        val itemViewTotalText = if (current.price > 0.0) {
          DecimalFormat("0.00").format(current.quantity.absoluteValue * current.price)
        } else {
          ""
        }
        val datetime: LocalDateTime =
          LocalDateTime.ofEpochSecond(current.date, 0, ZoneOffset.UTC)
        val itemViewDateText =
          datetime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
        val itemViewNoteText = current.note

        // Negative values in italic.
        if (current.quantity < 0.0) {
          holder.itemViewQuantity.text =
            SpannableStringBuilder().italic { append(itemViewQuantityText) }
          holder.itemViewPrice.text =
            SpannableStringBuilder().italic { append(itemViewPriceText) }
          holder.itemViewTotal.text =
            SpannableStringBuilder().italic { append(itemViewTotalText) }
          holder.itemViewDate.text =
            SpannableStringBuilder().italic { append(itemViewDateText) }
          holder.itemViewNote.text =
            SpannableStringBuilder().italic { append(itemViewNoteText) }
        } else {
          holder.itemViewQuantity.text = itemViewQuantityText
          holder.itemViewPrice.text = itemViewPriceText
          holder.itemViewTotal.text = itemViewTotalText
          holder.itemViewDate.text = itemViewDateText
          holder.itemViewNote.text = itemViewNoteText
        }

        holder.itemViewDelete.visibility = View.VISIBLE
        holder.assetSummaryView.visibility = View.GONE
        holder.itemViewLayout.background = null

        val background = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, background, true)
        holder.textViewAssetItemsLayout.setBackgroundResource(background.resourceId)
      }
    }
  }

  internal fun updateAssets(assets: List<Asset>) {
    assetsCopy = assets

    // Headline placeholder
    assetList = mutableListOf(
        Asset(
            symbol = "",
            quantity = 0.0,
            price = 0.0
        )
    )

    // Sort assets in the list by date.
    val sortedList = assets.sortedBy { asset ->
      asset.date
    }

    val (totalQuantity, totalPrice) = getAssets(sortedList, obsoleteAssetType)

    assetList.addAll(sortedList)

//    val totalQuantity = assetList.sumByDouble {
//      it.shares
//    }
//
//    val totalPrice = assetList.sumByDouble {
//      it.shares * it.price
//    }

    // Summary
    val symbol: String = assets.firstOrNull()?.symbol ?: ""
    assetList.add(
        Asset(
            id = null,
            symbol = symbol,
            quantity = totalQuantity,
            price = totalPrice
        )
    )

    notifyDataSetChanged()
  }

  override fun getItemCount() = assetList.size
}
