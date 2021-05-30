package com.thecloudsite.stockroom.utils

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.*

class RotaryControl : View {

  private var currentAngle = 0.0
  private var totalAngle = 0.0
  private var prevAngle = 0.0

  //    private lateinit var paint: Paint
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

  private fun init(
    context: Context,
    attrs: AttributeSet?
  ) {
//        paint = Paint(Paint.ANTI_ALIAS_FLAG)
//        paint.style = Paint.Style.FILL
//        paint.color = Color.GRAY
//        paint.textSize = 40f

    paintC1 = Paint(Paint.ANTI_ALIAS_FLAG)
    paintC1.style = Paint.Style.FILL
    paintC1.color = Color.LTGRAY

    paintC2 = Paint(Paint.ANTI_ALIAS_FLAG)
    paintC2.style = Paint.Style.FILL
    paintC2.color = Color.DKGRAY
  }

  private var valueChangeListener: (Double) -> Unit = ({})

  fun onValueChangeListener(onValueUpdated: (Double) -> Unit) {
    valueChangeListener = onValueUpdated
  }

  override fun onDraw(canvas: Canvas) {
    val r = min(width, height).toFloat() / 2f
    canvas.drawCircle(width / 2f, height / 2f, r, paintC1)

    val a = currentAngle
    val rr = r * 0.75f
    val x = rr * sin(a * 2.0 * Math.PI / 360.0).toFloat()
    val y = rr * cos(a * 2.0 * Math.PI / 360.0).toFloat()
    canvas.drawCircle(width / 2f + x, height / 2f - y, r * 0.15f, paintC2)

//         canvas.drawText("${rot.toInt()}", 20f, 40f, paint)
  }

  private fun calculateAngle(x: Float, y: Float): Double {
    val px = (x / width) - 0.5
    val py = (y / height) - 0.5

    // move origin to top position (+90.0)
    var angle = atan2(py, px) * 180.0 / Math.PI + 90.0 + 360.0

    // map range to 0..360
    if (angle > 360.0) {
      angle -= 360.0
    }
    return angle
  }

  override fun onTouchEvent(event: MotionEvent): Boolean {

    prevAngle = currentAngle
    currentAngle = calculateAngle(event.x, event.y)

    val diff = currentAngle - prevAngle

    totalAngle += diff +
            when {

              // transition from pos to neg
              diff > 180 -> {
                -360.0
              }

              // transition from neg to pos
              diff < -180 -> {
                360.0
              }

              else -> {
                0.0
              }
            }

    valueChangeListener(totalAngle)
    invalidate()

    // Prevent from scrolling the parent
    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
      parent?.requestDisallowInterceptTouchEvent(true)
      return true
    }

    return super.onTouchEvent(event)
  }
}
