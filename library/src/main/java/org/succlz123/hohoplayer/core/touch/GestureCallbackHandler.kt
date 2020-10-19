package org.succlz123.hohoplayer.core.touch

import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent

class GestureCallbackHandler(private val onTouchGestureListener: OnTouchGestureListener) : SimpleOnGestureListener() {
    var gestureEnable = false
    var gestureScrollEnable = false

    // OnDoubleTapListener
    override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
        return onTouchGestureListener.onDoubleTapEvent(e)
    }

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        return onTouchGestureListener.onSingleTapConfirmed(e)
    }

    override fun onDoubleTap(e: MotionEvent?): Boolean {
        return onTouchGestureListener.onDoubleTap(e)
    }

    // OnGestureListener
    override fun onDown(e: MotionEvent?): Boolean {
        onTouchGestureListener.onDown(e)
        return gestureEnable
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        return onTouchGestureListener.onFling(e1, e2, velocityX, velocityY)
    }

    override fun onLongPress(e: MotionEvent?) {
        onTouchGestureListener.onLongPress(e)
        super.onLongPress(e)
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (gestureScrollEnable) {
            onTouchGestureListener.onScroll(e1, e2, distanceX, distanceY)
        }
        return super.onScroll(e1, e2, distanceX, distanceY)
    }

    override fun onShowPress(e: MotionEvent?) {
        onTouchGestureListener.onShowPress(e)
        super.onShowPress(e)
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return onTouchGestureListener.onSingleTapUp(e)
    }

    fun onEndGesture(event: MotionEvent?) {
        onTouchGestureListener.onEndGesture(event)
    }
}
