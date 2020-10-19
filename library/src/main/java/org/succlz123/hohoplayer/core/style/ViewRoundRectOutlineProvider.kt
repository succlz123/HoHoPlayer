package org.succlz123.hohoplayer.core.style

import android.annotation.TargetApi
import android.graphics.Outline
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class ViewRoundRectOutlineProvider : ViewOutlineProvider {
    private var radius: Float
    private var rect: Rect? = null

    constructor(radius: Float) {
        this.radius = radius
    }

    constructor(radius: Float, rect: Rect?) {
        this.radius = radius
        this.rect = rect
    }

    override fun getOutline(view: View, outline: Outline) {
        val selfRect = if (rect != null) {
            rect
        } else {
            val rect = Rect()
            view.getGlobalVisibleRect(rect)
            val leftMargin = 0
            val topMargin = 0
            Rect(leftMargin, topMargin,
                    rect.right - rect.left - leftMargin, rect.bottom - rect.top - topMargin)
        }
        selfRect?.let {
            outline.setRoundRect(it, radius)
        }
    }
}