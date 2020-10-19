package org.succlz123.hohoplayer.window

import android.animation.Animator
import android.content.Context
import android.os.Build
import android.view.MotionEvent
import org.succlz123.hohoplayer.widget.videoview.VideoView
import org.succlz123.hohoplayer.window.IWindow.OnWindowListener

class WindowVideoView(context: Context, params: FloatWindowParams) : VideoView(context), IWindow {
    private val mWindowHelper: WindowHelper = WindowHelper(context, this, params)
    private var onWindowListener: OnWindowListener? = null

    init {
        mWindowHelper.setOnWindowListener(object : OnWindowListener {
            override fun onShow() {
                onWindowListener?.onShow()
            }

            override fun onClose() {
                stop()
                resetStyle()
                onWindowListener?.onClose()
            }
        })
    }

    override fun setOnWindowListener(onWindowListener: OnWindowListener?) {
        this.onWindowListener = onWindowListener
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

    override fun show(): Boolean {
        return mWindowHelper.show()
    }

    override fun show(vararg items: Animator?): Boolean {
        return mWindowHelper.show(*items)
    }

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
}