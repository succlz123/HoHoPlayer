package org.succlz123.hohoplayer.core.render

import android.view.View
import org.succlz123.hohoplayer.core.player.base.IPlayer

/**
 * Frame rendering view, using the method can refer to BaseVideoView
 */
interface IRender {

    companion object {
        const val RENDER_TYPE_TEXTURE_VIEW = 0
        const val RENDER_TYPE_SURFACE_VIEW = 1
    }

    fun setRenderCallback(renderCallback: IRenderCallback?)

    /**
     * update video rotation, such as some video maybe rotation 90 degree.
     */
    fun setVideoRotation(degree: Int)

    fun setVideoSampleAspectRatio(videoSarNum: Int, videoSarDen: Int)

    /**
     * [AspectRatio.AspectRatio_16_9]
     * [AspectRatio.AspectRatio_4_3]
     * [AspectRatio.AspectRatio_FIT_PARENT]
     * [AspectRatio.AspectRatio_FILL_PARENT]
     * [AspectRatio.AspectRatio_MATCH_PARENT]
     * [AspectRatio.AspectRatio_ORIGIN]
     */
    fun updateAspectRatio(aspectRatio: AspectRatio)

    /**
     * update video size ,width and height.
     */
    fun updateVideoSize(videoWidth: Int, videoHeight: Int)

    fun getRenderView(): View

    fun release()

    fun isReleased(): Boolean

    /**
     * IRenderHolder is responsible for associate the decoder with rendering views.
     *
     * [RenderSurfaceView.InternalRenderHolder.bindPlayer]
     * [RenderTextureView.InternalRenderHolder.bindPlayer]
     */
    interface IRenderHolder {
        fun bindPlayer(player: IPlayer)
    }

    /**
     * [RenderSurfaceView.IRenderCallback]
     * [RenderTextureView.IRenderCallback]
     */
    interface IRenderCallback {

        fun onSurfaceCreated(renderHolder: IRenderHolder, width: Int, height: Int)

        fun onSurfaceChanged(renderHolder: IRenderHolder, format: Int, width: Int, height: Int)

        fun onSurfaceDestroy(renderHolder: IRenderHolder)
    }
}