package org.succlz123.hohoplayer.core.style

import android.annotation.TargetApi
import android.graphics.Outline
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class ViewOvalRectOutlineProvider(private val rect: Rect?) : ViewOutlineProvider() {

    override fun getOutline(view: View, outline: Outline) {
        val selfRect: Rect
        selfRect = if (rect != null) {
            rect
        } else {
            val rect = Rect()
            view.getGlobalVisibleRect(rect)
            rect.getOvalRect()
        }
        outline.setOval(selfRect)
    }

    private fun Rect.getOvalRect(): Rect {
        val width = right - left
        val height = bottom - top
        val left: Int
        val top: Int
        val right: Int
        val bottom: Int
        val dW = width / 2
        val dH = height / 2
        if (width > height) {
            left = dW - dH
            top = 0
            right = dW + dH
            bottom = dH * 2
        } else {
            left = dH - dW
            top = 0
            right = dH + dW
            bottom = dW * 2
        }
        return Rect(left, top, right, bottom)
    }
}
