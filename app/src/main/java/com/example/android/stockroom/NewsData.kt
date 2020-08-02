package com.example.android.stockroom

import kotlinx.coroutines.Deferred
import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Path
import org.simpleframework.xml.Root
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// https://news.google.com/news/rss/headlines/section/topic/BUSINESS
// https://news.google.com/rss/search?q=msft&hl=en-US&gl=US&ceid=US:en
// https://news.google.com/rss/search?q=msft&hl=de&gl=DE&ceid=DE:de

/*
<rss xmlns:media="http://search.yahoo.com/mrss/" version="2.0">
<channel>
<generator>NFE/5.0</generator>
<title>"msft" - Google News</title>
<link>https://news.google.com/search?q=msft&hl=de&gl=DE&ceid=DE:de</link>
<language>de</language>
<webMaster>news-webmaster@google.com</webMaster>
<copyright>2020 Google Inc.</copyright>
<lastBuildDate>Sat, 01 Aug 2020 21:32:43 GMT</lastBuildDate>
<description>Google News</description>
<item>
<title>Microsoft (NASDAQ: MSFT) bei BidaskClub zu Strong-Buy aufgestockt » IMS - Internationales Magazin für Sicherheit (IMS)</title>
<link>http://www.ims-magazin.de/microsoft-nasdaq-msft-bei-bidaskclub-zu-strong-buy-aufgestockt-48972/</link>
<guid isPermaLink="false">CBMiX2h0dHA6Ly93d3cuaW1zLW1hZ2F6aW4uZGUvbWljcm9zb2Z0LW5hc2RhcS1tc2Z0LWJlaS1iaWRhc2tjbHViLXp1LXN0cm9uZy1idXktYXVmZ2VzdG9ja3QtNDg5NzIv0gEA</guid>
<pubDate>Wed, 15 Jul 2020 07:00:00 GMT</pubDate>
<description><a href="http://www.ims-magazin.de/microsoft-nasdaq-msft-bei-bidaskclub-zu-strong-buy-aufgestockt-48972/" target="_blank">Microsoft (NASDAQ: MSFT) bei BidaskClub zu Strong-Buy aufgestockt » IMS</a>&nbsp;&nbsp;<font color="#6f6f6f">Internationales Magazin für Sicherheit (IMS)</font></description>
<source url="http://www.ims-magazin.de">Internationales Magazin für Sicherheit (IMS)</source>
</item>
<item>
 */

@Root(name = "rss", strict = false)
class NewsResponse {
  @get:ElementList(name = "item", inline = true)
  @get:Path("channel")
  @set:ElementList(name = "item", inline = true)
  @set:Path("channel")
  var newsItems: List<NewsItem>? = null
}

@Root(name = "item", strict = false)
class NewsItem {
  @get:Element(name = "link")
  @set:Element(name = "link")
  var link: String = ""

  @get:Element(name = "title")
  @set:Element(name = "title")
  var title: String = ""

  @get:Element(name = "description")
  @set:Element(name = "description")
  var description: String = ""

  @get:Element(name = "pubDate")
  @set:Element(name = "pubDate")
  var pubDate: String = ""
}

// https://finance.yahoo.com/rss/topstories

//A retrofit Network Interface for the Api
interface NewsApi {
  @GET("rss/search/")
  fun getNewsDataAsync(
    @Query(
        value = "q"
    ) newsQuery: String
  ): Deferred<Response<NewsResponse>>
}
