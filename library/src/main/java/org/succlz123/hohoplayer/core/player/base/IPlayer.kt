package org.succlz123.hohoplayer.core.player.base

import android.view.Surface
import android.view.SurfaceHolder
import org.succlz123.hohoplayer.core.source.DataSource
import org.succlz123.hohoplayer.support.message.HoHoMessage

interface IPlayer {

    companion object {
        const val STATE_END = -2
        const val STATE_ERROR = -1
        const val STATE_IDLE = 0
        const val STATE_INITIALIZED = 1
        const val STATE_PREPARED = 2
        const val STATE_STARTED = 3
        const val STATE_PAUSED = 4
        const val STATE_STOPPED = 5
        const val STATE_PLAYBACK_COMPLETE = 6
    }

    fun option(message: HoHoMessage)

    fun setDataSource(dataSource: DataSource)

    fun getDataSource(): DataSource?

    fun setDisplay(surfaceHolder: SurfaceHolder?)

    fun setSurface(surface: Surface?)

    fun setVolume(left: Float, right: Float)

    fun setSpeed(speed: Float)

    fun setLooping(looping: Boolean)

    fun isPlaying(): Boolean

    fun isBuffering(): Boolean

    fun getBufferPercentage(): Int

    fun getCurrentPosition(): Int

    fun getDuration(): Int

    fun getAudioSessionId(): Int

    fun getVideoWidth(): Int

    fun getVideoHeight(): Int

    /**
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
    fun getState(): Int

    fun start()

    fun start(msc: Int)

    fun pause()

    fun resume()

    fun seekTo(msc: Int)

    fun stop()

    fun reset()

    fun destroy()
}

fun IPlayer.isInPlaybackState(): Boolean {
    val state = getState()
    return state != IPlayer.STATE_END
            && state != IPlayer.STATE_ERROR
            && state != IPlayer.STATE_IDLE
            && state != IPlayer.STATE_INITIALIZED
            && state != IPlayer.STATE_STOPPED
}