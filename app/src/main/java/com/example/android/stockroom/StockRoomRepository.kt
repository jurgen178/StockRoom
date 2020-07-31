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
package com.example.android.stockroom

import android.content.Context
import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.room.Query

/**
 * Abstracted Repository as promoted by the Architecture Guide.
 * https://developer.android.com/topic/libraries/architecture/guide.html
 */
class StockRoomRepository(private val stockRoomDao: StockRoomDao) {

  // Room executes all queries on a separate thread.
  // Observed LiveData will notify the observer when the data has changed.
  val allProperties: LiveData<List<StockDBdata>> = stockRoomDao.getAllProperties()

  val allAssets: LiveData<List<Assets>> = stockRoomDao.getAllAssetsLiveData()
  val allEvents: LiveData<List<Events>> = stockRoomDao.getAllEventsLiveData()
  val allGroups: LiveData<List<Group>> = stockRoomDao.getAllGroupsLiveData()

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
  suspend fun setPredefinedGroups(context: Context) {
    stockRoomDao.setPredefinedGroups(context)
  }

  // You must call this on a non-UI thread or your app will crash. So we're making this a
  // suspend function so the caller methods know this.
  // Like this, Room ensures that you're not doing any long running operations on the main
  // thread, blocking the UI.
  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun insert(stockDBdata: StockDBdata) {
    stockRoomDao.insert(stockDBdata)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updateAlertAbove(
    symbol: String,
    alertAbove: Float
  ) {
    stockRoomDao.updateAlertAbove(symbol, alertAbove)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updateAlertBelow(
    symbol: String,
    alertBelow: Float
  ) {
    stockRoomDao.updateAlertBelow(symbol, alertBelow)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updateNotes(
    symbol: String,
    notes: String
  ) {
    stockRoomDao.updateNotes(symbol, notes)
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
    stockRoomDao.deleteAsset(symbol = asset.symbol, shares = asset.shares, price = asset.price)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun deleteAssets(symbol: String) {
    stockRoomDao.deleteAssets(symbol)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updateAssets(symbol: String, assets: List<Asset>) {
    stockRoomDao.updateAssets(symbol = symbol, assets = assets)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun addEvent(event: Event) {
    stockRoomDao.addEvent(event)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun deleteEvent(event: Event) {
    stockRoomDao.deleteEvent(symbol = event.symbol, title = event.title, note = event.note, datetime = event.datetime)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updateEvents(symbol: String, events: List<Event>) {
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
  suspend fun updateGroupName(color: Int, name: String) {
    return stockRoomDao.updateGroupName(color, name)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun updateStockGroupColors(colorOld: Int, colorNew: Int) {
    stockRoomDao.updateStockGroupColors(colorOld, colorNew)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun setStockGroupColor(symbol: String, color: Int) {
    stockRoomDao.setStockGroupColor(symbol, color)
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
  suspend fun delete(symbol: String) {
    stockRoomDao.delete(symbol)
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun getStockSymbols(): List<String> {
    return stockRoomDao.getStockSymbols()
  }

  @Suppress("RedundantSuspendModifier")
  @WorkerThread
  suspend fun deleteAll(context: Context) {
    stockRoomDao.deleteAll(context)
  }
}
