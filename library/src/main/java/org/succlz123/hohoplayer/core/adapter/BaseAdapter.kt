package org.succlz123.hohoplayer.core.adapter

import org.succlz123.hohoplayer.core.adapter.bridge.IBridge
import org.succlz123.hohoplayer.core.adapter.data.DataCenter
import org.succlz123.hohoplayer.core.adapter.event.OnAdapterEventListener
import org.succlz123.hohoplayer.support.log.PlayerLog.d
import org.succlz123.hohoplayer.support.log.PlayerLog.e
import org.succlz123.hohoplayer.support.message.HoHoMessage
import java.lang.NullPointerException

abstract class BaseAdapter : IAdapter {
    var hostBridge: IBridge? = null

    private var onAdapterEventListener: OnAdapterEventListener? = null

    override fun bindBridge(iBridge: IBridge) {
        hostBridge = iBridge
    }

    override fun onAdapterBind() {
    }

    override fun onAdapterUnBind() {
    }

    protected fun getDataCenter(): DataCenter {
        return hostBridge?.getDataCenter() ?: throw NullPointerException("What is up, dude!")
    }

    override fun setAdapterEventListener(listener: OnAdapterEventListener?) {
        onAdapterEventListener = listener
    }

    override fun onPlayerEvent(message: HoHoMessage) {}

    override fun onErrorEvent(message: HoHoMessage) {}

    override fun sendOne2ManyAdapterEvent(message: HoHoMessage) {
        hostBridge?.forEach {
            it.receiveOne2ManyAdapterEvent(message)
        }
        onAdapterEventListener?.onAdapterEvent(message)
    }

    override fun receiveOne2ManyAdapterEvent(message: HoHoMessage) {}

    override fun sendOne2OneAdapterEvent(targetKey: String, message: HoHoMessage): HoHoMessage? {
        if (targetKey.isNotEmpty()) {
            val iAdapter = hostBridge?.getAdapter(targetKey)
            if (iAdapter != null) {
                return iAdapter.receiveOne2OneAdapterEvent(message)
            } else {
                e("notifyAdapterEvent", "not found adapter use you incoming key.")
            }
        }
        onAdapterEventListener?.onAdapterEvent(message)
        return null
    }

    override fun receiveOne2OneAdapterEvent(message: HoHoMessage): HoHoMessage? {
        return null
    }

    override fun receiveProducerEvent(message: HoHoMessage) {}
}