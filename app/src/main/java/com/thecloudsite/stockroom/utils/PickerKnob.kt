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

package com.thecloudsite.stockroom

import android.R.attr
import android.R.attr.centerX
import android.R.attr.centerY
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
import kotlin.math.abs
import kotlin.math.ceil

/*
http://eng.moldedbits.com/technical/android/2015/09/11/android-picker-knob.html

<com.thecloudsite.stockroom.PickerKnob
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
  /** Distance between dashes (in pixels)  */
  private var mDashGap = 20

  /** View height including dash and text  */
  private var mViewHeight = 0

  /** Height of the bigger dash  */
  private var mDashHeight = 0

  /** Total view width  */
  private var mViewWidth = 0

  /** Radius of the knob  */
  private var mRadius = 0f

  /** Used to draw to the canvas  */
  private var mPaint: Paint? = null

  /** Total number of dashes to draw  */
  private var mTotalDashCount = 0

  /** Current knob rotation  */
  private var mRotation = 0f

  /** Initial velocity when the user flings the knob  */
  private var mInitVelocity = .5f

  /** Knob deceleration  */
  private var mDeceleration = 15f

  /** Track the system time to update knob position  */
  private var mCurrentTime: Long = 0

  /** Minimum value for the knob. This can be set from the XML  */
  private var mMinValue = 0

  /** Maximum value for the knob. This can be set from the XML  */
  private var mMaxValue = 10

  /** Count of smaller dashes between two larger dashes. This can be set from the XML  */
  private var mDashCount = 4

  /** Maximum rotation allowed for the knob. This depends on the max value  */
  private var mMaxRotation = 0f

  /** Text size for the values on top  */
  private var mTextSize = 0

  /** Padding between the text and the dashes  */
  private var mTextPadding = 0

  /** Dash color  */
  private var mLineColor = 0

  /** Text color  */
  private var mTextColor = 0

  /** Velocity tracker used to get fling velocities  */
  private var mVelocityTracker: VelocityTracker? = null

  /** X-coordinate of the down event  */
  private var mTouchStartX = 0

  /** Y-coordinate of the down event  */
  private var mTouchStartY = 0

  /** Current touch state  */
  private var mTouchState = TOUCH_STATE_RESTING

  /** Rotation of the point where the touch started  */
  private var mTouchStartAngle = 0.0

  /** Update listener  */
  private var mUpdateListener: OnValueChangeListener? = null
  private var mStartValue = 0

  interface OnValueChangeListener {
    fun onValueUpdated(newValue: Int)
  }

  /** Physics implementation  */
  var mDynamicsRunnable: Runnable = object : Runnable {
    override fun run() {
      if (abs(mInitVelocity) < VELOCITY_THRESHOLD) {
        return
      }
      val newTime = System.nanoTime()
      val deltaNano = newTime - mCurrentTime
      val deltaSecs = deltaNano.toDouble() / 1000000000
      mCurrentTime = newTime
      val finalVelocity: Float
      finalVelocity = if (mInitVelocity > 0) {
        (mInitVelocity - mDeceleration * deltaSecs).toFloat()
      } else {
        (mInitVelocity + mDeceleration * deltaSecs).toFloat()
      }
      if (mInitVelocity * finalVelocity < 0) {
        return
      }
      rotate(finalVelocity * deltaSecs)
      postDelayed(this, 1000 / 60.toLong())
      mInitVelocity = finalVelocity
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

  fun setPositionListener(listener: OnValueChangeListener?) {
    mUpdateListener = listener
  }

  constructor(
    context: Context,
    attrs: AttributeSet?,
    defStyleAttr: Int,
    defStyleRes: Int
  ) : super(context, attrs, defStyleAttr, defStyleRes) {
    init(context, attrs)
  }

  private fun init(
    context: Context,
    attrs: AttributeSet?
  ) {
    mPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    mPaint!!.style = FILL
    mPaint!!.color = Color.BLUE
    mCurrentTime = System.nanoTime()
    if (attrs != null) {
      val attrsArray = intArrayOf(
          attr.color
      )
      var a: TypedArray = context.theme.obtainStyledAttributes(attrs, attrsArray, 0, 0)
      mLineColor = a.getColor(0, Color.GREEN)
      mPaint!!.color = mLineColor
      a.recycle()
      a = context.theme.obtainStyledAttributes(attrs, styleable.PickerKnob, 0, 0)
      mMinValue = a.getInt(styleable.PickerKnob_picker_min_value, mMinValue)
      mMaxValue = a.getInt(styleable.PickerKnob_picker_max_value, mMaxValue)
      mTextSize = a.getDimensionPixelSize(styleable.PickerKnob_picker_text_size, 12)
      mTextPadding = a.getDimensionPixelSize(styleable.PickerKnob_picker_text_padding, 10)
      mDashGap = a.getDimensionPixelSize(styleable.PickerKnob_picker_dash_gap, 20)
      mTextColor = a.getColor(styleable.PickerKnob_picker_text_color, Color.BLACK)
      mDashCount = a.getInteger(styleable.PickerKnob_picker_dash_count, mDashCount)
      mDeceleration = a.getFloat(styleable.PickerKnob_picker_friction, mDeceleration)
      mStartValue = a.getInt(
          styleable.PickerKnob_picker_start_value,
          (mMinValue + mMaxValue) / 2
      )
      a.recycle()
    }
    mPaint!!.textSize = mTextSize.toFloat()
    mViewHeight = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, MIN_HEIGHT_IN_DP.toFloat(),
        context.resources.displayMetrics
    )
        .toInt() + mTextSize
    mViewWidth = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, MIN_WIDTH_IN_DP.toFloat(),
        context.resources.displayMetrics
    )
        .toInt()
  }

  fun setValue(value: Int) {
    if (value <= mMaxValue && value >= mMinValue) {
      mStartValue = value
    }
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

    //Measure Width
    width = when (widthMode) {
      MeasureSpec.EXACTLY -> {
        //Must be this size
        widthSize
      }
      MeasureSpec.AT_MOST -> {
        //Can't be bigger than...
        Math.min(mViewWidth, widthSize)
      }
      else -> {
        //Be whatever you want
        mViewWidth
      }
    }

    //Measure Height
    height = if (heightMode == MeasureSpec.EXACTLY) {
      //Must be this size
      heightSize
    } else if (heightMode == MeasureSpec.AT_MOST) {
      //Can't be bigger than...
      Math.min(mViewHeight, heightSize)
    } else {
      //Be whatever you want
      mViewHeight
    }
    setMeasuredDimension(width, height)
    updateCount()
  }

  private fun updateCount() {
    mViewHeight = measuredHeight
    mDashHeight = mViewHeight - mTextSize - mTextPadding
    mRadius = measuredWidth / 2.toFloat()
    mTotalDashCount = mMaxValue - mMinValue
    val visibleDashCount = ceil(Math.PI * mRadius / mDashGap)
        .toInt()
    mMaxRotation = (mTotalDashCount * Math.PI / visibleDashCount - Math.PI / 2).toFloat()
    mRotation = (mDashGap * (mStartValue - mMinValue) / mRadius - Math.PI / 2).toFloat()
  }

  override fun onDraw(canvas: Canvas) {
    var startPosition = Math.ceil(mRadius * mRotation / mDashGap.toDouble())
        .toInt()
    startPosition = Math.max(0, startPosition)
    var oldX = -1f
    while (true) {
      var theta = startPosition * mDashGap / mRadius
      if (startPosition > mTotalDashCount) {
        break
      }
      theta = theta - mRotation
      val x = (mRadius * (1 - Math.cos(theta.toDouble()))).toFloat()
      if (x < oldX) {
        break
      }
      oldX = x
      if (startPosition % (mDashCount + 1) == 0) {
        val text = getValueAtPosition(startPosition).toString()
        val textWidth = mPaint!!.measureText(text)
        mPaint!!.color = mTextColor
        canvas.drawText(text, x - textWidth / 2, mTextSize.toFloat(), mPaint!!)
      }
      mPaint!!.color = mLineColor
      canvas.drawLine(
          x,
          ((if (startPosition % (mDashCount + 1) == 0) 0 else mDashHeight / 2) + mTextSize + mTextPadding).toFloat(),
          x, mViewHeight.toFloat(), mPaint!!
      )
      startPosition++
    }
  }

  override fun onTouchEvent(@NonNull event: MotionEvent): Boolean {
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

  fun processTouch(event: MotionEvent): Boolean {
    when (event.action) {
      MotionEvent.ACTION_MOVE -> {
        if (mTouchState == TOUCH_STATE_CLICK) {
          startScrollIfNeeded(event)
        }
        if (mTouchState == TOUCH_STATE_SCROLL) {
          mVelocityTracker?.addMovement(event)
          rotateOnTouch(
              event.x
                  .toInt()
          )
        }
      }
      MotionEvent.ACTION_UP -> {
        var velocity = 0f
        if (mTouchState == TOUCH_STATE_SCROLL) {
          mVelocityTracker?.addMovement(event)
          mVelocityTracker?.computeCurrentVelocity(RADIANS_PER_SECOND)
          velocity = if (mVelocityTracker != null) {
            -1 * mVelocityTracker!!.getXVelocity()
          } else {
            0f
          }
        }
        endTouch(velocity)
      }
    }
    return true
  }

  /**
   * Sets and initializes all things that need to when we start a touch
   * gesture.
   *
   * @param event The down event
   */
  private fun startTouch(event: MotionEvent) {
    // user is touching the list -> no more fling
    removeCallbacks(mDynamicsRunnable)

    // save the start place
    mTouchStartX = event.x
        .toInt()
    mTouchStartY = event.y
        .toInt()
    mTouchStartAngle = Math.acos((mRadius - mTouchStartX) / mRadius.toDouble())

    // obtain a velocity tracker and feed it its first event
    mVelocityTracker = VelocityTracker.obtain()
    (mVelocityTracker as VelocityTracker).addMovement(event)

    // we don't know if it's a click or a scroll yet, but until we know
    // assume it's a click
    mTouchState = TOUCH_STATE_CLICK
  }

  /**
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
    if (xPos < mTouchStartX - TOUCH_SCROLL_THRESHOLD || xPos > mTouchStartX + TOUCH_SCROLL_THRESHOLD || yPos < mTouchStartY - TOUCH_SCROLL_THRESHOLD || yPos > mTouchStartY + TOUCH_SCROLL_THRESHOLD) {
      // we've moved far enough for this to be a scroll
      mTouchState = TOUCH_STATE_SCROLL
      return true
    }
    return false
  }

  /**
   * Resets and recycles all things that need to when we end a touch gesture
   *
   * @param velocity The velocity of the gesture
   */
  private fun endTouch(velocity: Float) {
    // recycle the velocity tracker
    mVelocityTracker?.recycle()
    mVelocityTracker = null
    mCurrentTime = System.nanoTime()
    mInitVelocity = velocity
    post(mDynamicsRunnable)

    // reset touch state
    mTouchState = TOUCH_STATE_RESTING
  }

  private fun rotateOnTouch(finalX: Int) {
    var deltaX = mRadius - finalX
    if (deltaX > mRadius) {
      deltaX = mRadius
    }
    if (deltaX < -1 * mRadius) {
      deltaX = -1 * mRadius
    }
    val currentTouchAngle = Math.acos(deltaX / mRadius.toDouble())
    val delta = mTouchStartAngle - currentTouchAngle
    mTouchStartAngle = currentTouchAngle
    rotate(delta)
  }

  private fun rotate(deltaTheta: Double) {
    mRotation = (mRotation + deltaTheta).toFloat()
    mRotation = Math.max(mRotation, MIN_ROTATION)
    mRotation = Math.min(mRotation, mMaxRotation)
    invalidate()
    if (mUpdateListener != null) {
      val position = Math.ceil(mRadius * (mRotation + Math.PI / 2) / mDashGap)
          .toInt()
      mUpdateListener!!.onValueUpdated(getValueAtPosition(position))
    }
  }

  private fun getValueAtPosition(position: Int): Int {
    return mMinValue + position
  }

  companion object {
    /** Unit used for the velocity tracker  */
    private const val RADIANS_PER_SECOND = 1

    /** Minimum height of the view  */
    private const val MIN_HEIGHT_IN_DP = 30

    /** Minimum width of the view  */
    private const val MIN_WIDTH_IN_DP = 150

    /** The velocity below which the knob will stop rotating  */
    private const val VELOCITY_THRESHOLD = 0.05f

    /** The left rotation threshold  */
    private const val MIN_ROTATION = (-1 * Math.PI).toFloat() / 2

    /** User is not touching the list  */
    private const val TOUCH_STATE_RESTING = 0

    /** User is touching the list and right now it's still a "click"  */
    private const val TOUCH_STATE_CLICK = 1

    /** User is scrolling the list  */
    private const val TOUCH_STATE_SCROLL = 2

    /** Distance to drag before we intercept touch events  */
    private const val TOUCH_SCROLL_THRESHOLD = 10
  }
}
