package org.succlz123.hohoplayer.core.style

import android.annotation.TargetApi
import android.graphics.Outline
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class ViewRoundRectOutlineProvider(val radius: Float,val rect: Rect?) : ViewOutlineProvider() {

    override fun getOutline(view: View, outline: Outline) {
        (rect ?: Rect().apply {
            view.getDrawingRect(this)
        }).let {
            outline.setRoundRect(it, radius)
        }
    }
}