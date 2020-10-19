package org.succlz123.hohoplayer.window

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager

class FloatWindowParams {
    var windowType = WindowManager.LayoutParams.TYPE_PHONE
    var gravity = Gravity.TOP or Gravity.START
    var format = PixelFormat.RGBA_8888
    var flag = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
    var x = 0
    var y = 0
    var width = WindowManager.LayoutParams.WRAP_CONTENT
    var height = WindowManager.LayoutParams.WRAP_CONTENT
    var isDefaultAnimation = true
}