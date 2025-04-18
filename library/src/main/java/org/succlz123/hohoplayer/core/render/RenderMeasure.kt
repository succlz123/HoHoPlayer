package org.succlz123.hohoplayer.core.render

import android.view.View
import org.succlz123.hohoplayer.support.log.PlayerLog.d

class RenderMeasure {
    private val TAG = "RenderMeasure"
    private var mVideoWidth = 0
    private var mVideoHeight = 0
    private var mVideoSarNum = 0
    private var mVideoSarDen = 0
    var measureWidth = 0
        private set
    var measureHeight = 0
        private set
    private var mCurrAspectRatio =
        AspectRatio.AspectRatio_FIT_PARENT
    private var mVideoRotationDegree = 0
    fun doMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {

        //Log.i("@@@@", "onMeasure(" + MeasureSpec.toString(widthMeasureSpec) + ", "
        //        + MeasureSpec.toString(heightMeasureSpec) + ")");
        var widthMeasureSpec = widthMeasureSpec
        var heightMeasureSpec = heightMeasureSpec
        if (mVideoRotationDegree == 90 || mVideoRotationDegree == 270) {
            val tempSpec = widthMeasureSpec
            widthMeasureSpec = heightMeasureSpec
            heightMeasureSpec = tempSpec
        }
        var width = View.getDefaultSize(mVideoWidth, widthMeasureSpec)
        var height = View.getDefaultSize(mVideoHeight, heightMeasureSpec)
        if (mCurrAspectRatio == AspectRatio.AspectRatio_MATCH_PARENT) {
            width = widthMeasureSpec
            height = heightMeasureSpec
        } else if (mVideoWidth > 0 && mVideoHeight > 0) {
            val widthSpecMode = View.MeasureSpec.getMode(widthMeasureSpec)
            val widthSpecSize = View.MeasureSpec.getSize(widthMeasureSpec)
            val heightSpecMode = View.MeasureSpec.getMode(heightMeasureSpec)
            val heightSpecSize = View.MeasureSpec.getSize(heightMeasureSpec)
            if (widthSpecMode == View.MeasureSpec.AT_MOST && heightSpecMode == View.MeasureSpec.AT_MOST) {
                val specAspectRatio = widthSpecSize.toFloat() / heightSpecSize.toFloat()
                var displayAspectRatio: Float
                when (mCurrAspectRatio) {
                    AspectRatio.AspectRatio_16_9 -> {
                        displayAspectRatio = 16.0f / 9.0f
                        if (mVideoRotationDegree == 90 || mVideoRotationDegree == 270) displayAspectRatio = 1.0f / displayAspectRatio
                    }
                    AspectRatio.AspectRatio_4_3 -> {
                        displayAspectRatio = 4.0f / 3.0f
                        if (mVideoRotationDegree == 90 || mVideoRotationDegree == 270) displayAspectRatio = 1.0f / displayAspectRatio
                    }
                    AspectRatio.AspectRatio_FIT_PARENT, AspectRatio.AspectRatio_FILL_PARENT, AspectRatio.AspectRatio_ORIGIN, AspectRatio.AspectRatio_FILL_WIDTH, AspectRatio.AspectRatio_FILL_HEIGHT -> {
                        displayAspectRatio = mVideoWidth.toFloat() / mVideoHeight.toFloat()
                        if (mVideoSarNum > 0 && mVideoSarDen > 0) displayAspectRatio = displayAspectRatio * mVideoSarNum / mVideoSarDen
                    }
                    else -> {
                        displayAspectRatio = mVideoWidth.toFloat() / mVideoHeight.toFloat()
                        if (mVideoSarNum > 0 && mVideoSarDen > 0) displayAspectRatio = displayAspectRatio * mVideoSarNum / mVideoSarDen
                    }
                }
                val shouldBeWider = displayAspectRatio > specAspectRatio
                when (mCurrAspectRatio) {
                    AspectRatio.AspectRatio_FIT_PARENT, AspectRatio.AspectRatio_16_9, AspectRatio.AspectRatio_4_3 -> if (shouldBeWider) {
                        // too wide, fix width
                        width = widthSpecSize
                        height = (width / displayAspectRatio).toInt()
                    } else {
                        // too high, fix height
                        height = heightSpecSize
                        width = (height * displayAspectRatio).toInt()
                    }
                    AspectRatio.AspectRatio_FILL_PARENT -> if (shouldBeWider) {
                        // not high enough, fix height
                        height = heightSpecSize
                        width = (height * displayAspectRatio).toInt()
                    } else {
                        // not wide enough, fix width
                        width = widthSpecSize
                        height = (width / displayAspectRatio).toInt()
                    }
                    AspectRatio.AspectRatio_FILL_WIDTH -> {
                        width = widthSpecSize
                        height = widthSpecSize * mVideoHeight / mVideoWidth
                    }
                    AspectRatio.AspectRatio_FILL_HEIGHT -> {
                        height = heightSpecSize
                        width = heightSpecSize * mVideoWidth / mVideoHeight
                    }
                    AspectRatio.AspectRatio_ORIGIN -> if (shouldBeWider) {
                        // too wide, fix width
                        width = Math.min(mVideoWidth, widthSpecSize)
                        height = (width / displayAspectRatio).toInt()
                    } else {
                        // too high, fix height
                        height = Math.min(mVideoHeight, heightSpecSize)
                        width = (height * displayAspectRatio).toInt()
                    }
                    else -> if (shouldBeWider) {
                        width = Math.min(mVideoWidth, widthSpecSize)
                        height = (width / displayAspectRatio).toInt()
                    } else {
                        height = Math.min(mVideoHeight, heightSpecSize)
                        width = (height * displayAspectRatio).toInt()
                    }
                }
            } else if (widthSpecMode == View.MeasureSpec.EXACTLY && heightSpecMode == View.MeasureSpec.EXACTLY) {
                // the size is fixed
                width = widthSpecSize
                height = heightSpecSize

                // for compatibility, we adjust size based on aspect ratio
                if (mVideoWidth * height < width * mVideoHeight) {
                    //Log.i("@@@", "image too wide, correcting");
                    width = height * mVideoWidth / mVideoHeight
                } else if (mVideoWidth * height > width * mVideoHeight) {
                    //Log.i("@@@", "image too tall, correcting");
                    height = width * mVideoHeight / mVideoWidth
                }
            } else if (widthSpecMode == View.MeasureSpec.EXACTLY) {
                // only the width is fixed, adjust the height to match aspect ratio if possible
                width = widthSpecSize
                height = width * mVideoHeight / mVideoWidth
                if (heightSpecMode == View.MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    height = heightSpecSize
                }
            } else if (heightSpecMode == View.MeasureSpec.EXACTLY) {
                // only the height is fixed, adjust the width to match aspect ratio if possible
                height = heightSpecSize
                width = height * mVideoWidth / mVideoHeight
                if (widthSpecMode == View.MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // couldn't match aspect ratio within the constraints
                    width = widthSpecSize
                }
            } else {
                // neither the width nor the height are fixed, try to use actual video size
                width = mVideoWidth
                height = mVideoHeight
                if (heightSpecMode == View.MeasureSpec.AT_MOST && height > heightSpecSize) {
                    // too tall, decrease both width and height
                    height = heightSpecSize
                    width = height * mVideoWidth / mVideoHeight
                }
                if (widthSpecMode == View.MeasureSpec.AT_MOST && width > widthSpecSize) {
                    // too wide, decrease both width and height
                    width = widthSpecSize
                    height = width * mVideoHeight / mVideoWidth
                }
            }
        } else {
            // no size yet, just adopt the given spec sizes
        }
        measureWidth = width
        measureHeight = height
    }

    fun setVideoSampleAspectRatio(videoSarNum: Int, videoSarDen: Int) {
        mVideoSarNum = videoSarNum
        mVideoSarDen = videoSarDen
    }

    fun setVideoSize(videoWidth: Int, videoHeight: Int) {
        d(TAG, "videoWidth = $videoWidth videoHeight = $videoHeight")
        mVideoWidth = videoWidth
        mVideoHeight = videoHeight
    }

    fun setVideoRotation(videoRotationDegree: Int) {
        mVideoRotationDegree = videoRotationDegree
    }

    fun setAspectRatio(aspectRatio: AspectRatio?) {
        mCurrAspectRatio = aspectRatio ?: AspectRatio.AspectRatio_FIT_PARENT
    }
}