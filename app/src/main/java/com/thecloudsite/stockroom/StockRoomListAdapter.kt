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
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.net.toUri
import androidx.core.text.bold
import androidx.core.text.color
import androidx.core.text.italic
import androidx.core.text.scale
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.thecloudsite.stockroom.R.color
import com.thecloudsite.stockroom.databinding.StockroomListItemBinding
import com.thecloudsite.stockroom.utils.*
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.SHORT

// https://codelabs.developers.google.com/codelabs/kotlin-android-training-diffutil-databinding/#4

fun setBackgroundColor(
    view: View,
    color: Int
) {
    // Keep the corner radii and only change the background color.
    val gradientDrawable = view.background as GradientDrawable
    gradientDrawable.setColor(color)
    view.background = gradientDrawable
}

class StockRoomListAdapter internal constructor(
    val context: Context,
    private val clickListenerGroupLambda: (StockItem, View) -> Unit,
    private val clickListenerMarkerLambda: (StockItem, View) -> Unit,
    private val clickListenerSymbolLambda: (StockItem) -> Unit
) : ListAdapter<StockItem, StockRoomListAdapter.StockRoomViewHolder>(StockRoomDiffCallback()) {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var defaultTextColor: Int? = null

    class StockRoomViewHolder(
        val binding: StockroomListItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bindGroupOnClickListener(
            stockItem: StockItem,
            clickListenerLambda: (StockItem, View) -> Unit
        ) {
            binding.itemviewGroup.setOnClickListener { clickListenerLambda(stockItem, itemView) }
        }

        fun bindMarkerOnClickListener(
            stockItem: StockItem,
            clickListenerLambda: (StockItem, View) -> Unit
        ) {
            binding.itemviewGroupMarker.setOnClickListener {
                clickListenerLambda(
                    stockItem,
                    itemView
                )
            }
        }

        fun bindSummaryOnClickListener(
            stockItem: StockItem,
            clickListenerLambda: (StockItem) -> Unit
        ) {
            binding.itemSummary.setOnClickListener { clickListenerLambda(stockItem) }
            binding.itemRedGreen.setOnClickListener { clickListenerLambda(stockItem) }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StockRoomViewHolder {

        val binding = StockroomListItemBinding.inflate(inflater, parent, false)
        return StockRoomViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: StockRoomViewHolder,
        position: Int
    ) {
        val current = getItem(position)

        if (current != null) {
            if (defaultTextColor == null) {
                defaultTextColor = holder.binding.textViewAssets.currentTextColor
            }

            holder.bindGroupOnClickListener(current, clickListenerGroupLambda)
            holder.bindMarkerOnClickListener(current, clickListenerMarkerLambda)
            holder.bindSummaryOnClickListener(current, clickListenerSymbolLambda)

            holder.binding.itemSummary.setBackgroundColor(context.getColor(R.color.backgroundListColor))

            val displayName =
                if (current.stockDBdata.name.isEmpty()) current.stockDBdata.symbol else current.stockDBdata.name
            holder.binding.textViewSymbol.text = displayName

            holder.binding.imageViewSymbol.visibility = View.GONE
            // val imgUrl = "https://s.yimg.com/uc/fin/img/reports-thumbnails/1.png"
            val imgUrl = current.onlineMarketData.coinImageUrl
            if (imgUrl.isNotEmpty()) {
                val imgView: ImageView = holder.binding.imageViewSymbol
                val imgUri = imgUrl.toUri()
                // use imgUrl as it is, no need to build upon the https scheme (https://...)
                //.buildUpon()
                //.scheme("https")
                //.build()

                Glide.with(imgView.context)
                    .load(imgUri)
                    .listener(object : RequestListener<Drawable> {
                        override fun onLoadFailed(
                            e: GlideException?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<Drawable?>?,
                            isFirstResource: Boolean
                        ): Boolean {
                            return false
                        }

                        override fun onResourceReady(
                            resource: Drawable?,
                            model: Any?,
                            target: com.bumptech.glide.request.target.Target<Drawable>?,
                            dataSource: DataSource?,
                            isFirstResource: Boolean
                        ): Boolean {
                            holder.binding.imageViewSymbol.visibility = View.VISIBLE
                            return false
                        }
                    })
                    .into(imgView)
            }

            holder.binding.textViewName.text = getName(current.onlineMarketData)

            if (current.onlineMarketData.marketPrice > 0.0) {
                val marketValues = getMarketValues(current.onlineMarketData)

                if (current.onlineMarketData.postMarketData) {
                    holder.binding.textViewMarketPrice.text = SpannableStringBuilder()
                        .italic { append(marketValues.first) }

                    holder.binding.textViewChange.text = SpannableStringBuilder()
                        .italic { append(marketValues.second) }

                    holder.binding.textViewChangePercent.text = SpannableStringBuilder()
                        .italic { append(marketValues.third) }
                } else {
                    holder.binding.textViewMarketPrice.text = marketValues.first
                    holder.binding.textViewChange.text = marketValues.second
                    holder.binding.textViewChangePercent.text = marketValues.third
                }
            } else {
                holder.binding.textViewMarketPrice.text = ""
                holder.binding.textViewChange.text = ""
                holder.binding.textViewChangePercent.text = ""
                holder.binding.textViewAssets.text = ""
            }

            val (quantity, asset, fee) = getAssets(current.assets)
//      val quantity = current.assets.sumOf {
//        it.quantity
//      }

            val assets = SpannableStringBuilder()

//      var asset: Double = 0.0

            if (quantity > 0.0) {
//        asset = current.assets.sumOf {
//          it.quantity * it.price
//        }

                assets.append(
                    "${DecimalFormat(DecimalFormatQuantityDigits).format(quantity)}@${
                        to2To8Digits(asset / quantity)
                    }"
                )

                if (fee > 0.0) {
                    assets.scale(feeScale) {
                        append(
                            "+${
                                DecimalFormat(DecimalFormat2To4Digits).format(
                                    fee
                                )
                            }"
                        )
                    }
                }

                assets.append(
                    "\n${
                        DecimalFormat(
                            DecimalFormat2Digits
                        ).format(asset)
                    } "
                )

                if (current.onlineMarketData.marketPrice > 0.0) {
                    val capital = quantity * current.onlineMarketData.marketPrice
//          capital = current.assets.sumOf {
//            it.quantity * current.onlineMarketData.marketPrice
//          }

                    val assetChange = capital - asset
                    val capitalPercent = assetChange * 100.0 / asset

                    assets.color(
                        getChangeColor(
                            assetChange,
                            current.onlineMarketData.postMarketData,
                            defaultTextColor!!,
                            context
                        )
                    )
                    {
                        assets.append(
                            DecimalFormat("+ $DecimalFormat2Digits;- $DecimalFormat2Digits").format(
                                assetChange
                            )
                        )
                        if (capitalPercent < 10000.0) {
                            assets.append(
                                " (${
                                    DecimalFormat("+$DecimalFormat2Digits;-$DecimalFormat2Digits").format(
                                        capitalPercent
                                    )
                                }%)"
                            )
                        }
                    }

                    assets.append(" = ")
                    assets.bold { append(DecimalFormat(DecimalFormat2Digits).format(capital)) }
                    assets.scale(currencyScale) { append(getCurrency(current.onlineMarketData)) }
                }
            }

//      // set background to asset change
//      holder.itemRedGreen.setBackgroundColor(
//          getChangeColor(capital, asset, context.getColor(R.color.backgroundListColor), context)
//      )
            // set background to market change
            holder.binding.itemRedGreen.setBackgroundColor(
                getChangeColor(
                    current.onlineMarketData.marketChange,
                    current.onlineMarketData.postMarketData,
                    context.getColor(color.backgroundListColor),
                    context
                )
            )

            if (useWhiteOnRedGreen && current.onlineMarketData.marketChange != 0.0) {
                holder.binding.textViewMarketPrice.setTextColor(Color.WHITE)
                holder.binding.textViewChange.setTextColor(Color.WHITE)
                holder.binding.textViewChangePercent.setTextColor(Color.WHITE)
            } else {
                holder.binding.textViewMarketPrice.setTextColor(defaultTextColor!!)
                holder.binding.textViewChange.setTextColor(defaultTextColor!!)
                holder.binding.textViewChangePercent.setTextColor(defaultTextColor!!)
            }

            if (current.onlineMarketData.marketCap > 0L) {
                if (assets.isNotEmpty()) {
                    assets.append("\n")
                }

                assets.append(
                    "${context.getString(R.string.onlinedata_marketCap)}: ${
                        formatInt(
                            current.onlineMarketData.marketCap,
                            context
                        ).text
                    }"
                )
            }

            val dividendStr = getDividendStr(current, context)
            if (dividendStr.isNotEmpty()) {
                if (assets.isNotEmpty()) {
                    assets.append("\n")
                }

                assets.append(
                    dividendStr
                )
            }

            if (current.stockDBdata.alertAbove > 0.0) {
                if (assets.isNotEmpty()) {
                    assets.append("\n")
                }

                assets.append(
                    "${context.getString(R.string.alert_above_in_list)} ${
                        to2To8Digits(current.stockDBdata.alertAbove)
                    }"
                )
            }
            if (current.stockDBdata.alertBelow > 0.0) {
                if (assets.isNotEmpty()) {
                    assets.append("\n")
                }

                assets.append(
                    "${context.getString(R.string.alert_below_in_list)} ${
                        to2To8Digits(current.stockDBdata.alertBelow)
                    }"
                )
            }
            if (current.events.isNotEmpty()) {
                val count = current.events.size
                val eventStr =
                    context.resources.getQuantityString(R.plurals.events_in_list, count, count)

                if (assets.isNotEmpty()) {
                    assets.append("\n")
                }

                assets.append(eventStr)
                current.events.forEach {
                    val localDateTime =
                        ZonedDateTime.ofInstant(
                            Instant.ofEpochSecond(it.datetime),
                            ZoneOffset.systemDefault()
                        )
                    val datetime =
                        localDateTime.format(DateTimeFormatter.ofLocalizedDateTime(SHORT))
                    assets.append(
                        "\n${
                            context.getString(
                                R.string.event_datetime_format, it.title, datetime
                            )
                        }"
                    )
                }
            }
            if (current.stockDBdata.note.isNotEmpty()) {
                if (assets.isNotEmpty()) {
                    assets.append("\n")
                }

                assets.append(
                    "${
                        context.getString(
                            R.string.note_in_list
                        )
                    } ${current.stockDBdata.note}"
                )
            }

            holder.binding.textViewAssets.text = assets

            var color = current.stockDBdata.groupColor
            if (color == 0) {
                color = context.getColor(R.color.backgroundListColor)
            }
            setGroupBackground(
                context,
                current.stockDBdata.marker,
                color,
                holder.binding.itemviewGroup,
                holder.binding.itemviewGroupSep,
                holder.binding.itemviewGroupMarker
            )

            /*
            // Keep the corner radii and only change the background color.
            val gradientDrawable = holder.itemLinearLayoutGroup.background as GradientDrawable
            gradientDrawable.setColor(color)
            holder.itemLinearLayoutGroup.background = gradientDrawable
            */
        }
    }

    internal fun setStockItems(stockItems: List<StockItem>) {
        submitList(stockItems)
        notifyDataSetChanged()
    }
}

// https://codelabs.developers.google.com/codelabs/kotlin-android-training-diffutil-databinding/#3

class StockRoomDiffCallback : DiffUtil.ItemCallback<StockItem>() {
    override fun areItemsTheSame(
        oldItem: StockItem,
        newItem: StockItem
    ): Boolean {
        return oldItem.onlineMarketData.symbol == newItem.onlineMarketData.symbol
    }

    override fun areContentsTheSame(
        oldItem: StockItem,
        newItem: StockItem
    ): Boolean {
        return oldItem == newItem
    }
}
