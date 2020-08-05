package com.android.stockroom

class GoogleNewsRepository : NewsRepository(GoogleNewsApiFactory.newsApi, news_type_google)
