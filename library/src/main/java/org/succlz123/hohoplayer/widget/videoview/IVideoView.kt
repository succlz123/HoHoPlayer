package org.succlz123.hohoplayer.widget.videoview

import org.succlz123.hohoplayer.core.source.DataSource
import org.succlz123.hohoplayer.core.render.AspectRatio
import org.succlz123.hohoplayer.core.render.IRender

interface IVideoView {

    fun getRender(): IRender?

    fun isInPlaybackState(): Boolean

    fun isPlaying(): Boolean

    fun getCurrentPosition(): Int

    fun getDuration(): Int

    fun getAudioSessionId(): Int

    fun getBufferPercentage(): Int

    fun getState(): Int

    fun setDataSource(dataSource: DataSource)

    fun setRenderType(renderType: Int, force: Boolean)

    fun setAspectRatio(aspectRatio: AspectRatio)

    fun switchDecoder(decoderName: String): Boolean

    fun setVolume(left: Float, right: Float)

    fun setSpeed(speed: Float)

    fun start()

    fun start(msc: Int)

    fun pause()

    fun resume()

    fun seekTo(msc: Int)

    fun stop()

    fun stopPlayback()
}