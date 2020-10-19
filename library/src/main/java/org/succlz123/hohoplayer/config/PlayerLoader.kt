package org.succlz123.hohoplayer.config

import org.succlz123.hohoplayer.config.PlayerConfig.getDecoder
import org.succlz123.hohoplayer.core.player.base.BasePlayer
import org.succlz123.hohoplayer.support.log.PlayerLog

object PlayerLoader {
    private const val TAG = "PlayerLoader"

    fun loadDecoderPlayer(decoderName: String): BasePlayer? {
        return try {
            val decoderInstance = getDecoderInstance(decoderName)
            if (decoderInstance is BasePlayer) {
                decoderInstance
            } else {
                null
            }
        } catch (e: Exception) {
            PlayerLog.d("$TAG loadInternalPlayer", e.toString())
            null
        }
    }

    private fun getDecoderInstance(decoderName: String): Any? {
        return try {
            getDecoder(decoderName)?.let {
                val clz = Class.forName(it.classPath) ?: return@let null
                clz.getConstructor()?.newInstance()
            }
        } catch (e: Exception) {
            PlayerLog.d("$TAG getDecoderInstance", e.toString())
            null
        }
    }
}