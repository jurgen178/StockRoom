package com.thecloudsite.stockroom

class GoogleNewsRepository : NewsRepository(GoogleNewsApiFactory.newsApi, news_type_google)
