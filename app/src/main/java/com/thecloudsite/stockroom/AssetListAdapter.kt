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
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.text.italic
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.databinding.AssetviewItemBinding
import com.thecloudsite.stockroom.utils.DecimalFormat0To4Digits
import com.thecloudsite.stockroom.utils.DecimalFormat2Digits
import com.thecloudsite.stockroom.utils.DecimalFormat2To4Digits
import com.thecloudsite.stockroom.utils.getAssetChange
import com.thecloudsite.stockroom.utils.getAssets
import com.thecloudsite.stockroom.utils.getAssetsCapitalGain
import com.thecloudsite.stockroom.utils.getCapitalGainLossText
import com.thecloudsite.stockroom.utils.obsoleteAssetType
import java.text.DecimalFormat
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM
import kotlin.math.absoluteValue

// https://codelabs.developers.google.com/codelabs/kotlin-android-training-diffutil-databinding/#4

data class AssetListData(
  var asset: Asset,
  var onlineMarketData: OnlineMarketData? = null,
  var assetChangeText: SpannableStringBuilder = SpannableStringBuilder(),
  var assetText: SpannableStringBuilder = SpannableStringBuilder()
)

class AssetListAdapter internal constructor(
  private val context: Context,
  private val clickListenerUpdate: (Asset) -> Unit,
  private val clickListenerDelete: (String?, Asset?) -> Unit
) : RecyclerView.Adapter<AssetListAdapter.AssetViewHolder>() {

  private val inflater: LayoutInflater = LayoutInflater.from(context)
  private var assetList = mutableListOf<AssetListData>()
  private var assetsCopy = listOf<Asset>()
  private var defaultTextColor: Int? = null

  class AssetViewHolder(
    val binding: AssetviewItemBinding
  ) : RecyclerView.ViewHolder(binding.root) {
    fun bindUpdate(
      asset: Asset,
      clickListenerUpdate: (Asset) -> Unit
    ) {
      binding.textViewAssetItemsLayout.setOnClickListener { clickListenerUpdate(asset) }
    }

    fun bindDelete(
      symbol: String?,
      asset: Asset?,
      clickListenerDelete: (String?, Asset?) -> Unit
    ) {
      binding.textViewAssetDelete.setOnClickListener { clickListenerDelete(symbol, asset) }
    }
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): AssetViewHolder {

    val binding = AssetviewItemBinding.inflate(inflater, parent, false)
    return AssetViewHolder(binding)
  }

  override fun onBindViewHolder(
    holder: AssetViewHolder,
    position: Int
  ) {
    val current: AssetListData = assetList[position]

    if (defaultTextColor == null) {
      defaultTextColor = holder.binding.textViewAssetQuantity.currentTextColor
    }

    // First entry is headline.
    if (position == 0) {
      holder.binding.textViewAssetQuantity.text = context.getString(R.string.assetlistquantity)
      holder.binding.textViewAssetPrice.text = context.getString(R.string.assetlistprice)
      holder.binding.textViewAssetTotal.text = context.getString(R.string.assetlisttotal)
      holder.binding.textViewAssetChange.text = context.getString(R.string.assetlistchange)
      holder.binding.textViewAssetValue.text = context.getString(R.string.assetlistvalue)
      holder.binding.textViewAssetDate.text = context.getString(R.string.assetlistdate)
      holder.binding.textViewAssetNote.text = context.getString(R.string.assetlistnote)
      holder.binding.textViewAssetDelete.visibility = View.GONE
      holder.binding.assetSummaryView.visibility = View.GONE
      holder.binding.textViewAssetLayout.setBackgroundColor(
        context.getColor(R.color.backgroundListColor)
      )

      val background = TypedValue()
      holder.binding.textViewAssetItemsLayout.setBackgroundResource(background.resourceId)
    } else {
      // Last entry is summary.
      if (position == assetList.size - 1) {
        // handler for delete all
        holder.bindDelete(current.asset.symbol, null, clickListenerDelete)

        val isSum = current.asset.quantity > 0.0 && current.asset.price > 0.0

        // Summary line is always black on yellow
        holder.binding.textViewAssetQuantity.text = SpannableStringBuilder()
          .color(Color.BLACK) {
            append(
              if (isSum) {
                DecimalFormat(DecimalFormat0To4Digits).format(current.asset.quantity)
              } else {
                ""
              }
            )
          }

        holder.binding.textViewAssetPrice.text = SpannableStringBuilder()
          .color(Color.BLACK) {
            append(
              if (isSum) {
                DecimalFormat(DecimalFormat2To4Digits).format(
                  current.asset.price / current.asset.quantity
                )
              } else {
                ""
              }
            )
          }

        holder.binding.textViewAssetTotal.text =
          SpannableStringBuilder()
            .color(Color.BLACK) { append(DecimalFormat(DecimalFormat2Digits).format(current.asset.price)) }
        holder.binding.textViewAssetChange.text = current.assetChangeText
        holder.binding.textViewAssetValue.text = SpannableStringBuilder()
          .color(Color.BLACK) { append(current.assetText) }
        holder.binding.textViewAssetDate.text = ""
        holder.binding.textViewAssetNote.text = ""

        // no delete icon for empty list, headline + summaryline = 2
        if (assetList.size <= 2) {
          holder.binding.textViewAssetDelete.visibility = View.GONE
        } else {
          holder.binding.textViewAssetDelete.visibility = View.VISIBLE
        }

        holder.binding.assetSummaryView.visibility = View.VISIBLE

        if (assetsCopy.isNotEmpty()) {
          val (capitalGain, capitalLoss, gainLossMap) = getAssetsCapitalGain(assetsCopy)
          val capitalGainLossText = getCapitalGainLossText(context, capitalGain, capitalLoss)
          holder.binding.assetSummaryTextView.text = SpannableStringBuilder()
            .append("${context.getString(R.string.summary_capital_gain)} ")
            .append(capitalGainLossText)
            .append("\n${context.getString(R.string.asset_summary_text)}")
        } else {
          holder.binding.assetSummaryTextView.text = context.getString(R.string.asset_summary_text)
        }

        holder.binding.textViewAssetLayout.setBackgroundColor(Color.YELLOW)

        val background = TypedValue()
        holder.binding.textViewAssetItemsLayout.setBackgroundResource(background.resourceId)
      } else {
        // Asset items
        holder.bindUpdate(current.asset, clickListenerUpdate)
        holder.bindDelete(null, current.asset, clickListenerDelete)

        val colorNegativeAsset = context.getColor(R.color.negativeAsset)
        val colorObsoleteAsset = context.getColor(R.color.obsoleteAsset)

        // Removed and obsolete entries are colored gray.
        when {
          current.asset.quantity < 0.0 -> {
            holder.binding.textViewAssetQuantity.setTextColor(colorNegativeAsset)
            holder.binding.textViewAssetPrice.setTextColor(colorNegativeAsset)
            holder.binding.textViewAssetTotal.setTextColor(colorNegativeAsset)
            holder.binding.textViewAssetDate.setTextColor(colorNegativeAsset)
            holder.binding.textViewAssetNote.setTextColor(colorNegativeAsset)
          }
          current.asset.type and obsoleteAssetType != 0 -> {
            holder.binding.textViewAssetQuantity.setTextColor(colorObsoleteAsset)
            holder.binding.textViewAssetPrice.setTextColor(colorObsoleteAsset)
            holder.binding.textViewAssetTotal.setTextColor(colorObsoleteAsset)
            holder.binding.textViewAssetDate.setTextColor(colorObsoleteAsset)
            holder.binding.textViewAssetNote.setTextColor(colorObsoleteAsset)
          }
          defaultTextColor != null -> {
            holder.binding.textViewAssetQuantity.setTextColor(defaultTextColor!!)
            holder.binding.textViewAssetPrice.setTextColor(defaultTextColor!!)
            holder.binding.textViewAssetTotal.setTextColor(defaultTextColor!!)
            holder.binding.textViewAssetDate.setTextColor(defaultTextColor!!)
            holder.binding.textViewAssetNote.setTextColor(defaultTextColor!!)
          }
        }

        val itemViewQuantityText =
          DecimalFormat(DecimalFormat0To4Digits).format(current.asset.quantity)
        val itemViewPriceText = if (current.asset.price > 0.0) {
          DecimalFormat(DecimalFormat2To4Digits).format(current.asset.price)
        } else {
          ""
        }
        val itemViewTotalText = if (current.asset.price > 0.0) {
          DecimalFormat(DecimalFormat2Digits).format(
            current.asset.quantity.absoluteValue * current.asset.price
          )
        } else {
          ""
        }
        val itemViewChangeText =
          if (current.asset.price > 0.0 && current.onlineMarketData != null) {
            getAssetChange(
              current.asset.quantity.absoluteValue,
              current.asset.quantity.absoluteValue * current.asset.price,
              current.onlineMarketData!!.marketPrice,
              current.onlineMarketData!!.postMarketData,
              Color.DKGRAY,
              context
            ).second
          } else {
            ""
          }
        val itemViewValueText =
          if (current.asset.price > 0.0 && current.onlineMarketData != null) {
            val marketPrice = current.onlineMarketData!!.marketPrice
            SpannableStringBuilder()
              .bold {
                append(
                  DecimalFormat(DecimalFormat2Digits).format(
                    current.asset.quantity * marketPrice
                  )
                )
              }
          } else {
            ""
          }
        val datetime: LocalDateTime =
          LocalDateTime.ofEpochSecond(current.asset.date, 0, ZoneOffset.UTC)
        val itemViewDateText =
          datetime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
        val itemViewNoteText = current.asset.note

        // Negative values in italic.
        if (current.asset.quantity < 0.0) {
          holder.binding.textViewAssetQuantity.text =
            SpannableStringBuilder().italic { append(itemViewQuantityText) }
          holder.binding.textViewAssetPrice.text =
            SpannableStringBuilder().italic { append(itemViewPriceText) }
          holder.binding.textViewAssetTotal.text =
            SpannableStringBuilder().italic { append(itemViewTotalText) }
          holder.binding.textViewAssetChange.text =
            SpannableStringBuilder().italic { append(itemViewChangeText) }
          holder.binding.textViewAssetValue.text =
            SpannableStringBuilder().italic { append(itemViewValueText) }
          holder.binding.textViewAssetDate.text =
            SpannableStringBuilder().italic { append(itemViewDateText) }
          holder.binding.textViewAssetNote.text =
            SpannableStringBuilder().italic { append(itemViewNoteText) }
        } else {
          holder.binding.textViewAssetQuantity.text = itemViewQuantityText
          holder.binding.textViewAssetPrice.text = itemViewPriceText
          holder.binding.textViewAssetTotal.text = itemViewTotalText
          holder.binding.textViewAssetChange.text = itemViewChangeText
          holder.binding.textViewAssetValue.text = itemViewValueText
          holder.binding.textViewAssetDate.text = itemViewDateText
          holder.binding.textViewAssetNote.text = itemViewNoteText
        }

        holder.binding.textViewAssetDelete.visibility = View.VISIBLE
        holder.binding.assetSummaryView.visibility = View.GONE
        holder.binding.textViewAssetLayout.background = null

        val background = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, background, true)
        holder.binding.textViewAssetItemsLayout.setBackgroundResource(background.resourceId)
      }
    }
  }

  internal fun updateAssets(assetData: StockAssetsLiveData) {
    if (assetData.assets != null) {
      assetsCopy = assetData.assets!!.assets

      // Headline placeholder
      assetList = mutableListOf(
        AssetListData(
          asset = Asset(
            symbol = "",
            quantity = 0.0,
            price = 0.0
          )
        )
      )

      // Sort assets in the list by date.
      val sortedList = assetData.assets!!.assets.sortedBy { asset ->
        asset.date
      }

      val (totalQuantity, totalPrice) = getAssets(sortedList, obsoleteAssetType)

      val sortedDataList = sortedList.map {
        AssetListData(
          asset = it,
          onlineMarketData = assetData.onlineMarketData
        )
      }

      assetList.addAll(sortedDataList)

      // Summary
      val symbol: String = assetData.assets!!.assets.firstOrNull()?.symbol ?: ""
      val assetChange = if (assetData.onlineMarketData != null) {
        getAssetChange(
          assetData.assets!!.assets,
          assetData.onlineMarketData!!.marketPrice,
          assetData.onlineMarketData!!.postMarketData,
          Color.DKGRAY,
          context
        ).second
      } else {
        SpannableStringBuilder()
      }

      val asset = if (assetData.onlineMarketData != null) {
        SpannableStringBuilder()
          .bold {
            append(
              DecimalFormat(DecimalFormat2Digits).format(
                totalQuantity * assetData.onlineMarketData!!.marketPrice
              )
            )
          }
      } else {
        SpannableStringBuilder()
      }

      assetList.add(
        AssetListData(
          asset = Asset(
            id = null,
            symbol = symbol,
            quantity = totalQuantity,
            price = totalPrice
          ),
          assetChangeText = assetChange,
          assetText = asset
        )
      )
    }

    notifyDataSetChanged()
  }

  override fun getItemCount() = assetList.size
}
