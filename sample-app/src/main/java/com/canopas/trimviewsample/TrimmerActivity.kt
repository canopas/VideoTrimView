package com.canopas.trimviewsample

import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.Toast
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import com.canopas.trimview.VideoTrimView
import com.canopas.trimview.interfaces.TrimViewListener
import com.canopas.videotrimmersample.databinding.ActivityTrimmerBinding

class TrimmerActivity : AppCompatActivity(), TrimViewListener {
    private lateinit var binding: ActivityTrimmerBinding
    private lateinit var mVideoTrimView: VideoTrimView
    private lateinit var mLinearVideo: RelativeLayout
    private lateinit var mVideoView: VideoView
    private lateinit var mPlayView: ImageView

    private var mResetSeekBar = true
    private var mStartPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrimmerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mLinearVideo = binding.videoContainerView
        mVideoView = binding.videoLoader
        mPlayView = binding.iconVideoPlay
        mVideoTrimView = binding.timeLine

        val path: String = intent?.getStringExtra(EXTRA_VIDEO_PATH) ?: ""

        mVideoView.setOnErrorListener { _, what, _ ->
            Toast.makeText(this@TrimmerActivity, "Something went wrong reason : $what", Toast.LENGTH_SHORT).show()
            false
        }

        mVideoView.setOnClickListener { onClickVideoPlayPause() }
        mVideoView.setOnPreparedListener { mp -> onVideoPrepared(mp) }
        mVideoView.setOnCompletionListener { onVideoCompleted() }
        mVideoTrimView.setVideoTrimViewListener(this)

        mVideoView.setVideoURI(Uri.parse(path))
        mVideoView.requestFocus()
        mVideoTrimView.setVideoURI(Uri.parse(path))

    }

    private fun onVideoCompleted() {
        mVideoView.seekTo(mStartPosition)
    }

    private fun onClickVideoPlayPause() {
        if (mVideoView.isPlaying) {
            mPlayView.visibility = View.VISIBLE
            mVideoTrimView.stopProgress()
            mVideoView.pause()
        } else {
            mPlayView.visibility = View.GONE
            if (mResetSeekBar) {
                mResetSeekBar = false
                mVideoView.seekTo(mStartPosition)
            }
            mVideoTrimView.startProgress()
            mVideoView.start()
        }
    }

    private fun onVideoPrepared(mp: MediaPlayer) {
        val videoWidth = mp.videoWidth
        val videoHeight = mp.videoHeight
        val videoProportion = videoWidth.toFloat() / videoHeight.toFloat()

        val screenWidth = mLinearVideo.width
        val screenHeight = mLinearVideo.height
        val screenProportion = screenWidth.toFloat() / screenHeight.toFloat()

        val lp = mVideoView.layoutParams
        if (videoProportion > screenProportion) {
            lp.width = screenWidth
            lp.height = (screenWidth.toFloat() / videoProportion).toInt()
        } else {
            lp.width = (videoProportion * screenHeight.toFloat()).toInt()
            lp.height = screenHeight
        }
        mVideoView.layoutParams = lp

        mPlayView.visibility = View.VISIBLE
        mVideoTrimView.setDuration(mVideoView.duration)
    }

    private fun cancelAction() {
        mVideoView.stopPlayback()
    }

    override fun onStop() {
        super.onStop()
        cancelAction()
    }

    override fun onSeekingRange(startDuration: Int, endDuration: Int) {
        mStartPosition = startDuration
        mVideoView.seekTo(mStartPosition)
    }

    override fun pauseVideo() {
        mVideoView.pause()
        mPlayView.visibility = View.VISIBLE
    }

    override fun seekTo(position: Int) {
        mStartPosition = position
        mVideoView.seekTo(position)
        mResetSeekBar = true
    }

    override fun getCurrentPosition(): Int {
        val pos = mVideoView.currentPosition
        return pos
    }

    override fun resetPlaying() {
        mResetSeekBar = true
    }
}