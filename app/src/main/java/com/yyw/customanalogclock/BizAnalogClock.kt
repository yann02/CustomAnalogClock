package com.yyw.customanalogclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.text.format.DateUtils
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import java.util.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

const val ARC_LENGTH = Math.PI * 2
const val CLOCK_HOUR_HAND_ARC_LENGTH = ARC_LENGTH / 12
const val CLOCK_MINUTE_HAND_ARC_LENGTH = ARC_LENGTH / 60
const val CLOCK_START_ARC = Math.PI / 2

class BizAnalogClock(context: Context, att: AttributeSet) : AppCompatImageView(context, att) {
    private var mTimeZone: TimeZone? = null
    private var mTime = Calendar.getInstance(TimeZone.getDefault())
    private var mHour: Int = 0
    private var mMinute: Int = 0
    private var mSecond: Int = 0

    private var mHandHourLength = 90f  //  时针长度
    private var mHandMinuteLength = 0f  //  分针长度
    private var mThinHandHourLength = 0f  //  细针的长度
    private var mOverHandSecondLength = 0f  //  秒针超出圆心部分的长度


    init {
        background = ContextCompat.getDrawable(context, R.mipmap.home_alarm_bg)
    }

    private val mIntentReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mTimeZone == null && Intent.ACTION_TIMEZONE_CHANGED == intent.action) {
                val tz = intent.getStringExtra("time-zone")
                mTime = Calendar.getInstance(TimeZone.getTimeZone(tz))
            }
            onTimeChanged()
        }
    }

    private val mClockTick: Runnable = object : Runnable {
        override fun run() {
            onTimeChanged()
            val now = System.currentTimeMillis()
            val delay = DateUtils.SECOND_IN_MILLIS - now % DateUtils.SECOND_IN_MILLIS
            postDelayed(this, delay)
        }
    }

    private fun onTimeChanged() {
        updateTime()
        invalidate()
    }

    private fun updateTime() {
        mTime.timeInMillis = System.currentTimeMillis()
        mHour = mTime[Calendar.HOUR]
        mMinute = mTime[Calendar.MINUTE]
        mSecond = mTime[Calendar.SECOND]
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        registerTimeChangeReceiver()
        onTimeChanged()
        mClockTick.run()
    }

    private fun registerTimeChangeReceiver() {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_TIME_TICK)
        filter.addAction(Intent.ACTION_TIME_CHANGED)
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
        context.registerReceiver(mIntentReceiver, filter)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        context.unregisterReceiver(mIntentReceiver)
        removeCallbacks(mClockTick)
    }

    private var mCenterX: Float = 0f
    private var mCenterY: Float = 0f
    private var mRadius: Float = 0f
    private val mRectF: RectF = RectF()

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCenterX = w / 2f
        mCenterY = h / 2f
        mRadius = min(w, h) / 2f
        mHandMinuteLength = mRadius - 15
        mHandHourLength = mHandMinuteLength / 2 + 6
        mThinHandHourLength = mHandHourLength / 3
        mOverHandSecondLength = mThinHandHourLength
        mRectF.apply {
            left = mCenterX - 5.5f
            top = mCenterY - 5.5f
            right = mCenterX + 5.5f
            bottom = mCenterY + 5.5f
        }
    }

    private val mAntiPaint = Paint(ANTI_ALIAS_FLAG)
    private val mWhitePaint = Paint(mAntiPaint).apply {
        color = Color.WHITE
    }

    private val mPaint = Paint(mWhitePaint).apply {
        strokeWidth = 9.3f
        strokeCap = Paint.Cap.ROUND
    }

    private val mThinPaint = Paint(mWhitePaint).apply {
        strokeWidth = 4f
    }

    private val mSecondPaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = 3f
    }

    private val centerCircleBitmap = BitmapFactory.decodeResource(resources, R.mipmap.clock_center)


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        canvas?.let {
            drawHandHour(it)
            drawHandMinute(it)
            drawHandSecond(it)
            drawInnerCircle(it)
        }
    }

    private fun drawInnerCircle(canvas: Canvas) {
        canvas.drawBitmap(centerCircleBitmap, null, mRectF, mAntiPaint)
    }

    private fun drawHandSecond(canvas: Canvas) {
        val angle = (mSecond * CLOCK_MINUTE_HAND_ARC_LENGTH - CLOCK_START_ARC).toFloat()
        val overAngle = ((mSecond + 30) % 60 * CLOCK_MINUTE_HAND_ARC_LENGTH - CLOCK_START_ARC).toFloat()
        val endX = mCenterX * cos(angle) + mCenterX
        val endY = mCenterY * sin(angle) + mCenterY
        val startX = mOverHandSecondLength * cos(overAngle) + mCenterX
        val startY = mOverHandSecondLength * sin(overAngle) + mCenterY
        canvas.drawLine(startX, startY, endX, endY, mSecondPaint)
    }

    private fun drawHandMinute(canvas: Canvas) {
        val angle = (mMinute * CLOCK_MINUTE_HAND_ARC_LENGTH - CLOCK_START_ARC).toFloat()
        val minuteEndX = mHandMinuteLength * cos(angle) + mCenterX
        val minuteEndY = mHandMinuteLength * sin(angle) + mCenterY

        val startX = mThinHandHourLength * cos(angle) + mCenterX
        val startY = mThinHandHourLength * sin(angle) + mCenterY
        canvas.drawLine(mCenterX, mCenterY, startX, startY, mThinPaint)
        canvas.drawLine(startX, startY, minuteEndX, minuteEndY, mPaint)
    }

    private fun drawHandHour(canvas: Canvas) {
        val angle = ((mHour + mMinute / 60f) * CLOCK_HOUR_HAND_ARC_LENGTH - CLOCK_START_ARC).toFloat()
        val hourEndX = mHandHourLength * cos(angle) + mCenterX
        val hourEndY = mHandHourLength * sin(angle) + mCenterY

        val hourStartX = mThinHandHourLength * cos(angle) + mCenterX
        val hourStartY = mThinHandHourLength * sin(angle) + mCenterY
        canvas.drawLine(mCenterX, mCenterY, hourStartX, hourStartY, mThinPaint)
        canvas.drawLine(hourStartX, hourStartY, hourEndX, hourEndY, mPaint)
    }

}