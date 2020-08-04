package com.android.stockroom

import android.content.res.Resources
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.RawRes
import androidx.lifecycle.ViewModelProvider
import kotlinx.android.synthetic.main.fragment_debug.webview

// https://kotlin-android.com/android-webview-kotlin/

class DebugFragment : Fragment() {

  companion object {
    fun newInstance() = DebugFragment()
  }

  private lateinit var debugViewModel: DebugViewModel

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_debug, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)

    // webview.loadUrl("file:///android_asset/html/debug.html")
    val htmlText = resources.getRawTextFile(R.raw.debug)
    val mimeType: String = "text/html"
    val utfType: String = "UTF-8"
    webview.loadDataWithBaseURL(null, htmlText, mimeType, utfType, null);
  }

  fun Resources.getRawTextFile(@RawRes id: Int) =
    openRawResource(id).bufferedReader()
        .use { it.readText() }

  override fun onActivityCreated(savedInstanceState: Bundle?) {
    super.onActivityCreated(savedInstanceState)
    debugViewModel = ViewModelProvider(this).get(DebugViewModel::class.java)
  }
}