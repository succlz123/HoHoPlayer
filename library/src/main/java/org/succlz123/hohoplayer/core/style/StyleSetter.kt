package org.succlz123.hohoplayer.core.style

import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.view.View

/**
 * style setter ,include round rect,oval rect and shadow.
 * The Shape Style settings support only more than LOLLIPOP.
 */
class StyleSetter(private val view: View) :
        IStyleSetter {

    override fun setRoundRectShape(radius: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setRoundRectShape(null, radius)
        }
    }

    override fun setRoundRectShape(rect: Rect?, radius: Float) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.clipToOutline = true
            view.outlineProvider =
                ViewRoundRectOutlineProvider(
                    radius,
                    rect
                )
        }
    }

    override fun setOvalRectShape() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setOvalRectShape(null)
        }
    }

    override fun setOvalRectShape(rect: Rect?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.clipToOutline = true
            view.outlineProvider =
                ViewOvalRectOutlineProvider(rect)
        }
    }

    override fun clearShapeStyle() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view.clipToOutline = false
        }
    }

    override fun setElevationShadow(elevation: Float) {
        setElevationShadow(Color.BLACK, elevation)
    }

    override fun setElevationShadow(backgroundColor: Int, elevation: Float) {
        view.setBackgroundColor(backgroundColor)
        if (Build.VERSION.SDK_INT >= 21) {
            view.elevation = elevation
        }
        view.invalidate()
    }
}