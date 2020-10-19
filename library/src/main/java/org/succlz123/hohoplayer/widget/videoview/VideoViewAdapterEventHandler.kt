package org.succlz123.hohoplayer.widget.videoview

import org.succlz123.hohoplayer.core.adapter.event.BaseAdapterEventHandler
import org.succlz123.hohoplayer.core.player.base.IPlayer
import org.succlz123.hohoplayer.core.source.DataSource
import org.succlz123.hohoplayer.support.message.HoHoMessage

open class VideoViewAdapterEventHandler : BaseAdapterEventHandler<VideoView>() {

    override fun requestPause(t: VideoView, message: HoHoMessage) {
        if (isInPlaybackState(t)) {
            t.pause()
        } else {
            t.stop()
        }
    }

    override fun requestResume(t: VideoView, message: HoHoMessage) {
        if (isInPlaybackState(t)) {
            t.resume()
        } else {
            requestRetry(t, message)
        }
    }

    override fun requestSeek(t: VideoView, message: HoHoMessage) {
        t.seekTo(message.argInt)
    }

    override fun requestStop(t: VideoView, message: HoHoMessage) {
        t.stop()
    }

    override fun requestReset(t: VideoView, message: HoHoMessage) {
        t.stop()
    }

    override fun requestRetry(t: VideoView, message: HoHoMessage) {
        t.rePlay(message.argInt)
    }

    override fun requestReplay(t: VideoView, message: HoHoMessage) {
        t.rePlay(0)
    }

    override fun requestPlayDataSource(t: VideoView, message: HoHoMessage) {
        val data = message.argObj as DataSource? ?: return
        t.stop()
        t.setDataSource(data)
        t.start()
    }

    private fun isInPlaybackState(videoView: VideoView): Boolean {
        val state = videoView.getState()
        return state != IPlayer.STATE_END &&
                state != IPlayer.STATE_ERROR &&
                state != IPlayer.STATE_IDLE &&
                state != IPlayer.STATE_INITIALIZED &&
                state != IPlayer.STATE_STOPPED
    }
}