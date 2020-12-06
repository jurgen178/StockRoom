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

package com.thecloudsite.stockroom.news

import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView
import com.thecloudsite.stockroom.R.color
import com.thecloudsite.stockroom.R.id
import com.thecloudsite.stockroom.R.layout
import com.thecloudsite.stockroom.R.string
import com.thecloudsite.stockroom.news.NewsAdapter.BaseViewHolder
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.FULL
import java.time.format.FormatStyle.MEDIUM

const val news_type_headline: Int = 0
const val news_type_yahoo: Int = 1
const val news_type_google: Int = 2
const val news_type_nasdaq: Int = 3

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
    "kws24.com",
    "crainsnewyork.com",
    "digitalmarketnews.com",
    "rejerusalem.com",
    "reporter.am",
    "kyt24.com",
    "prnewsleader.com",
    "thinkcuriouser.com",
    "theunionjournal.com",
    "icotodaymagazine.com",
    "oaoa.com",
    "streetinsider.com",
    "cheshire.media",
    "birmingham-alive.com",
    "mccourier.com",
    "bangordailynews.com",
    "hcnn.ht",
    "canaanmountainherald.com",
    "murphyshockeylaw.net",
    "law360.com",
    "theblend.ie",
    "khabarsouthasia.com",
    "express-journal.com",
    "factorymaintenance.com.au",
    "pulse2.com"
)

data class NewsData(
  val title: String,
  val text: String,
  val date: Long,
  val link: String,
  val type: Int
)

class NewsAdapter(
  private val context: Context,
  headline: String = "",
) : RecyclerView.Adapter<BaseViewHolder<*>>() {

  abstract class BaseViewHolder<T>(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(item: T)
  }

  private var newsDataList = if (headline.isNotEmpty()) {
    // Add headline to news list.
    listOf(
        // News is sorted by time, date=Long.MAX_VALUE is top entry.
        NewsData(
            headline, "", Long.MAX_VALUE, "", news_type_headline
        )
    )
  } else {
    listOf()
  }

  // https://medium.com/@ivancse.58/android-and-kotlin-recyclerview-with-multiple-view-types-65285a254393
  class NewsHeadlineViewHolder(itemView: View) : BaseViewHolder<NewsData>(itemView) {
    override fun bind(item: NewsData) {
    }

    val newsHeadline: TextView = itemView.findViewById(id.newsHeadline)
  }

  class YahooNewsViewHolder(itemView: View) : BaseViewHolder<NewsData>(itemView) {
    override fun bind(item: NewsData) {
    }

    val yahooNewsItemTitle: TextView = itemView.findViewById(id.yahooNewsItemTitle)
    val yahooNewsItemDate: TextView = itemView.findViewById(id.yahooNewsItemDate)
    val yahooNewsItemLink: TextView = itemView.findViewById(id.yahooNewsItemLink)
    val yahooNewsItemText: TextView = itemView.findViewById(id.yahooNewsItemText)
  }

  class GoogleNewsViewHolder(itemView: View) : BaseViewHolder<NewsData>(itemView) {
    override fun bind(item: NewsData) {
    }

    val googleNewsItemTitle: TextView = itemView.findViewById(id.googleNewsItemTitle)
    val googleNewsItemDate: TextView = itemView.findViewById(id.googleNewsItemDate)
    val googleNewsItemPreviewText: TextView = itemView.findViewById(id.googleNewsItemPreviewText)
  }

  class NasdaqNewsViewHolder(itemView: View) : BaseViewHolder<NewsData>(itemView) {
    override fun bind(item: NewsData) {
    }

    val nasdaqNewsItemTitle: TextView = itemView.findViewById(id.nasdaqNewsItemTitle)
    val nasdaqNewsItemDate: TextView = itemView.findViewById(id.nasdaqNewsItemDate)
    val nasdaqNewsItemLink: TextView = itemView.findViewById(id.nasdaqNewsItemLink)
    val nasdaqNewsItemText: TextView = itemView.findViewById(id.nasdaqNewsItemText)
  }

  override fun onCreateViewHolder(
    parent: ViewGroup,
    viewType: Int
  ): BaseViewHolder<*> {

    return when (viewType) {
      news_type_headline -> {
        val view = LayoutInflater.from(context)
            .inflate(layout.newsheadline_item, parent, false)
        NewsHeadlineViewHolder(view)
      }
      news_type_yahoo -> {
        val view = LayoutInflater.from(context)
            .inflate(layout.yahoonewsview_item, parent, false)
        YahooNewsViewHolder(view)
      }
      news_type_google -> {
        val view = LayoutInflater.from(context)
            .inflate(layout.googlenewsview_item, parent, false)
        GoogleNewsViewHolder(view)
      }
      news_type_nasdaq -> {
        val view = LayoutInflater.from(context)
            .inflate(layout.nasdaqnewsview_item, parent, false)
        NasdaqNewsViewHolder(view)
      }
      else -> throw IllegalArgumentException("Invalid view type")
    }
  }

  private fun getTimeDateStr(date: Long): String {
    val localDateTime = LocalDateTime.ofEpochSecond(date, 0, ZoneOffset.UTC)
    val dateStr = localDateTime.format(DateTimeFormatter.ofLocalizedDate(FULL))
    val timeStr = localDateTime.format(DateTimeFormatter.ofLocalizedTime(MEDIUM))

    return context.getString(string.news_date_time, dateStr, timeStr)
  }

  //-----------onCreateViewHolder: bind view with data model---------
  override fun onBindViewHolder(
    holder: BaseViewHolder<*>,
    position: Int
  ) {

    val current: NewsData = newsDataList[position]

    when (holder) {

      is NewsHeadlineViewHolder -> {
        holder.bind(current)

        holder.newsHeadline.text = current.title
      }

      is YahooNewsViewHolder -> {
        holder.bind(current)

        holder.yahooNewsItemTitle.text =
          HtmlCompat.fromHtml(current.title, HtmlCompat.FROM_HTML_MODE_LEGACY)
        holder.yahooNewsItemDate.text = getTimeDateStr(current.date)

        holder.yahooNewsItemLink.text = HtmlCompat.fromHtml(
            "<a href=\"${current.link}\" target=\"_blank\">${current.link}</a>",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        holder.yahooNewsItemLink.setLinkTextColor(
            context.getColor(color.material_on_background_emphasis_medium)
        )
        holder.yahooNewsItemText.text =
          HtmlCompat.fromHtml(current.text, HtmlCompat.FROM_HTML_MODE_LEGACY)
        // Make links clickable.
        holder.yahooNewsItemLink.movementMethod = LinkMovementMethod.getInstance()
      }

      is GoogleNewsViewHolder -> {
        holder.bind(current)

        holder.googleNewsItemTitle.text =
          HtmlCompat.fromHtml(current.title, HtmlCompat.FROM_HTML_MODE_LEGACY)
        holder.googleNewsItemDate.text = getTimeDateStr(current.date)

        holder.googleNewsItemPreviewText.text =
          HtmlCompat.fromHtml(current.text, HtmlCompat.FROM_HTML_MODE_LEGACY)
        // Make links clickable.
        holder.googleNewsItemPreviewText.movementMethod = LinkMovementMethod.getInstance()
      }

      is NasdaqNewsViewHolder -> {
        holder.bind(current)

        holder.nasdaqNewsItemTitle.text =
          HtmlCompat.fromHtml(current.title, HtmlCompat.FROM_HTML_MODE_LEGACY)
        holder.nasdaqNewsItemDate.text = getTimeDateStr(current.date)

        holder.nasdaqNewsItemLink.text = HtmlCompat.fromHtml(
            "<a href=\"${current.link}\" target=\"_blank\">${current.link}</a>",
            HtmlCompat.FROM_HTML_MODE_LEGACY
        )
        holder.nasdaqNewsItemLink.setLinkTextColor(
            context.getColor(color.material_on_background_emphasis_medium)
        )
        holder.nasdaqNewsItemText.text =
          HtmlCompat.fromHtml(current.text, HtmlCompat.FROM_HTML_MODE_LEGACY)
        // Make links clickable.
        holder.nasdaqNewsItemLink.movementMethod = LinkMovementMethod.getInstance()
      }

      else -> {
        throw IllegalArgumentException()
      }
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
