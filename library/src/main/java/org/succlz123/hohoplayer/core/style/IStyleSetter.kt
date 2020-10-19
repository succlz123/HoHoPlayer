package org.succlz123.hohoplayer.core.style

import android.graphics.Rect

interface IStyleSetter {

    fun setRoundRectShape(radius: Float)

    fun setRoundRectShape(rect: Rect?, radius: Float)

    fun setOvalRectShape()

    fun setOvalRectShape(rect: Rect?)

    fun clearShapeStyle()

    fun setElevationShadow(elevation: Float)

    fun setElevationShadow(backgroundColor: Int, elevation: Float)
}