package com.thecloudsite.stockroom

class YahooNewsRepository : NewsRepository({ YahooNewsApiFactory.newsApi }, news_type_yahoo)
