package org.succlz123.hohoplayer.core.render

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Build
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import android.view.View
import org.succlz123.hohoplayer.support.log.PlayerLog.d
import org.succlz123.hohoplayer.core.player.base.IPlayer
import org.succlz123.hohoplayer.core.render.IRender.IRenderCallback
import org.succlz123.hohoplayer.core.render.IRender.IRenderHolder
import java.lang.ref.WeakReference

/**
 * 使用TextureView时，需要开启硬件加速（系统默认是开启的）。
 *
 * 如果硬件加速是关闭的，会造成[SurfaceTextureListener.onSurfaceTextureAvailable]不执行。
 */
class RenderTextureView(context: Context, attrs: AttributeSet? = null) :
    TextureView(context, attrs), IRender {

    companion object {
        const val TAG = "RenderTextureView"
    }

    var surface: Surface? = null
    private var mRenderCallback: IRenderCallback? = null
    private val mRenderMeasure: RenderMeasure =
        RenderMeasure()

    private var surfaceTextureListener: InternalSurfaceTextureListener? =
        InternalSurfaceTextureListener()

    var ownSurfaceTexture: SurfaceTexture? = null
        private set

    /**
     * If you want to take over the life cycle of SurfaceTexture,
     * please set the tag to true.
     *
     */
    var isTakeOverSurfaceTexture = false

    private var isReleased = false

    init {
        setSurfaceTextureListener(surfaceTextureListener)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
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
        mRenderMeasure.setVideoRotation(degree)
        rotation = degree.toFloat()
    }

    override fun updateAspectRatio(aspectRatio: AspectRatio) {
        mRenderMeasure.setAspectRatio(aspectRatio)
        requestLayout()
    }

    override fun updateVideoSize(videoWidth: Int, videoHeight: Int) {
        d(TAG, "onUpdateVideoSize : videoWidth = $videoWidth videoHeight = $videoHeight")
        mRenderMeasure.setVideoSize(videoWidth, videoHeight)
        requestLayout()
    }

    override fun getRenderView(): View {
        return this
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        d(TAG, "onTextureViewAttachedToWindow")
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        d(TAG, "onTextureViewDetachedFromWindow")
        //  fixed bug on before android 4.4
        //  modify 2018/11/16
        //  java.lang.RuntimeException: Error during detachFromGLContext (see logcat for details) at android.graphics.SurfaceTexture.detachFromGLContext(SurfaceTexture.java:215)
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            release()
        }
    }

    override fun release() {
        ownSurfaceTexture?.release()
        ownSurfaceTexture = null
        surface?.release()
        surface = null
        surfaceTextureListener = null
        isReleased = true
    }

    override fun isReleased(): Boolean {
        return isReleased
    }

    private class InternalRenderHolder(
            textureView: RenderTextureView,
            surfaceTexture: SurfaceTexture?
    ) : IRenderHolder {
        private val mSurfaceRefer: WeakReference<Surface> = WeakReference(Surface(surfaceTexture))
        private val mTextureRefer: WeakReference<RenderTextureView> = WeakReference(textureView)

        override fun bindPlayer(player: IPlayer) {
            val textureView = mTextureRefer.get()
            if (textureView != null) {
                val surfaceTexture = textureView.ownSurfaceTexture
                val useTexture = textureView.surfaceTexture
                var isReleased = false
                // check the SurfaceTexture is released is Android O.
                if (surfaceTexture != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    isReleased = surfaceTexture.isReleased
                }
                val available = surfaceTexture != null && !isReleased
                // When the user sets the takeover flag and SurfaceTexture is available.
                if (textureView.isTakeOverSurfaceTexture && available && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    // if SurfaceTexture not set or current is null, need set it.
                    if (surfaceTexture != useTexture && surfaceTexture != null) {
                        textureView.setSurfaceTexture(surfaceTexture)
                        d("RenderTextureView", "****setSurfaceTexture****")
                    } else {
                        val surface = textureView.surface
                        // release current Surface if not null.
                        surface?.release()
                        // create Surface use update SurfaceTexture
                        val newSurface = Surface(surfaceTexture)
                        // set it for player
                        player.setSurface(newSurface)
                        // record the new Surface
                        textureView.surface = newSurface
                        d("RenderTextureView", "****bindSurface****")
                    }
                } else {
                    val surface = mSurfaceRefer.get()
                    if (surface != null) {
                        player.setSurface(surface)
                        // record the Surface
                        textureView.surface = surface
                        d("RenderTextureView", "****bindSurface****")
                    }
                }
            }
        }
    }

    private inner class InternalSurfaceTextureListener : SurfaceTextureListener {

        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            d(TAG, "<---onSurfaceTextureAvailable---> : width = $width height = $height")
            mRenderCallback?.onSurfaceCreated(
                InternalRenderHolder(
                    this@RenderTextureView,
                    surface
                ),
                width,
                height
            )
        }

        override fun onSurfaceTextureSizeChanged(
            surface: SurfaceTexture,
            width: Int,
            height: Int
        ) {
            d(TAG, "onSurfaceTextureSizeChanged : width = $width height = $height")
            mRenderCallback?.onSurfaceChanged(
                InternalRenderHolder(
                    this@RenderTextureView,
                    surface
                ),
                0,
                width,
                height
            )
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
            d(TAG, "***onSurfaceTextureDestroyed***")
            mRenderCallback?.onSurfaceDestroy(
                InternalRenderHolder(
                    this@RenderTextureView,
                    surface
                )
            )
            if (isTakeOverSurfaceTexture) {
                ownSurfaceTexture = surface
            }
            // fixed bug on before android 4.4
            // modify 2018/11/16
            // java.lang.RuntimeException: Error during detachFromGLContext (see logcat for details) at android.graphics.SurfaceTexture.detachFromGLContext(SurfaceTexture.java:215)
            // all return false.
            return if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
                false
            } else {
                !isTakeOverSurfaceTexture
            }
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
    }
}