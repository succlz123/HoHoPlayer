package org.succlz123.hohoplayer.core.player.time

import android.os.Handler
import android.os.Looper
import android.os.Message
import org.succlz123.hohoplayer.core.player.listener.OnPlayerEventListener
import org.succlz123.hohoplayer.support.log.PlayerLog.e
import org.succlz123.hohoplayer.support.message.HoHoMessage

class TimerCounterProxy(private val counterInterval: Int) {

    companion object {
        private const val MSG_CODE_COUNTER = 1
    }

    //proxy state, default use it.
    private var useProxy = true

    private var onCounterUpdateListener: OnCounterUpdateListener? = null

    private val mHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                MSG_CODE_COUNTER -> {
                    if (!useProxy) return
                    if (onCounterUpdateListener != null) onCounterUpdateListener!!.onCounter()
                    loopNext()
                }
            }
        }
    }

    fun setUseProxy(useProxy: Boolean) {
        this.useProxy = useProxy
        if (!useProxy) {
            cancel()
            e("TimerCounterProxy", "Timer Stopped")
        } else {
            start()
            e("TimerCounterProxy", "Timer Started")
        }
    }

    fun setOnCounterUpdateListener(onCounterUpdateListener: OnCounterUpdateListener?) {
        this.onCounterUpdateListener = onCounterUpdateListener
    }

    fun proxyPlayEvent(eventCode: Int) {
        when (eventCode) {
            OnPlayerEventListener.PLAYER_EVENT_ON_DATA_SOURCE_SET,
            OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_RENDER_START,
            OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_START,
            OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_END,
            OnPlayerEventListener.PLAYER_EVENT_ON_PAUSE,
            OnPlayerEventListener.PLAYER_EVENT_ON_RESUME,
            OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_COMPLETE -> {
                if (!useProxy) {
                    return
                }
                start()
            }
            OnPlayerEventListener.PLAYER_EVENT_ON_STOP,
            OnPlayerEventListener.PLAYER_EVENT_ON_RESET,
            OnPlayerEventListener.PLAYER_EVENT_ON_DESTROY,
            OnPlayerEventListener.PLAYER_EVENT_ON_PLAY_COMPLETE -> {
                cancel()
            }
        }
    }

    fun proxyErrorEvent(message: HoHoMessage) {
        cancel()
    }

    fun start() {
        removeMessage()
        mHandler.sendEmptyMessage(MSG_CODE_COUNTER)
    }

    private fun loopNext() {
        removeMessage()
        mHandler.sendEmptyMessageDelayed(MSG_CODE_COUNTER, counterInterval.toLong())
    }

    fun cancel() {
        removeMessage()
    }

    private fun removeMessage() {
        mHandler.removeMessages(MSG_CODE_COUNTER)
    }

    interface OnCounterUpdateListener {
        fun onCounter()
    }
}