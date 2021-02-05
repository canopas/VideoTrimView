/*
 * MIT License
 *
 * Copyright (c) 2016 Knowledge, education for life.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.canopas.trimview.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.AttributeSet
import android.util.LongSparseArray
import android.view.View
import com.canopas.trimview.R

class TimeLineView @JvmOverloads constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    private var mVideoUri: Uri? = null
    private var mHeightView = 0
    private var mWidthView = 0

    private var mBitmapList: LongSparseArray<Bitmap>? = null

    init {
        mHeightView = context.resources.getDimensionPixelOffset(R.dimen.frames_video_height)
        mWidthView = context.resources.getDimensionPixelOffset(R.dimen.frames_video_width)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minW = paddingLeft + paddingRight + suggestedMinimumWidth
        val w = resolveSizeAndState(minW, widthMeasureSpec, 1)
        val minH = paddingBottom + paddingTop + mHeightView
        val h = resolveSizeAndState(minH, heightMeasureSpec, 1)
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        if (w != oldW) {
            getBitmap(w)
        }
    }

    private fun getBitmap(viewWidth: Int) {
        if (mVideoUri == null || viewWidth == 0) return
        try {
            val thumbnailList = LongSparseArray<Bitmap>()
            val mediaMetadataRetriever = MediaMetadataRetriever()
            mediaMetadataRetriever.setDataSource(context, mVideoUri)

            // Retrieve media data
            val videoLengthInMs = (mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION).toInt() * 1000).toLong()

            // Set thumbnail properties (Thumbs are squares)
            val thumbWidth = mWidthView
            val thumbHeight = mHeightView
            val numThumbs = Math.ceil((viewWidth.toFloat() / thumbWidth).toDouble()).toInt()
            val interval = videoLengthInMs / numThumbs
            for (i in 0 until numThumbs) {
                var bitmap = mediaMetadataRetriever.getFrameAtTime(i * interval, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                try {
                    bitmap = Bitmap.createScaledBitmap(bitmap, thumbWidth, thumbHeight, false)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                thumbnailList.put(i.toLong(), bitmap)
            }
            mediaMetadataRetriever.release()
            mBitmapList = thumbnailList
            invalidate()
        } catch (e: Throwable) {
            e.printStackTrace()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mBitmapList?.let {
            canvas.save()
            var x = 0
            for (i in 0 until it.size()) {
                val bitmap = it[i.toLong()]
                if (bitmap != null) {
                    canvas.drawBitmap(bitmap, x.toFloat(), 0f, null)
                    x += bitmap.width
                }
            }
        }
    }

    fun setVideo(data: Uri) {
        mVideoUri = data
        getBitmap(width)
        invalidate()
    }
}