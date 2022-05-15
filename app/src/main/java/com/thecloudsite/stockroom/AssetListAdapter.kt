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
import com.thecloudsite.stockroom.AssetListAdapter.BaseViewHolder
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.databinding.AssetviewItemBinding
import com.thecloudsite.stockroom.utils.*
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM
import kotlin.math.absoluteValue

// https://codelabs.developers.google.com/codelabs/kotlin-android-training-diffutil-databinding/#4

const val asset_headline_type: Int = 0
const val asset_item_type: Int = 1
const val asset_summary_type: Int = 2

data class AssetListData(
    val viewType: Int,
    var transferItem: Boolean = false,
    val deleteAll: Boolean = false,
    var asset: Asset,
    var onlineMarketData: OnlineMarketData? = null,
    var assetChangeText: SpannableStringBuilder = SpannableStringBuilder(),
    var assetText: SpannableStringBuilder = SpannableStringBuilder(),
    var capitalGainLossText: SpannableStringBuilder = SpannableStringBuilder(),
)

class AssetListAdapter internal constructor(
    private val context: Context,
    private val clickListenerUpdateLambda: (Asset) -> Unit,
    private val clickListenerDeleteLambda: (String?, Asset?) -> Unit
) : RecyclerView.Adapter<BaseViewHolder<*>>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var assetList = mutableListOf<AssetListData>()
    private var defaultTextColor: Int? = null

    abstract class BaseViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {

        abstract fun bindUpdate(
            asset: Asset,
            clickListenerUpdateLambda: (Asset) -> Unit
        )

        abstract fun bindDelete(
            symbol: String?,
            asset: Asset?,
            clickListenerDeleteLambda: (String?, Asset?) -> Unit
        )
    }

    class HeadlineViewHolder(
        val binding: AssetviewItemBinding
    ) : BaseViewHolder<AssetListData>(binding.root) {

        override fun bindUpdate(
            asset: Asset,
            clickListenerUpdateLambda: (Asset) -> Unit
        ) {
        }

        override fun bindDelete(
            symbol: String?,
            asset: Asset?,
            clickListenerDeleteLambda: (String?, Asset?) -> Unit
        ) {
        }
    }

    class AssetViewHolder(
        val binding: AssetviewItemBinding
    ) : BaseViewHolder<AssetListData>(binding.root) {

        override fun bindUpdate(
            asset: Asset,
            clickListenerUpdateLambda: (Asset) -> Unit
        ) {
            binding.textViewAssetItemsLayout.setOnClickListener { clickListenerUpdateLambda(asset) }
        }

        override fun bindDelete(
            symbol: String?,
            asset: Asset?,
            clickListenerDeleteLambda: (String?, Asset?) -> Unit
        ) {
            binding.textViewAssetDelete.setOnClickListener {
                clickListenerDeleteLambda(
                    symbol,
                    asset
                )
            }
        }
    }

    class SummaryViewHolder(
        val binding: AssetviewItemBinding
    ) : BaseViewHolder<AssetListData>(binding.root) {

        override fun bindUpdate(
            asset: Asset,
            clickListenerUpdateLambda: (Asset) -> Unit
        ) {
        }

        override fun bindDelete(
            symbol: String?,
            asset: Asset?,
            clickListenerDeleteLambda: (String?, Asset?) -> Unit
        ) {
            binding.textViewAssetDelete.setOnClickListener {
                clickListenerDeleteLambda(
                    symbol,
                    asset
                )
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BaseViewHolder<*> {

        return when (viewType) {
            asset_headline_type -> {
                val binding = AssetviewItemBinding.inflate(inflater, parent, false)
                HeadlineViewHolder(binding)
            }

            asset_item_type -> {
                val binding = AssetviewItemBinding.inflate(inflater, parent, false)
                AssetViewHolder(binding)
            }

            asset_summary_type -> {
                val binding = AssetviewItemBinding.inflate(inflater, parent, false)
                SummaryViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(
        holder: BaseViewHolder<*>,
        position: Int
    ) {

        val current: AssetListData = assetList[position]

        when (holder) {

            is HeadlineViewHolder -> {

                holder.binding.textViewAssetQuantity.text =
                    context.getString(R.string.assetlistquantity)
                holder.binding.textViewAssetPrice.text = context.getString(R.string.assetlistprice)
                holder.binding.textViewAssetTotal.text = context.getString(R.string.assetlisttotal)
                holder.binding.textViewAssetChange.text =
                    context.getString(R.string.assetlistchange)
                holder.binding.textViewAssetValue.text = context.getString(R.string.assetlistvalue)
                holder.binding.textViewAssetFee.text =
                    context.getString(R.string.assetlistfee)
                holder.binding.textViewAssetDate.text = context.getString(R.string.assetlistdate)
                holder.binding.textViewAssetAccount.text =
                    context.getString(R.string.assetlistaccount)
                holder.binding.textViewAssetNote.text = context.getString(R.string.assetlistnote)

                holder.binding.textViewAssetDelete.visibility = View.GONE
                holder.binding.assetSummaryView.visibility = View.GONE

                holder.binding.textViewAssetLayout.setBackgroundColor(
                    context.getColor(R.color.backgroundListColor)
                )
                val background = TypedValue()
                holder.binding.textViewAssetItemsLayout.setBackgroundResource(background.resourceId)
            }

            is AssetViewHolder -> {

                if (defaultTextColor == null) {
                    defaultTextColor = holder.binding.textViewAssetQuantity.currentTextColor
                }

                // Asset items
                holder.bindUpdate(current.asset, clickListenerUpdateLambda)
                holder.bindDelete(null, current.asset, clickListenerDeleteLambda)

                val colorNegativeAsset = context.getColor(R.color.negativeAsset)
                val colorObsoleteAsset = context.getColor(R.color.obsoleteAsset)

                // Removed, Transfer items and obsolete entries are colored gray.
                when {
                    current.asset.quantity < 0.0 -> {
                        holder.binding.textViewAssetQuantity.setTextColor(colorNegativeAsset)
                        holder.binding.textViewAssetPrice.setTextColor(colorNegativeAsset)
                        holder.binding.textViewAssetTotal.setTextColor(colorNegativeAsset)
                        holder.binding.textViewAssetFee.setTextColor(colorNegativeAsset)
                        holder.binding.textViewAssetDate.setTextColor(colorNegativeAsset)
                        holder.binding.textViewAssetAccount.setTextColor(colorNegativeAsset)
                        holder.binding.textViewAssetNote.setTextColor(colorNegativeAsset)
                    }
                    current.asset.type and (obsoleteAssetType or transferAssetType) != 0 || current.transferItem -> {
                        holder.binding.textViewAssetQuantity.setTextColor(colorObsoleteAsset)
                        holder.binding.textViewAssetPrice.setTextColor(colorObsoleteAsset)
                        holder.binding.textViewAssetTotal.setTextColor(colorObsoleteAsset)
                        holder.binding.textViewAssetFee.setTextColor(colorObsoleteAsset)
                        holder.binding.textViewAssetDate.setTextColor(colorObsoleteAsset)
                        holder.binding.textViewAssetAccount.setTextColor(colorObsoleteAsset)
                        holder.binding.textViewAssetNote.setTextColor(colorObsoleteAsset)
                    }
                    defaultTextColor != null -> {
                        holder.binding.textViewAssetQuantity.setTextColor(defaultTextColor!!)
                        holder.binding.textViewAssetPrice.setTextColor(defaultTextColor!!)
                        holder.binding.textViewAssetTotal.setTextColor(defaultTextColor!!)
                        holder.binding.textViewAssetFee.setTextColor(defaultTextColor!!)
                        holder.binding.textViewAssetDate.setTextColor(defaultTextColor!!)
                        holder.binding.textViewAssetAccount.setTextColor(defaultTextColor!!)
                        holder.binding.textViewAssetNote.setTextColor(defaultTextColor!!)
                    }
                }

                // Set color marker for the item.
                holder.binding.textViewAssetMarkerColor.setBackgroundColor(
                    if (current.transferItem) {
                        Color.CYAN     // transfer item
                    } else
                        if (current.asset.quantity > 0.0) {

                            if (current.asset.price == 0.0) {
                                0xffC23FFF.toInt()  // for free, asset.price = 0.0, C23FF=Violet
                            } else {
                                Color.BLUE  // bought
                            }

                        } else
                            if (current.asset.quantity < 0.0) {

                                if (current.asset.price == 0.0) {
                                    Color.YELLOW        // miner fee, asset.price = 0.0
                                } else {
                                    0xffFF6A00.toInt()  // sold, FF6A00=Orange
                                }

                            } else {

                                context.getColor(R.color.backgroundListColor)

                            }
                )

                val itemViewQuantityText =
                    DecimalFormat(DecimalFormatQuantityDigits).format(current.asset.quantity)

                // No price for display items.
                val itemViewPriceText = if (current.transferItem) {
                    ""
                } else {
                    to2To8Digits(current.asset.price)
                }

                val itemViewTotalText = if (current.transferItem) {
                    ""
                } else {
                    DecimalFormat(DecimalFormat2Digits).format(
                        current.asset.quantity.absoluteValue * current.asset.price
                    )
                }

                val itemViewChangeText =
                    if (!current.transferItem && current.onlineMarketData != null) {
                        getAssetChange(
                            current.asset.quantity.absoluteValue,
                            current.asset.quantity.absoluteValue * current.asset.price,
                            current.onlineMarketData!!.marketPrice,
                            current.onlineMarketData!!.postMarketData,
                            Color.DKGRAY,
                            context
                        ).displayColorStr
                    } else {
                        ""
                    }

                val itemViewValueText =
                    if (current.onlineMarketData != null) {
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
                val itemViewFeeText =
                    if (current.asset.fee > 0.0) {
                        DecimalFormat(DecimalFormat2Digits).format(current.asset.fee)
                    } else {
                        ""
                    }
                val datetime
                        : ZonedDateTime =
                    ZonedDateTime.ofInstant(
                        Instant.ofEpochSecond(current.asset.date),
                        ZoneOffset.systemDefault()
                    )
                val itemViewDateText =
                    datetime.format(DateTimeFormatter.ofLocalizedDate(MEDIUM))
                val itemViewAccountText = current.asset.account
                val itemViewNoteText = current.asset.note

                // Negative values in italic.
                if (current.asset.quantity < 0.0) {
                    holder.binding.textViewAssetQuantity.text =
                        SpannableStringBuilder().italic { append(itemViewQuantityText) }
                    holder.binding.textViewAssetTotal.text =
                        SpannableStringBuilder().italic { append(itemViewTotalText) }
                    holder.binding.textViewAssetChange.text =
                        SpannableStringBuilder().italic { append(itemViewChangeText) }
                    holder.binding.textViewAssetPrice.text =
                        SpannableStringBuilder().italic { append(itemViewPriceText) }
                    holder.binding.textViewAssetValue.text =
                        SpannableStringBuilder().italic { append(itemViewValueText) }
                    holder.binding.textViewAssetFee.text =
                        SpannableStringBuilder().italic { append(itemViewFeeText) }
                    holder.binding.textViewAssetDate.text =
                        SpannableStringBuilder().italic { append(itemViewDateText) }
                    holder.binding.textViewAssetAccount.text =
                        SpannableStringBuilder().italic { append(itemViewAccountText) }
                    holder.binding.textViewAssetNote.text =
                        SpannableStringBuilder().italic { append(itemViewNoteText) }
                } else {
                    holder.binding.textViewAssetQuantity.text = itemViewQuantityText
                    holder.binding.textViewAssetPrice.text = itemViewPriceText
                    holder.binding.textViewAssetTotal.text = itemViewTotalText
                    holder.binding.textViewAssetChange.text = itemViewChangeText
                    holder.binding.textViewAssetValue.text = itemViewValueText
                    holder.binding.textViewAssetFee.text = itemViewFeeText
                    holder.binding.textViewAssetDate.text = itemViewDateText
                    holder.binding.textViewAssetAccount.text = itemViewAccountText
                    holder.binding.textViewAssetNote.text = itemViewNoteText
                }

                holder.binding.textViewAssetDelete.visibility = View.VISIBLE
                holder.binding.assetSummaryView.visibility = View.GONE

                holder.binding.textViewAssetLayout.background = null
                val background = TypedValue()
                context.theme.resolveAttribute(
                    android.R.attr.selectableItemBackground,
                    background,
                    true
                )
                holder.binding.textViewAssetItemsLayout.setBackgroundResource(background.resourceId)
            }

            is SummaryViewHolder -> {

                // Summary line is always black on yellow
                holder.binding.textViewAssetQuantity.text = SpannableStringBuilder()
                    .color(Color.BLACK) {
                        append(DecimalFormat(DecimalFormatQuantityDigits).format(current.asset.quantity))
                    }

                holder.binding.textViewAssetPrice.text = SpannableStringBuilder()
                    .color(Color.BLACK) {
                        append(
                            if (current.asset.quantity > 0.0) {
                                to2To8Digits(
                                    current.asset.price / current.asset.quantity
                                )
                            } else {
                                ""
                            }
                        )
                    }

                holder.binding.textViewAssetFee.text = if (current.asset.fee > 0.0) {
                    SpannableStringBuilder()
                        .color(Color.BLACK) {
                            append(
                                DecimalFormat(DecimalFormat2To4Digits).format(
                                    current.asset.fee
                                )
                            )
                        }
                } else {
                    ""
                }

                holder.binding.textViewAssetTotal.text =
                    SpannableStringBuilder()
                        .color(Color.BLACK) {
                            append(
                                DecimalFormat(DecimalFormat2Digits).format(
                                    current.asset.price
                                )
                            )
                        }
                holder.binding.textViewAssetChange.text = current.assetChangeText
                holder.binding.textViewAssetValue.text = SpannableStringBuilder()
                    .color(Color.BLACK) { append(current.assetText) }
                holder.binding.textViewAssetDate.text = ""
                holder.binding.textViewAssetAccount.text = current.asset.account
                holder.binding.textViewAssetNote.text = ""

                // no delete icon for empty list, headline + summaryline = 2
                if (current.deleteAll && assetList.size > 1) {
                    holder.binding.textViewAssetDelete.visibility = View.VISIBLE
                    // handler for delete all
                    holder.bindDelete(current.asset.symbol, null, clickListenerDeleteLambda)
                } else {
                    holder.binding.textViewAssetDelete.visibility = View.GONE
                }

                holder.binding.assetSummaryView.visibility = View.VISIBLE

                holder.binding.assetSummaryTextView.text = current.capitalGainLossText

                holder.binding.textViewAssetLayout.setBackgroundColor(Color.YELLOW)
                val background = TypedValue()
                holder.binding.textViewAssetItemsLayout.setBackgroundResource(background.resourceId)
            }

        }
    }

    override fun getItemViewType(position: Int): Int {
        val element: AssetListData = assetList[position]
        return element.viewType
    }

    internal fun updateAssets(assetData: StockAssetsLiveData) {

        if (assetData.assets != null) {
            val assets = assetData.assets!!.assets

            // Headline
            assetList = mutableListOf(
                AssetListData(
                    viewType = asset_headline_type,
                    asset = Asset(
                        symbol = "",
                        quantity = 0.0,
                        price = 0.0
                    )
                )
            )

            // Sort assets in the list by date.
            val sortedList = assets.sortedBy { asset ->
                asset.date
            }

            val (totalQuantity, totalPrice, totalFee) = getAssets(sortedList, obsoleteAssetType)

            val sortedDataList = sortedList.map {
                AssetListData(
                    viewType = asset_item_type,
                    asset = it,
                    onlineMarketData = assetData.onlineMarketData
                )
            }.toMutableList()

            // Set the transferItem
            tagTransferItemsInAssetList(sortedDataList)

            assetList.addAll(sortedDataList)

            // Summary
            val symbol: String = assets.firstOrNull()?.symbol ?: ""
            val assetChange = if (assetData.onlineMarketData != null) {
                getAssetChange(
                    assets,
                    assetData.onlineMarketData!!.marketPrice,
                    assetData.onlineMarketData!!.postMarketData,
                    Color.DKGRAY,
                    context
                ).displayColorStr
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

            val capitalGainLossText = if (assets.isNotEmpty()) {
                val (capitalGain, capitalLoss, gainLossMap) = getAssetsCapitalGain(assets)
                SpannableStringBuilder()
                    .append(context.getString(R.string.asset_summary_text))
                    .append("\n${context.getString(R.string.summary_capital_gain)} ")
                    .append(getCapitalGainLossText(context, capitalGain, capitalLoss))
            } else {
                SpannableStringBuilder().append(context.getString(R.string.asset_summary_text))
            }

            assetList.add(
                AssetListData(
                    viewType = asset_summary_type,
                    deleteAll = true, // only the main summary gets a delete icon
                    asset = Asset(
                        id = null,
                        symbol = symbol,
                        quantity = totalQuantity,
                        price = totalPrice,
                        fee = totalFee,
                    ),
                    assetChangeText = assetChange,
                    assetText = asset,
                    capitalGainLossText = capitalGainLossText
                )
            )

            // Add Summary for each Account.
            val map: java.util.HashSet<String> = hashSetOf()

            sortedList.forEach { account ->
                map.add(account.account)
            }

            val assetsAccounts =
                map.map { account ->
                    account
                }

            val assetsListCopy1 = updateTransferAssets(sortedList.map { it.copy() })

            if (assetsAccounts.size > 1) {
                assetsAccounts.sorted().forEach { account ->

                    // Filter for stockitems matching the account.
                    val assetsListCopy = assetsListCopy1.filter { asset ->
                        asset.account == account
                    }

                    val (totalQuantity2, totalPrice2, totalFee2) = getAssets(
                        assetsListCopy,
                        obsoleteAssetType
                    )

                    val assetChangeAccount = if (assetData.onlineMarketData != null) {
                        getAssetChange(
                            assetsListCopy,
                            assetData.onlineMarketData!!.marketPrice,
                            assetData.onlineMarketData!!.postMarketData,
                            Color.DKGRAY,
                            context
                        ).displayColorStr
                    } else {
                        SpannableStringBuilder()
                    }

                    val assetAccount = if (assetData.onlineMarketData != null) {
                        SpannableStringBuilder()
                            .bold {
                                append(
                                    DecimalFormat(DecimalFormat2Digits).format(
                                        totalQuantity2 * assetData.onlineMarketData!!.marketPrice
                                    )
                                )
                            }
                    } else {
                        SpannableStringBuilder()
                    }

                    val accountStr = account.ifEmpty {
                        context.getString(R.string.standard_account)
                    }

                    val capitalGainLossTextAccount = if (assetsListCopy.isNotEmpty()) {
                        val (capitalGain, capitalLoss, gainLossMap) = getAssetsCapitalGain(
                            assetsListCopy
                        )
                        SpannableStringBuilder()
                            .append(
                                context.getString(
                                    R.string.asset_summary_account_text,
                                    accountStr
                                )
                            )
                            .append("\n${context.getString(R.string.summary_capital_gain)} ")
                            .append(getCapitalGainLossText(context, capitalGain, capitalLoss))
                    } else {
                        SpannableStringBuilder().append(
                            context.getString(
                                R.string.asset_summary_account_text,
                                accountStr
                            )
                        )
                    }

                    assetList.add(
                        AssetListData(
                            viewType = asset_summary_type,
                            asset = Asset(
                                id = null,
                                symbol = symbol,
                                quantity = totalQuantity2,
                                price = totalPrice2,
                                account = account,
                                fee = totalFee2,
                            ),
                            assetChangeText = assetChangeAccount,
                            assetText = assetAccount,
                            capitalGainLossText = capitalGainLossTextAccount
                        )
                    )
                }
            }

        } else {
            assetList = mutableListOf()
        }

        notifyDataSetChanged()
    }

    override fun getItemCount() = assetList.size
}
