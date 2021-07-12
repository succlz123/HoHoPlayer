package org.succlz123.hohoplayer.widget.videoview

import android.content.Context
import android.graphics.Rect
import android.media.AudioManager
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import org.succlz123.hohoplayer.config.PlayerConfig.isUseDefaultNetworkEventProducer
import org.succlz123.hohoplayer.core.AVPlayer
import org.succlz123.hohoplayer.core.PlayerContainer
import org.succlz123.hohoplayer.core.adapter.bridge.IBridge
import org.succlz123.hohoplayer.core.adapter.event.OnAdapterEventListener
import org.succlz123.hohoplayer.core.adapter.event.PlayerAdapterKtx
import org.succlz123.hohoplayer.core.player.base.IPlayer
import org.succlz123.hohoplayer.core.player.listener.OnErrorEventListener
import org.succlz123.hohoplayer.core.player.listener.OnPlayerEventListener
import org.succlz123.hohoplayer.core.render.AspectRatio
import org.succlz123.hohoplayer.core.render.IRender
import org.succlz123.hohoplayer.core.render.IRender.IRenderCallback
import org.succlz123.hohoplayer.core.render.IRender.IRenderHolder
import org.succlz123.hohoplayer.core.render.RenderSurfaceView
import org.succlz123.hohoplayer.core.render.RenderTextureView
import org.succlz123.hohoplayer.core.source.DataSource
import org.succlz123.hohoplayer.core.style.IStyleSetter
import org.succlz123.hohoplayer.core.style.StyleSetter
import org.succlz123.hohoplayer.support.log.PlayerLog.d
import org.succlz123.hohoplayer.support.log.PlayerLog.e
import org.succlz123.hohoplayer.support.message.HoHoMessage
import org.succlz123.hohoplayer.support.network.NetworkProducer

open class VideoView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr),
        IVideoView, IStyleSetter {

    companion object {
        const val TAG = "VideoView"
    }

    private var renderType = IRender.RENDER_TYPE_SURFACE_VIEW
    private var avPlayer = AVPlayer()

    // style setter for round rect or oval rect.
    private var styleSetter = StyleSetter(this)

    var playerContainer = onCreatePlayerContainer(context)

    var onPlayerEventListener: OnPlayerEventListener? = null
    var onErrorEventListener: OnErrorEventListener? = null
    var onAdapterEventListener: OnAdapterEventListener? = null

    // render view, such as TextureView or SurfaceView.
    private var render: IRender? = null
    private var aspectRatio = AspectRatio.AspectRatio_FIT_PARENT
    private var videoWidth = 0
    private var videoHeight = 0
    private var videoSarNum = 0
    private var videoSarDen = 0
    private var videoRotation = 0
    private var iRenderHolder: IRenderHolder? = null

    var videoViewEventHandler: VideoViewAdapterEventHandler? = null

    init {
        avPlayer.onPlayerEventListener = object : OnPlayerEventListener {

            override fun onPlayerEvent(message: HoHoMessage) {
                when (message.what) {
                    OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_SIZE_CHANGE -> if (message.extra != null) {
                        videoWidth = message.getIntFromExtra("videoWidth", 0)
                        videoHeight = message.getIntFromExtra("videoHeight", 0)
                        videoSarNum = message.getIntFromExtra("videoSarNum", 0)
                        videoSarDen = message.getIntFromExtra("videoSarDen", 0)
                        d(TAG, "onVideoSizeChange : videoWidth = " + videoWidth
                                + ", videoHeight = " + videoHeight
                                + ", videoSarNum = " + videoSarNum
                                + ", videoSarDen = " + videoSarDen
                        )
                        render?.updateVideoSize(videoWidth, videoHeight)
                        render?.setVideoSampleAspectRatio(videoSarNum, videoSarDen)
                    }
                    OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_ROTATION_CHANGED -> if (message.extra != null) {
                        videoRotation = message.getIntFromExtra("rotation", 0)
                        d(TAG, "onVideoRotationChange : videoRotation = $videoRotation")
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
                }
                onPlayerEventListener?.onPlayerEvent(message)
                playerContainer.dispatchPlayNormalEvent(message)
            }
        }
        avPlayer.onErrorEventListener = object : OnErrorEventListener {

            override fun onErrorEvent(message: HoHoMessage) {
                e(TAG, "onError : code = ${message.what}, Message = $message")
                onErrorEventListener?.onErrorEvent(message)
                playerContainer.dispatchPlayerErrorEvent(message)
            }
        }
        playerContainer.onAdapterEventListener = object : OnAdapterEventListener {

            override fun onAdapterEvent(message: HoHoMessage) {
                if (message.what == PlayerAdapterKtx.CODE_REQUEST_NOTIFY_TIMER) {
                    avPlayer.setUseTimerProxy(true)
                } else if (message.what == PlayerAdapterKtx.CODE_REQUEST_STOP_TIMER) {
                    avPlayer.setUseTimerProxy(false)
                }
                videoViewEventHandler?.onHandle(this@VideoView, message)
                onAdapterEventListener?.onAdapterEvent(message)
            }
        }
        addView(
                playerContainer,
                ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        )
    }

    protected fun onCreatePlayerContainer(context: Context): PlayerContainer {
        val superContainer = PlayerContainer(context)
        if (isUseDefaultNetworkEventProducer) {
            superContainer.producerGroup.addEventProducer(NetworkProducer(context))
        }
        return superContainer
    }

    override fun switchDecoder(decoderName: String): Boolean {
        val switchDecoder = avPlayer.switchDecoder(decoderName)
        if (switchDecoder) {
            releaseRender()
        }
        return switchDecoder
    }

    fun option(message: HoHoMessage) {
        avPlayer.option(message)
    }

    override fun setDataSource(dataSource: DataSource) {
        requestAudioFocus()
        setRenderType(renderType, true)
        avPlayer.setDataSource(dataSource)
    }

    private fun requestAudioFocus() {
        d(TAG, ">> requestAudioFocus <<")
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
    }

    private fun releaseAudioFocus() {
        d(TAG, ">> releaseAudioFocus <<")
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.abandonAudioFocus(null)
    }

    fun setBridge(bridge: IBridge) {
        bridge.setPlayer(avPlayer)
        playerContainer.setBridge(bridge)
    }

    private fun bindRenderHolder(renderHolder: IRenderHolder?) {
        renderHolder?.bindPlayer(avPlayer)
    }

    fun rePlay(msc: Int) {
        avPlayer.rePlay(msc)
    }

    override fun setAspectRatio(aspectRatio: AspectRatio) {
        this.aspectRatio = aspectRatio
        render?.updateAspectRatio(aspectRatio)
    }

    override fun setVolume(left: Float, right: Float) {
        avPlayer.setVolume(left, right)
    }

    override fun setSpeed(speed: Float) {
        avPlayer.setSpeed(speed)
    }

    private val renderCallback: IRenderCallback = object : IRenderCallback {

        override fun onSurfaceCreated(renderHolder: IRenderHolder, width: Int, height: Int) {
            d(TAG, "onSurfaceCreated : width = $width, height = $height")
            // on surface create ,try to attach player.
            this@VideoView.iRenderHolder = renderHolder
            bindRenderHolder(this@VideoView.iRenderHolder)
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
            // on surface destroy detach player
            this@VideoView.iRenderHolder = null
        }
    }

    fun refreshRender() {
        setRenderType(renderType, true)
    }

    override fun setRenderType(renderType: Int, force: Boolean) {
        if (force) {
            releaseRender()
        } else {
            val renderChange = this.renderType != renderType
            if (!renderChange && render != null && render?.isReleased() == false) {
                return
            }
        }
        when (renderType) {
            IRender.RENDER_TYPE_SURFACE_VIEW -> {
                this.renderType = IRender.RENDER_TYPE_SURFACE_VIEW
                render = RenderSurfaceView(context)
            }
            IRender.RENDER_TYPE_TEXTURE_VIEW -> {
                this.renderType = IRender.RENDER_TYPE_TEXTURE_VIEW
                render = RenderTextureView(context)
                (render as RenderTextureView).isTakeOverSurfaceTexture = true
            }
        }
        // clear render holder
        iRenderHolder = null
        avPlayer.setSurface(null)
        render?.let { rd ->
            rd.updateAspectRatio(aspectRatio)
            rd.setRenderCallback(renderCallback)
            rd.updateVideoSize(videoWidth, videoHeight)
            rd.setVideoSampleAspectRatio(videoSarNum, videoSarDen)
            rd.setVideoRotation(videoRotation)
            // add to container
            playerContainer.setRenderView(rd.getRenderView())
        }
    }

    override fun isInPlaybackState(): Boolean {
        val state = getState()
        return state != IPlayer.STATE_END && state != IPlayer.STATE_ERROR
                && state != IPlayer.STATE_IDLE && state != IPlayer.STATE_INITIALIZED
                && state != IPlayer.STATE_STOPPED
    }

    override fun getRender(): IRender? {
        return render
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

    // getAudioSessionId from player
    override fun getAudioSessionId(): Int {
        return avPlayer.getAudioSessionId()
    }

    // stream buffer percent
    // min 0, and max 100.
    override fun getBufferPercentage(): Int {
        return avPlayer.getBufferPercentage()
    }

    /**
     * See also
     * [IPlayer.STATE_END]
     * [IPlayer.STATE_ERROR]
     * [IPlayer.STATE_IDLE]
     * [IPlayer.STATE_INITIALIZED]
     * [IPlayer.STATE_PREPARED]
     * [IPlayer.STATE_STARTED]
     * [IPlayer.STATE_PAUSED]
     * [IPlayer.STATE_STOPPED]
     * [IPlayer.STATE_PLAYBACK_COMPLETE]
     */
    override fun getState(): Int {
        return avPlayer.getState()
    }

    override fun start() {
        avPlayer.start()
    }

    /**
     * If you want to start play at a specified time,
     * please set this method.
     * @param msc
     */
    override fun start(msc: Int) {
        avPlayer.start(msc)
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

    override fun stopPlayback() {
        e(TAG, "stopPlayback release.")
        releaseAudioFocus()
        avPlayer.destroy()
        iRenderHolder = null
        releaseRender()
        playerContainer.destroy()
    }

    /**
     * release render
     * see also
     * [RenderTextureView.release]
     */
    private fun releaseRender() {
        render?.release()
        render = null
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return playerContainer.videoViewOnKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        return playerContainer.videoViewOnKeyUp(keyCode, event)
    }

    override fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        return playerContainer.videoViewOnKeyLongPress(keyCode, event)
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
//        val xx = playerContainer.videoViewDispatchKeyEvent(event)
        return super.dispatchKeyEvent(event)
    }

    override fun setRoundRectShape(radius: Float) {
        styleSetter.setRoundRectShape(radius)
    }

    override fun setRoundRectShape(rect: Rect?, radius: Float) {
        styleSetter.setRoundRectShape(rect, radius)
    }

    override fun setOvalRectShape() {
        styleSetter.setOvalRectShape()
    }

    override fun setOvalRectShape(rect: Rect?) {
        styleSetter.setOvalRectShape(rect)
    }

    override fun clearShapeStyle() {
        styleSetter.clearShapeStyle()
    }

    override fun setElevationShadow(elevation: Float) {
        styleSetter.setElevationShadow(elevation)
    }

    override fun setElevationShadow(backgroundColor: Int, elevation: Float) {
        styleSetter.setElevationShadow(backgroundColor, elevation)
    }
}