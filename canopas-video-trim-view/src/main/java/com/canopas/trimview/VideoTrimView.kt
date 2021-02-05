package com.canopas.trimview

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import com.canopas.trimview.interfaces.OnProgressVideoListener
import com.canopas.trimview.interfaces.OnRangeSeekBarListener
import com.canopas.trimview.interfaces.TrimViewListener
import com.canopas.trimview.view.ProgressBarView
import com.canopas.trimview.view.RangeSeekBarView
import com.canopas.trimview.view.Thumb
import com.canopas.trimview.view.TimeLineView

class VideoTrimView @JvmOverloads constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int = 0) :
        FrameLayout(context, attrs, defStyleAttr) {

    private var mHolderTopView: SeekBar
    private var mRangeSeekBarView: RangeSeekBarView
    private var mTimeLineView: TimeLineView
    private var mVideoProgressIndicator: ProgressBarView

    private var mProgressListeners: OnProgressVideoListener? = null
    private var mTrimViewListener: TrimViewListener? = null

    private var mDuration = 0
    private var mStartPosition = 0
    private var mEndPosition = 0

    private val progressHandler = Handler()
    private val progressRunnable: Runnable = object : Runnable {
        override fun run() {
            notifyProgressUpdate(true)
            progressHandler.postDelayed(this, 100)
        }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_time_line, this, true)
        mHolderTopView = findViewById<SeekBar>(R.id.handlerTop)
        mRangeSeekBarView = findViewById<RangeSeekBarView>(R.id.timeLineBar)
        mVideoProgressIndicator = findViewById<ProgressBarView>(R.id.timeVideoView)
        mTimeLineView = findViewById<TimeLineView>(R.id.timeLineView)
        setUpListeners()
        setUpMargins()
    }

    private fun setUpListeners() {
        mProgressListeners = mVideoProgressIndicator

        mRangeSeekBarView.addOnRangeSeekBarListener(object : OnRangeSeekBarListener {
            override fun onCreate(index: Int, value: Float) {
                // Do nothing
            }

            override fun onSeek(index: Int, value: Float) {
                onSeekThumbs(index, value)
            }

            override fun onSeekStart(index: Int, value: Float) {
                // Do nothing
            }

            override fun onSeekStop(index: Int, value: Float) {
                onStopSeekThumbs()
            }
        })

        mRangeSeekBarView.addOnRangeSeekBarListener(mVideoProgressIndicator)
        mHolderTopView.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                onPlayerIndicatorSeekChanged(progress, fromUser)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
                onPlayerIndicatorSeekStart()
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                onPlayerIndicatorSeekStop(seekBar)
            }
        })
    }

    private fun setUpMargins() {
        val marge = resources.getDimension(R.dimen.thumb_width).toInt()
        val widthSeek = mHolderTopView.thumb.minimumWidth
        val lp = mHolderTopView.layoutParams as LayoutParams
        lp.setMargins(marge - widthSeek, 0, marge - widthSeek, 0)
        mHolderTopView.layoutParams = lp
        val frameLp = mTimeLineView.layoutParams as LayoutParams
        frameLp.setMargins(marge, 0, marge, 0)
        mTimeLineView.layoutParams = frameLp
        val lp1 = mVideoProgressIndicator.layoutParams as RelativeLayout.LayoutParams
        lp1.setMargins(marge, 0, marge, 0)
        mVideoProgressIndicator.layoutParams = lp1
    }

    private fun onPlayerIndicatorSeekChanged(progress: Int, fromUser: Boolean) {
        val duration = (mDuration * progress / 1000L).toInt()
        if (fromUser) {
            if (duration < mStartPosition) {
                setProgressBarPosition(mStartPosition)
            } else if (duration > mEndPosition) {
                setProgressBarPosition(mEndPosition)
            }
        }
    }

    private fun onPlayerIndicatorSeekStart() {
        stopProgress()
        if (mTrimViewListener != null) {
            mTrimViewListener!!.pauseVideo()
        }
        notifyProgressUpdate(false)
    }

    private fun onPlayerIndicatorSeekStop(seekBar: SeekBar) {
        stopProgress()
        mTrimViewListener?.pauseVideo()

        val duration = (mDuration * seekBar.progress / 1000L).toInt()
        mTrimViewListener?.seekTo(duration)

        notifyProgressUpdate(false)
    }

    fun setDuration(duration: Int) {
        mDuration = duration
        setSeekBarPosition()
    }

    private fun setSeekBarPosition() {
        if (mEndPosition == 0 || mEndPosition > mDuration) {
            mEndPosition = mDuration
        }
        mRangeSeekBarView.setThumbValue(0, mStartPosition * 100f / mDuration)
        mRangeSeekBarView.setThumbValue(1, mEndPosition * 100f / mDuration)
        setProgressBarPosition(mStartPosition)
        if (mTrimViewListener != null) {
            mTrimViewListener!!.seekTo(mStartPosition)
        }
        mRangeSeekBarView.initMaxWidth()
    }

    private fun onSeekThumbs(index: Int, value: Float) {
        when (index) {
            Thumb.LEFT -> {
                mStartPosition = (mDuration * value / 100L).toInt()
                if (mTrimViewListener != null) {
                    mTrimViewListener!!.seekTo(mStartPosition)
                }
            }
            Thumb.RIGHT -> {
                mEndPosition = (mDuration * value / 100L).toInt()
            }
        }
        setProgressBarPosition(mStartPosition)
    }

    private fun onStopSeekThumbs() {
        stopProgress()
        mTrimViewListener?.pauseVideo()
        mTrimViewListener?.onSeekingRange(mStartPosition, mEndPosition)
    }

    private fun notifyProgressUpdate(all: Boolean) {
        if (mDuration == 0) return
        val position = mTrimViewListener?.getCurrentPosition() ?: 0
        val scale = position * 100f / mDuration
        updateVideoProgress(position)

        if (all) {
            mProgressListeners?.updateProgress(position, mDuration, scale)
        }
    }

    private fun updateVideoProgress(time: Int) {
        if (time >= mEndPosition) {
            stopProgress()
            mTrimViewListener?.pauseVideo()
            mTrimViewListener?.resetPlaying()
            return
        }

        setProgressBarPosition(time)

    }

    private fun setProgressBarPosition(position: Int) {
        if (mDuration > 0) {
            val pos = 1000L * position / mDuration
            mHolderTopView.progress = pos.toInt()
        }
    }

    /**
     * Listener for events
     *
     * @param trimViewListener interface for events
     */
    fun setVideoTrimViewListener(trimViewListener: TrimViewListener) {
        mTrimViewListener = trimViewListener
    }

    /**
     * Sets the uri of the video to be trimmer
     *
     * @param videoURI Uri of the video
     */
    fun setVideoURI(videoURI: Uri) {
        mTimeLineView.setVideo(videoURI)
    }

    fun setRange(starMs: Int, endMs: Int) {
        mStartPosition = starMs
        mEndPosition = endMs
    }

    fun stopProgress() {
        progressHandler.removeCallbacks(progressRunnable)
    }

    fun startProgress() {
        progressHandler.removeCallbacks(progressRunnable)
        progressHandler.post(progressRunnable)
    }
}