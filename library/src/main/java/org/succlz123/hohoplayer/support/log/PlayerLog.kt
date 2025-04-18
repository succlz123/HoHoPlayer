package org.succlz123.hohoplayer.support.log

import android.util.Log

object PlayerLog {
    var LOG_OPEN = false

    fun d(tag: String?, message: String?) {
        if (!LOG_OPEN) {
            return
        }
        message ?: return
        Log.d(tag, message)
    }

    fun w(tag: String?, message: String?) {
        if (!LOG_OPEN) {
            return
        }
        message ?: return
        Log.w(tag, message)
    }

    fun e(tag: String?, message: String?) {
        if (!LOG_OPEN) {
            return
        }
        message ?: return
        Log.e(tag, message)
    }
}