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
        (rect ?: Rect().run {
            view.getGlobalVisibleRect(this)
            getOvalRect(this)
        }).let {
            outline.setOval(it)
        }
    }

    private fun getOvalRect(re: Rect): Rect {
        val width = re.right - re.left
        val height = re.bottom - re.top

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
