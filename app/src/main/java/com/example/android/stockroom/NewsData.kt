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

// https://finance.yahoo.com/rss/topstories
/*
<rss xmlns:media="http://search.yahoo.com/mrss/" version="2.0">
<channel>
<title>Yahoo Finance</title>
<link>https://finance.yahoo.com/</link>
<description>At Yahoo Finance, you get free stock quotes, up-to-date news, portfolio management resources, international market data, social interaction and mortgage rates that help you manage your financial life.</description>
<language>en-US</language>
<copyright>Copyright (c) 2020 Yahoo! Inc. All rights reserved</copyright>
<pubDate>Sun, 02 Aug 2020 00:18:31 -0400</pubDate>
<ttl>5</ttl>
<image>
<title>Yahoo Finance</title>
<link>https://finance.yahoo.com/</link>
<url>https://s.yimg.com/zz/nn/lib/metro/g/my/yahoo_en-US_f_p_190x45_2x.png</url>
</image>
<item>
<title>Exclusive: Eastman Kodak top executive got Trump deal windfall on an &#39;understanding&#39;</title>
<description><p><a href="https://finance.yahoo.com/news/exclusive-eastman-kodak-top-executive-213014813.html"><img src="http://l1.yimg.com/uu/api/res/1.2/EjeRNop24tm_8LcfbyaezA--/YXBwaWQ9eXRhY2h5b247aD04Njt3PTEzMDs-/https://media.zenfs.com/en-US/reuters-finance.com/4d0e647b26534d366f594ffb83bc8df2" width="130" height="86" alt="Exclusive: Eastman Kodak top executive got Trump deal windfall on an &#39;understanding&#39;" align="left" title="Exclusive: Eastman Kodak top executive got Trump deal windfall on an &#39;understanding&#39;" border="0" ></a>Eastman Kodak Co <KODK.N> on Monday granted its executive chairman options for 1.75 million shares as the result of what a person familiar with the arrangement described as an "understanding" with its board that had previously neither been listed in his employment contract nor made public. One day later, the administration of President Donald Trump announced a $765 million financing deal with Eastman Kodak, and in the days that followed the stock soared, making those additional options now held by executive chairman Jim Continenza worth tens of millions. The decision to grant Continenza options was never formalized or made into a binding agreement, which is why it was not disclosed previously, according to the person familiar with the arrangement.<p><br clear="all"></description>
<link>https://finance.yahoo.com/news/exclusive-eastman-kodak-top-executive-213014813.html</link>
<pubDate>Sat, 01 Aug 2020 17:30:14 -0400</pubDate>
<source url="http://www.reuters.com/">Reuters</source>
<guid isPermaLink="false">exclusive-eastman-kodak-top-executive-213014813.html</guid>
<media:content height="86" url="http://l1.yimg.com/uu/api/res/1.2/EjeRNop24tm_8LcfbyaezA--/YXBwaWQ9eXRhY2h5b247aD04Njt3PTEzMDs-/https://media.zenfs.com/en-US/reuters-finance.com/4d0e647b26534d366f594ffb83bc8df2" width="130"/>
<media:text type="html"><p><a href="https://finance.yahoo.com/news/exclusive-eastman-kodak-top-executive-213014813.html"><img src="http://l1.yimg.com/uu/api/res/1.2/EjeRNop24tm_8LcfbyaezA--/YXBwaWQ9eXRhY2h5b247aD04Njt3PTEzMDs-/https://media.zenfs.com/en-US/reuters-finance.com/4d0e647b26534d366f594ffb83bc8df2" width="130" height="86" alt="Exclusive: Eastman Kodak top executive got Trump deal windfall on an &#39;understanding&#39;" align="left" title="Exclusive: Eastman Kodak top executive got Trump deal windfall on an &#39;understanding&#39;" border="0" ></a>Eastman Kodak Co <KODK.N> on Monday granted its executive chairman options for 1.75 million shares as the result of what a person familiar with the arrangement described as an "understanding" with its board that had previously neither been listed in his employment contract nor made public. One day later, the administration of President Donald Trump announced a $765 million financing deal with Eastman Kodak, and in the days that followed the stock soared, making those additional options now held by executive chairman Jim Continenza worth tens of millions. The decision to grant Continenza options was never formalized or made into a binding agreement, which is why it was not disclosed previously, according to the person familiar with the arrangement.<p><br clear="all"></media:text>
<media:credit role="publishing company"/>
</item>
<item> */


// https://feeds.finance.yahoo.com/rss/2.0/headline?s=msft

/*
<rss version="2.0">
<channel>
<copyright>Copyright (c) 2020 Yahoo! Inc. All rights reserved.</copyright>
<description>Latest Financial News for msft</description>
<image>
<height>45</height>
<link>http://finance.yahoo.com/q/h?s=msft</link>
<title>Yahoo! Finance: msft News</title>
<url>http://l.yimg.com/a/i/brand/purplelogo/uh/us/fin.gif</url>
<width>144</width>
</image>
<item>
<description>(Bloomberg) -- The Trump administration is poised to announce a decision on Chinese-owned TikTok, potentially preventing the popular music-video app from operating in the U.S. and escalating the administration’s clash with China, a White House official said.President Donald Trump’s determination may come within hours, said the official, who wasn’t authorized to speak publicly.It follows Trump’s comment, made Friday night, that “As far as TikTok is concerned, we’re banning them from the United States.” Trump told reporters that it would happen “Soon, immediately. I mean essentially immediately.”Trump said he had the authority to ban the app, owned by ByteDance Ltd., one of China’s biggest tech companies, a move he could make by executive order or under the International Emergency Economic Powers Act.“I will sign the document tomorrow,” he said just before Air Force One landed in Washington from a visit to Florida.Trump’s move could upend a potential bid from Microsoft Corp., which was exploring an acquisition, according to people familiar with the matter. Microsoft has broken off talks for now in the face of Trump’s position, the Wall Street Journal reported on Saturday.TikTok, Hong Kong and More U.S.-China Flashpoints: QuickTakeMicrosoft declined to comment.ByteDance is prepared to sell 100% of TikTok’s U.S. operations as a way to head off a ban by Trump, two people with knowledge of the situation said earlier.TikTok has hired almost 1,000 people in the U.S. this year and will be employing another 10,000 into “great paying jobs” in the U.S., a company spokeswoman said in a statement. The business’s $1 billion creator fund also supports people in the country who are building livelihoods from the platform, she added.“TikTok U.S. user data is stored in the U.S., with strict controls on employee access,” she said. “TikTok’s biggest investors come from the U.S. We are committed to protecting our users’ privacy and safety.”A ban of TikTok would be the latest move by the administration to curb China’s power in global technology. TikTok has become one of the world’s most popular apps. It’s been downloaded more than 2 billion times globally and more than 165 million times in the U.S.The administration’s move came as the Committee on Foreign Investment in the U.S., which reviews acquisitions of American businesses by overseas investors, was investigating the company. ByteDance in 2017 bought Musical.ly and merged it with TikTok. Musicaly.ly was founded in Shanghai but had a substantial following in the U.S. at the time.Trump said in early July that he was considering a ban as a way to retaliate against China over its handling of the coronavirus. But the app has also sparked concerns about the massive amount of data collected about Americans.Earlier: TikTok’s Huge Data Harvesting Prompts U.S. Security ConcernsTikTok says American user data is stored in servers in the U.S. and Singapore, not China. But TikTok’s terms of service stipulate that the company may share information with its parent, subsidiary or other affiliates. Previous versions of its privacy policy warned users it could exchange information with its Chinese businesses, law enforcement agencies and public authorities, if legally required to do so.The administration had been considering requiring the sale of TikTok’s U.S. operations. The administration was prepared to announce a sale order on Friday, according to three people familiar with the matter. Another person said later that the decision was put on hold, pending further review by Trump.White House adviser Peter Navarro said earlier in July that a sale of TikTok to an American buyer wouldn’t address the U.S. concerns.“If TikTok separates as an American company, that doesn’t help us,” Navarro said. “Because it’s going to be worse -- we’re going to have to give China billions of dollars for the privilege of having TikTok operate on U.S. soil.”Viral MemesA Microsoft takeover would have given the software maker a popular consumer app that has won over young people with a steady diet of dance videos, lip-syncing clips and viral memes. The company has dabbled in social-media investments in the past, but hasn’t developed a popular service of its own in the lucrative sector.TikTok has repeatedly rejected accusations that it feeds user data to China or is beholden to Beijing, even though ByteDance is based there. TikTok now has a U.S.-based chief executive officer and ByteDance has considered making other organizational changes to satisfy U.S. authorities.“Hundreds of millions of people come to TikTok for entertainment and connection, including our community of creators and artists who are building livelihoods from the platform,” the TikTok spokeswoman said. “We’re motivated by their passion and creativity, and committed to protecting their privacy and safety as we continue working to bring joy to families and meaningful careers to those who create on our platform.”For more articles like this, please visit us at bloomberg.comSubscribe now to stay ahead with the most trusted business news source.©2020 Bloomberg L.P.</description>
<guid isPermaLink="false">a9ad41d2-24a2-380d-a9fa-3ce7b7047603</guid>
<link>https://finance.yahoo.com/news/trump-says-ban-china-owned-023554705.html?.tsrc=rss</link>
<pubDate>Sat, 01 Aug 2020 20:57:44 +0000</pubDate>
<title>White House Poised to Announce Move on TikTok’s U.S. Operations</title>
</item>
<item>
<description>This chip company is experiencing tremendous momentum, but the high valuation should give investors pause.</description>
<guid isPermaLink="false">719429c8-0b4f-3880-b287-c6d4b2ae0241</guid>
<link>https://finance.yahoo.com/m/719429c8-0b4f-3880-b287-c6d4b2ae0241/is-nvidia-a-buy%3F.html?.tsrc=rss</link>
<pubDate>Sat, 01 Aug 2020 14:48:00 +0000</pubDate>
<title>Is NVIDIA a Buy?</title>
</item>
<item> */

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
  @get:Element(name = "link", required = false)
  @set:Element(name = "link", required = false)
  var link: String = ""

  @get:Element(name = "title", required = false)
  @set:Element(name = "title", required = false)
  var title: String = ""

  @get:Element(name = "description", required = false)
  @set:Element(name = "description", required = false)
  var description: String = ""

  @get:Element(name = "pubDate", required = false)
  @set:Element(name = "pubDate", required = false)
  var pubDate: String = ""
}

interface NewsApi {
  fun getNewsDataAsync(newsQuery: String): Deferred<Response<NewsResponse>>
}

//A retrofit Network Interface for the Api
interface YahooNewsApi : NewsApi {
  @GET("rss/2.0/headline")
  override fun getNewsDataAsync(
    @Query(
        value = "s"
    ) newsQuery: String
  ): Deferred<Response<NewsResponse>>
}

interface GoogleNewsApi : NewsApi {
  @GET("rss/search/")
  override fun getNewsDataAsync(
    @Query(
        value = "q"
    ) newsQuery: String
  ): Deferred<Response<NewsResponse>>
}
