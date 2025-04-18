package org.succlz123.hohoplayer.widget.helper

import org.succlz123.hohoplayer.core.adapter.event.BaseAdapterEventHandler
import org.succlz123.hohoplayer.core.source.DataSource
import org.succlz123.hohoplayer.support.message.HoHoMessage

open class VideoPlayHelperAdapterEventHandler : BaseAdapterEventHandler<IPlayerHelper>() {

    override fun requestPause(t: IPlayerHelper, message: HoHoMessage) {
        if (t.isInPlaybackState()) {
            t.pause()
        } else {
            t.stop()
            t.reset()
        }
    }

    override fun requestResume(t: IPlayerHelper, message: HoHoMessage) {
        if (t.isInPlaybackState()) {
            t.resume()
        } else {
            requestRetry(t, message)
        }
    }

    override fun requestSeek(t: IPlayerHelper, message: HoHoMessage) {
        t.seekTo(message.argInt)
    }

    override fun requestStop(t: IPlayerHelper, message: HoHoMessage) {
        t.stop()
    }

    override fun requestReset(t: IPlayerHelper, message: HoHoMessage) {
        t.reset()
    }

    override fun requestRetry(t: IPlayerHelper, message: HoHoMessage) {
        t.rePlay(message.argInt)
    }

    override fun requestReplay(t: IPlayerHelper, message: HoHoMessage) {
        t.rePlay(0)
    }

    override fun requestPlayDataSource(t: IPlayerHelper, message: HoHoMessage) {
        val data = message.argObj as DataSource? ?: return
        t.stop()
        t.dataSource = (data)
        t.play()
    }
}