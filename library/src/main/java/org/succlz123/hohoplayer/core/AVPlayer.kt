package org.succlz123.hohoplayer.core

import android.view.Surface
import android.view.SurfaceHolder
import org.succlz123.hohoplayer.config.PlayerConfig
import org.succlz123.hohoplayer.config.PlayerConfig.SYS_MEDIA_PLAYER
import org.succlz123.hohoplayer.config.PlayerConfig.getDecoder
import org.succlz123.hohoplayer.config.PlayerLoader
import org.succlz123.hohoplayer.core.player.base.BasePlayer
import org.succlz123.hohoplayer.core.player.base.IPlayer
import org.succlz123.hohoplayer.core.player.listener.OnBufferingListener
import org.succlz123.hohoplayer.core.player.listener.OnErrorEventListener
import org.succlz123.hohoplayer.core.player.listener.OnPlayerEventListener
import org.succlz123.hohoplayer.core.player.time.TimerCounterProxy
import org.succlz123.hohoplayer.core.player.time.TimerCounterProxy.OnCounterUpdateListener
import org.succlz123.hohoplayer.core.source.DataSource
import org.succlz123.hohoplayer.support.log.PlayerLog.d
import org.succlz123.hohoplayer.support.message.HoHoMessage

class AVPlayer(decoderName: String = PlayerConfig.getDefaultDecoderName()) : IPlayer {

    companion object {
        private const val TAG = "AVPlayer"
    }

    private lateinit var basePlayer: BasePlayer

    private var currentDecoderName = ""

    private var dataSource: DataSource? = null

    private val timerCounterProxy = TimerCounterProxy(1000)

    private var volumeLeft = -1f

    private var volumeRight = -1f

    var onPlayerEventListener: OnPlayerEventListener? = null

    var onErrorEventListener: OnErrorEventListener? = null

    var onBufferingListener: OnBufferingListener? = null

    init {
        loadInternalPlayer(decoderName, true)
    }

    private fun loadInternalPlayer(decoderName: String, fromInit: Boolean) {
        if (!fromInit) {
            destroy()
        }
        currentDecoderName = decoderName
        // loader decoder instance from the configuration.
        basePlayer = PlayerLoader.loadDecoderPlayer(decoderName)
                ?: throw RuntimeException("init decoder instance failure, please check your configuration, maybe your config classpath not found.")
        val decoder = getDecoder(currentDecoderName)
        if (decoder != null) {
            d(TAG, "=============================")
            d(TAG, "Player Decoder Info : decoder name = " + decoder.name)
            d(TAG, "Player Decoder Info : classPath  = " + decoder.classPath)
            d(TAG, "=============================")
        }
    }

    fun switchDecoder(decoderName: String): Boolean {
        if (currentDecoderName == decoderName) {
            d(
                    this.javaClass.simpleName,
                    "Your incoming decoder name is the same as the current use decoder!"
            )
            return false
        }
        return if (getDecoder(decoderName) != null) {
            // reload internal player instance.
            loadInternalPlayer(decoderName, false)
            true
        } else {
            throw IllegalArgumentException("Illegal decoder name = ${decoderName}, please check your config!")
        }
    }

    override fun option(message: HoHoMessage) {
        basePlayer.option(message)
    }

    fun setUseTimerProxy(useTimerProxy: Boolean) {
        timerCounterProxy.setUseProxy(useTimerProxy)
    }

    private fun addListener() {
        timerCounterProxy.setOnCounterUpdateListener(object : OnCounterUpdateListener {
            override fun onCounter() {
                val curr = getCurrentPosition()
                val duration = getDuration()
                val bufferPercentage = getBufferPercentage()
                // check valid data.
                if (duration <= 0 && dataSource?.isLive == false) {
                    return
                }
                onTimerUpdateEvent(curr, duration, bufferPercentage)
            }
        })
        basePlayer.onPlayerEventListener = object : OnPlayerEventListener {
            override fun onPlayerEvent(message: HoHoMessage) {
                timerCounterProxy.proxyPlayEvent(message.what)
                if (message.what == OnPlayerEventListener.PLAYER_EVENT_ON_PREPARED) {
                    // when prepared set volume value
                    if (volumeLeft >= 0 || volumeRight >= 0) {
                        basePlayer.setVolume(volumeLeft, volumeRight)
                    }
                } else if (message.what == OnPlayerEventListener.PLAYER_EVENT_ON_PLAY_COMPLETE) {
                    val duration = getDuration()
                    val bufferPercentage = getBufferPercentage()
                    // check valid data.
                    if (duration <= 0 && dataSource?.isLive == false) {
                        return
                    }
                    onTimerUpdateEvent(duration, duration, bufferPercentage)
                }
                onPlayerEventListener?.onPlayerEvent(message)
            }
        }
        basePlayer.onErrorEventListener = object : OnErrorEventListener {
            override fun onErrorEvent(message: HoHoMessage) {
                timerCounterProxy.proxyErrorEvent(message)
                onErrorEventListener?.onErrorEvent(message)
            }
        }
        basePlayer.onBufferingListener = object : OnBufferingListener {
            override fun onBufferingUpdate(bufferPercentage: Int) {
                onBufferingListener?.onBufferingUpdate(bufferPercentage)
            }
        }
    }

    private fun removeListener() {
        timerCounterProxy.setOnCounterUpdateListener(null)
        basePlayer.onPlayerEventListener = null
        basePlayer.onErrorEventListener = null
        basePlayer.onBufferingListener = null
    }

    fun onTimerUpdateEvent(current: Int, duration: Int, bufferPercentage: Int) {
        onPlayerEventListener?.onPlayerEvent(
                HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_TIMER_UPDATE, extra = hashMapOf(
                        "current" to current, "duration" to duration, "bufferPercentage" to bufferPercentage
                )),
        )
    }

    override fun setDataSource(dataSource: DataSource) {
        addListener()
        this.dataSource = dataSource
        basePlayer.setDataSource(dataSource)
    }

    override fun start() {
        internalPlayerStart(0)
    }

    override fun start(msc: Int) {
        internalPlayerStart(msc)
    }

    fun rePlay(msc: Int) {
        interPlayerSetDataSource(dataSource)
        internalPlayerStart(msc)
    }

    private fun internalPlayerStart(msc: Int) {
        basePlayer.start(msc)
    }

    private fun interPlayerSetDataSource(dataSource: DataSource?) {
        dataSource ?: return
        basePlayer.setDataSource(dataSource)
    }

    override fun setDisplay(surfaceHolder: SurfaceHolder?) {
        basePlayer.setDisplay(surfaceHolder)
    }

    override fun setSurface(surface: Surface?) {
        basePlayer.setSurface(surface)
    }

    override fun setVolume(left: Float, right: Float) {
        volumeLeft = left
        volumeRight = right
        basePlayer.setVolume(left, right)
    }

    override fun setSpeed(speed: Float) {
        basePlayer.setSpeed(speed)
    }

    override fun setLooping(looping: Boolean) {
        basePlayer.setLooping(looping)
    }

    override fun isPlaying(): Boolean {
        return basePlayer.isPlaying()
    }

    override fun getCurrentPosition(): Int {
        return basePlayer.getCurrentPosition()
    }

    override fun getDuration(): Int {
        return basePlayer.getDuration()
    }

    override fun getAudioSessionId(): Int {
        return basePlayer.getAudioSessionId()
    }

    override fun getVideoWidth(): Int {
        return basePlayer.getVideoWidth()
    }

    override fun getVideoHeight(): Int {
        return basePlayer.getVideoHeight()
    }

    override fun getState(): Int {
        return basePlayer.getState()
    }

    override fun getBufferPercentage(): Int {
        return basePlayer.getBufferPercentage()
    }

    override fun pause() {
        basePlayer.pause()
    }

    override fun resume() {
        basePlayer.resume()
    }

    override fun seekTo(msc: Int) {
        basePlayer.seekTo(msc)
    }

    override fun stop() {
        basePlayer.stop()
    }

    override fun reset() {
        basePlayer.reset()
    }

    override fun destroy() {
        basePlayer.destroy()
        timerCounterProxy.cancel()
        removeListener()
    }
}
