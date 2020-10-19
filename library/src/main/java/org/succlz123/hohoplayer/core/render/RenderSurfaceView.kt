package org.succlz123.hohoplayer.core.render

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import org.succlz123.hohoplayer.support.log.PlayerLog.d
import org.succlz123.hohoplayer.support.log.PlayerLog.e
import org.succlz123.hohoplayer.core.player.base.IPlayer
import org.succlz123.hohoplayer.core.render.IRender.IRenderCallback
import org.succlz123.hohoplayer.core.render.IRender.IRenderHolder
import java.lang.ref.WeakReference

class RenderSurfaceView @JvmOverloads constructor(context: Context?, attrs: AttributeSet? = null) :
    SurfaceView(context, attrs), IRender {

    companion object {
        const val TAG = "RenderSurfaceView"
    }

    private var mRenderCallback: IRenderCallback? = null
    private val mRenderMeasure: RenderMeasure
    private var isReleased = false

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mRenderMeasure.doMeasure(widthMeasureSpec, heightMeasureSpec)
        setMeasuredDimension(mRenderMeasure.measureWidth, mRenderMeasure.measureHeight)
    }

    override fun setRenderCallback(renderCallback: IRenderCallback?) {
        mRenderCallback = renderCallback
    }

    override fun setVideoSampleAspectRatio(videoSarNum: Int, videoSarDen: Int) {
        if (videoSarNum > 0 && videoSarDen > 0) {
            mRenderMeasure.setVideoSampleAspectRatio(videoSarNum, videoSarDen)
            requestLayout()
        }
    }

    override fun setVideoRotation(degree: Int) {
        e(TAG, "surface view not support rotation ... ")
    }

    override fun updateAspectRatio(aspectRatio: AspectRatio) {
        mRenderMeasure.setAspectRatio(aspectRatio)
        requestLayout()
    }

    override fun updateVideoSize(videoWidth: Int, videoHeight: Int) {
        mRenderMeasure.setVideoSize(videoWidth, videoHeight)
        fixedSize(videoWidth, videoHeight)
        requestLayout()
    }

    fun fixedSize(videoWidth: Int, videoHeight: Int) {
        if (videoWidth != 0 && videoHeight != 0) {
            holder.setFixedSize(videoWidth, videoHeight)
        }
    }

    override fun getRenderView(): View {
        return this
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        d(TAG, "onSurfaceViewDetachedFromWindow")
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        d(TAG, "onSurfaceViewAttachedToWindow")
    }

    override fun release() {
        isReleased = true
    }

    override fun isReleased(): Boolean {
        return isReleased
    }

    private class InternalRenderHolder(surfaceHolder: SurfaceHolder?) : IRenderHolder {
        private val mSurfaceHolder: WeakReference<SurfaceHolder?> = WeakReference(surfaceHolder)

        override fun bindPlayer(player: IPlayer) {
            if (player != null && mSurfaceHolder.get() != null) {
                player.setDisplay(mSurfaceHolder.get())
            }
        }
    }

    private inner class InternalSurfaceHolderCallback : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            d(TAG, "<---surfaceCreated---->")
            mRenderCallback?.onSurfaceCreated(
                InternalRenderHolder(
                    holder
                ), 0, 0
            )
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            d(TAG, "surfaceChanged : width = $width height = $height")
            mRenderCallback?.onSurfaceChanged(
                InternalRenderHolder(
                    holder
                ), format, width, height
            )
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
            d(TAG, "***surfaceDestroyed***")
            mRenderCallback?.onSurfaceDestroy(
                InternalRenderHolder(
                    holder
                )
            )
        }
    }

    init {
        mRenderMeasure = RenderMeasure()
        holder.addCallback(InternalSurfaceHolderCallback())
    }
}