package org.succlz123.hohoplayer.widget.helper

import android.view.ViewGroup
import org.succlz123.hohoplayer.core.adapter.event.OnAdapterEventListener
import org.succlz123.hohoplayer.core.adapter.bridge.IBridge
import org.succlz123.hohoplayer.core.adapter.event.BaseAdapterEventHandler
import org.succlz123.hohoplayer.core.player.listener.OnErrorEventListener
import org.succlz123.hohoplayer.core.player.listener.OnPlayerEventListener
import org.succlz123.hohoplayer.core.render.AspectRatio
import org.succlz123.hohoplayer.core.source.DataSource

interface IPlayerHelper {

    var onPlayerEventListener: OnPlayerEventListener?

    var onErrorEventListener: OnErrorEventListener?

    var onAdapterEventListener: OnAdapterEventListener?

    var adapterEventHandler: BaseAdapterEventHandler<IPlayerHelper>?

    var bridge: IBridge

    fun attachContainer(userContainer: ViewGroup, updateRender: Boolean = false)

    var dataSource: DataSource?

    var renderType: Int

    var aspectRatio: AspectRatio

    fun switchDecoder(decoderName: String): Boolean

    fun setVolume(left: Float, right: Float)
    fun setSpeed(speed: Float)
    fun setLooping(looping: Boolean)
    fun play(updateRender: Boolean = false)
    fun rePlay(msc: Int)
    fun pause()
    fun resume()
    fun seekTo(msc: Int)
    fun stop()
    fun reset()
    fun destroy()

    fun isInPlaybackState(): Boolean
    fun isPlaying(): Boolean
    fun getCurrentPosition(): Int
    fun getDuration(): Int
    fun getAudioSessionId(): Int
    fun getBufferPercentage(): Int
    fun getState(): Int
}