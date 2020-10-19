package org.succlz123.hohoplayer.app.adpater

import android.content.Context
import android.view.View
import org.succlz123.hohoplayer.app.R
import org.succlz123.hohoplayer.core.adapter.BaseCoverAdapter
import org.succlz123.hohoplayer.core.player.listener.OnPlayerEventListener
import org.succlz123.hohoplayer.support.message.HoHoMessage

class LoadingCoverAdapter(context: Context) : BaseCoverAdapter(context) {

    override val key: String
        get() = "LoadingCoverAdapter"

    public override fun onCreateCoverView(context: Context): View {
        return View.inflate(context, R.layout.layout_loading_cover, null)
    }

    override fun getCoverLevel(): Int {
        return levelMedium(1)
    }

    override fun onCoverAttachedToWindow() {
        super.onCoverAttachedToWindow()
//        videoViewState?.let {
//            if (isInPlaybackState(it)) {
//                setLoadingState(it.isBuffering())
//            }
//        }
    }

//    private fun isInPlaybackState(IVideoViewState: IVideoViewState): Boolean {
//        val state = IVideoViewState.getState()
//        return state != IPlayer.STATE_END
//                && state != IPlayer.STATE_ERROR
//                && state != IPlayer.STATE_IDLE
//                && state != IPlayer.STATE_INITIALIZED
//                && state != IPlayer.STATE_STOPPED
//    }

    override fun onPlayerEvent(message: HoHoMessage) {
        when (message.what) {
            OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_START,
            OnPlayerEventListener.PLAYER_EVENT_ON_DATA_SOURCE_SET,
            OnPlayerEventListener.PLAYER_EVENT_ON_PROVIDER_DATA_START,
            OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_TO -> {
                setLoadingState(true)
            }
            OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_RENDER_START,
            OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_END,
            OnPlayerEventListener.PLAYER_EVENT_ON_STOP,
            OnPlayerEventListener.PLAYER_EVENT_ON_PROVIDER_DATA_ERROR,
            OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_COMPLETE -> {
                setLoadingState(false)
            }
        }
    }

    override fun onErrorEvent(message: HoHoMessage) {
        setLoadingState(false)
    }

    private fun setLoadingState(show: Boolean) {
        setCoverVisibility(if (show) {
            View.VISIBLE
        } else {
            View.GONE
        })
    }
}