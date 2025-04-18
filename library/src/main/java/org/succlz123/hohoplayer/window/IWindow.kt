package org.succlz123.hohoplayer.window

import android.animation.Animator

interface IWindow {

    companion object {
        const val MIN_MOVE_DISTANCE = 20
        const val DURATION_ANIMATION = 200
    }

    fun setOnWindowListener(onWindowListener: OnWindowListener?)

    fun updateWindowViewLayout(x: Int, y: Int)

    fun setDragEnable(dragEnable: Boolean)

    fun show(): Boolean

    fun show(vararg items: Animator?): Boolean

    fun close()

    fun close(vararg items: Animator?)

    fun isWindowShow(): Boolean

    interface OnWindowListener {
        fun onShow()
        fun onClose()
    }
}