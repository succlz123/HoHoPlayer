package org.succlz123.hohoplayer.core.adapter

import android.content.Context
import android.view.View
import org.succlz123.hohoplayer.core.adapter.BaseAdapter
import org.succlz123.hohoplayer.core.adapter.cover.ICover

abstract class BaseCoverAdapter(context: Context) : BaseAdapter(), ICover,
    View.OnAttachStateChangeListener {
    private val coverView: View

    init {
        coverView = onCreateCoverView(context)
        coverView.addOnAttachStateChangeListener(this)
    }

    override fun setCoverVisibility(visibility: Int) {
        coverView.visibility = visibility
    }

    override fun getView(): View {
        return coverView
    }

    override fun onViewAttachedToWindow(v: View) {
        onCoverAttachedToWindow()
    }

    override fun onViewDetachedFromWindow(v: View) {
        onCoverDetachedToWindow()
    }

    protected open fun onCoverAttachedToWindow() {}

    protected open fun onCoverDetachedToWindow() {}

    protected abstract fun onCreateCoverView(context: Context): View

    override fun getCoverLevel(): Int {
        return ICover.COVER_LEVEL_LOW
    }

    protected fun levelLow(priority: Int): Int {
        return levelPriority(ICover.COVER_LEVEL_LOW, priority)
    }

    protected fun levelMedium(priority: Int): Int {
        return levelPriority(ICover.COVER_LEVEL_MEDIUM, priority)
    }

    protected fun levelHigh(priority: Int): Int {
        return levelPriority(ICover.COVER_LEVEL_HIGH, priority)
    }

    /**
     * The high priority cover will be placed above,
     * otherwise the lower priority will be placed below.
     *
     * @param priority range from 0-31
     */
    private fun levelPriority(level: Int, priority: Int): Int {
        return level + priority % ICover.LEVEL_MAX
    }
}