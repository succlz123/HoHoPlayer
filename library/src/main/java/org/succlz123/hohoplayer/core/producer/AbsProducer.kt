package org.succlz123.hohoplayer.core.producer

abstract class AbsProducer {

    var sender: ProducerEventSender? = null

    open fun onAdded() {}

    open fun onRemoved() {}

    open fun destroy() {}
}
