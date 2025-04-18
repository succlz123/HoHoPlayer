package org.succlz123.hohoplayer.core.adapter.event

import org.succlz123.hohoplayer.core.adapter.BaseAdapter
import org.succlz123.hohoplayer.core.source.DataSource
import org.succlz123.hohoplayer.support.message.HoHoMessage

object PlayerAdapterKtx {
    const val CODE_REQUEST_PAUSE = -66001
    const val CODE_REQUEST_RESUME = -66003
    const val CODE_REQUEST_SEEK = -66005
    const val CODE_REQUEST_STOP = -66007
    const val CODE_REQUEST_RESET = -66009
    const val CODE_REQUEST_RETRY = -660011
    const val CODE_REQUEST_REPLAY = -66013
    const val CODE_REQUEST_PLAY_DATA_SOURCE = -66014
    const val CODE_REQUEST_NOTIFY_TIMER = -66015
    const val CODE_REQUEST_STOP_TIMER = -66016

    fun BaseAdapter.requestPause() {
        sendOne2ManyAdapterEvent(HoHoMessage.obtain(what = CODE_REQUEST_PAUSE))
    }

    fun BaseAdapter.requestResume() {
        sendOne2ManyAdapterEvent(HoHoMessage.obtain(what = CODE_REQUEST_RESUME))
    }

    fun BaseAdapter.requestSeek(time: Int) {
        sendOne2ManyAdapterEvent(HoHoMessage.obtain(what = CODE_REQUEST_SEEK, argInt = time))
    }

    fun BaseAdapter.requestStop() {
        sendOne2ManyAdapterEvent(HoHoMessage.obtain(what = CODE_REQUEST_STOP))
    }

    fun BaseAdapter.requestReset() {
        sendOne2ManyAdapterEvent(HoHoMessage.obtain(what = CODE_REQUEST_RESET))
    }

    fun BaseAdapter.requestRetry(time: Int) {
        sendOne2ManyAdapterEvent(HoHoMessage.obtain(what = CODE_REQUEST_RETRY, argInt = time))
    }

    fun BaseAdapter.requestReplay() {
        sendOne2ManyAdapterEvent(HoHoMessage.obtain(what = CODE_REQUEST_REPLAY))
    }

    fun BaseAdapter.requestPlayDataSource(dataSource: DataSource) {
        sendOne2ManyAdapterEvent(HoHoMessage.obtain(what = CODE_REQUEST_PLAY_DATA_SOURCE, argObj = dataSource))
    }

    fun BaseAdapter.requestNotifyTimer() {
        sendOne2ManyAdapterEvent(HoHoMessage.obtain(what = CODE_REQUEST_NOTIFY_TIMER))
    }

    fun BaseAdapter.requestStopTimer() {
        sendOne2ManyAdapterEvent(HoHoMessage.obtain(what = CODE_REQUEST_STOP_TIMER))
    }
}