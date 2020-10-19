package org.succlz123.hohoplayer.app.support

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.graphics.Point
import android.os.Build
import android.text.TextUtils
import android.util.DisplayMetrics
import android.view.Display

internal object PlayerKtx {

    fun Activity.getScreenRealHeight(): Int {
        var heightPixels: Int
        val w = windowManager
        val d = w.defaultDisplay
        val metrics = DisplayMetrics()
        d.getMetrics(metrics)
        // since SDK_INT = 1
        heightPixels = metrics.heightPixels
        // includes window decorations (statusbar bar/navigation bar)
        if (Build.VERSION.SDK_INT >= 14 && Build.VERSION.SDK_INT < 17) try {
            heightPixels = Display::class.java.getMethod("getRawHeight").invoke(d) as Int
        } catch (ignored: Exception) {
        } else if (Build.VERSION.SDK_INT >= 17) try {
            val realSize = Point()
            Display::class.java.getMethod("getRealSize", Point::class.java).invoke(d, realSize)
            heightPixels = realSize.y
        } catch (ignored: Exception) {
        }
        return heightPixels
    }

    fun Context.dp2px(dpValue: Float): Int {
        val scale = resources.displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    fun Context.px2dp(context: Context, pxValue: Float): Int {
        val scale = context.resources.displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    fun Context.screenWidth(): Int {
        val displayMetrics = resources.displayMetrics
        return displayMetrics.widthPixels
    }

    fun Context.screenHeight(): Int {
        val displayMetrics = resources.displayMetrics
        return displayMetrics.heightPixels
    }

    fun Context?.activity(): Activity? {
        return if (this is Activity) {
            this
        } else {
            null
        }
    }

    fun Activity?.isTopActivity(): Boolean {
        return this != null && isTopActivity(this.javaClass.name)
    }

    fun Context?.isTopActivity(activityName: String): Boolean {
        return this.isForeground(activityName)
    }

    fun Context?.isForeground(className: String): Boolean {
        if (this == null || TextUtils.isEmpty(className)) {
            return false
        }
        val am = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val list = am.getRunningTasks(1)
        if (list != null && list.size > 0) {
            val cpn = list[0].topActivity
            if (className == cpn!!.className) {
                return true
            }
        }
        return false
    }
}