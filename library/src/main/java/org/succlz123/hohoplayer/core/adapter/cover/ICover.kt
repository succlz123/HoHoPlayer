package org.succlz123.hohoplayer.core.adapter.cover

import android.view.View

interface ICover {

    fun getView(): View

    fun setCoverVisibility(visibility: Int)

    fun getCoverLevel(): Int

    companion object {
        // max cover priority value per level container.
        const val LEVEL_MAX = 1 shl 5

        // level low container start value.
        const val COVER_LEVEL_LOW = 0

        // level medium container start value.
        const val COVER_LEVEL_MEDIUM = 1 shl 5

        // level high container start value.
        const val COVER_LEVEL_HIGH = 1 shl 6
    }
}