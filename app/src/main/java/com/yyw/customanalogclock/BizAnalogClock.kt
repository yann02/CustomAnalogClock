package com.yyw.customanalogclock

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.text.format.DateUtils
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import java.util.*
import kotlin.math.cos
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
    private var mHandMinuteLength = 110f  //  分针长度
    private var mThinHandHourLength = 30f  //  细针的长度
    private var mOverHandSecondLength = 20f  //  秒针超出圆心部分的长度


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
        mTime.timeInMillis = System.currentTimeMillis()
        mHour = mTime[Calendar.HOUR]
        mMinute = mTime[Calendar.MINUTE]
        mSecond = mTime[Calendar.SECOND]
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_TIME_TICK)
        filter.addAction(Intent.ACTION_TIME_CHANGED)
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED)
        context.registerReceiver(mIntentReceiver, filter)
        onTimeChanged()
        mClockTick.run()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        context.unregisterReceiver(mIntentReceiver)
        removeCallbacks(mClockTick)
    }

    private var mCenterX: Float = 0f
    private var mCenterY: Float = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mCenterX = w / 2f
        mCenterY = h / 2f
    }

    private val mPaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 15f
        strokeCap = Paint.Cap.ROUND
    }

    private val mThinPaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 8f
    }

    private val mSecondPaint = Paint(ANTI_ALIAS_FLAG).apply {
        color = Color.RED
        strokeWidth = 4f
    }


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