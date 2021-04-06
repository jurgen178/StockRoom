package com.thecloudsite.stockroom.timeline.ext

import android.view.View

fun View.shouldUseLayoutRtl(): Boolean {
    return View.LAYOUT_DIRECTION_RTL == this.layoutDirection
}