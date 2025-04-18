package org.succlz123.hohoplayer.core.producer

import org.succlz123.hohoplayer.core.producer.AbsProducer

interface IProducerGroup {

    fun addEventProducer(absProducer: AbsProducer)

    fun removeEventProducer(absProducer: AbsProducer): Boolean

    fun destroy()
}