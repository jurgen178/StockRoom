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

import android.R.layout
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.ArrayAdapter
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.thecloudsite.stockroom.databinding.ActivityAddBinding
import com.thecloudsite.stockroom.list.ListActivity
import java.util.Locale

/*
 * Activity for entering a new symbol.
 */

class GetImportContent : ActivityResultContracts.GetContent() {
  override fun createIntent(context: Context, input: String): Intent {
    val mimeTypes = arrayOf(

      // .json
      "application/json",
      "text/x-json",

      // .csv
      "text/csv",
      "text/comma-separated-values",
      "application/octet-stream",

      // .txt
      "text/plain"
    )

    return super.createIntent(context, input)
      .setType("*/*")
      .setAction(Intent.ACTION_OPEN_DOCUMENT)
      .putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
  }
}

class AddActivity : AppCompatActivity() {

  private lateinit var binding: ActivityAddBinding
  private lateinit var addView: EditText
  private lateinit var importRequest: ActivityResultLauncher<String>

  private lateinit var stockRoomViewModel: StockRoomViewModel
  private lateinit var dataProviderSymbolsViewModelCoingecko: DataProviderSymbolsViewModel
  private var dataProviderSymbolsCoingecko: List<DataProviderSymbolEntry> = emptyList()

  private lateinit var dataProviderSymbolsViewModelCoinpaprika: DataProviderSymbolsViewModel
  private var dataProviderSymbolsCoinpaprika: List<DataProviderSymbolEntry> = emptyList()

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding = ActivityAddBinding.inflate(layoutInflater)
    val view = binding.root
    setContentView(view)

    addView = binding.editAdd
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    // Crashlytics test
    //throw RuntimeException("Test Crash") // Force a crash

    stockRoomViewModel = ViewModelProvider(this).get(StockRoomViewModel::class.java)

    dataProviderSymbolsViewModelCoingecko = ViewModelProvider(this).get(DataProviderSymbolsViewModel::class.java)

    dataProviderSymbolsViewModelCoingecko.symbols.observe(this, Observer { symbols ->
      this.dataProviderSymbolsCoingecko = symbols

      // maxL is 50 for coingecko ids
      // see isValidSymbol
//      val maxL = cryptoSymbols.maxOf { symbol ->
//        symbol.id.length
//      }

      binding.symbolsSpinnerCoingecko.adapter =
        ArrayAdapter(this, layout.simple_list_item_1, this.dataProviderSymbolsCoingecko.map { symbolEntry ->
          symbolEntry.name
        })
    })

    dataProviderSymbolsViewModelCoingecko.getData { CoingeckoSymbolsApiFactory.coingeckoApi }

    binding.symbolsSpinnerCoingecko.onItemSelectedListener = object : OnItemSelectedListener {
      override fun onNothingSelected(parent: AdapterView<*>?) {
      }

      override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
      ) {
        binding.editAdd.setText(dataProviderSymbolsCoingecko[position].id)
      }
    }


    dataProviderSymbolsViewModelCoinpaprika = ViewModelProvider(this).get(DataProviderSymbolsViewModel::class.java)

    dataProviderSymbolsViewModelCoinpaprika.symbols.observe(this, Observer { symbols ->
      this.dataProviderSymbolsCoinpaprika = symbols.sortedBy { symbol -> symbol.name }

      // maxL is 50 for coingecko ids
      // see isValidSymbol
//      val maxL = cryptoSymbols.maxOf { symbol ->
//        symbol.id.length
//      }

      binding.symbolsSpinnerCoinpaprika.adapter =
        ArrayAdapter(this, layout.simple_list_item_1, this.dataProviderSymbolsCoinpaprika.map { symbolEntry ->
          symbolEntry.name
        })
    })

    dataProviderSymbolsViewModelCoinpaprika.getData { CoinpaprikaSymbolsApiFactory.coinpaprikaApi }

    binding.symbolsSpinnerCoinpaprika.onItemSelectedListener = object : OnItemSelectedListener {
      override fun onNothingSelected(parent: AdapterView<*>?) {
      }

      override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
      ) {
        binding.editAdd.setText(dataProviderSymbolsCoinpaprika[position].id)
      }
    }


    binding.dataproviderSpinner.onItemSelectedListener = object : OnItemSelectedListener {
      override fun onNothingSelected(parent: AdapterView<*>?) {
      }

      override fun onItemSelected(
        parent: AdapterView<*>?,
        view: View?,
        position: Int,
        id: Long
      ) {
        binding.symbolsSpinnerCoingecko.visibility = if (position == 1) {
          View.VISIBLE
        } else {
          View.GONE
        }
        binding.symbolsSpinnerCoinpaprika.visibility = if (position == 2) {
          View.VISIBLE
        } else {
          View.GONE
        }
      }
    }

    binding.buttonAdd.setOnClickListener {
      val replyIntent = Intent()
      if (TextUtils.isEmpty(addView.text)) {
        setResult(Activity.RESULT_CANCELED, replyIntent)
      } else {
        val symbol = addView.text.toString()
          .trim()

        val type = binding.dataproviderSpinner.selectedItemPosition

        // https://convertcodes.com/unicode-converter-encode-decode-utf/
        if (symbol.lowercase(Locale.ROOT) == "\u0064\u0065\u0062\u0075\u0067") {
          val intent = Intent(this@AddActivity, ListActivity::class.java)
          startActivity(intent)
          setResult(Activity.RESULT_CANCELED, replyIntent)
        } else {
          replyIntent.putExtra(EXTRA_SYMBOL, symbol)
          replyIntent.putExtra(EXTRA_TYPE, type)
          setResult(Activity.RESULT_OK, replyIntent)
        }
      }
      finish()
    }

    binding.importButton.setOnClickListener {
      // match importList()

      importRequest.launch(getString(R.string.import_select_file))
    }

    importRequest =
      registerForActivityResult(GetImportContent())
      { uri ->
        stockRoomViewModel.importList(applicationContext, uri)
        finish()
      }
  }

  override fun onSupportNavigateUp(): Boolean {
    onBackPressed()
    return true
  }
}

