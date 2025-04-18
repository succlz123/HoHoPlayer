package org.succlz123.hohoplayer.core.touch

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent

class ContainerTouchHelper(context: Context, private val gestureCallback: GestureCallbackHandler) {
    private val gestureDetector = GestureDetector(context, gestureCallback)

    fun onTouch(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_UP -> {
                onEndGesture(event)
            }
        }
        return gestureDetector.onTouchEvent(event)
    }

    fun setGestureEnable(enable: Boolean) {
        gestureCallback.gestureEnable = enable
    }

    fun setGestureScrollEnable(enable: Boolean) {
        gestureCallback.gestureScrollEnable = enable
    }

    fun onEndGesture(event: MotionEvent?) {
        gestureCallback.onEndGesture(event)
    }
}
