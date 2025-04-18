package org.succlz123.hohoplayer.core.adapter.bridge

import android.view.KeyEvent
import android.view.MotionEvent
import org.succlz123.hohoplayer.core.adapter.IAdapter
import org.succlz123.hohoplayer.core.adapter.data.DataCenter
import org.succlz123.hohoplayer.core.adapter.event.OnAdapterKeyEventListener
import org.succlz123.hohoplayer.core.player.base.IPlayer
import org.succlz123.hohoplayer.core.player.listener.OnPlayerEventListener
import org.succlz123.hohoplayer.core.player.time.OnTimerUpdateListener
import org.succlz123.hohoplayer.core.touch.OnTouchGestureListener
import org.succlz123.hohoplayer.support.log.PlayerEventLog
import org.succlz123.hohoplayer.support.message.HoHoMessage
import java.util.*

interface IBridge {

    fun addChangeListener(onAdapterChange: OnAdapterChange)

    fun removeChangeListener(onAdapterChange: OnAdapterChange)

    fun addAdapter(adapter: IAdapter)

    fun removeAdapter(key: String)

    fun sort(comparator: Comparator<IAdapter>)

    fun forEach(lopper: (adapter: IAdapter) -> Unit)

    fun forEach(filter: OnAdapterFilter?, lopper: (adapter: IAdapter) -> Unit)

    fun setPlayer(player: IPlayer)

    fun getPlayer(): IPlayer?

    fun getAdapter(key: String): IAdapter?

    fun getDataCenter(): DataCenter?

    fun clear()

    interface OnAdapterFilter {

        fun filter(adapter: IAdapter?): Boolean
    }

    interface OnValueListener {

        fun keys(): Array<String>

        fun onValueUpdate(key: String, value: Any)
    }

    interface OnAdapterChange {

        fun onAdapterAdd(key: String, adapter: IAdapter)

        fun onAdapterRemove(key: String, adapter: IAdapter)
    }

    // Player
    fun dispatchPlayNormalEvent(message: HoHoMessage) {
        PlayerEventLog.onPlayEventLog(message)
        when (message.what) {
            OnPlayerEventListener.PLAYER_EVENT_ON_TIMER_UPDATE -> this.forEach {
                if (it is OnTimerUpdateListener) {
                    val current = message.getIntFromExtra("current", 0)
                    val duration = message.getIntFromExtra("duration", 0)
                    val bufferPercentage = message.getIntFromExtra("bufferPercentage", 0)
                    it.onTimerUpdate(current, duration, bufferPercentage)
                }
                it.onPlayerEvent(message)
            }
            else -> this.forEach { it.onPlayerEvent(message) }
        }
        message.recycle()
    }

    fun dispatchPlayerErrorEvent(message: HoHoMessage) {
        PlayerEventLog.onErrorEventLog(message)
        this.forEach { it.onErrorEvent(message) }
        message.recycle()
    }

    // Adapter
    fun dispatchAdapterEvent(message: HoHoMessage, filter: OnAdapterFilter? = null) {
        this.forEach(filter) { it.receiveOne2ManyAdapterEvent(message) }
        message.recycle()
    }

    // Producer
    fun dispatchProducerEvent(message: HoHoMessage, filter: OnAdapterFilter? = null) {
        this.forEach(filter) { it.receiveProducerEvent(message) }
        message.recycle()
    }

    // Touch
    fun dispatchTouchEventOnDoubleTapEvent(event: MotionEvent?): Boolean {
        var bol = false
        filterImplOnTouchEventListener {
            bol = (it as OnTouchGestureListener).onDoubleTapEvent(event)
            if (bol) {
                return@filterImplOnTouchEventListener
            }
        }
        return bol
    }

    fun dispatchTouchEventOnSingleTapConfirmed(event: MotionEvent?) {
        filterImplOnTouchEventListener {
            (it as OnTouchGestureListener).onSingleTapConfirmed(event)
        }
    }

    fun dispatchTouchEventOnDoubleTabUp(event: MotionEvent?) {
        filterImplOnTouchEventListener {
            (it as OnTouchGestureListener).onDoubleTap(event)
        }
    }

    // ---
    fun dispatchTouchEventOnDown(event: MotionEvent?) {
        filterImplOnTouchEventListener {
            (it as OnTouchGestureListener).onDown(event)
        }
    }

    fun dispatchTouchEventOnFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float) {
        filterImplOnTouchEventListener {
            (it as OnTouchGestureListener).onFling(e1, e2, velocityX, velocityY)
        }
    }

    fun dispatchTouchEventOnLongPress(event: MotionEvent?) {
        filterImplOnTouchEventListener {
            (it as OnTouchGestureListener).onLongPress(event)
        }
    }

    fun dispatchTouchEventOnScroll(
            e1: MotionEvent?,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
    ) {
        filterImplOnTouchEventListener {
            (it as OnTouchGestureListener).onScroll(e1, e2, distanceX, distanceY)
        }
    }

    fun dispatchTouchEventOnShoPress(event: MotionEvent?) {
        filterImplOnTouchEventListener {
            (it as OnTouchGestureListener).onShowPress(event)
        }
    }

    fun dispatchTouchEventOnSingleTapUp(event: MotionEvent?) {
        filterImplOnTouchEventListener {
            (it as OnTouchGestureListener).onSingleTapUp(event)
        }
    }

    fun dispatchTouchEventOnEndGesture(event: MotionEvent?) {
        filterImplOnTouchEventListener {
            (it as OnTouchGestureListener).onEndGesture(event)
        }
    }

    private fun filterImplOnTouchEventListener(onLoopListener: (adapter: IAdapter) -> Unit) {
        this.forEach(object : OnAdapterFilter {
            override fun filter(adapter: IAdapter?): Boolean {
                return adapter is OnTouchGestureListener
            }
        }) { onLoopListener.invoke(it) }
    }

    // Key
    fun dispatchKeyEventOnKeyDown(keyCode: Int, event: KeyEvent?) {
        filterImplOnKeyEventListener {
            (it as OnAdapterKeyEventListener).onKeyDown(keyCode, event)
        }
    }

    fun dispatchKeyEventOnKeyUp(keyCode: Int, event: KeyEvent?) {
        filterImplOnKeyEventListener {
            (it as OnAdapterKeyEventListener).onKeyUp(keyCode, event)
        }
    }

    fun dispatchKeyEventOnLongPress(keyCode: Int, event: KeyEvent?) {
        filterImplOnKeyEventListener {
            (it as OnAdapterKeyEventListener).onKeyLongPress(keyCode, event)
        }
    }

    fun dispatchKeyEventDispatchKeyEvent(event: KeyEvent?) {
        filterImplOnKeyEventListener {
            (it as OnAdapterKeyEventListener).dispatchKeyEvent(event)
        }
    }

    private fun filterImplOnKeyEventListener(onLoopListener: (adapter: IAdapter) -> Unit) {
        this.forEach(object : OnAdapterFilter {
            override fun filter(adapter: IAdapter?): Boolean {
                return adapter is OnAdapterKeyEventListener
            }
        }) { onLoopListener.invoke(it) }
    }
}