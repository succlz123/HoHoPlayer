package org.succlz123.hohoplayer.window

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import org.succlz123.hohoplayer.support.utils.ViewKtx.getStatusBarHeight
import org.succlz123.hohoplayer.window.IWindow.OnWindowListener
import kotlin.math.abs

class WindowHelper(context: Context, private val mWindowView: View, params: FloatWindowParams) :
        IWindow {
    private val wmParams: WindowManager.LayoutParams
    private val wm: WindowManager?
    private var isWindowShow = false
    private var mDragEnable = true
    private var mShowAnimatorSet: AnimatorSet? = null
    private var mCloseAnimatorSet: AnimatorSet? = null
    private val defaultAnimation: Boolean
    private var mOnWindowListener: OnWindowListener? = null

    override fun setOnWindowListener(onWindowListener: OnWindowListener?) {
        mOnWindowListener = onWindowListener
    }

    override fun setDragEnable(dragEnable: Boolean) {
        mDragEnable = dragEnable
    }

    override fun updateWindowViewLayout(x: Int, y: Int) {
        wmParams.x = x
        wmParams.y = y
        wm!!.updateViewLayout(mWindowView, wmParams)
    }

    override fun isWindowShow(): Boolean {
        return isWindowShow
    }

    override fun show(): Boolean {
        return show(*(if (defaultAnimation) getDefaultAnimators(true) else null)!!)
    }

    override fun show(vararg items: Animator?): Boolean {
        val addToWindow = addToWindow()
        if (!addToWindow) return false
        val parent = mWindowView.parent
        parent?.requestLayout()
        if (items != null && items.isNotEmpty()) {
            cancelCloseAnimation()
            cancelShowAnimation()
            mShowAnimatorSet = AnimatorSet()
            mShowAnimatorSet!!.playTogether(*items)
            mShowAnimatorSet!!.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mShowAnimatorSet!!.removeAllListeners()
                }
            })
            mShowAnimatorSet!!.start()
        }
        if (mOnWindowListener != null) {
            mOnWindowListener!!.onShow()
        }
        return true
    }

    private fun cancelShowAnimation() {
        if (mShowAnimatorSet != null) {
            mShowAnimatorSet!!.cancel()
            mShowAnimatorSet!!.removeAllListeners()
        }
    }

    private fun addToWindow(): Boolean {
        return if (wm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (!mWindowView.isAttachedToWindow) {
                    wm.addView(mWindowView, wmParams)
                    isWindowShow = true
                    true
                } else {
                    false
                }
            } else {
                try {
                    if (mWindowView.parent == null) {
                        wm.addView(mWindowView, wmParams)
                        isWindowShow = true
                    }
                    true
                } catch (e: Exception) {
                    false
                }
            }
        } else {
            false
        }
    }

    private fun getDefaultAnimators(showAnimators: Boolean): Array<Animator> {
        val startV: Float = if (showAnimators) 0f else 1f
        val endV: Float = if (showAnimators) 1f else 0f
        return arrayOf(
                ObjectAnimator.ofFloat(mWindowView, "scaleX", startV, endV).setDuration(
                        IWindow.DURATION_ANIMATION.toLong()),
                ObjectAnimator.ofFloat(mWindowView, "scaleY", startV, endV).setDuration(
                        IWindow.DURATION_ANIMATION.toLong()),
                ObjectAnimator.ofFloat(mWindowView, "alpha", startV, endV).setDuration(
                        IWindow.DURATION_ANIMATION.toLong())
        )
    }

    /**
     * remove from WindowManager
     *
     * @return
     */
    override fun close() {
        close(*(if (defaultAnimation) getDefaultAnimators(false) else null)!!)
    }

    override fun close(vararg items: Animator?) {
        if (items != null && items.isNotEmpty()) {
            cancelShowAnimation()
            cancelCloseAnimation()
            mCloseAnimatorSet = AnimatorSet()
            mCloseAnimatorSet!!.playTogether(*items)
            mCloseAnimatorSet!!.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mCloseAnimatorSet!!.removeAllListeners()
                    removeFromWindow()
                }
            })
            mCloseAnimatorSet!!.start()
        } else {
            removeFromWindow()
        }
    }

    private fun cancelCloseAnimation() {
        if (mCloseAnimatorSet != null) {
            mCloseAnimatorSet!!.cancel()
            mCloseAnimatorSet!!.removeAllListeners()
        }
    }

    private fun removeFromWindow(): Boolean {
        var isClose = false
        if (wm != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                if (mWindowView.isAttachedToWindow) {
                    wm.removeViewImmediate(mWindowView)
                    isWindowShow = false
                    isClose = true
                }
            } else {
                try {
                    if (mWindowView.parent != null) {
                        wm.removeViewImmediate(mWindowView)
                        isWindowShow = false
                        isClose = true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        if (isClose && mOnWindowListener != null) {
            mOnWindowListener!!.onClose()
        }
        return isClose
    }

    fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (!mDragEnable) return false
        when (ev.action) {
            MotionEvent.ACTION_DOWN -> {
                mDownX = ev.rawX
                mDownY = ev.rawY
            }
            MotionEvent.ACTION_MOVE -> {
                return (abs(ev.rawX - mDownX) > IWindow.MIN_MOVE_DISTANCE || abs(ev.rawY - mDownY) > IWindow.MIN_MOVE_DISTANCE)
            }
        }
        return false
    }

    private var mDownX = 0f
    private var mDownY = 0f
    private var floatX = 0
    private var floatY = 0
    private var firstTouch = true
    private var wX = 0
    private var wY = 0
    fun onTouchEvent(event: MotionEvent): Boolean {
        if (!mDragEnable) return false
        val X = event.rawX.toInt()
        val Y = event.rawY.toInt()
        when (event.action) {
            MotionEvent.ACTION_UP -> firstTouch = true
            MotionEvent.ACTION_MOVE -> {
                if (firstTouch) {
                    floatX = event.x.toInt()
                    floatY = (event.y + mWindowView.context.getStatusBarHeight()).toInt()
                    firstTouch = false
                }
                wX = X - floatX
                wY = Y - floatY
                updateWindowViewLayout(wX, wY)
            }
        }
        return false
    }

    init {
        wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wmParams = WindowManager.LayoutParams()
        wmParams.type = params.windowType
        wmParams.gravity = params.gravity
        wmParams.format = params.format
        wmParams.flags = params.flag
        wmParams.width = params.width
        wmParams.height = params.height
        wmParams.x = params.x
        wmParams.y = params.y
        defaultAnimation = params.isDefaultAnimation
    }
}