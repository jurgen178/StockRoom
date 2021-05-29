package com.thecloudsite.stockroom.utils

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.GestureDetectorCompat
import java.text.DecimalFormat
import kotlin.math.*


class RotaryControl : View {
    var rot = 0.0
    var rotationDegrees = 0.0

    private lateinit var paint: Paint
    private lateinit var paintC1: Paint
    private lateinit var paintC2: Paint

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?
    ) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr) {
        init(context, attrs)
    }

    private fun calculateAngle(x: Float, y: Float): Double {
        val px = (x / width) - 0.5
        val py = (1.0 - y / height) - 0.5
        var angle = -(Math.toDegrees(atan2(py, px))) + 90.0
        if (angle > 180.0) angle -= 360.0
        return angle
    }

    private fun init(
        context: Context,
        attrs: AttributeSet?
    ) {
        paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.style = Paint.Style.FILL
        paint.color = Color.GRAY
        paint.textSize = 40f

        paintC1 = Paint(Paint.ANTI_ALIAS_FLAG)
        paintC1.style = Paint.Style.FILL
        paintC1.color = Color.DKGRAY

        paintC2 = Paint(Paint.ANTI_ALIAS_FLAG)
        paintC2.style = Paint.Style.FILL
        paintC2.color = Color.LTGRAY
    }

    private var valueChangeListener: (Double) -> Unit = ({})

    fun onValueChangeListener(onValueUpdated: (Double) -> Unit) {
        valueChangeListener = onValueUpdated
    }

    override fun onDraw(canvas: Canvas) {
        val r = min(width, height).toFloat() / 2f
        canvas.drawCircle(width / 2f, height / 2f, r, paintC1)

        val rr = r * 0.8f
        val x = rr * sin(rotationDegrees * 2.0 * Math.PI / 360.0).toFloat()
        val y = rr * cos(rotationDegrees * 2.0 * Math.PI / 360.0).toFloat()
        canvas.drawCircle(width / 2f + x, height / 2f - y, r * 0.1f, paintC2)

        canvas.drawText("$rot", 20f, 40f, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {

        rotationDegrees = calculateAngle(event.x, event.y)
        rot += rotationDegrees
        valueChangeListener(rot)
        invalidate()

        // Prevent from scrolling the parent
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            parent?.requestDisallowInterceptTouchEvent(true)
            return true
        }

        return super.onTouchEvent(event)
    }
}
