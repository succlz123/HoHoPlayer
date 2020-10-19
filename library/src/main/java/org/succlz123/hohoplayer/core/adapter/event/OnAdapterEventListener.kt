package org.succlz123.hohoplayer.core.adapter.event

import org.succlz123.hohoplayer.support.message.HoHoMessage

interface OnAdapterEventListener {

    fun onAdapterEvent(message: HoHoMessage)
}