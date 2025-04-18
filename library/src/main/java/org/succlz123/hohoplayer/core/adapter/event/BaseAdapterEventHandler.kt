package org.succlz123.hohoplayer.core.adapter.event

import org.succlz123.hohoplayer.support.message.HoHoMessage

abstract class BaseAdapterEventHandler<T> {

    open fun onHandle(t: T, message: HoHoMessage) {
        when (message.what) {
            PlayerAdapterKtx.CODE_REQUEST_PAUSE -> requestPause(t, message)
            PlayerAdapterKtx.CODE_REQUEST_RESUME -> requestResume(t, message)
            PlayerAdapterKtx.CODE_REQUEST_SEEK -> requestSeek(t, message)
            PlayerAdapterKtx.CODE_REQUEST_STOP -> requestStop(t, message)
            PlayerAdapterKtx.CODE_REQUEST_RESET -> requestReset(t, message)
            PlayerAdapterKtx.CODE_REQUEST_RETRY -> requestRetry(t, message)
            PlayerAdapterKtx.CODE_REQUEST_REPLAY -> requestReplay(t, message)
            PlayerAdapterKtx.CODE_REQUEST_PLAY_DATA_SOURCE -> requestPlayDataSource(t, message)
        }
    }

    abstract fun requestPause(t: T, message: HoHoMessage)

    abstract fun requestResume(t: T, message: HoHoMessage)

    abstract fun requestSeek(t: T, message: HoHoMessage)

    abstract fun requestStop(t: T, message: HoHoMessage)

    abstract fun requestReset(t: T, message: HoHoMessage)

    abstract fun requestRetry(t: T, message: HoHoMessage)

    abstract fun requestReplay(t: T, message: HoHoMessage)

    abstract fun requestPlayDataSource(t: T, message: HoHoMessage)
}