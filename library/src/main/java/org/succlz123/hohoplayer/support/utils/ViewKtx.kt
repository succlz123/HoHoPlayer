package org.succlz123.hohoplayer.support.utils

import android.content.Context
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
}