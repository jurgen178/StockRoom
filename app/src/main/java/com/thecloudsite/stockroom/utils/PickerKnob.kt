/*
 * Copyright (C) 2020
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

package com.thecloudsite.stockroom.utils

import android.R.attr
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.Style.FILL
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import androidx.annotation.NonNull
import com.thecloudsite.stockroom.R.styleable
import java.text.DecimalFormat
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.ceil
import kotlin.math.cos

/*
http://eng.moldedbits.com/technical/android/2015/09/11/android-picker-knob.html

<com.thecloudsite.stockroom.utils.PickerKnob
android:id="@+id/picker_knob"
android:layout_width="match_parent"
android:layout_height="wrap_content"
app:picker_min_value="0"
app:picker_max_value="500"
app:picker_text_size="10sp"
app:picker_dash_gap="10dp"
app:picker_text_color="@android:color/black"/>


attrs.xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
  <declare-styleable name="PickerKnob">
    <attr name="picker_min_value" format="integer"/>
    <attr name="picker_max_value" format="integer"/>
    <attr name="picker_dash_gap" format="dimension"/>
    <attr name="picker_text_size" format="dimension"/>
    <attr name="picker_text_padding" format="dimension"/>
    <attr name="picker_text_color" format="integer"/>
    <attr name="picker_dash_count" format="integer"/>
    <attr name="picker_friction" format="float"/>
    <attr name="picker_start_value" format="integer"/>
  </declare-styleable>
</resources>
*/

class PickerKnob : View {
  /* Distance between dashes (in pixels)  */
  private var dashGap = 20

  /* View height including dash and text  */
  private var viewHeight = 0

  /* Height of the bigger dash  */
  private var dashHeight = 0

  /* Total view width  */
  private var viewWidth = 0

  /* Radius of the knob  */
  private var radius = 0f

  /* Used to draw to the canvas  */
  private var paint: Paint? = null

  /* Total number of dashes to draw  */
  private var totalDashCount = 0

  /* Current knob rotation  */
  private var knobRotation = 0f

  /* Initial velocity when the user flings the knob  */
  private var initVelocity = .5f

  /* Knob deceleration  */
  private var deceleration = 15f

  /* Track the system time to update knob position  */
  private var currentTime: Long = 0

  /* Minimum value for the knob. This can be set from the XML  */
  private var minValue: Double = 0.0

  /* Maximum value for the knob. This can be set from the XML  */
  private var maxValue: Double = 100.0

  private var minValueOrig: Double = 0.0
  private var maxValueOrig: Double = 0.0

  /* Count of smaller dashes between two larger dashes. This can be set from the XML  */
  private var dashCount = 4

  /* Maximum rotation allowed for the knob. This depends on the max value  */
  private var maxRotation = 0f

  /* Text size for the values on top  */
  private var textSize = 0

  /* Padding between the text and the dashes  */
  private var textPadding = 0

  /* Dash color  */
  private var lineColor = 0

  /* Text color  */
  private var textColor = 0

  /* Velocity tracker used to get fling velocities  */
  private var velocityTracker: VelocityTracker? = null

  /* X-coordinate of the down event  */
  private var touchStartX = 0

  /* Y-coordinate of the down event  */
  private var touchStartY = 0

  /* Current touch state  */
  private var touchState = TOUCH_STATE_RESTING

  /* Rotation of the point where the touch started  */
  private var touchStartAngle = 0.0

  /* Update listener  */
  private var startValue: Double = 0.0

  private var valueChangeListener: (Double) -> Unit = ({})

  fun onValueChangeListener(onValueUpdated: (Double) -> Unit) {
    valueChangeListener = onValueUpdated
  }

  /* Physics implementation  */
  private var dynamicsRunnable: Runnable = object : Runnable {
    override fun run() {
      if (abs(initVelocity) < VELOCITY_THRESHOLD) {
        return
      }
      val newTime = System.nanoTime()
      val deltaNano = newTime - currentTime
      val deltaSecs = deltaNano.toDouble() / 1000000000
      currentTime = newTime
      val finalVelocity: Float
      finalVelocity = if (initVelocity > 0) {
        (initVelocity - deceleration * deltaSecs).toFloat()
      } else {
        (initVelocity + deceleration * deltaSecs).toFloat()
      }
      if (initVelocity * finalVelocity < 0) {
        return
      }
      rotate(finalVelocity * deltaSecs)
      postDelayed(this, 1000 / 60.toLong())
      initVelocity = finalVelocity
    }
  }

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
    paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint!!.style = FILL
    paint!!.color = Color.BLUE
    currentTime = System.nanoTime()
    if (attrs != null) {
      val attrsArray = intArrayOf(
          attr.color
      )
      var a: TypedArray = context.theme.obtainStyledAttributes(attrs, attrsArray, 0, 0)
      lineColor = a.getColor(0, Color.GREEN)
      paint!!.color = lineColor
      a.recycle()
      a = context.theme.obtainStyledAttributes(attrs, styleable.PickerKnob, 0, 0)
      textSize = a.getDimensionPixelSize(styleable.PickerKnob_picker_text_size, 12)
      textPadding = a.getDimensionPixelSize(styleable.PickerKnob_picker_text_padding, 10)
      dashGap = a.getDimensionPixelSize(styleable.PickerKnob_picker_dash_gap, 20)
      textColor = a.getColor(styleable.PickerKnob_picker_text_color, Color.BLACK)
      dashCount = a.getInteger(styleable.PickerKnob_picker_dash_count, dashCount)
      deceleration = a.getFloat(styleable.PickerKnob_picker_friction, deceleration)
      a.recycle()
    }
    paint!!.textSize = textSize.toFloat()
    viewHeight = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, MIN_HEIGHT_IN_DP.toFloat(),
        context.resources.displayMetrics
    )
        .toInt() + textSize
    viewWidth = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, MIN_WIDTH_IN_DP.toFloat(),
        context.resources.displayMetrics
    )
        .toInt()
  }

  fun setValue(valueMin: Double, valueMax: Double, value: Double) {
    minValueOrig = valueMin
    maxValueOrig = valueMax

    startValue = (value - minValueOrig) * 100 / (maxValueOrig - minValueOrig)
    //val newValue = minValueOrig + value / 100 * (maxValueOrig - minValueOrig)
    valueChangeListener(value)
  }

  override fun onMeasure(
    widthMeasureSpec: Int,
    heightMeasureSpec: Int
  ) {
    val widthMode = MeasureSpec.getMode(widthMeasureSpec)
    val widthSize = MeasureSpec.getSize(widthMeasureSpec)
    val heightMode = MeasureSpec.getMode(heightMeasureSpec)
    val heightSize = MeasureSpec.getSize(heightMeasureSpec)
    val width: Int
    val height: Int

    // Measure Width
    width = when (widthMode) {
      MeasureSpec.EXACTLY -> {
        //Must be this size
        widthSize
      }
      MeasureSpec.AT_MOST -> {
        // Can't be bigger than...
        viewWidth.coerceAtMost(widthSize)
      }
      else -> {
        // Be whatever you want
        viewWidth
      }
    }

    // Measure Height
    height = when (heightMode) {
      MeasureSpec.EXACTLY -> {
        // Must be this size
        heightSize
      }
      MeasureSpec.AT_MOST -> {
        // Can't be bigger than...
        viewHeight.coerceAtMost(heightSize)
      }
      else -> {
        // Be whatever you want
        viewHeight
      }
    }
    setMeasuredDimension(width, height)
    updateCount()
  }

  private fun updateCount() {
    viewHeight = measuredHeight
    dashHeight = viewHeight - textSize - textPadding
    radius = measuredWidth / 2.toFloat()
    totalDashCount = (maxValue - minValue).toInt()
    val visibleDashCount = ceil(Math.PI * radius / dashGap)
        .toInt()
    maxRotation = (totalDashCount * Math.PI / visibleDashCount - Math.PI / 2).toFloat()
    knobRotation = (dashGap * (startValue - minValue) / radius - Math.PI / 2).toFloat()
  }

  override fun onDraw(canvas: Canvas) {
    var startPosition = ceil(radius * knobRotation / dashGap.toDouble())
        .toInt()
    startPosition = 0.coerceAtLeast(startPosition)
    var oldX = -1f
    while (true) {
      var theta = startPosition * dashGap / radius
      if (startPosition > totalDashCount) {
        break
      }
      theta -= knobRotation
      val x = (radius * (1 - cos(theta.toDouble()))).toFloat()
      if (x < oldX) {
        break
      }
      oldX = x
      if (startPosition % (dashCount + 1) == 0) {
        val value = getValueAtPosition(startPosition.toDouble())
        val newValue = minValueOrig + value / 100 * (maxValueOrig - minValueOrig)
        val text = DecimalFormat("0.00").format(newValue)
        val textWidth = paint!!.measureText(text)
        paint!!.color = textColor
        canvas.drawText(text, x - textWidth / 2, textSize.toFloat(), paint!!)
      }
      paint!!.color = lineColor
      canvas.drawLine(
          x,
          ((if (startPosition % (dashCount + 1) == 0) 0 else dashHeight / 2) + textSize + textPadding).toFloat(),
          x, viewHeight.toFloat(), paint!!
      )
      startPosition++
    }
  }

  override fun onTouchEvent(@NonNull event: MotionEvent): Boolean {

    // Prevent from scrolling the parent
    if (event.actionMasked == MotionEvent.ACTION_DOWN) {
      parent?.requestDisallowInterceptTouchEvent(true)
    }

    return when (event.action) {
      MotionEvent.ACTION_DOWN -> {
        startTouch(event)
        true
      }
      MotionEvent.ACTION_MOVE -> if (startScrollIfNeeded(event)) {
        processTouch(event)
        true
      } else {
        false
      }
      MotionEvent.ACTION_UP -> {
        processTouch(event)
        true
      }
      else -> false
    }
  }

  private fun processTouch(event: MotionEvent): Boolean {
    when (event.action) {
      MotionEvent.ACTION_MOVE -> {
        if (touchState == TOUCH_STATE_CLICK) {
          startScrollIfNeeded(event)
        }
        if (touchState == TOUCH_STATE_SCROLL) {
          velocityTracker?.addMovement(event)
          rotateOnTouch(
              event.x
                  .toInt()
          )
        }
      }
      MotionEvent.ACTION_UP -> {
        var velocity = 0f
        if (touchState == TOUCH_STATE_SCROLL) {
          velocityTracker?.addMovement(event)
          velocityTracker?.computeCurrentVelocity(RADIANS_PER_SECOND)
          velocity = if (velocityTracker != null) {
            -1 * velocityTracker!!.xVelocity
          } else {
            0f
          }
        }
        endTouch(velocity)
      }
    }
    return true
  }

  /*
   * Sets and initializes all things that need to when we start a touch
   * gesture.
   *
   * @param event The down event
   */
  private fun startTouch(event: MotionEvent) {
    // user is touching the list -> no more fling
    removeCallbacks(dynamicsRunnable)

    // save the start place
    touchStartX = event.x
        .toInt()
    touchStartY = event.y
        .toInt()
    touchStartAngle = acos((radius - touchStartX) / radius.toDouble())

    // obtain a velocity tracker and feed it its first event
    velocityTracker = VelocityTracker.obtain()
    (velocityTracker as VelocityTracker).addMovement(event)

    // we don't know if it's a click or a scroll yet, but until we know
    // assume it's a click
    touchState = TOUCH_STATE_CLICK
  }

  /*
   * Checks if the user has moved far enough for this to be a scroll and if
   * so, sets the list in scroll mode
   *
   * @param event The (move) event
   * @return true if scroll was started, false otherwise
   */
  private fun startScrollIfNeeded(event: MotionEvent): Boolean {
    val xPos = event.x
        .toInt()
    val yPos = event.y
        .toInt()
    if (xPos < touchStartX - TOUCH_SCROLL_THRESHOLD
        || xPos > touchStartX + TOUCH_SCROLL_THRESHOLD
        || yPos < touchStartY - TOUCH_SCROLL_THRESHOLD
        || yPos > touchStartY + TOUCH_SCROLL_THRESHOLD
    ) {
      // we've moved far enough for this to be a scroll
      touchState = TOUCH_STATE_SCROLL
      return true
    }
    return false
  }

  /*
   * Resets and recycles all things that need to when we end a touch gesture
   *
   * @param velocity The velocity of the gesture
   */
  private fun endTouch(velocity: Float) {
    // recycle the velocity tracker
    velocityTracker?.recycle()
    velocityTracker = null
    currentTime = System.nanoTime()
    initVelocity = velocity
    post(dynamicsRunnable)

    // reset touch state
    touchState = TOUCH_STATE_RESTING
  }

  private fun rotateOnTouch(finalX: Int) {
    var deltaX = radius - finalX
    if (deltaX > radius) {
      deltaX = radius
    }
    if (deltaX < -1 * radius) {
      deltaX = -1 * radius
    }
    val currentTouchAngle = acos(deltaX / radius.toDouble())
    val delta = touchStartAngle - currentTouchAngle
    touchStartAngle = currentTouchAngle
    rotate(delta)
  }

  private fun rotate(deltaTheta: Double) {
    knobRotation = (knobRotation + deltaTheta).toFloat()
    knobRotation = knobRotation.coerceAtLeast(MIN_ROTATION)
    knobRotation = knobRotation.coerceAtMost(maxRotation)
    invalidate()

    val position = ceil(radius * (knobRotation + Math.PI / 2) / dashGap)
    val value = getValueAtPosition(position)
    val newValue = minValueOrig + value / 100 * (maxValueOrig - minValueOrig)
    valueChangeListener(newValue)
  }

  private fun getValueAtPosition(position: Double): Double {
    return minValue + position
  }

  companion object {
    /* Unit used for the velocity tracker  */
    private const val RADIANS_PER_SECOND = 1

    /* Minimum height of the view  */
    private const val MIN_HEIGHT_IN_DP = 40

    /* Minimum width of the view  */
    private const val MIN_WIDTH_IN_DP = 150

    /* The velocity below which the knob will stop rotating  */
    private const val VELOCITY_THRESHOLD = 0.05f

    /* The left rotation threshold  */
    private const val MIN_ROTATION = (-1 * Math.PI).toFloat() / 2

    /* User is not touching the list  */
    private const val TOUCH_STATE_RESTING = 0

    /* User is touching the list and right now it's still a "click"  */
    private const val TOUCH_STATE_CLICK = 1

    /* User is scrolling the list  */
    private const val TOUCH_STATE_SCROLL = 2

    /* Distance to drag before we intercept touch events  */
    private const val TOUCH_SCROLL_THRESHOLD = 10
  }
}
