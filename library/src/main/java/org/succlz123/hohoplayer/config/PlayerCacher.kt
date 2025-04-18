package org.succlz123.hohoplayer.config

import android.content.Context
import com.danikula.videocache.HttpProxyCacheServer

object PlayerCacher {
    private var context: Context? = null
    private var maxSize: Long = 1024 * 1024 * 256
    private var proxy: HttpProxyCacheServer? = null

    fun init(context: Context, maxSize: Long ) {
        PlayerCacher.context = context
        PlayerCacher.maxSize = maxSize
    }

    fun getProxyUrl(url: String): String {
        val p = getProxy()
        return if (p != null) {
            p.getProxyUrl(url)
        } else {
            url
        }
    }

    private fun getProxy(): HttpProxyCacheServer? {
        if (context == null) {
            return null
        }
        return if (proxy == null) {
            HttpProxyCacheServer.Builder(context)
                .maxCacheSize(maxSize)
                .build().also { proxy = it }
        } else {
            proxy
        }
    }
}
