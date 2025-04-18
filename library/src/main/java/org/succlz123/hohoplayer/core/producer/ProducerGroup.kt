package org.succlz123.hohoplayer.core.producer

import org.succlz123.hohoplayer.core.producer.AbsProducer
import org.succlz123.hohoplayer.core.producer.IProducerGroup
import org.succlz123.hohoplayer.core.producer.ProducerEventSender
import java.util.concurrent.CopyOnWriteArrayList

class ProducerGroup(private val eventSender: ProducerEventSender) :
        IProducerGroup {
    private val eventProducers = CopyOnWriteArrayList<AbsProducer>()

    override fun addEventProducer(absProducer: AbsProducer) {
        if (!eventProducers.contains(absProducer)) {
            absProducer.sender = eventSender
            eventProducers.add(absProducer)
            absProducer.onAdded()
        }
    }

    override fun removeEventProducer(absProducer: AbsProducer): Boolean {
        val remove = eventProducers.remove(absProducer)
        if (remove) {
            absProducer.onRemoved()
            absProducer.sender = null
        }
        return remove
    }

    override fun destroy() {
        for (eventProducer in eventProducers) {
            eventProducer.onRemoved()
            eventProducer.destroy()
            eventProducer.sender = null
        }
        eventProducers.clear()
    }
}
