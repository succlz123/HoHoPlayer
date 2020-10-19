package org.succlz123.hohoplayer.core.adapter

import org.succlz123.hohoplayer.core.adapter.bridge.IBridge
import org.succlz123.hohoplayer.core.adapter.event.OnAdapterEventListener
import org.succlz123.hohoplayer.support.message.HoHoMessage

interface IAdapter {

    val key: String

    // bind - unbind
    fun bindBridge(iBridge: IBridge)

    fun onAdapterBind()

    fun onAdapterUnBind()

    // player
    fun onPlayerEvent(message: HoHoMessage)

    fun onErrorEvent(message: HoHoMessage)

    // adapter send - receive
    // one-many event
    fun sendOne2ManyAdapterEvent(message: HoHoMessage)

    fun receiveOne2ManyAdapterEvent(message: HoHoMessage)

    // one-one event
    fun sendOne2OneAdapterEvent(targetKey: String, message: HoHoMessage): HoHoMessage?

    fun receiveOne2OneAdapterEvent(message: HoHoMessage): HoHoMessage?

    fun setAdapterEventListener(listener: OnAdapterEventListener?)

    // producer
    fun receiveProducerEvent(message: HoHoMessage)
}
