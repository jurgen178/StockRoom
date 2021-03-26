package com.thecloudsite.stockroom.timeline.ext

import android.view.View

fun View.shouldUseLayoutRtl(): Boolean {
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
        View.LAYOUT_DIRECTION_RTL == this.layoutDirection
    } else {
        false
    }
}