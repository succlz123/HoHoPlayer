package org.succlz123.hohoplayer.core.touch

import android.view.MotionEvent

// https://juejin.im/post/6844903874151579662
interface OnTouchGestureListener {

    // OnDoubleTapListener
    fun onDoubleTapEvent(e: MotionEvent?): Boolean {
        // double click down!
        return false
    }

    fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        // single click!
        return false
    }

    fun onDoubleTap(e: MotionEvent?): Boolean {
        // double click up!
        return false
    }

    // OnGestureListener
    fun onDown(e: MotionEvent?): Boolean {
        // onDown
        return false
    }

    fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    fun onLongPress(e: MotionEvent?) {}

    fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        return false
    }

    fun onShowPress(e: MotionEvent?) {}

    fun onSingleTapUp(e: MotionEvent?): Boolean {
        return false
    }

    fun onEndGesture(event: MotionEvent?) {}
}