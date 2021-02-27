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
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.database.Dividend
import com.thecloudsite.stockroom.database.Event
import com.thecloudsite.stockroom.database.Group
import com.thecloudsite.stockroom.database.StockDBdata
import com.thecloudsite.stockroom.database.StockRoomDao
import com.thecloudsite.stockroom.database.StockRoomDatabase
import com.thecloudsite.stockroom.database.StoreData
import com.thecloudsite.stockroom.utils.epsilon
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.time.LocalDateTime
import java.time.ZoneOffset

/*
 * This is not meant to be a full set of tests. For simplicity, most of your samples do not
 * include tests. However, when building the Room, it is helpful to make sure it works before
 * adding the UI.
 */

@RunWith(AndroidJUnit4::class)
class StockRoomDaoTest {

  @get:Rule
  val instantTaskExecutorRule = InstantTaskExecutorRule()

  private lateinit var stockRoomDao: StockRoomDao
  private lateinit var db: StockRoomDatabase

  @Before
  fun createDb() {
    val context: Context = ApplicationProvider.getApplicationContext()
    // Using an in-memory database because the information stored here disappears when the
    // process is killed.
    db = Room.inMemoryDatabaseBuilder(context, StockRoomDatabase::class.java)
      // Allowing main thread queries, just for testing.
      .allowMainThreadQueries()
      .build()
    stockRoomDao = db.stockRoomDao()
  }

  @After
  @Throws(IOException::class)
  fun closeDb() {
    db.close()
  }

  @Test
  @Throws(Exception::class)
  fun addStoreData() {
    val storeData = StoreData(keyId = "key1", data = "data1", value = 1.2)

    val storeData1 = stockRoomDao.getStoreData("key1")

    // key does not exist and the result is null
    assertEquals(null, storeData1)

    // insert data
    stockRoomDao.insertStoreData(storeData)

    // get data
    val storeData2 = stockRoomDao.getStoreData("key1")
    assertEquals("data1", storeData2.data)
    assertEquals(1.2, storeData2.value, epsilon)

    // update data
    storeData.data = "data3"
    storeData.value = 1.3
    stockRoomDao.insertStoreData(storeData)
    val storeData3 = stockRoomDao.getStoreData("key1")
    assertEquals("data3", storeData3.data)
    assertEquals(1.3, storeData3.value, epsilon)

    // get all data
    val allStoreData = stockRoomDao.getAllStoreLiveData()
      .waitForValue()
    assertEquals(1, allStoreData.size)
    assertEquals("key1", allStoreData[0].keyId)
    assertEquals("data3", allStoreData[0].data)
    assertEquals(1.3, allStoreData[0].value, epsilon)
  }

  @Test
  @Throws(Exception::class)
  fun deleteStoreDataByKey() {
    val storeData = StoreData(keyId = "key1", data = "data1", value = 1.2)

    // insert data
    stockRoomDao.insertStoreData(storeData)

    // get data
    val storeData2 = stockRoomDao.getStoreData("key1")
    assertEquals("data1", storeData2.data)
    assertEquals(1.2, storeData2.value, epsilon)

    // delete data by matching key
    stockRoomDao.deleteStoreData("key1")
    val storeData3 = stockRoomDao.getStoreData("key1")
    assertEquals(null, storeData3)

    // get all data
    val allStoreData = stockRoomDao.getAllStoreLiveData()
      .waitForValue()
    assertEquals(0, allStoreData.size)
  }

  @Test
  @Throws(Exception::class)
  fun deleteStoreDataByStoreData() {
    val storeData = StoreData(keyId = "key1", data = "data1", value = 1.2)

    // insert data
    stockRoomDao.insertStoreData(storeData)

    // get data
    val storeData2 = stockRoomDao.getStoreData("key1")
    assertEquals("data1", storeData2.data)
    assertEquals(1.2, storeData2.value, epsilon)

    // try delete non matching data
    storeData2.data = "notmatching"
    stockRoomDao.deleteStoreData(storeData2)
    val storeData3 = stockRoomDao.getStoreData("key1")
    // expect not deleted
    assertEquals("notmatching", storeData2.data)

    // delete data matching StoreData
    stockRoomDao.deleteStoreData(storeData)
    val storeData4 = stockRoomDao.getStoreData("key1")
    // expect deleted
    assertEquals(null, storeData4)

    // get all data
    val allStoreData = stockRoomDao.getAllStoreLiveData()
      .waitForValue()
    assertEquals(0, allStoreData.size)
  }

  @Test
  @Throws(Exception::class)
  fun deleteAllStoreData() {
    val storeData = StoreData(keyId = "key1", data = "data1", value = 1.2)

    // insert data
    stockRoomDao.insertStoreData(storeData)

    // get all data
    val allStoreData1 = stockRoomDao.getAllStoreLiveData()
      .waitForValue()
    assertEquals(1, allStoreData1.size)

    // delete all data
    stockRoomDao.deleteAllStoreTable()

    // get all data
    val allStoreData2 = stockRoomDao.getAllStoreLiveData()
      .waitForValue()
    assertEquals(0, allStoreData2.size)
  }

  @Test
  @Throws(Exception::class)
  fun insertAndGet() {
    val data = StockDBdata("symbol")
    stockRoomDao.insert(data)
    val allStockDBdata = stockRoomDao.getAllProperties()
      .waitForValue()
    assertEquals(allStockDBdata[0].symbol, data.symbol)
  }

  @Test
  @Throws(Exception::class)
  fun updateProperties() {
    val stockDBdata1 = StockDBdata("symbol1")
    stockRoomDao.insert(stockDBdata1)
    val allStockDBdata1 = stockRoomDao.getAllProperties()
      .waitForValue()
    assertEquals(allStockDBdata1[0].symbol, stockDBdata1.symbol)
    assertEquals(allStockDBdata1[0].note, "")
    assertEquals(allStockDBdata1[0].alertBelow, 0.0, epsilon)
    assertEquals(allStockDBdata1[0].alertAbove, 0.0, epsilon)

    val stockDBdata2 = StockDBdata("symbol1", alertBelow = 1.0, alertAbove = 2.0)
    stockRoomDao.insert(stockDBdata2)
    val allStockDBdata2 = stockRoomDao.getAllProperties()
      .waitForValue()
    assertEquals(allStockDBdata2.size, 1)
    assertEquals(allStockDBdata2[0].symbol, stockDBdata2.symbol)
    assertEquals(allStockDBdata2[0].note, stockDBdata2.note)
    assertEquals(allStockDBdata2[0].alertBelow, stockDBdata2.alertBelow, epsilon)
    assertEquals(allStockDBdata2[0].alertAbove, stockDBdata2.alertAbove, epsilon)

    stockRoomDao.updateAlertAbove("symbol1", 123.0, "noteAbove")
    val allStockDBdata3 = stockRoomDao.getAllProperties()
      .waitForValue()
    assertEquals(allStockDBdata3.size, 1)
    assertEquals(allStockDBdata3[0].symbol, stockDBdata2.symbol)
    assertEquals(allStockDBdata3[0].note, stockDBdata2.note)
    assertEquals(allStockDBdata3[0].alertBelow, stockDBdata2.alertBelow, epsilon)
    assertEquals(allStockDBdata3[0].alertAbove, 123.0, epsilon)
    assertEquals(allStockDBdata3[0].alertAboveNote, "noteAbove")

    stockRoomDao.updateAlertBelow("symbol1", 10.0, "noteBelow")
    val allStockDBdata4 = stockRoomDao.getAllProperties()
      .waitForValue()
    assertEquals(allStockDBdata3.size, 1)
    assertEquals(allStockDBdata4[0].symbol, stockDBdata2.symbol)
    assertEquals(allStockDBdata4[0].note, stockDBdata2.note)
    assertEquals(allStockDBdata4[0].alertBelow, 10.0, epsilon)
    assertEquals(allStockDBdata4[0].alertBelowNote, "noteBelow")
    assertEquals(allStockDBdata4[0].alertAbove, 123.0, epsilon)

    stockRoomDao.updateNote("symbol1", "new note")
    val allStockDBdata5 = stockRoomDao.getAllProperties()
      .waitForValue()
    assertEquals(allStockDBdata3.size, 1)
    assertEquals(allStockDBdata5[0].symbol, stockDBdata2.symbol)
    assertEquals(allStockDBdata5[0].note, "new note")
    assertEquals(allStockDBdata5[0].alertBelow, 10.0, epsilon)
    assertEquals(allStockDBdata5[0].alertAbove, 123.0, epsilon)
  }

  @Test
  @Throws(Exception::class)
  fun getAllStockDBdata() {
//        val assets: ArrayList<Asset> = arrayListOf(Asset(symbol = "symbol", shares = 123.0, price = 321.0))
//        val assets1: List<String> = listOf("symbol1", "symbol2")

//        val gson = Gson()
//        val assetStr = gson.toJson(assets)
//        val stockDBdata1 = StockDBdata("aaa", alertBelow = 1.0, alertAbove = 2.0, assets1 = assetStr, assets2 = assets)
    val stockDBdata1 = StockDBdata("aaa", alertBelow = 1.0, alertAbove = 2.0)
    stockRoomDao.insert(stockDBdata1)
    val stockDBdata2 = StockDBdata("bbb")
    stockRoomDao.insert(stockDBdata2)
    val allStockDBdata = stockRoomDao.getAll()
      .waitForValue()

//        val sType = object : TypeToken<List<Asset>>() { }.type
//        val assetJson = gson.fromJson<List<Asset>>(allStockDBdata[0].assets1, sType)
    assertEquals(allStockDBdata[0].symbol, stockDBdata1.symbol)
    assertEquals(allStockDBdata[1].symbol, stockDBdata2.symbol)
  }

  @Test
  @Throws(Exception::class)
  fun updateStockDBdata() {
    val stockDBdata1 = StockDBdata(
      symbol = "symbol1",
      portfolio = "portfolio1",
      data = "data1",
      groupColor = 123,
      alertBelow = 1.0,
      alertAbove = 2.0,
      note = "note"
    )
    stockRoomDao.insert(stockDBdata1)

    val stockDBdata2 = stockRoomDao.getStockDBdata("symbol1")
    assertEquals("portfolio1", stockDBdata2.portfolio)
    assertEquals("data1", stockDBdata2.data)
    assertEquals(123, stockDBdata2.groupColor)
    assertEquals(1.0, stockDBdata2.alertBelow, epsilon)
    assertEquals(2.0, stockDBdata2.alertAbove, epsilon)
    assertEquals("note", stockDBdata2.note)

    val stockDBdataMatchingSymbol = StockDBdata("symbol1", alertAbove = 10.0)
    stockRoomDao.insert(stockDBdataMatchingSymbol)

    val stockDBdataNotMatchingSymbol = StockDBdata("symbol2", alertAbove = 10.0)
    stockRoomDao.insert(stockDBdataNotMatchingSymbol)

    // Check that insert does not override existing values
    val stockDBdata4 = stockRoomDao.getStockDBdata("symbol1")
    assertEquals(stockDBdata4.groupColor, 123)
    assertEquals(stockDBdata4.alertBelow, 1.0, epsilon)
    assertEquals(stockDBdata4.alertAbove, 10.0, epsilon)
    assertEquals(stockDBdata4.note, "note")

    val stockDBdata5 = stockRoomDao.getStockDBdata("symbol2")
    assertEquals(stockDBdata5.groupColor, 0)
    assertEquals(stockDBdata5.alertBelow, 0.0, epsilon)
    assertEquals(stockDBdata5.alertAbove, 10.0, epsilon)
    assertEquals(stockDBdata5.note, "")
  }

  @Test
  @Throws(Exception::class)
  fun resetPortfoliosTest() {
    val stockDBdata1 = StockDBdata(
      symbol = "symbol1",
      portfolio = "portfolio1",
      data = "",
      groupColor = 0,
      alertBelow = 0.0,
      alertAbove = 0.0,
      note = ""
    )
    stockRoomDao.insert(stockDBdata1)
    val stockDBdata2 = StockDBdata(
      symbol = "symbol2",
      portfolio = "portfolio2",
      data = "",
      groupColor = 0,
      alertBelow = 0.0,
      alertAbove = 0.0,
      note = ""
    )
    stockRoomDao.insert(stockDBdata2)

    val stockDBdata3 = stockRoomDao.getStockDBdata("symbol1")
    val stockDBdata4 = stockRoomDao.getStockDBdata("symbol2")
    assertEquals("portfolio1", stockDBdata3.portfolio)
    assertEquals("portfolio2", stockDBdata4.portfolio)

    stockRoomDao.resetPortfolios()

    // Check that portfolio is reset
    val stockDBdata5 = stockRoomDao.getStockDBdata("symbol1")
    val stockDBdata6 = stockRoomDao.getStockDBdata("symbol2")
    assertEquals("", stockDBdata5.portfolio)
    assertEquals("", stockDBdata6.portfolio)
  }

  @Test
  @Throws(Exception::class)
  fun getAssets() {
    val stockDBdata1 = StockDBdata("symbol1")
    stockRoomDao.insert(stockDBdata1)
    val asset1 = Asset(symbol = "symbol1", quantity = 10.0, price = 123.0, date = 0L)
    stockRoomDao.addAsset(asset1)
    stockRoomDao.addAsset(asset1)
    val asset2 = Asset(symbol = "symbol1", quantity = 20.0, price = 223.0, date = 0L)
    stockRoomDao.addAsset(asset2)
    val asset3 = Asset(symbol = "symbol2", quantity = 30.0, price = 323.0, date = 0L)
    stockRoomDao.addAsset(asset3)
    val stockDBdata2 = StockDBdata("symbol2")
    stockRoomDao.insert(stockDBdata2)

    val assets1 = stockRoomDao.getAssets("symbol1")
    assertEquals(assets1.assets.size, 3)
    assertEquals(assets1.assets[0].quantity, asset1.quantity, epsilon)
    assertEquals(assets1.assets[0].price, asset1.price, epsilon)
    assertEquals(assets1.assets[2].quantity, asset2.quantity, epsilon)
    assertEquals(assets1.assets[2].price, asset2.price, epsilon)

    stockRoomDao.deleteAsset(
      symbol = asset1.symbol,
      quantity = asset1.quantity,
      price = asset1.price
    )
    val assetsDel1 = stockRoomDao.getAssets("symbol1")
    assertEquals(assetsDel1.assets.size, 1)

    val assets2 = stockRoomDao.getAssets("symbol2")
    assertEquals(assets2.assets.size, 1)
    assertEquals(assets2.assets[0].quantity, asset3.quantity, epsilon)
    assertEquals(assets2.assets[0].price, asset3.price, epsilon)

    // Expect one asset
    val assets3a = stockRoomDao.getAssets("symbol1")
    assertEquals(assets3a.assets.size, 1)
    // Delete the asset
    stockRoomDao.deleteAssets("symbol1")
    // Expect no assets
    val assets3b = stockRoomDao.getAssets("symbol1")
    assertEquals(assets3b.assets.size, 0)
  }

  @Test
  @Throws(Exception::class)
  fun deleteStockDBdata() {
    val stockDBdata1 = StockDBdata("symbol1")
    stockRoomDao.insert(stockDBdata1)

    stockRoomDao.addAsset(Asset(symbol = "symbol1", quantity = 10.0, price = 123.0, date = 0L))
    val assets1 = stockRoomDao.getAssets("symbol1")
    assertEquals(1, assets1.assets.size)

    stockRoomDao.addEvent(
      Event(symbol = "symbol1", type = 1, title = "title1", note = "note1", datetime = 1)
    )
    val events1 = stockRoomDao.getEvents("symbol1")
    assertEquals(1, events1.events.size)

    // delete stockDBdata also deletes assets and events
    stockRoomDao.deleteSymbol("symbol1")

    val assetsdel = stockRoomDao.getAssets("symbol1")
    assertEquals(null, assetsdel)
    val eventsdel = stockRoomDao.getEvents("symbol1")
    assertEquals(null, eventsdel)
  }

  @Test
  @Throws(Exception::class)
  fun updateAssets() {
    val stockDBdata1 = StockDBdata("symbol1")
    stockRoomDao.insert(stockDBdata1)

    val assets: MutableList<Asset> = mutableListOf()
    assets.add(Asset(symbol = "symbol1", quantity = 10.0, price = 123.0, date = 0L))
    assets.add(Asset(symbol = "symbol1", quantity = 20.0, price = 223.0, date = 0L))
    //assets.add(Asset(symbol = "symbol2", shares = 30.0, price = 323.0))

    // Update = delete + add
    stockRoomDao.deleteAssets("symbol1")
    assets.forEach { asset ->
      stockRoomDao.addAsset(asset)
    }

    val assets1 = stockRoomDao.getAssets("symbol1")
    assertEquals(assets1.assets.size, 2)

    stockRoomDao.deleteAssets("symbol1")
    val assetsdel = stockRoomDao.getAssets("symbol1")
    assertEquals(assetsdel.assets.size, 0)
  }

  @Test
  @Throws(Exception::class)
  fun addDeleteGroups() {
    val groups1: MutableList<Group> = mutableListOf()
    groups1.add(Group(color = 1, name = "g1"))
    groups1.add(Group(color = 2, name = "g2"))
    stockRoomDao.setGroups(groups1)

    // Add groups with two elements.
    val groups2 = stockRoomDao.getGroups()
    assertEquals(groups2.size, 2)

    // Add one group
    stockRoomDao.setGroup(Group(color = 10, name = "g10"))

    val groups3 = stockRoomDao.getGroups()
    assertEquals(groups3.size, 3)

    // Set again groups with two elements.
    stockRoomDao.setGroups(groups1)

    // previously set group g10,10 is not deleted
    val groups4 = stockRoomDao.getGroups()
    assertEquals(groups4.size, 3)

    // Delete all groups
    stockRoomDao.deleteAllGroupTable()
    val groups5 = stockRoomDao.getGroups()
    assertEquals(groups5.size, 0)
  }

  @Test
  @Throws(Exception::class)
  fun updateGroups() {
    val stockDBdata1 = StockDBdata(symbol = "symbol")
    stockRoomDao.insert(stockDBdata1)
    val stockDBdata2 = stockRoomDao.getStockDBdata("symbol")
    assertEquals(stockDBdata2.groupColor, 0)
    stockRoomDao.setStockGroupColor("symbol", 0xfff000)
    val stockDBdata3 = stockRoomDao.getStockDBdata("symbol")
    assertEquals(stockDBdata3.groupColor, 0xfff000)

    val groups1: MutableList<Group> = mutableListOf()
    groups1.add(Group(color = 1, name = "g1"))
    groups1.add(Group(color = 2, name = "g2"))
    val groups12: MutableList<Group> = mutableListOf()
    groups12.add(Group(color = 12, name = "g12"))
    groups12.add(Group(color = 22, name = "g22"))
    stockRoomDao.setGroups(groups12)

    val groups2 = stockRoomDao.getGroups()
    assertEquals(groups2.size, 2)
    assertEquals(groups2[0].name, "g12")
    assertEquals(groups2[0].color, 12)
    assertEquals(groups2[1].name, "g22")
    assertEquals(groups2[1].color, 22)

    val group1 = stockRoomDao.getGroup(12)
    assertEquals(group1.name, "g12")
    val group10 = stockRoomDao.getGroup(10)
    assertEquals(group10, null)

    val groupsA = stockRoomDao.getGroups()
    assertEquals(groupsA.size, 2)

    // group with color 10 does not exist and will be added
    stockRoomDao.setGroup(Group(color = 10, name = "g10"))

    val groupsB = stockRoomDao.getGroups()
    assertEquals(groupsB.size, 3)

    // group with color 10 does exist and will be updated
    stockRoomDao.setGroup(Group(color = 10, name = "g100"))

    val groupsC = stockRoomDao.getGroups()
    assertEquals(groupsC.size, 3)

    val group10Updated = stockRoomDao.getGroup(10)
    assertEquals(group10Updated.name, "g100")
    stockRoomDao.updateGroupName(10, "g1010")
    val group1010 = stockRoomDao.getGroup(10)
    assertEquals(group1010.name, "g1010")

    val groupsD = stockRoomDao.getGroups()
    assertEquals(groupsD.size, 3)
  }

  @Test
  @Throws(Exception::class)
  fun setGroupColor() {
    val groups: MutableList<Group> = mutableListOf()
    groups.add(Group(color = 1, name = "Kaufen"))
    groups.add(Group(color = 2, name = "Verkaufen"))
    groups.add(Group(color = 3, name = "Beobachten"))
    stockRoomDao.setGroups(groups)

    stockRoomDao.insert(StockDBdata(symbol = "MSFT", groupColor = 1))
    stockRoomDao.insert(StockDBdata(symbol = "AAPL", groupColor = 2))
    stockRoomDao.insert(StockDBdata(symbol = "AMZN"))
    stockRoomDao.insert(StockDBdata(symbol = "TLSA", groupColor = Color.WHITE))
    stockRoomDao.insert(StockDBdata(symbol = "VZ", groupColor = 5))

    stockRoomDao.setGroup(Group(color = 5, name = "test1"))
    // Overwrite the previous group
    stockRoomDao.setGroup(Group(color = 5, name = "test2"))
    // Same name, but different color
    stockRoomDao.setGroup(Group(color = 6, name = "test2"))
    stockRoomDao.setStockGroupColor(symbol = "VZ", color = Color.BLACK)
    val stockDBdata3 = stockRoomDao.getStockDBdata("VZ")
    assertEquals(stockDBdata3.groupColor, Color.BLACK)

    val groups1 = stockRoomDao.getGroups()
    assertEquals(5, groups1.size)
    assertEquals("test2", groups1[3].name)
    assertEquals(3, groups1[2].color)
  }

  @Test
  @Throws(Exception::class)
  fun deleteGroup() {
    val groups: MutableList<Group> = mutableListOf()
    groups.add(Group(color = 1, name = "Kaufen"))
    groups.add(Group(color = 2, name = "Verkaufen"))
    groups.add(Group(color = 3, name = "Beobachten"))
    stockRoomDao.setGroups(groups)

    val groups1 = stockRoomDao.getGroups()
    assertEquals(groups1.size, 3)
    assertEquals(groups1[1].name, "Verkaufen")
    assertEquals(groups1[1].color, 2)

    stockRoomDao.deleteGroup(2)

    val groups2 = stockRoomDao.getGroups()
    assertEquals(groups2.size, 2)
    assertNotEquals(groups2[1].name, "Verkaufen")
    assertNotEquals(groups2[1].color, 2)
  }

  @Test
  @Throws(Exception::class)
  fun updateGroupColor() {
    stockRoomDao.insert(StockDBdata(symbol = "MSFT", groupColor = 1))
    stockRoomDao.insert(StockDBdata(symbol = "AAPL", groupColor = 2))
    stockRoomDao.insert(StockDBdata(symbol = "AMZN"))
    stockRoomDao.insert(StockDBdata(symbol = "TLSA", groupColor = 2))
    stockRoomDao.insert(StockDBdata(symbol = "VZ", groupColor = 5))

    stockRoomDao.updateStockGroupColors(2, 10)
    val stockDBdata1 = stockRoomDao.getStockDBdata("AAPL")
    assertEquals(10, stockDBdata1.groupColor)
    val stockDBdata2 = stockRoomDao.getStockDBdata("TLSA")
    assertEquals(10, stockDBdata2.groupColor)
    val stockDBdata3 = stockRoomDao.getStockDBdata("VZ")
    assertNotEquals(10, stockDBdata3.groupColor)

    stockRoomDao.setStockGroupColor("MSFT", 123)
    val stockDBdata4 = stockRoomDao.getStockDBdata("MSFT")
    assertEquals(123, stockDBdata4.groupColor)
  }

  @Test
  @Throws(Exception::class)
  fun getEvents() {
    val stockDBdata1 = StockDBdata("symbol1")
    stockRoomDao.insert(stockDBdata1)
    val current = LocalDateTime.now()

    val dateTime1 = current.toEpochSecond(ZoneOffset.MIN)
    val event1 =
      Event(symbol = "symbol1", type = 1, title = "title1", note = "note1", datetime = dateTime1)
    stockRoomDao.addEvent(event1)

    val dateTime2 = current.toEpochSecond(ZoneOffset.UTC)
    val event2 =
      Event(symbol = "symbol1", type = 2, title = "title2", note = "note2", datetime = dateTime2)
    stockRoomDao.addEvent(event2)

    val dateTime3 = current.toEpochSecond(ZoneOffset.MAX)
    val event3 =
      Event(symbol = "symbol2", type = 3, title = "title3", note = "note3", datetime = dateTime3)
    stockRoomDao.addEvent(event3)
    val stockDBdata2 = StockDBdata("symbol2")
    stockRoomDao.insert(stockDBdata2)

    val events1 = stockRoomDao.getEvents("symbol1")
    assertEquals(events1.events.size, 2)
    assertEquals(events1.events[0].type, event1.type)
    assertEquals(events1.events[0].datetime, dateTime1)
    assertEquals(events1.events[1].type, event2.type)
    assertEquals(events1.events[1].datetime, dateTime2)

    val events2 = stockRoomDao.getEvents("symbol2")
    assertEquals(events2.events.size, 1)
    assertEquals(events2.events[0].type, event3.type)
    assertEquals(events2.events[0].datetime, dateTime3)
  }

  @Test
  @Throws(Exception::class)
  fun dividendTest() {
    val stockDBdata1 = StockDBdata("symbol1", dividendNote = "dividendNote1")
    stockRoomDao.insert(stockDBdata1)

    stockRoomDao.addDividend(
      Dividend(symbol = "symbol1", amount = 11.0, type = 21, cycle = 1, paydate = 21L, exdate = 31L)
    )
    stockRoomDao.addDividend(
      Dividend(symbol = "symbol1", amount = 12.0, type = 22, cycle = 3, paydate = 22L, exdate = 32L)
    )

    val stockDBdata3 = StockDBdata("symbol3", dividendNote = "dividendNote3")
    stockRoomDao.insert(stockDBdata3)

    stockRoomDao.addDividend(
      Dividend(symbol = "symbol3", amount = 13.0, type = 23, cycle = 1, paydate = 23L, exdate = 33L)
    )

    val dividends = stockRoomDao.getDividends("symbol1")

    assertEquals("dividendNote1", dividends.stockDBdata.dividendNote)
    assertEquals(2, dividends.dividends.size)
    assertEquals("symbol1", dividends.dividends[0].symbol)
    assertEquals(11.0, dividends.dividends[0].amount, epsilon)
    assertEquals(21, dividends.dividends[0].type)
    assertEquals(21L, dividends.dividends[0].paydate)
    assertEquals(31L, dividends.dividends[0].exdate)

    assertEquals("symbol1", dividends.dividends[1].symbol)
    assertEquals(12.0, dividends.dividends[1].amount, epsilon)
    assertEquals(22, dividends.dividends[1].type)
    assertEquals(22L, dividends.dividends[1].paydate)
    assertEquals(32L, dividends.dividends[1].exdate)

    stockRoomDao.deleteSymbol("symbol1")

    val dividends2 = stockRoomDao.getDividends("symbol1")
    assertEquals(null, dividends2)
    val dividends3 = stockRoomDao.getDividends("symbol3")
    assertEquals(1, dividends3.dividends.size)

    stockRoomDao.deleteDividend(
      symbol = "symbol3",
      amount = 13.0,
      type = 23,
      cycle = 1,
      paydate = 23L,
      exdate = 33L,
      note = ""
    )

    val dividends4 = stockRoomDao.getDividends("symbol3")
    assertEquals(0, dividends4.dividends.size)
  }

  @Test
  @Throws(Exception::class)
  fun deleteDividendsTest() {
    val stockDBdata1 = StockDBdata("symbol1", dividendNote = "dividendNote1")
    stockRoomDao.insert(stockDBdata1)

    stockRoomDao.addDividend(
      Dividend(symbol = "symbol1", amount = 11.0, type = 21, cycle = 1, paydate = 21L, exdate = 31L)
    )
    stockRoomDao.addDividend(
      Dividend(symbol = "symbol1", amount = 12.0, type = 22, cycle = 3, paydate = 22L, exdate = 32L)
    )

    val dividends = stockRoomDao.getDividends("symbol1")

    assertEquals(2, dividends.dividends.size)

    stockRoomDao.deleteDividends("symbol1")

    val dividends1 = stockRoomDao.getDividends("symbol1")
    assertEquals(0, dividends1.dividends.size)
  }

  @Test
  @Throws(Exception::class)
  fun delete() {
    val stockDBdata1 = StockDBdata("symbol1")
    stockRoomDao.insert(stockDBdata1)
    val stockDBdata2 = StockDBdata("symbol2")
    stockRoomDao.insert(stockDBdata2)
    stockRoomDao.deleteSymbol("symbol1")
    val allStockDBdata = stockRoomDao.getAllProperties()
      .waitForValue()
    assertEquals(allStockDBdata.size, 1)
    assertEquals(allStockDBdata[0].symbol, stockDBdata2.symbol)
  }

  @Test
  @Throws(Exception::class)
  fun deleteAll() {
    val stockDBdata1 = StockDBdata("symbol1")
    stockRoomDao.insert(stockDBdata1)
    val stockDBdata2 = StockDBdata("symbol2")
    stockRoomDao.insert(stockDBdata2)
    stockRoomDao.deleteAllStockTable()
    val allStockDBdata = stockRoomDao.getAllProperties()
      .waitForValue()
    assertTrue(allStockDBdata.isEmpty())
  }

  @Test
  @Throws(Exception::class)
  fun renameSymbolStockDBdata() {
    val stockDBdata1 = StockDBdata("symbol1")
    stockRoomDao.insert(stockDBdata1)
    val stockDBdata2 = StockDBdata("symbol2")
    stockRoomDao.insert(stockDBdata2)
    val stockDBdata3 = StockDBdata("symbol3")
    stockRoomDao.insert(stockDBdata3)

    // test symbol aready exists
    val stockDBdataExists = stockRoomDao.getStockDBdata("symbol2")
    assertNotEquals(null, stockDBdataExists)

    // should not be renamed as symbol2 already exist
    val rename1 = stockRoomDao.renameSymbol("symbol1", "symbol2")
    assertTrue(!rename1)
    val allStockDBdata1 = stockRoomDao.getAllProperties()
      .waitForValue()
    assertEquals(3, allStockDBdata1.size)
    assertEquals("symbol1", allStockDBdata1[0].symbol)

    // test symbol4 not exists
    val stockDBdataNotExists = stockRoomDao.getStockDBdata("symbol4")
    assertEquals(null, stockDBdataNotExists)

    // rename symbol1 to symbol4
    val rename2 = stockRoomDao.renameSymbol("symbol1", "symbol4")
    assertTrue(rename2)
    val allStockDBdata2 = stockRoomDao.getAllProperties()
      .waitForValue()
    assertEquals(3, allStockDBdata2.size)
    assertEquals("symbol4", allStockDBdata2[0].symbol)
  }

  @Test
  @Throws(Exception::class)
  fun renameSymbolAsset() {
    val stockDBdata1 = StockDBdata("symbol1")
    stockRoomDao.insert(stockDBdata1)
    val asset1 = Asset(symbol = "symbol1", quantity = 10.0, price = 123.0, date = 0L)
    stockRoomDao.addAsset(asset1)

    val rename1 = stockRoomDao.renameSymbol("symbol1", "symbol2")
    assertTrue(rename1)
    val assets1 = stockRoomDao.getAssets("symbol2")
    assertEquals(1, assets1.assets.size)
    assertEquals("symbol2", assets1.assets[0].symbol)
  }

  @Test
  @Throws(Exception::class)
  fun renameSymbolEvent() {
    val stockDBdata1 = StockDBdata("symbol1")
    stockRoomDao.insert(stockDBdata1)
    val event1 =
      Event(symbol = "symbol1", type = 1, title = "title1", note = "note1", datetime = 1)
    stockRoomDao.addEvent(event1)

    val rename1 = stockRoomDao.renameSymbol("symbol1", "symbol2")
    assertTrue(rename1)
    val events1 = stockRoomDao.getEvents("symbol2")
    assertEquals(1, events1.events.size)
    assertEquals("symbol2", events1.events[0].symbol)
  }

  @Test
  @Throws(Exception::class)
  fun renameSymbolDividend() {
    val stockDBdata1 = StockDBdata("symbol1")
    stockRoomDao.insert(stockDBdata1)
    val dividend1 =
      Dividend(symbol = "symbol1", amount = 11.0, type = 21, cycle = 1, paydate = 21L, exdate = 31L)
    stockRoomDao.addDividend(dividend1)

    val rename1 = stockRoomDao.renameSymbol("symbol1", "symbol2")
    assertTrue(rename1)
    val dividends1 = stockRoomDao.getDividends("symbol2")
    assertEquals(1, dividends1.dividends.size)
    assertEquals("symbol2", dividends1.dividends[0].symbol)
  }
}
