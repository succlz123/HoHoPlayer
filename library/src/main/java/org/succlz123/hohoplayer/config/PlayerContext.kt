package org.succlz123.hohoplayer.config

import android.content.Context

object PlayerContext {

    var appContext: Context? = null

    fun context(): Context {
        return appContext ?: throw RuntimeException("app context not init !!!")
    }
}
