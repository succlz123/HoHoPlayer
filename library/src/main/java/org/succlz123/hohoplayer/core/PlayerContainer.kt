package org.succlz123.hohoplayer.core

import android.content.Context
import android.view.*
import android.widget.FrameLayout
import org.succlz123.hohoplayer.core.adapter.BaseCoverAdapter
import org.succlz123.hohoplayer.core.adapter.IAdapter
import org.succlz123.hohoplayer.core.adapter.bridge.BridgeAdapterComparator
import org.succlz123.hohoplayer.core.adapter.bridge.IBridge
import org.succlz123.hohoplayer.core.adapter.bridge.IBridge.OnAdapterFilter
import org.succlz123.hohoplayer.core.adapter.cover.AbsCoverContainer
import org.succlz123.hohoplayer.core.adapter.cover.LevelCoverContainer
import org.succlz123.hohoplayer.core.adapter.event.OnAdapterEventListener
import org.succlz123.hohoplayer.core.producer.IProducerGroup
import org.succlz123.hohoplayer.core.producer.ProducerEventSender
import org.succlz123.hohoplayer.core.producer.ProducerGroup
import org.succlz123.hohoplayer.core.touch.ContainerTouchHelper
import org.succlz123.hohoplayer.core.touch.GestureCallbackHandler
import org.succlz123.hohoplayer.core.touch.OnTouchGestureListener
import org.succlz123.hohoplayer.support.log.PlayerLog.d
import org.succlz123.hohoplayer.support.message.HoHoMessage

/**
 * View Hierarchy
 *
 * - ViewGroup -> The layout given by the user
 *      - FrameLayout -> PlayerContainer
 *          - FrameLayout -> RenderContainer
 *          - FrameLayout -> CoverContainer
 *
 *          - Function1 -> ProducerGroup
 *          - Function2 -> ContainerTouchHelper
 */
class PlayerContainer(context: Context) : FrameLayout(context), OnTouchGestureListener {

    companion object {
        const val TAG = "PlayerContainer"
    }

    private lateinit var renderContainer: FrameLayout

    private lateinit var coverContainer: AbsCoverContainer

    private var bridge: IBridge? = null

    lateinit var producerGroup: IProducerGroup
        private set

    private lateinit var touchHelper: ContainerTouchHelper

    var onAdapterEventListener: OnAdapterEventListener? = null

    private fun init(context: Context) {
        // init Producer Group
        producerGroup = ProducerGroup(object : ProducerEventSender {
            override fun sendEvent(message: HoHoMessage, adapterFilter: OnAdapterFilter?) {
                bridge?.dispatchProducerEvent(message, adapterFilter)
            }
        })
        // init Gesture Helper
        touchHelper = ContainerTouchHelper(context, GestureCallbackHandler(this))
        touchHelper.setGestureEnable(true)
        // init RenderContainer
        renderContainer = FrameLayout(context)
        addView(
                renderContainer,
                ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        )
        // init LevelCoverContainer
        coverContainer = LevelCoverContainer(context)
        addView(
                coverContainer.getContainerView(),
                ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return touchHelper.onTouch(event)
    }

    fun videoViewOnKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        bridge?.dispatchKeyEventOnKeyDown(keyCode, event)
        return true
    }

    fun videoViewOnKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        bridge?.dispatchKeyEventOnKeyUp(keyCode, event)
        return true
    }

    fun videoViewOnKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean {
        bridge?.dispatchKeyEventOnLongPress(keyCode, event)
        return true
    }

    fun videoViewDispatchKeyEvent(event: KeyEvent?): Boolean {
        bridge?.dispatchKeyEventDispatchKeyEvent(event)
        return false
    }

    fun setRenderView(view: View?) {
        removeRender()
        // must set WRAP_CONTENT and CENTER for render aspect ratio and measure.
        val lp = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER)
        renderContainer.addView(view, lp)
    }

    fun dispatchPlayNormalEvent(message: HoHoMessage) {
        bridge?.dispatchPlayNormalEvent(message)
    }

    fun dispatchPlayerErrorEvent(message: HoHoMessage) {
        bridge?.dispatchPlayerErrorEvent(message)
    }

    fun setBridge(iBridge: IBridge) {
        if (iBridge == bridge) {
            return
        }
        removeAllCovers() // remove all old covers from root container.
        bridge?.removeChangeListener(internalAdapterChange)
        bridge = iBridge
        iBridge.sort(BridgeAdapterComparator()) // sort it by CoverLevel
        iBridge.forEach { attachAdapter(it) }
        iBridge.addChangeListener(internalAdapterChange)
    }

    // dynamic attach an adapter when user add it
    // detach an adapter when user remove it.
    private val internalAdapterChange: IBridge.OnAdapterChange = object : IBridge.OnAdapterChange {

        override fun onAdapterAdd(
                key: String,
                adapter: IAdapter
        ) {
            attachAdapter(adapter)
        }

        override fun onAdapterRemove(
                key: String,
                adapter: IAdapter
        ) {
            detachAdapter(adapter)
        }
    }

    private fun attachAdapter(adapter: IAdapter) {
        adapter.setAdapterEventListener(object : OnAdapterEventListener {
            override fun onAdapterEvent(message: HoHoMessage) {
                onAdapterEventListener?.onAdapterEvent(message)
                bridge?.dispatchAdapterEvent(message)
            }
        })
        if (adapter is BaseCoverAdapter) {
            coverContainer.addCover(adapter)
            d(TAG, "on cover adapter attach : " + adapter.key + " ," + adapter.getCoverLevel())
        } else {
            d(TAG, "on normal an adapter attach : " + adapter.key)
        }
    }

    private fun detachAdapter(adapter: IAdapter) {
        if (adapter is BaseCoverAdapter) {
            coverContainer.removeCover(adapter)
            d(TAG, "on cover detach : " + adapter.key + " ," + adapter.getCoverLevel())
        } else {
            d(TAG, "on normal adapter detach : " + adapter.key)
        }
        adapter.setAdapterEventListener(null)
    }

    private fun removeRender() {
        renderContainer.removeAllViews()
    }

    protected fun removeAllCovers() {
        coverContainer.removeAllCovers()
        d(TAG, "detach all covers")
    }

    fun destroy() {
        producerGroup.destroy()
        bridge?.let {
            it.removeChangeListener(internalAdapterChange)
            it.clear()
        }
        onAdapterEventListener = null
        removeRender()
        removeAllCovers()
    }

    // OnDoubleTapListener
    override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
        return bridge?.dispatchTouchEventOnDoubleTapEvent(e) ?: false
    }

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        bridge?.dispatchTouchEventOnSingleTapConfirmed(e)
        return super.onSingleTapConfirmed(e)
    }

    override fun onDoubleTap(e: MotionEvent?): Boolean {
        bridge?.dispatchTouchEventOnDoubleTabUp(e)
        return super.onDoubleTap(e)
    }

    // OnGestureListener
    override fun onDown(e: MotionEvent?): Boolean {
        bridge?.dispatchTouchEventOnDown(e)
        return super.onDown(e)
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        bridge?.dispatchTouchEventOnFling(e1, e2, velocityX, velocityY)
        return super.onFling(e1, e2, velocityX, velocityY)
    }

    override fun onLongPress(e: MotionEvent?) {
        bridge?.dispatchTouchEventOnLongPress(e)
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        bridge?.dispatchTouchEventOnScroll(e1, e2, distanceX, distanceY)
        return super.onScroll(e1, e2, distanceX, distanceY)
    }

    override fun onShowPress(e: MotionEvent?) {
        bridge?.dispatchTouchEventOnShoPress(e)
    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        bridge?.dispatchTouchEventOnSingleTapUp(e)
        return super.onSingleTapUp(e)
    }

    override fun onEndGesture(event: MotionEvent?) {
        bridge?.dispatchTouchEventOnEndGesture(event)
    }

    init {
        init(context)
    }
}