package org.succlz123.hohoplayer.app.adpater

import android.content.Context
import android.view.View
import android.widget.TextView
import org.succlz123.hohoplayer.app.R
import org.succlz123.hohoplayer.app.support.AppPlayerData
import org.succlz123.hohoplayer.core.adapter.BaseCoverAdapter
import org.succlz123.hohoplayer.core.adapter.event.PlayerAdapterKtx.requestReplay
import org.succlz123.hohoplayer.core.player.listener.OnPlayerEventListener
import org.succlz123.hohoplayer.support.message.HoHoMessage

class CompleteCoverAdapter(context: Context) : BaseCoverAdapter(context) {
    private var replay: TextView? = null

    override val key: String
        get() = "CompleteCoverAdapter"

    public override fun onCreateCoverView(context: Context): View {
        return View.inflate(context, R.layout.layout_complete_cover, null)
    }

    override fun getCoverLevel(): Int {
        return levelMedium(20)
    }

    override fun onAdapterBind() {
        super.onAdapterBind()
        replay = getView().findViewById(R.id.tv_replay)
        replay?.setOnClickListener(onClickListener)
    }

    override fun onCoverAttachedToWindow() {
        super.onCoverAttachedToWindow()
        if (getDataCenter().getBoolean(AppPlayerData.Key.KEY_COMPLETE_SHOW)) {
            setPlayCompleteState(true)
        }
    }

    override fun onCoverDetachedToWindow() {
        super.onCoverDetachedToWindow()
        setCoverVisibility(View.GONE)
    }

    private val onClickListener = View.OnClickListener { v ->
        when (v.id) {
            R.id.tv_replay -> requestReplay()
        }
        setPlayCompleteState(false)
    }

    private fun setPlayCompleteState(state: Boolean) {
        setCoverVisibility(if (state) {
            View.VISIBLE
        } else {
            View.GONE
        })
        getDataCenter().putObject(AppPlayerData.Key.KEY_COMPLETE_SHOW, state)
    }

    override fun onPlayerEvent(message: HoHoMessage) {
        when (message.what) {
            OnPlayerEventListener.PLAYER_EVENT_ON_DATA_SOURCE_SET,
            OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_RENDER_START -> {
                setPlayCompleteState(false)
            }
            OnPlayerEventListener.PLAYER_EVENT_ON_PLAY_COMPLETE -> {
                setPlayCompleteState(true)
            }
        }
    }
}