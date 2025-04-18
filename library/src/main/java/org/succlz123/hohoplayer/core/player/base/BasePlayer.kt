package org.succlz123.hohoplayer.core.player.base

import org.succlz123.hohoplayer.core.player.listener.OnBufferingListener
import org.succlz123.hohoplayer.core.player.listener.OnErrorEventListener
import org.succlz123.hohoplayer.core.player.listener.OnPlayerEventListener
import org.succlz123.hohoplayer.support.message.HoHoMessage

abstract class BasePlayer : IPlayer {
    private var currentState = IPlayer.STATE_IDLE

    private var percentage = 0

    private var playerLooping = false

    var onPlayerEventListener: OnPlayerEventListener? = null

    var onErrorEventListener: OnErrorEventListener? = null

    var onBufferingListener: OnBufferingListener? = null

    protected fun submitPlayerEvent(message: HoHoMessage) {
        onPlayerEventListener?.onPlayerEvent(message)
    }

    protected fun submitErrorEvent(message: HoHoMessage) {
        onErrorEventListener?.onErrorEvent(message)
    }

    protected fun submitBufferingUpdate(percentage: Int) {
        this.percentage = percentage
        onBufferingListener?.onBufferingUpdate(percentage)
    }

    protected fun updateStatus(status: Int) {
        currentState = status
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_STATUS_CHANGE, argInt = status))
    }

    open fun isLooping(): Boolean {
        return playerLooping
    }

    override fun setLooping(looping: Boolean) {
        playerLooping = looping
    }

    override fun getState(): Int {
        return currentState
    }

    override fun getBufferPercentage(): Int {
        return percentage
    }
}