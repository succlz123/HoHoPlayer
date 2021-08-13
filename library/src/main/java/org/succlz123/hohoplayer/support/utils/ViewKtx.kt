package org.succlz123.hohoplayer.support.utils

import android.app.Activity
import android.app.Application
import android.app.Service
import android.content.Context
import android.content.ContextWrapper
import kotlin.math.ceil


internal object ViewKtx {

    internal fun Context.getStatusBarHeight(): Int {
        var height = getStatusBarHeightMethod1()
        if (height <= 0) {
            height = getStatusBarHeightMethod2()
        }
        return height
    }

    internal fun Context.getStatusBarHeightMethod1(): Int {
        var statusBarHeight = -1
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            statusBarHeight = resources.getDimensionPixelSize(resourceId)
        }
        return statusBarHeight
    }

    internal fun Context.getStatusBarHeightMethod2(): Int {
        return ceil(20 * resources.displayMetrics.density.toDouble()).toInt()
    }

    internal fun Context?.getActivity(): Activity? {
        if (this == null) {
            return null
        }
        if (this is Activity) {
            return this
        }
        if (this is Application || this is Service) {
            return null
        }
        var c = this
        while (c != null) {
            if (c is ContextWrapper) {
                c = c.baseContext
                if (c is Activity) {
                    return c
                }
            } else {
                return null
            }
        }
        return null
    }
}