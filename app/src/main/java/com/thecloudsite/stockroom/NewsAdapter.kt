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

package com.thecloudsite.stockroom

import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.FULL
import java.time.format.FormatStyle.MEDIUM

const val news_type_yahoo: Int = 0
const val news_type_google: Int = 1

val excludeUrlList = arrayListOf(
    "newsdaemon.com",
    "newsheater.com",
    "investorsobserver.com",
    "stocksregister.com",
    "marketingsentinel.com",
    "dbtnews.com",
    "oracledispatch.com",
    "friscoherald.com",
    "investchronicle.com",
    "scientect.com",
    "mzpnews.com",
    "cometreports.com",
    "onenewspage.com",
    "pinevillevoice.com",
    "reportswatch.com",
    "newsbrok.com",
    "startupng.com.ng",
    "primefeed.in",
    "thedailychronicle.in",
    "galusaustralis.com",
    "bulletinline.com",
    "investmillion.com",
    "weeklywall.com",
    "clarkscarlet.com",
    "algosonline.com",
    "baytownsun.com",
    "openpr.com",
    "gnghockey.com",
    "redandblackonline.com",
    "njmmanews.com",
    "prnewswire.co.uk",
    "themarketchronicles.com",
    "kws24.com"
)

data class NewsData(
  val title: String,
  val text: String,
  val date: Long,
  val link: String,
  val type: Int
)

class NewsAdapter(
  private val context: Context
) : RecyclerView.Adapter<NewsAdapter.BaseViewHolder<*>>() {

  abstract class BaseViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: T)
  }

  private var newsDataList = listOf<NewsData>()

  // https://medium.com/@ivancse.58/android-and-kotlin-recyclerview-with-multiple-view-types-65285a254393
  class YahooNewsViewHolder(itemView: View) : BaseViewHolder<NewsData>(itemView) {
    override fun bind(item: NewsData) {
    }

    val yahooNewsItemTitle: TextView = itemView.findViewById(R.id.yahooNewsItemTitle)
    val yahooNewsItemDate: TextView = itemView.findViewById(R.id.yahooNewsItemDate)
    val yahooNewsItemLink: TextView = itemView.findViewById(R.id.yahooNewsItemLink)
    val yahooNewsItemText: TextView = itemView.findViewById(R.id.yahooNewsItemText)
  }

  class GoogleNewsViewHolder(itemView: View) : BaseViewHolder<NewsData>(itemView) {
    override fun bind(item: NewsData) {
    }

    val googleNewsItemTitle: TextView = itemView.findViewById(R.id.googleNewsItemTitle)
    val googleNewsItemDate: TextView = itemView.findViewById(R.id.googleNewsItemDate)
    val googleNewsItemPreviewText: TextView = itemView.findViewById(R.id.googleNewsItemPreviewText)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder<*> {
    return when (viewType) {
      news_type_yahoo -> {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.yahoonewsview_item, parent, false)
        YahooNewsViewHolder(view)
      }
      news_type_google -> {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.googlenewsview_item, parent, false)
        GoogleNewsViewHolder(view)
      }
      else -> throw IllegalArgumentException("Invalid view type")
    }
  }

  private fun getTimeDateStr(date: Long): String {
    val localDateTime = LocalDateTime.ofEpochSecond(date, 0, ZoneOffset.UTC)
    val dateStr = localDateTime.format(DateTimeFormatter.ofLocalizedDate(FULL))
    val timeStr = localDateTime.format(DateTimeFormatter.ofLocalizedTime(MEDIUM))

    return context.getString(R.string.news_date_time, dateStr, timeStr)
  }

  //-----------onCreateViewHolder: bind view with data model---------
  override fun onBindViewHolder(
    holder: BaseViewHolder<*>,
    position: Int
  ) {
    val element: NewsData = newsDataList[position]
    when (holder) {

      is YahooNewsViewHolder -> {
        holder.bind(element)

        val current: NewsData = newsDataList[position]

        holder.yahooNewsItemTitle.text =
          HtmlCompat.fromHtml(current.title, HtmlCompat.FROM_HTML_MODE_LEGACY)
        holder.yahooNewsItemDate.text = getTimeDateStr(current.date)

        holder.yahooNewsItemLink.text = HtmlCompat.fromHtml(
            "<a href=\"${current.link}\" target=\"_blank\">${current.link}</a>",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        holder.yahooNewsItemLink.setLinkTextColor(
            context.getColor(R.color.material_on_background_emphasis_medium)
        )
        holder.yahooNewsItemText.text =
          HtmlCompat.fromHtml(current.text, HtmlCompat.FROM_HTML_MODE_LEGACY)
        // Make links clickable.
        holder.yahooNewsItemLink.movementMethod = LinkMovementMethod.getInstance()
      }

      is GoogleNewsViewHolder -> {
        holder.bind(element)

        val current: NewsData = newsDataList[position]

        holder.googleNewsItemTitle.text =
          HtmlCompat.fromHtml(current.title, HtmlCompat.FROM_HTML_MODE_LEGACY)
        holder.googleNewsItemDate.text = getTimeDateStr(current.date)

        holder.googleNewsItemPreviewText.text =
          HtmlCompat.fromHtml(current.text, HtmlCompat.FROM_HTML_MODE_LEGACY)
        // Make links clickable.
        holder.googleNewsItemPreviewText.movementMethod = LinkMovementMethod.getInstance()
      }
      else -> throw IllegalArgumentException()
    }
  }

  override fun getItemViewType(position: Int): Int {
    val element: NewsData = newsDataList[position]
    return element.type
  }

  fun updateData(dataList: List<NewsData>) {
    val newsMap: MutableMap<Long, NewsData> = mutableMapOf()

    // Copy existing news to the date map.
    this.newsDataList.forEach { data ->
      newsMap[data.date] = data
    }

    // Filter news urls and add to the date map.
    dataList.filterNot { data ->
      excludeUrlList.any { data.link.contains(it) }
    }
        .forEach { data ->
          // Do not overwrite existing news for that time stamp.
          if (newsMap[data.date]?.date != data.date) {
            newsMap[data.date] = data
          }
        }

    // Map back to news list.
    this.newsDataList = newsMap.map { mapdata ->
      mapdata.value
    }
        .sortedByDescending { newsdata ->
          newsdata.date
        }

    notifyDataSetChanged()
  }

  override fun getItemCount() = newsDataList.size
}
