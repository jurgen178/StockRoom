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

import android.content.Intent
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.scale
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.thecloudsite.stockroom.database.Asset
import com.thecloudsite.stockroom.database.Dividend
import com.thecloudsite.stockroom.databinding.FragmentTransactionsBinding
import com.thecloudsite.stockroom.utils.*
import java.text.DecimalFormat

open class StockRoomTransactionsBaseFragment : Fragment() {

    private var _binding: FragmentTransactionsBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    val binding get() = _binding!!

    lateinit var stockRoomViewModel: StockRoomViewModel
    private lateinit var adapter: StockRoomTransactionsAdapter

    private val transactionDataList: MutableList<TransactionData> = mutableListOf()

    private val assetBoughtMap = HashMap<String, Int>()
    private var assetBought = 0
    private val assetSoldMap = HashMap<String, Int>()
    private var assetSold = 0
    private val dividendReceivedMap = HashMap<String, Int>()
    private var dividendReceived = 0

    private fun clickListenerSummary(transactionData: TransactionData) {
        if (transactionData.symbol.isNotEmpty()) {
            val intent = Intent(context, StockDataActivity::class.java)
            intent.putExtra(EXTRA_SYMBOL, transactionData.symbol)
            //stockRoomViewModel.runOnlineTaskNow()
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Get a new or existing ViewModel from the ViewModelProvider.
        // use requireActivity() instead of this to have only one shared viewmodel
        stockRoomViewModel =
            ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)

        val clickListenerSummary =
            { transactionData: TransactionData -> clickListenerSummary(transactionData) }
        adapter = StockRoomTransactionsAdapter(requireContext(), clickListenerSummary)

        val recyclerView = binding.recyclerview
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate the layout for this fragment
        _binding = FragmentTransactionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        stockRoomViewModel.runOnlineTaskNow()
    }

    private fun getDate(date: Long): Long =
        if (date != 0L) date else 1L // ensure the stats with date=0 is top entry.

    private fun getAssetData(
        quantity: Double,
        price: Double,
        fee: Double
    ): SpannableStringBuilder {
        val assetStr = SpannableStringBuilder().append(
            "${DecimalFormat(DecimalFormatQuantityDigits).format(quantity)}@${
                to2To8Digits(price)
            }=${DecimalFormat(DecimalFormat2To4Digits).format(quantity * price)}"
        )

        if (fee > 0.0) {
            assetStr.scale(feeScale) {
                append("+${DecimalFormat(DecimalFormat2To4Digits).format(fee)}")
            }
        }

        return assetStr
    }

    fun resetTransactionDataList() {
        transactionDataList.clear()
        assetBoughtMap.clear()
        assetBought = 0
        assetSoldMap.clear()
        assetSold = 0
        dividendReceivedMap.clear()
        dividendReceived = 0
    }

    fun addAssetsBought(assets: List<Asset>) {
        assets.filter { asset ->
            asset.quantity > 0.0
        }.forEach { asset ->
            transactionDataList.add(
                TransactionData(
                    viewType = transaction_data_type,
                    date = getDate(asset.date),
                    symbol = asset.symbol,
                    name = getSymbolDisplayName(asset.symbol),
                    type = TransactionType.AssetBoughtType,
                    account = asset.account,
                    value = asset.quantity * asset.price + asset.fee,
                    amountStr = getAssetData(asset.quantity, asset.price, asset.fee),
                )
            )

            updateMap(assetBoughtMap, asset.account)
            assetBought++
        }
    }

    fun addAssetsSold(assets: List<Asset>) {
        assets.filter { asset ->
            asset.quantity < 0.0
        }.forEach { asset ->
            transactionDataList.add(
                TransactionData(
                    viewType = transaction_data_type,
                    date = getDate(asset.date),
                    symbol = asset.symbol,
                    name = getSymbolDisplayName(asset.symbol),
                    type = TransactionType.AssetSoldType,
                    account = asset.account,
                    value = -asset.quantity * asset.price + asset.fee,
                    amountStr = getAssetData(-asset.quantity, asset.price, asset.fee),
                )
            )

            updateMap(assetSoldMap, asset.account)
            assetSold++
        }
    }

    fun addDividendsReceived(dividends: List<Dividend>) {
        dividends.filter { dividend ->
            dividend.type == DividendType.Received.value
        }.forEach { dividend ->
            transactionDataList.add(
                TransactionData(
                    viewType = transaction_data_type,
                    date = getDate(dividend.paydate),
                    symbol = dividend.symbol,
                    name = getSymbolDisplayName(dividend.symbol),
                    type = TransactionType.DividendReceivedType,
                    account = dividend.account,
                    value = dividend.amount,
                    amountStr = SpannableStringBuilder().append(
                        DecimalFormat(DecimalFormat2To4Digits).format(
                            dividend.amount
                        )
                    ),
                )
            )

            updateMap(dividendReceivedMap, dividend.account)
            dividendReceived++
        }
    }

    fun updateData() {

        adapter.updateData(
            transactionDataList,
            TransactionData(
                viewType = transaction_stats_type,
                date = 0,
                symbol = "",
                name = "",
                type = TransactionType.StatsType,
                assetBoughtMap = assetBoughtMap,
                assetBought = assetBought,
                assetSoldMap = assetSoldMap,
                assetSold = assetSold,
                dividendReceivedMap = dividendReceivedMap,
                dividendReceived = dividendReceived,
            )
        )
    }

    private fun updateMap(map: HashMap<String, Int>, account: String) {
        if (map.containsKey(account)) {
            map[account] = map[account]!! + 1
        } else {
            map[account] = 1
        }
    }
}