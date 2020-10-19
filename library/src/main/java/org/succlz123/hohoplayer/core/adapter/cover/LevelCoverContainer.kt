package org.succlz123.hohoplayer.core.adapter.cover

import android.content.Context
import android.graphics.Color
import android.view.ViewGroup
import android.widget.FrameLayout
import org.succlz123.hohoplayer.core.adapter.BaseCoverAdapter
import org.succlz123.hohoplayer.support.log.PlayerLog.d

class LevelCoverContainer(context: Context) : AbsCoverContainer(context) {

    companion object {
        private const val TAG = "LevelCoverContainer"
    }

    private val rootView: ViewGroup

    private var levelLowCoverContainer: FrameLayout? = null

    private var levelMediumCoverContainer: FrameLayout? = null

    private var levelHighCoverContainer: FrameLayout? = null

    init {
        rootView = FrameLayout(context)
        rootView.setBackgroundColor(Color.TRANSPARENT)
        initLevelContainers(context)
    }

    private fun initLevelContainers(context: Context) {
        levelLowCoverContainer = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }
        addLevelContainerView(levelLowCoverContainer)
        levelMediumCoverContainer = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }
        addLevelContainerView(levelMediumCoverContainer)
        levelHighCoverContainer = FrameLayout(context).apply {
            setBackgroundColor(Color.TRANSPARENT)
        }
        addLevelContainerView(levelHighCoverContainer)
    }

    private fun addLevelContainerView(container: ViewGroup?) {
        val view = getContainerView()
        view.addView(
            container, ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    override fun getContainerView(): ViewGroup {
        return rootView
    }

    override fun onCoverAdd(coverAdapter: BaseCoverAdapter) {
        val level = coverAdapter.getCoverLevel()
        when {
            level < ICover.COVER_LEVEL_MEDIUM -> {
                levelLowCoverContainer?.addView(
                    coverAdapter.getView(),
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                d(TAG, "Low Level Cover Add : level = $level")
            }
            level < ICover.COVER_LEVEL_HIGH -> {
                levelMediumCoverContainer?.addView(
                    coverAdapter.getView(),
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                d(TAG, "Medium Level Cover Add : level = $level")
            }
            else -> {
                levelHighCoverContainer?.addView(
                    coverAdapter.getView(),
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                d(TAG, "High Level Cover Add : level = $level")
            }
        }
        d(TAG, "on cover add : now count = " + getCoverCount())
    }

    override fun onCoverRemove(coverAdapter: BaseCoverAdapter) {
        levelLowCoverContainer?.removeView(coverAdapter.getView())
        levelMediumCoverContainer?.removeView(coverAdapter.getView())
        levelHighCoverContainer?.removeView(coverAdapter.getView())
        d(TAG, "on cover remove : now count = " + getCoverCount())
    }

    override fun onCoversRemoveAll() {
        levelLowCoverContainer?.removeAllViews()
        levelMediumCoverContainer?.removeAllViews()
        levelHighCoverContainer?.removeAllViews()
        d(TAG, "on covers remove all ...")
    }
}
