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

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.underline
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.listener.ChartTouchListener.ChartGesture
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.thecloudsite.stockroom.database.Group
import com.thecloudsite.stockroom.databinding.FragmentSummarygroupBinding
import com.thecloudsite.stockroom.invaders.InvadersActivity
import com.thecloudsite.stockroom.utils.DecimalFormat2Digits
import com.thecloudsite.stockroom.utils.epsilon
import com.thecloudsite.stockroom.utils.getAssets
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

class SummaryGroupFragment : Fragment() {

    private var _binding: FragmentSummarygroupBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private lateinit var stockRoomViewModel: StockRoomViewModel
    private var longPressedCounter = 0
    private var stockItemsList: List<StockItem> = emptyList()
    private var groupList: List<Group> = emptyList()

    companion object {
        fun newInstance() = SummaryGroupFragment()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        // Inflate the layout for this fragment
        _binding = FragmentSummarygroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        // use requireActivity() instead of this to have only one shared viewmodel
        stockRoomViewModel =
            ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)

        val summaryGroupAdapter = SummaryGroupAdapter(requireContext())
        val summaryGroup = binding.summaryGroup
        summaryGroup.adapter = summaryGroupAdapter

        // Set column number depending on screen width.
        val scale = 494
        val spanCount =
            (resources.configuration.screenWidthDp / (scale * resources.configuration.fontScale) + 0.5).roundToInt()

        summaryGroup.layoutManager = GridLayoutManager(
            requireContext(),
            Integer.min(Integer.max(spanCount, 1), 10)
        )

        stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { items ->
            items?.let { stockItems ->
                summaryGroupAdapter.updateData(stockItems)
                updatePieData(stockItems)
                updateGroupPieData(requireContext(), stockItems, null)
            }
        })

        stockRoomViewModel.allGroupTable.observe(viewLifecycleOwner, Observer { groups ->
            if (groups != null) {
                summaryGroupAdapter.addGroups(groups)
                updateGroupPieData(requireContext(), null, groups)
            }
        })

        longPressedCounter = 0

        binding.summaryPieChart.onChartGestureListener = object : OnChartGestureListener {
            override fun onChartGestureStart(
                me: MotionEvent?,
                lastPerformedGesture: ChartGesture?
            ) {
            }

            override fun onChartGestureEnd(
                me: MotionEvent?,
                lastPerformedGesture: ChartGesture?
            ) {
            }

            override fun onChartLongPressed(me: MotionEvent?) {
                longPressedCounter++

                updatePieChartsAndBitmap()
            }

            override fun onChartDoubleTapped(me: MotionEvent?) {
            }

            override fun onChartSingleTapped(me: MotionEvent?) {
            }

            override fun onChartFling(
                me1: MotionEvent?,
                me2: MotionEvent?,
                velocityX: Float,
                velocityY: Float
            ) {
            }

            override fun onChartScale(
                me: MotionEvent?,
                scaleX: Float,
                scaleY: Float
            ) {
            }

            override fun onChartTranslate(
                me: MotionEvent?,
                dX: Float,
                dY: Float
            ) {
            }
        }

        binding.imageView.setOnClickListener {
            AlertDialog.Builder(context)
                // https://convertcodes.com/unicode-converter-encode-decode-utf/
                .setTitle(
                    "\u0054\u0068\u0065\u0020\u0041\u006c\u0069\u0065\u006e\u0073\u0020\u0061\u0072\u0065\u0020\u0063\u006f\u006d\u0069\u006e\u0067"
                )
                .setMessage(
                    "\u0047\u0065\u0074\u0020\u0074\u0068\u0065\u006d"
                )
                .setNegativeButton("\u004c\u0061\u0074\u0065\u0072") { dialog, _ ->
                    dialog.dismiss()
                    longPressedCounter = 0
                    updatePieChartsAndBitmap()
                }
                .setPositiveButton("\u004e\u006f\u0077") { dialog, _ ->
                    val intent = Intent(activity, InvadersActivity::class.java)
                    activity?.startActivity(intent)
                    dialog.dismiss()
                    longPressedCounter = 0
                    updatePieChartsAndBitmap()
                }
                .setOnCancelListener {
                    longPressedCounter = 0
                    updatePieChartsAndBitmap()
                }
                .show()
        }

        // Rotating device keeps sending alerts.
        // State changes of the lifecycle trigger the notification.
        stockRoomViewModel.resetAlerts()
    }

    override fun onPause() {
        super.onPause()

        longPressedCounter = 0
        updatePieChartsAndBitmap()
    }

    override fun onResume() {
        super.onResume()
        stockRoomViewModel.runOnlineTaskNow()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_sync -> {
                stockRoomViewModel.runOnlineTaskNow("Request to get online data manually.")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updatePieData(
        stockItems: List<StockItem>
    ) {
        val listPie = ArrayList<PieEntry>()
        val listColors = ArrayList<Int>()

        data class AssetSummary(
            val symbol: String,
            val assets: Double,
            val color: Int
        )

        val assetList: MutableList<AssetSummary> = mutableListOf()
        var totalAssets = 0.0
        stockItems.forEach { stockItem ->
            val (totalQuantity, totalPrice, totalCommission) = getAssets(stockItem.assets)

//      val totalShares: Double = stockItem.assets.sumByDouble { asset ->
//        asset.shares
//      }
            val assets = totalQuantity * stockItem.onlineMarketData.marketPrice
            totalAssets += assets
            val color = if (stockItem.stockDBdata.groupColor != 0) {
                stockItem.stockDBdata.groupColor
            } else {
                context?.getColor(R.color.backgroundListColor)
            }

            assetList.add(
                AssetSummary(stockItem.stockDBdata.symbol, assets, color!!)
            )
        }

        if (totalAssets >= epsilon) {

            binding.summarySectionHeader.visibility = View.VISIBLE
            binding.summaryDivider.visibility = View.VISIBLE

            updatePieChartsAndBitmap()

            val sortedAssetList = assetList.filter { assetSummary ->
                assetSummary.assets > 0.0
            }
                .sortedByDescending { assetSummary ->
                    assetSummary.assets
                }

            // Display first 10 values from asset high to low.
            val n = 10
            sortedAssetList.take(n)
                .forEach { assetItem ->
                    listPie.add(PieEntry(assetItem.assets.toFloat(), assetItem.symbol))
                    //listPie.add(PieEntry(assetItem.assets.toFloat(), "${assetItem.symbol} ${DecimalFormat(DecimalFormat2Digits).format(assetItem.assets)}"))
                    listColors.add(assetItem.color)
                }

            // Add the sum of the remaining values.
            if (sortedAssetList.size == n + 1) {
                val assetItem = sortedAssetList.last()

                listPie.add(PieEntry(assetItem.assets.toFloat(), assetItem.symbol))
                listColors.add(Color.GRAY)
            } else
                if (sortedAssetList.size > n + 1) {
                    val otherAssetList = sortedAssetList.drop(n)
                    val otherAssets = otherAssetList.sumByDouble { assetItem ->
                        assetItem.assets
                    }

                    listPie.add(
                        PieEntry(
                            otherAssets.toFloat(),
                            "[${otherAssetList.first().symbol}-${otherAssetList.last().symbol}]"
                        )
                    )
                    listColors.add(Color.GRAY)
                }

            val pieDataSet = PieDataSet(listPie, "")
            pieDataSet.colors = listColors
            pieDataSet.valueTextColor = requireContext().getColor(R.color.black)
            pieDataSet.valueTextSize = 10f
            // pieDataSet.valueFormatter = DefaultValueFormatter(2)
            pieDataSet.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) =
                    DecimalFormat(DecimalFormat2Digits).format(value)
            }

            // Line start
            pieDataSet.valueLinePart1OffsetPercentage = 80f
            // Radial length
            pieDataSet.valueLinePart1Length = 0.4f
            // Horizontal length
            pieDataSet.valueLinePart2Length = .2f
            pieDataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

            val pieData = PieData(pieDataSet)
            binding.summaryPieChart.data = pieData

            //view.summaryPieChart.setUsePercentValues(true)
            binding.summaryPieChart.isDrawHoleEnabled = true

            val centerText =
                SpannableStringBuilder()
                    .append("${context?.getString(R.string.summary_total_assets)} ")
                    .underline {
                        bold {
                            append(
                                DecimalFormat(DecimalFormat2Digits).format(totalAssets)
                            )
                        }
                    }

            binding.summaryPieChart.centerText = centerText

            binding.summaryPieChart.description.isEnabled = false
            binding.summaryPieChart.legend.orientation = Legend.LegendOrientation.VERTICAL
            binding.summaryPieChart.legend.verticalAlignment = Legend.LegendVerticalAlignment.CENTER

            binding.summaryPieChart.setCenterTextColor(requireContext().getColor(R.color.black))
            binding.summaryPieChart.setHoleColor(requireContext().getColor(R.color.white))
            binding.summaryPieChart.setBackgroundColor(requireContext().getColor(R.color.white))
            binding.summaryPieChart.legend.textColor = requireContext().getColor(R.color.black)
            binding.summaryPieChart.setExtraOffsets(0f, 3f, 26f, 4f)

            //val legendList: MutableList<LegendEntry> = mutableListOf()
            //legendList.add(LegendEntry("test", SQUARE, 10f, 100f, null, Color.RED))
            //view.summaryPieChart.legend.setCustom(legendList)

            binding.summaryPieChart.invalidate()
        } else {
            binding.summarySectionHeader.visibility = View.GONE
            binding.summaryPieChart.visibility = View.GONE
            binding.summaryGroupPieChart.visibility = View.GONE
            binding.summaryDivider.visibility = View.GONE
        }
    }

    private fun updateGroupPieData(
        context: Context,
        stockItems: List<StockItem>?,
        groupList: List<Group>?
    ) {
        if (groupList != null) {
            this.groupList = groupList
        }

        if (stockItems != null) {
            stockItemsList = stockItems
        }

        val listPie = ArrayList<PieEntry>()
        val listColors = ArrayList<Int>()

        data class AssetSummary(
            val symbol: String,
            val assets: Double,
            val color: Int
        )

        val assetList: MutableList<AssetSummary> = mutableListOf()

        val groupStandardName = context.getString(R.string.standard_group)

        // Get all groups.
        val groupSet = HashSet<Int>()
        stockItemsList.forEach { stockItem ->
            groupSet.add(stockItem.stockDBdata.groupColor)
        }

        // Get all names assigned to each color.
        val groups = groupSet.map { color ->
            val name = this.groupList.find { group ->
                group.color == color
            }?.name
            if (name == null) {
                Group(color = 0, name = groupStandardName)
            } else {
                Group(color = color, name = name)
            }
        }

        // Display stats for each group.
        if (groups.size > 1) {
            groups.forEach { group ->
                //val (text1, text2) = getTotal(group.color, false, stockItemsList)

                val totalAssets = stockItemsList.filter { stockItem ->
                    stockItem.stockDBdata.groupColor == group.color
                }
                    .sumByDouble { stockItem ->
                        val (totalQuantity, totalPrice, totalCommission) = getAssets(stockItem.assets)

                        totalQuantity * stockItem.onlineMarketData.marketPrice
                    }
                assetList.add(
                    AssetSummary(group.name, totalAssets, group.color)
                )
            }

            val sortedAssetList = assetList.filter { assetSummary ->
                assetSummary.assets > 0.0
            }
                .sortedByDescending { assetSummary ->
                    assetSummary.assets
                }

            // Display first 10 values from asset high to low.
            val n = 10
            sortedAssetList.take(n)
                .forEach { assetItem ->
                    listPie.add(PieEntry(assetItem.assets.toFloat(), assetItem.symbol))
                    //listPie.add(PieEntry(assetItem.assets.toFloat(), "${assetItem.symbol} ${DecimalFormat(DecimalFormat2Digits).format(assetItem.assets)}"))
                    listColors.add(assetItem.color)
                }

            // Add the sum of the remaining values.
            if (sortedAssetList.size == n + 1) {
                val assetItem = sortedAssetList.last()

                listPie.add(PieEntry(assetItem.assets.toFloat(), assetItem.symbol))
                listColors.add(Color.GRAY)
            } else
                if (sortedAssetList.size > n + 1) {
                    val otherAssetList = sortedAssetList.drop(n)
                    val otherAssets = otherAssetList.sumByDouble { assetItem ->
                        assetItem.assets
                    }

                    listPie.add(
                        PieEntry(
                            otherAssets.toFloat(),
                            "[${otherAssetList.first().symbol}-${otherAssetList.last().symbol}]"
                        )
                    )
                    listColors.add(Color.GRAY)
                }

            val pieDataSet = PieDataSet(listPie, "")
            pieDataSet.colors = listColors
            pieDataSet.valueTextColor = requireContext().getColor(R.color.black)
            pieDataSet.valueTextSize = 10f
            // pieDataSet.valueFormatter = DefaultValueFormatter(2)
            pieDataSet.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float) =
                    DecimalFormat(DecimalFormat2Digits).format(value)
            }

            // Line start
            pieDataSet.valueLinePart1OffsetPercentage = 80f
            // Radial length
            pieDataSet.valueLinePart1Length = 0.4f
            // Horizontal length
            pieDataSet.valueLinePart2Length = .2f
            pieDataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

            val pieData = PieData(pieDataSet)
            binding.summaryGroupPieChart.data = pieData

            //view.summaryGroupPieChart.setUsePercentValues(true)
            binding.summaryGroupPieChart.isDrawHoleEnabled = true

//            val centerText =
//                SpannableStringBuilder()
//                    .append("${context?.getString(R.string.summary_total_assets)} ")
//                    .underline {
//                        bold {
//                            append(
//                                DecimalFormat(DecimalFormat2Digits).format(totalAssets)
//                            )
//                        }
//                    }
//
//            binding.summaryGroupPieChart.centerText = centerText

            binding.summaryGroupPieChart.description.isEnabled = false
            binding.summaryGroupPieChart.legend.orientation = Legend.LegendOrientation.VERTICAL
            binding.summaryGroupPieChart.legend.verticalAlignment =
                Legend.LegendVerticalAlignment.CENTER

            binding.summaryGroupPieChart.setCenterTextColor(requireContext().getColor(R.color.black))
            binding.summaryGroupPieChart.setHoleColor(requireContext().getColor(R.color.white))
            binding.summaryGroupPieChart.setBackgroundColor(requireContext().getColor(R.color.white))
            binding.summaryGroupPieChart.legend.textColor = requireContext().getColor(R.color.black)
            binding.summaryGroupPieChart.setExtraOffsets(0f, 3f, 26f, 4f)

            //val legendList: MutableList<LegendEntry> = mutableListOf()
            //legendList.add(LegendEntry("test", SQUARE, 10f, 100f, null, Color.RED))
            //view.summaryGroupPieChart.legend.setCustom(legendList)

            binding.summaryGroupPieChart.invalidate()
        }
    }

    private fun updatePieChartsAndBitmap() {
        if (longPressedCounter == 2) {
            binding.summaryPieChart.visibility = View.GONE
            binding.summaryGroupPieChart.visibility = View.GONE
            binding.imageView.visibility = View.VISIBLE
        } else {
            binding.summaryPieChart.visibility = View.VISIBLE
            binding.summaryGroupPieChart.visibility = View.VISIBLE
            binding.imageView.visibility = View.GONE
        }
    }
}

