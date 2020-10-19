package org.succlz123.hohoplayer.core.adapter.cover

import android.content.Context
import android.view.View
import android.view.ViewGroup
import org.succlz123.hohoplayer.core.adapter.BaseCoverAdapter
import java.util.*

abstract class AbsCoverContainer(var context: Context) {
    private val covers = ArrayList<BaseCoverAdapter>()

    abstract fun getContainerView(): ViewGroup

    fun addCover(coverAdapter: BaseCoverAdapter) {
        covers.add(coverAdapter)
        onCoverAdd(coverAdapter)
    }

    protected abstract fun onCoverAdd(coverAdapter: BaseCoverAdapter)

    fun removeCover(coverAdapter: BaseCoverAdapter) {
        covers.remove(coverAdapter)
        onCoverRemove(coverAdapter)
    }

    protected abstract fun onCoverRemove(coverAdapter: BaseCoverAdapter)

    fun removeAllCovers() {
        covers.clear()
        onCoversRemoveAll()
    }

    protected abstract fun onCoversRemoveAll()

    fun isCoverInContainer(coverAdapter: BaseCoverAdapter): Boolean {
        val index = rootIndexOfChild(coverAdapter.getView())
        if (index != -1) {
            return true
        }
        val childCount = rootChildCount
        if (childCount <= 0) {
            return false
        }
        var result = false
        for (i in 0 until childCount) {
            val view = rootGetChildAt(i)
            if (view is ViewGroup && view.indexOfChild(coverAdapter.getView()) != -1) {
                result = true
                break
            }
        }
        return result
    }

    fun getCoverCount(): Int {
        return covers.size
    }

    private fun rootIndexOfChild(view: View?): Int {
        return getContainerView().indexOfChild(view)
    }

    private val rootChildCount: Int
        get() = getContainerView().childCount

    private fun rootGetChildAt(index: Int): View? {
        return getContainerView().getChildAt(index)
    }
}
