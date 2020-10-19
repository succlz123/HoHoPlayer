package org.succlz123.hohoplayer.core.producer

import org.succlz123.hohoplayer.core.adapter.bridge.IBridge.OnAdapterFilter
import org.succlz123.hohoplayer.support.message.HoHoMessage

interface ProducerEventSender {

    fun sendEvent(message: HoHoMessage, adapterFilter: OnAdapterFilter? = null)
}
