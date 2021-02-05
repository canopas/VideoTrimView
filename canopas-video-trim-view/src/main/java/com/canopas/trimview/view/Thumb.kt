package com.canopas.trimview.view

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.canopas.trimview.R
import java.util.*

class Thumb private constructor(var index: Int, var bitmap: Bitmap) {

    var scale = 0f
    var pos = 0f
    var lastTouchX = 0f

    companion object {
        const val LEFT = 0
        const val RIGHT = 1

        fun initThumbs(resources: Resources): List<Thumb> {
            val thumbs: MutableList<Thumb> = Vector()
            val indicatorWidth = resources.getDimension(R.dimen.thumb_indicator_width).toInt()
            val indicatorHeight = resources.getDimension(R.dimen.thumb_indicator_height).toInt()
            for (i in 0..1) {

                val res = if (i == 0) R.drawable.image_left else R.drawable.image_right
                val bitmap = Bitmap.createScaledBitmap(
                        BitmapFactory.decodeResource(resources, res), indicatorWidth, indicatorHeight, false)
                thumbs.add(Thumb(i, bitmap))
            }
            return thumbs
        }
    }
}