package org.succlz123.hohoplayer.window

import android.animation.Animator
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import org.succlz123.hohoplayer.core.style.IStyleSetter
import org.succlz123.hohoplayer.core.style.StyleSetter
import org.succlz123.hohoplayer.window.IWindow.OnWindowListener

class FloatWindow(context: Context, windowView: View, params: FloatWindowParams) : FrameLayout(context), IWindow, IStyleSetter {
    private val mStyleSetter: IStyleSetter

    private val mWindowHelper: WindowHelper

    private var onWindowListener: OnWindowListener? = null

    private val mInternalWindowListener: OnWindowListener = object : OnWindowListener {
        override fun onShow() {
            onWindowListener?.onShow()
        }

        override fun onClose() {
            resetStyle()
            onWindowListener?.onClose()
        }
    }

    override fun setOnWindowListener(onWindowListener: OnWindowListener?) {
        this.onWindowListener = onWindowListener
    }

    override fun setRoundRectShape(radius: Float) {
        mStyleSetter.setRoundRectShape(radius)
    }

    override fun setRoundRectShape(rect: Rect?, radius: Float) {
        mStyleSetter.setRoundRectShape(rect, radius)
    }

    override fun setOvalRectShape() {
        mStyleSetter.setOvalRectShape()
    }

    override fun setOvalRectShape(rect: Rect?) {
        mStyleSetter.setOvalRectShape(rect)
    }

    override fun clearShapeStyle() {
        mStyleSetter.clearShapeStyle()
    }

    /**
     * set shadow.
     * @param elevation
     */
    override fun setElevationShadow(elevation: Float) {
        setElevationShadow(Color.BLACK, elevation)
    }

    /**
     * must setting a color when set shadow, not transparent.
     */
    override fun setElevationShadow(backgroundColor: Int, elevation: Float) {
        setBackgroundColor(backgroundColor)
        if (Build.VERSION.SDK_INT >= 21) {
            this.elevation = elevation
        }
    }

    override fun setDragEnable(dragEnable: Boolean) {
        mWindowHelper.setDragEnable(dragEnable)
    }

    override fun isWindowShow(): Boolean {
        return mWindowHelper.isWindowShow()
    }

    override fun updateWindowViewLayout(x: Int, y: Int) {
        mWindowHelper.updateWindowViewLayout(x, y)
    }

    /**
     * add to WindowManager
     * @return
     */
    override fun show(): Boolean {
        return mWindowHelper.show()
    }

    override fun show(vararg items: Animator?): Boolean {
        return mWindowHelper.show(*items)
    }

    /**
     * remove from WindowManager
     *
     * @return
     */
    override fun close() {
        setElevationShadow(0f)
        mWindowHelper.close()
    }

    override fun close(vararg items: Animator?) {
        setElevationShadow(0f)
        mWindowHelper.close(*items)
    }

    fun resetStyle() {
        setElevationShadow(0f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            clearShapeStyle()
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return mWindowHelper.onInterceptTouchEvent(ev) || super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return mWindowHelper.onTouchEvent(event) || super.onTouchEvent(event)
    }

    init {
        addView(windowView)
        mStyleSetter = StyleSetter(this)
        mWindowHelper = WindowHelper(context, this, params)
        mWindowHelper.setOnWindowListener(mInternalWindowListener)
    }
}