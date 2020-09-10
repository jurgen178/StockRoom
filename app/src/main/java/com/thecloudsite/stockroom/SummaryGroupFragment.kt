package com.thecloudsite.stockroom

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.text.bold
import androidx.core.text.underline
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.android.synthetic.main.fragment_summarygroup.view.summaryPieChart
import java.text.DecimalFormat

class SummaryGroupFragment : Fragment() {

  private lateinit var stockRoomViewModel: StockRoomViewModel

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
  ): View? {
    // Inflate the layout for this fragment
    return inflater.inflate(R.layout.fragment_summarygroup, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    // use requireActivity() instead of this to have only one shared viewmodel
    stockRoomViewModel = ViewModelProvider(requireActivity()).get(StockRoomViewModel::class.java)

    val summaryGroupAdapter = SummaryGroupAdapter(requireContext())
    val summaryGroup = view.findViewById<RecyclerView>(R.id.summaryGroup)
    summaryGroup.adapter = summaryGroupAdapter

    // Set column number depending on orientation.
    val spanCount = if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
      1
    } else {
      2
    }

    summaryGroup.layoutManager = GridLayoutManager(requireContext(), spanCount)

    stockRoomViewModel.allStockItems.observe(viewLifecycleOwner, Observer { items ->
      items?.let { stockItemSet ->
        summaryGroupAdapter.updateData(stockItemSet.stockItems)
        updatePieData(view, stockItemSet.stockItems)
      }
    })

    stockRoomViewModel.allGroupTable.observe(viewLifecycleOwner, Observer { groups ->
      summaryGroupAdapter.addGroups(groups)
    })
  }

  override fun onResume() {
    super.onResume()
    stockRoomViewModel.updateOnlineDataManually()
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    return when (item.itemId) {
      R.id.menu_sync -> {
        stockRoomViewModel.updateOnlineDataManually("Schedule to get online data manually.")
        true
      }
      else -> super.onOptionsItemSelected(item)
    }
  }

  private fun updatePieData(
    view: View,
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
      val shares: Double = stockItem.assets.sumByDouble { asset ->
        asset.shares
      }
      val assets = shares * stockItem.onlineMarketData.marketPrice
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

    if (totalAssets > 0.0) {
      val sortedAssetList = assetList.sortedByDescending { item -> item.assets }

      // Display first 10 values from asset high to low.
      val n = 10
      sortedAssetList.take(n)
          .forEach { assetItem ->
            listPie.add(PieEntry(assetItem.assets.toFloat(), assetItem.symbol))
            //listPie.add(PieEntry(assetItem.assets.toFloat(), "${assetItem.symbol} ${DecimalFormat("0.00").format(assetItem.assets)}"))
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
    }

    val pieDataSet = PieDataSet(listPie, "")
    pieDataSet.colors = listColors
    pieDataSet.valueTextSize = 10f
    // pieDataSet.valueFormatter = DefaultValueFormatter(2)
    pieDataSet.valueFormatter = object : ValueFormatter() {
      override fun getFormattedValue(value: Float) =
        DecimalFormat("0.00").format(value)
    }

    // Line start
    pieDataSet.valueLinePart1OffsetPercentage = 80f
    // Radial length
    pieDataSet.valueLinePart1Length = 0.4f
    // Horizontal length
    pieDataSet.valueLinePart2Length = .2f
    pieDataSet.yValuePosition = PieDataSet.ValuePosition.OUTSIDE_SLICE

    val pieData = PieData(pieDataSet)
    view.summaryPieChart.data = pieData

    //view.summaryPieChart.setUsePercentValues(true)
    view.summaryPieChart.isDrawHoleEnabled = true

    val centerText = SpannableStringBuilder()
        .append("${context?.getString(R.string.summary_total_assets)} ")
        .underline { bold { append(DecimalFormat("0.00").format(totalAssets)) } }
    view.summaryPieChart.centerText = centerText

    view.summaryPieChart.description.isEnabled = false
    view.summaryPieChart.legend.orientation = Legend.LegendOrientation.VERTICAL
    view.summaryPieChart.legend.verticalAlignment = Legend.LegendVerticalAlignment.CENTER

    view.summaryPieChart.setExtraOffsets(0f, 3f, 26f, 4f)

    //val legendList: MutableList<LegendEntry> = mutableListOf()
    //legendList.add(LegendEntry("test", SQUARE, 10f, 100f, null, Color.RED))
    //view.summaryPieChart.legend.setCustom(legendList)

    view.summaryPieChart.invalidate()
  }
}
