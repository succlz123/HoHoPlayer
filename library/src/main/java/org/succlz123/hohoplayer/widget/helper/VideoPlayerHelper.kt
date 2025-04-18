package org.succlz123.hohoplayer.widget.helper

import android.content.Context
import android.view.ViewGroup
import org.succlz123.hohoplayer.config.PlayerConfig.isUseDefaultNetworkEventProducer
import org.succlz123.hohoplayer.core.AVPlayer
import org.succlz123.hohoplayer.core.PlayerContainer
import org.succlz123.hohoplayer.core.adapter.bridge.Bridge
import org.succlz123.hohoplayer.core.adapter.bridge.IBridge
import org.succlz123.hohoplayer.core.adapter.event.BaseAdapterEventHandler
import org.succlz123.hohoplayer.core.adapter.event.OnAdapterEventListener
import org.succlz123.hohoplayer.core.adapter.event.PlayerAdapterKtx
import org.succlz123.hohoplayer.core.player.base.IPlayer
import org.succlz123.hohoplayer.core.player.listener.OnErrorEventListener
import org.succlz123.hohoplayer.core.player.listener.OnPlayerEventListener
import org.succlz123.hohoplayer.core.render.AspectRatio
import org.succlz123.hohoplayer.core.render.IRender
import org.succlz123.hohoplayer.core.render.IRender.Companion.RENDER_TYPE_TEXTURE_VIEW
import org.succlz123.hohoplayer.core.render.IRender.IRenderCallback
import org.succlz123.hohoplayer.core.render.IRender.IRenderHolder
import org.succlz123.hohoplayer.core.render.RenderSurfaceView
import org.succlz123.hohoplayer.core.render.RenderTextureView
import org.succlz123.hohoplayer.core.source.DataSource
import org.succlz123.hohoplayer.support.log.PlayerLog.d
import org.succlz123.hohoplayer.support.message.HoHoMessage
import org.succlz123.hohoplayer.support.network.NetworkProducer
import org.succlz123.hohoplayer.support.utils.ViewKtx.getActivity
import org.succlz123.hohoplayer.widget.videoview.VideoView

class VideoPlayerHelper(private var context: Context) : IPlayerHelper {

    companion object {
        const val TAG = "VideoPlayerHelper"
    }

    private val avPlayer = AVPlayer()

    var playerContainer: PlayerContainer = PlayerContainer(context)

    override var bridge: IBridge = Bridge()

    override var renderType = RENDER_TYPE_TEXTURE_VIEW
        set(value) {
            renderTypeChange = field != value
            field = value
            updateRender()
        }
    private var renderTypeChange = false
    private var render: IRender? = null
    private var iRenderHolder: IRenderHolder? = null

    override var aspectRatio = AspectRatio.AspectRatio_FIT_PARENT
        set(value) {
            field = value
            render?.updateAspectRatio(value)
        }
    private var videoWidth = 0
    private var videoHeight = 0
    private var videoSarNum = 0
    private var videoSarDen = 0
    private var videoRotation = 0

    override var dataSource: DataSource? = null

    override var onPlayerEventListener: OnPlayerEventListener? = null

    override var onErrorEventListener: OnErrorEventListener? = null

    override var onAdapterEventListener: OnAdapterEventListener? = null

    override var adapterEventHandler: BaseAdapterEventHandler<IPlayerHelper>? = null

    private var isBuffering = false

    init {
        if (isUseDefaultNetworkEventProducer) {
            context.getActivity()?.let { activityInstance ->
                playerContainer.producerGroup.addEventProducer(NetworkProducer(activityInstance))
            }
        }
    }

    private fun attachPlayerListener() {
        avPlayer.onPlayerEventListener = object : OnPlayerEventListener {
            override fun onPlayerEvent(message: HoHoMessage) {
                onInternalHandlePlayerEvent(message)
                onPlayerEventListener?.onPlayerEvent(message)
                playerContainer.dispatchPlayNormalEvent(message)
            }
        }
        avPlayer.onErrorEventListener = object : OnErrorEventListener {
            override fun onErrorEvent(message: HoHoMessage) {
                onErrorEventListener?.onErrorEvent(message)
                playerContainer.dispatchPlayerErrorEvent(message)
            }
        }
        // adapter -> ktx - requestStart... -> AdapterEventListener -> adapterEventHandler#onHandle -> do real action
        playerContainer.onAdapterEventListener = object : OnAdapterEventListener {
            override fun onAdapterEvent(message: HoHoMessage) {
                if (message.what == PlayerAdapterKtx.CODE_REQUEST_NOTIFY_TIMER) {
                    avPlayer.setUseTimerProxy(true)
                } else if (message.what == PlayerAdapterKtx.CODE_REQUEST_STOP_TIMER) {
                    avPlayer.setUseTimerProxy(false)
                }
                // if setting AssistEventHandler, call back it to handle.
                adapterEventHandler?.onHandle(this@VideoPlayerHelper, message)
                onAdapterEventListener?.onAdapterEvent(message)
            }
        }
    }

    private fun detachPlayerListener() {
        avPlayer.onPlayerEventListener = null
        avPlayer.onErrorEventListener = null
        playerContainer.onAdapterEventListener = null
    }

    private fun onInternalHandlePlayerEvent(message: HoHoMessage) {
        when (message.what) {
            OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_SIZE_CHANGE -> if (message.extra != null) {
                videoWidth = message.getIntFromExtra("videoWidth", 0)
                videoHeight = message.getIntFromExtra("videoHeight", 0)
                videoSarNum = message.getIntFromExtra("videoSarNum", 0)
                videoSarDen = message.getIntFromExtra("videoSarDen", 0)
                d(
                    VideoView.TAG, "onVideoSizeChange : videoWidth = " + videoWidth
                            + ", videoHeight = " + videoHeight
                            + ", videoSarNum = " + videoSarNum
                            + ", videoSarDen = " + videoSarDen
                )
                render?.updateVideoSize(videoWidth, videoHeight)
                render?.setVideoSampleAspectRatio(videoSarNum, videoSarDen)
            }
            OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_ROTATION_CHANGED -> if (message.extra != null) {
                videoRotation = message.getIntFromExtra("rotation", 0)
                d(VideoView.TAG, "onVideoRotationChange : videoRotation = $videoRotation")
                render?.setVideoRotation(videoRotation)
            }
            OnPlayerEventListener.PLAYER_EVENT_ON_PREPARED -> {
                videoWidth = message.getIntFromExtra("videoWidth", 0)
                videoHeight = message.getIntFromExtra("videoHeight", 0)
                if (videoWidth != 0 && videoHeight != 0) {
                    render?.updateVideoSize(videoWidth, videoHeight)
                }
                bindRenderHolder(iRenderHolder)
            }
            OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_START -> {
                isBuffering = true
            }
            OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_END -> {
                isBuffering = false
            }
        }
    }

    override fun switchDecoder(decoderName: String): Boolean {
        val switchDecoder = avPlayer.switchDecoder(decoderName)
        if (switchDecoder) {
            releaseRender()
        }
        return switchDecoder
    }

    override fun attachContainer(userContainer: ViewGroup, updateRender: Boolean) {
        attachPlayerListener()
        detachSuperContainer()
        bridge.setPlayer(avPlayer)
        playerContainer.setBridge(bridge)
        if (updateRender || isNeedForceUpdateRender()) {
            releaseRender()
            updateRender()
        }
        userContainer.addView(
            playerContainer,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun isNeedForceUpdateRender(): Boolean {
        return renderTypeChange || render?.run { isReleased() } ?: true
    }

    private fun updateRender() {
        if (isNeedForceUpdateRender()) {
            renderTypeChange = false
            releaseRender()
            when (renderType) {
                IRender.RENDER_TYPE_SURFACE_VIEW -> {
                    render = RenderSurfaceView(context)
                }
                IRender.RENDER_TYPE_TEXTURE_VIEW -> {
                    render = RenderTextureView(context).apply {
                        isTakeOverSurfaceTexture = true
                    }
                }
            }
            iRenderHolder = null
            avPlayer.setSurface(null)
            render?.let {
                it.updateAspectRatio(aspectRatio)
                it.setRenderCallback(mRenderCallback)
                //update some params for render type change
                it.updateVideoSize(videoWidth, videoHeight)
                it.setVideoSampleAspectRatio(videoSarNum, videoSarDen)
                //update video rotation
                it.setVideoRotation(videoRotation)
                playerContainer.setRenderView(it.getRenderView())
            }
        }
    }

    override fun play(updateRender: Boolean, pos: Int) {
        if (updateRender) {
            releaseRender()
            updateRender()
        }
        dataSource?.let {
            avPlayer.setDataSource(it)
            avPlayer.start(pos)
        }
    }

    private val mRenderCallback: IRenderCallback = object : IRenderCallback {

        override fun onSurfaceCreated(renderHolder: IRenderHolder, width: Int, height: Int) {
            d(TAG, "onSurfaceCreated : width = $width, height = $height")
            iRenderHolder = renderHolder
            bindRenderHolder(renderHolder)
        }

        override fun onSurfaceChanged(
            renderHolder: IRenderHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
        }

        override fun onSurfaceDestroy(renderHolder: IRenderHolder) {
            d(TAG, "onSurfaceDestroy...")
            iRenderHolder = null
        }
    }

    private fun bindRenderHolder(renderHolder: IRenderHolder?) {
        renderHolder?.bindPlayer(avPlayer)
    }

    private fun releaseRender() {
        render?.let {
            it.setRenderCallback(null)
            it.release()
        }
        render = null
    }

    private fun detachSuperContainer() {
        val parent = playerContainer.parent
        if (parent != null && parent is ViewGroup) {
            parent.removeView(playerContainer)
        }
    }

    fun option(message: HoHoMessage) {
        avPlayer.option(message)
    }

    override fun setVolume(left: Float, right: Float) {
        avPlayer.setVolume(left, right)
    }

    override fun setSpeed(speed: Float) {
        avPlayer.setSpeed(speed)
    }

    override fun setLooping(looping: Boolean) {
        avPlayer.setLooping(looping)
    }

    override fun isInPlaybackState(): Boolean {
        val state = getState()
        return state != IPlayer.STATE_END && state != IPlayer.STATE_ERROR &&
                state != IPlayer.STATE_IDLE && state != IPlayer.STATE_INITIALIZED &&
                state != IPlayer.STATE_PLAYBACK_COMPLETE && state != IPlayer.STATE_STOPPED
    }

    override fun isPlaying(): Boolean {
        return avPlayer.isPlaying()
    }

    override fun getCurrentPosition(): Int {
        return avPlayer.getCurrentPosition()
    }

    override fun getDuration(): Int {
        return avPlayer.getDuration()
    }

    override fun getAudioSessionId(): Int {
        return avPlayer.getAudioSessionId()
    }

    override fun getBufferPercentage(): Int {
        return avPlayer.getBufferPercentage()
    }

    override fun getState(): Int {
        return avPlayer.getState()
    }

    override fun rePlay() {
        avPlayer.rePlay()
    }

    override fun rePlay(msc: Int) {
        avPlayer.rePlay(msc)
    }

    override fun pause() {
        avPlayer.pause()
    }

    override fun resume() {
        avPlayer.resume()
    }

    override fun seekTo(msc: Int) {
        avPlayer.seekTo(msc)
    }

    override fun stop() {
        avPlayer.stop()
    }

    override fun reset() {
        avPlayer.reset()
    }

    override fun destroy() {
        detachPlayerListener()
        bridge.clear()
        iRenderHolder = null
        releaseRender()
        playerContainer.destroy()
        detachSuperContainer()
        avPlayer.destroy()
    }
}