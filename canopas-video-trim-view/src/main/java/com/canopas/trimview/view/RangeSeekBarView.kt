package com.canopas.trimview.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import com.canopas.trimview.R
import com.canopas.trimview.interfaces.OnRangeSeekBarListener
import java.util.*

class RangeSeekBarView @JvmOverloads constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {
    private var mHeightTimeLine = 0

    var thumbs: List<Thumb>

    private var mListeners: MutableList<OnRangeSeekBarListener> = ArrayList()

    private var mMaxWidth = 0f
    private var mThumbWidth = 0f
    private var mViewWidth = 0
    private var mPixelRangeMin = 0f
    private var mPixelRangeMax = 0f
    private var mScaleRangeMax = 0f
    private var mFirstRun = false

    @ColorInt
    private var shadowColor = 0

    @ColorInt
    private var thumbColor = 0
    private var currentThumb = 0
    private val mShadow = Paint()
    private val mLine = Paint()
    private val mThumbPaint = Paint()

    init {
        val styleAttrs = context.theme.obtainStyledAttributes(attrs, R.styleable.VideoTrimView, 0, 0)
        @ColorInt val progressShadowColor = styleAttrs.getColor(R.styleable.VideoTrimView_progressShadowColor,
                resources.getColor(R.color.shadow_color))
        @ColorInt val thumbColor = styleAttrs.getColor(R.styleable.VideoTrimView_thumbTint,
                resources.getColor(R.color.thumb_color))

        setShadowColor(progressShadowColor)
        setThumbColor(thumbColor)
        styleAttrs.recycle()

        thumbs = Thumb.initThumbs(resources)
        mThumbWidth = resources.getDimension(R.dimen.thumb_width)

        mScaleRangeMax = 100f
        mHeightTimeLine = getContext().resources.getDimensionPixelOffset(R.dimen.frames_video_height)
        isFocusable = true
        isFocusableInTouchMode = true
        mFirstRun = true

        mShadow.isAntiAlias = true
        mShadow.color = shadowColor

        mThumbPaint.isAntiAlias = true
        mThumbPaint.color = thumbColor
        mThumbPaint.style = Paint.Style.FILL

        mLine.isAntiAlias = true
        mLine.color = thumbColor
        mLine.style = Paint.Style.STROKE
        mLine.strokeWidth = 20f
    }

    private fun setShadowColor(shadowColor: Int) {
        this.shadowColor = shadowColor
    }

    private fun setThumbColor(thumbColor: Int) {
        this.thumbColor = thumbColor
    }

    fun initMaxWidth() {
        mMaxWidth = mViewWidth.toFloat()
        onSeekStop(0, thumbs[0].scale)
        onSeekStop(1, thumbs[1].scale)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val minW = paddingLeft + paddingRight + suggestedMinimumWidth
        mViewWidth = resolveSizeAndState(minW, widthMeasureSpec, 1)
        val minH = paddingBottom + paddingTop + mHeightTimeLine
        val viewHeight = resolveSizeAndState(minH, heightMeasureSpec, 1)
        setMeasuredDimension(mViewWidth, viewHeight)
        mPixelRangeMin = 0f
        mPixelRangeMax = mViewWidth - mThumbWidth
        if (mFirstRun) {
            for (i in thumbs.indices) {
                val th = thumbs[i]
                th.scale = mScaleRangeMax * i
                th.pos = mPixelRangeMax * i
            }
            // Fire listener callback
            onCreate(currentThumb, getThumbValue(currentThumb))
            mFirstRun = false
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawShadow(canvas)
        drawThumbs(canvas)
        drawBorder(canvas)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(ev: MotionEvent): Boolean {
        val mThumb: Thumb
        val mThumb2: Thumb
        val coordinate = ev.x
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                // Remember where we started
                currentThumb = getClosestThumb(coordinate)
                if (currentThumb == -1) {
                    return false
                }
                mThumb = thumbs[currentThumb]
                mThumb.lastTouchX = coordinate
                onSeekStart(currentThumb, mThumb.scale)
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (currentThumb == -1) {
                    return false
                }
                mThumb = thumbs[currentThumb]
                onSeekStop(currentThumb, mThumb.scale)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                mThumb = thumbs[currentThumb]
                mThumb2 = thumbs[if (currentThumb == 0) 1 else 0]
                // Calculate the distance moved
                val dx = coordinate - mThumb.lastTouchX
                val newX = mThumb.pos + dx
                if (currentThumb == 0) {
                    when {
                        newX + mThumbWidth >= mThumb2.pos -> {
                            mThumb.pos = mThumb2.pos - mThumbWidth
                        }
                        newX <= mPixelRangeMin -> {
                            mThumb.pos = mPixelRangeMin
                        }
                        else -> {
                            //Check if thumb is not out of max width
                            checkPositionThumb(mThumb, mThumb2, dx, true)
                            // Move the object
                            mThumb.pos = mThumb.pos + dx

                            // Remember this touch position for the next move event
                            mThumb.lastTouchX = coordinate
                        }
                    }
                } else {
                    when {
                        newX <= mThumb2.pos + mThumbWidth -> {
                            mThumb.pos = mThumb2.pos + mThumbWidth
                        }
                        newX >= mPixelRangeMax -> {
                            mThumb.pos = mPixelRangeMax
                        }
                        else -> {
                            //Check if thumb is not out of max width
                            checkPositionThumb(mThumb2, mThumb, dx, false)
                            // Move the object
                            mThumb.pos = mThumb.pos + dx
                            // Remember this touch position for the next move event
                            mThumb.lastTouchX = coordinate
                        }
                    }
                }
                setThumbPos(currentThumb, mThumb.pos)
                invalidate()
                return true
            }
        }
        return false
    }

    private fun checkPositionThumb(mThumbLeft: Thumb, mThumbRight: Thumb, dx: Float, isLeftMove: Boolean) {
        if (isLeftMove && dx < 0) {
            if (mThumbRight.pos - (mThumbLeft.pos + dx) > mMaxWidth) {
                mThumbRight.pos = mThumbLeft.pos + dx + mMaxWidth
                setThumbPos(1, mThumbRight.pos)
            }
        } else if (!isLeftMove && dx > 0) {
            if (mThumbRight.pos + dx - mThumbLeft.pos > mMaxWidth) {
                mThumbLeft.pos = mThumbRight.pos + dx - mMaxWidth
                setThumbPos(0, mThumbLeft.pos)
            }
        }
    }

    private fun pixelToScale(index: Int, pixelValue: Float): Float {
        val scale = pixelValue * 100 / mPixelRangeMax
        return if (index == 0) {
            val pxThumb = scale * mThumbWidth / 100
            scale + pxThumb * 100 / mPixelRangeMax
        } else {
            val pxThumb = (100 - scale) * mThumbWidth / 100
            scale - pxThumb * 100 / mPixelRangeMax
        }
    }

    private fun scaleToPixel(index: Int, scaleValue: Float): Float {
        val px = scaleValue * mPixelRangeMax / 100
        return if (index == 0) {
            val pxThumb = scaleValue * mThumbWidth / 100
            px - pxThumb
        } else {
            val pxThumb = (100 - scaleValue) * mThumbWidth / 100
            px + pxThumb
        }
    }

    private fun calculateThumbValue(index: Int) {
        if (index < thumbs.size && thumbs.isNotEmpty()) {
            val th = thumbs[index]
            th.scale = pixelToScale(index, th.pos)
            onSeek(index, th.scale)
        }
    }

    private fun calculateThumbPos(index: Int) {
        if (index < thumbs.size && thumbs.isNotEmpty()) {
            val th = thumbs[index]
            th.pos = scaleToPixel(index, th.scale)
        }
    }

    private fun getThumbValue(index: Int): Float {
        return thumbs[index].scale
    }

    fun setThumbValue(index: Int, value: Float) {
        thumbs[index].scale = value
        calculateThumbPos(index)
        invalidate()
    }

    private fun setThumbPos(index: Int, pos: Float) {
        thumbs[index].pos = pos
        calculateThumbValue(index)
        invalidate()
    }

    private fun getClosestThumb(coordinate: Float): Int {
        var closest = -1
        if (thumbs.isNotEmpty()) {
            for (i in thumbs.indices) {
                // Find thumb closest to x coordinate
                val tCoordinate = thumbs[i].pos + mThumbWidth
                if (coordinate >= thumbs[i].pos && coordinate <= tCoordinate) {
                    closest = thumbs[i].index
                }
            }
        }
        return closest
    }

    private fun drawShadow(canvas: Canvas) {
        if (thumbs.isNotEmpty()) {
            for (th in thumbs) {
                if (th.index == 0) {
                    val x = th.pos + paddingLeft
                    if (x > mPixelRangeMin) {
                        val mRect = Rect(mThumbWidth.toInt(), 0, (x + mThumbWidth).toInt(), mHeightTimeLine)
                        canvas.drawRect(mRect, mShadow)
                    }
                } else {
                    val x = th.pos - paddingRight
                    if (x < mPixelRangeMax) {
                        val mRect = Rect(x.toInt(), 0, (mViewWidth - mThumbWidth).toInt(), mHeightTimeLine)
                        canvas.drawRect(mRect, mShadow)
                    }
                }
            }
        }
    }

    private fun drawThumbs(canvas: Canvas) {
        for (th in thumbs) {
            val indicatorHeight = th.bitmap.height
            val indicatorWidth = th.bitmap.width
            val indicatorTop = (mHeightTimeLine - indicatorHeight) / 2f
            val indicatorLeft = th.pos + indicatorWidth / 2f
            if (th.index == 0) {
                val mRect = RectF(th.pos + paddingLeft, 0f,
                        (th.pos + mThumbWidth), mHeightTimeLine.toFloat())
                canvas.drawRect(mRect, mThumbPaint)
                canvas.drawBitmap(th.bitmap, indicatorLeft + paddingLeft, indicatorTop, null)
            } else {
                val mRect = RectF(th.pos - paddingRight, 0f,
                        (th.pos + mThumbWidth), mHeightTimeLine.toFloat())
                canvas.drawRect(mRect, mThumbPaint)
                canvas.drawBitmap(th.bitmap, indicatorLeft - paddingRight, indicatorTop, null)
            }
        }
    }

    private fun drawBorder(canvas: Canvas) {
        val thumb1 = thumbs[0]
        val thumb2 = thumbs[1]

        canvas.drawLine(thumb1.pos + paddingLeft,
                paddingTop.toFloat(),
                thumb2.pos + mThumbWidth,
                paddingTop.toFloat(), mLine)

        canvas.drawLine(thumb1.pos + paddingLeft,
                mHeightTimeLine.toFloat(),
                thumb2.pos + mThumbWidth,
                mHeightTimeLine.toFloat(), mLine)
    }

    fun addOnRangeSeekBarListener(listener: OnRangeSeekBarListener) {
        mListeners.add(listener)
    }

    private fun onCreate(index: Int, value: Float) {
        mListeners.forEach { item ->
            item.onCreate(index, value)
        }
    }

    private fun onSeek(index: Int, value: Float) {
        mListeners.forEach { item ->
            item.onSeek(index, value)
        }
    }

    private fun onSeekStart(index: Int, value: Float) {
        mListeners.forEach { item ->
            item.onSeekStart(index, value)
        }
    }

    private fun onSeekStop(index: Int, value: Float) {
        mListeners.forEach { item ->
            item.onSeekStop(index, value)
        }
    }
}