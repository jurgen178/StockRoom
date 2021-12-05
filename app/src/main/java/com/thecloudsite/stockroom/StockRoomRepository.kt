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
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.database.Assets
import com.thecloudsite.stockroom.database.Dividend
import com.thecloudsite.stockroom.database.Dividends
import com.thecloudsite.stockroom.database.Event
import com.thecloudsite.stockroom.database.Events
import com.thecloudsite.stockroom.database.Group
import com.thecloudsite.stockroom.database.StockDBdata
import com.thecloudsite.stockroom.database.StockRoomDao
import com.thecloudsite.stockroom.database.StoreData

/*
 * Abstracted Repository as promoted by the Architecture Guide.
 * https://developer.android.com/topic/libraries/architecture/guide.html
 */
class StockRoomRepository(private val stockRoomDao: StockRoomDao) {

  // Room executes all queries on a separate thread.
  // Observed LiveData will notify the observer when the data has changed.
  val allStockDBdata: LiveData<List<StockDBdata>> = stockRoomDao.getAllStockDBdataLiveData()
  val allAssets: LiveData<List<Assets>> = stockRoomDao.getAllAssetsLiveData()
  val allEvents: LiveData<List<Events>> = stockRoomDao.getAllEventsLiveData()
  val allDividends: LiveData<List<Dividends>> = stockRoomDao.getAllDividendsLiveData()

  val allAssetTable: LiveData<List<Asset>> = stockRoomDao.getAllAssetTableLiveData()
  val allEventTable: LiveData<List<Event>> = stockRoomDao.getAllEventTableLiveData()
  val allDividendTable: LiveData<List<Dividend>> = stockRoomDao.getAllDividendTableLiveData()
  val allStoreTable: LiveData<List<StoreData>> = stockRoomDao.getAllStoreLiveData()
  val allGroupTable: LiveData<List<Group>> = stockRoomDao.getAllGroupTableLiveData()

  // You must call this on a non-UI thread or your app will crash. So we're making this a
  // suspend function so the caller methods know this.
  // Like this, Room ensures that you're not doing any long running operations on the main
  // thread, blocking the UI.
  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  fun getStockDBLiveData(symbol: String): LiveData<StockDBdata> {
    return stockRoomDao.getStockDBLiveData(symbol)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  fun getAssetsLiveData(symbol: String): LiveData<Assets> {
    return stockRoomDao.getAssetsLiveData(symbol)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  fun getEventsLiveData(symbol: String): LiveData<Events> {
    return stockRoomDao.getEventsLiveData(symbol)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  fun getDividendsLiveData(symbol: String): LiveData<Dividends> {
    return stockRoomDao.getDividendsLiveData(symbol)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun setPredefinedGroups(context: Context) {
    stockRoomDao.setPredefinedGroups(context)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun insert(stockDBdata: StockDBdata) {
    stockRoomDao.insert(stockDBdata)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun setPortfolio(
    symbol: String,
    portfolio: String
  ) {
    stockRoomDao.setPortfolio(symbol, portfolio)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun getType(symbol: String): Int {
    return stockRoomDao.getType(symbol)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun setType(
    symbol: String,
    type: Int
  ) {
    stockRoomDao.setType(symbol, type)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun setData(
    symbol: String,
    data: String
  ) {
    stockRoomDao.setData(symbol, data)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updatePortfolio(
    portfolioOld: String,
    portfolioNew: String
  ) {
    stockRoomDao.updatePortfolio(portfolioOld, portfolioNew)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun setName(
    symbol: String,
    name: String
  ) {
    stockRoomDao.setName(symbol, name)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updateAccount(
    accountOld: String,
    accountNew: String
  ) {
    stockRoomDao.updateAccount(accountOld, accountNew)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updateAlertAbove(
    symbol: String,
    alertAbove: Double,
    alertAboveNote: String
  ) {
    stockRoomDao.updateAlertAbove(symbol, alertAbove, alertAboveNote)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updateAlertBelow(
    symbol: String,
    alertBelow: Double,
    alertBelowNote: String
  ) {
    stockRoomDao.updateAlertBelow(symbol, alertBelow, alertBelowNote)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updateNote(
    symbol: String,
    note: String
  ) {
    stockRoomDao.updateNote(symbol, note)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updateDividendNote(
    symbol: String,
    note: String
  ) {
    stockRoomDao.updateDividendNote(symbol, note)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updateAnnualDividendRate(
    symbol: String,
    annualDividendRate: Double
  ) {
    stockRoomDao.updateAnnualDividendRate(symbol, annualDividendRate)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun addAsset(asset: Asset) {
    stockRoomDao.addAsset(asset)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun getAssets(symbol: String): Assets {
    return stockRoomDao.getAssets(symbol)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun getEvents(symbol: String): Events {
    return stockRoomDao.getEvents(symbol)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun deleteAsset(asset: Asset) {
    //stockRoomDao.deleteAsset(symbol = asset.symbol, shares = asset.shares, price = asset.price)
    stockRoomDao.deleteAsset(asset)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun deleteAssets(symbol: String) {
    stockRoomDao.deleteAssets(symbol)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updateAsset2(
    assetOld: Asset,
    assetNew: Asset
  ) {
    stockRoomDao.updateAsset2(assetOld, assetNew)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updateAssets(
    symbol: String,
    assets: List<Asset>
  ) {
    stockRoomDao.updateAssets(symbol = symbol, assets = assets)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun addEvent(event: Event) {
    stockRoomDao.addEvent(event)
  }

//  @Suppress("RedundantSuspendModifier")
//  @WorkerThread
//  suspend fun deleteEvent(event: Event) {
//    stockRoomDao.deleteEvent(
//        symbol = event.symbol, title = event.title, note = event.note, datetime = event.datetime
//    )
//  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun deleteEvent(event: Event) {
    stockRoomDao.deleteEvent(event)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun deleteEvents(symbol: String) {
    stockRoomDao.deleteEvents(symbol)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updateEvent2(
    eventOld: Event,
    eventNew: Event
  ) {
    stockRoomDao.updateEvent2(eventOld, eventNew)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updateEvents(
    symbol: String,
    events: List<Event>
  ) {
    stockRoomDao.updateEvents(symbol = symbol, events = events)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun getStockDBdata(symbol: String): StockDBdata {
    return stockRoomDao.getStockDBdata(symbol)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun getGroups(): List<Group> {
    return stockRoomDao.getGroups()
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun getGroup(color: Int): Group {
    return stockRoomDao.getGroup(color)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun deleteGroup(color: Int) {
    return stockRoomDao.deleteGroup(color)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updateGroupName(
    color: Int,
    name: String
  ) {
    return stockRoomDao.updateGroupName(color, name)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun renameSymbol(
    symbolOld: String,
    symbolNew: String
  ): Boolean {
    return stockRoomDao.renameSymbol(symbolOld, symbolNew)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updateStockGroupColors(
    colorOld: Int,
    colorNew: Int
  ) {
    stockRoomDao.updateStockGroupColors(colorOld, colorNew)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun setStockGroupColor(
    symbol: String,
    color: Int
  ) {
    stockRoomDao.setStockGroupColor(symbol, color)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun setMarker(
    symbol: String,
    marker: Int
  ) {
    stockRoomDao.setMarker(symbol, marker)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun deleteAllGroups() {
    stockRoomDao.deleteAllGroupTable()
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun setGroup(
    color: Int,
    name: String
  ) {
    stockRoomDao.setGroup(Group(color, name))
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun setGroup(
    symbol: String,
    name: String,
    color: Int
  ) {
    stockRoomDao.setGroup(symbol, name, color)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun deleteStock(symbol: String) {
    stockRoomDao.deleteStock(symbol)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun getStockSymbols(): List<String> {
    return stockRoomDao.getStockSymbols()
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun resetPortfolios() {
    stockRoomDao.resetPortfolios()
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun deleteAll() {
    stockRoomDao.deleteAll()
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updateDividend2(
    dividendOld: Dividend,
    dividendNew: Dividend
  ) {
    stockRoomDao.updateDividend2(dividendOld, dividendNew)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updateDividend(dividend: Dividend) {
    stockRoomDao.updateDividend(dividend)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun deleteDividend(dividend: Dividend) {
    stockRoomDao.deleteDividend(
      symbol = dividend.symbol,
      amount = dividend.amount,
      type = dividend.type,
      cycle = dividend.cycle,
      paydate = dividend.paydate,
      exdate = dividend.exdate,
      note = dividend.note
    )
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun deleteDividends(symbol: String) {
    stockRoomDao.deleteDividends(symbol)
  }
}
