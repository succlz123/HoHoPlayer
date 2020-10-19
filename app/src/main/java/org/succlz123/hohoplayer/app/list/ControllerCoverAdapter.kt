package org.succlz123.hohoplayer.app.list

import android.content.Context
import android.view.View
import android.widget.ImageView
import org.succlz123.hohoplayer.app.R
import org.succlz123.hohoplayer.core.adapter.BaseCoverAdapter
import org.succlz123.hohoplayer.core.player.listener.OnPlayerEventListener
import org.succlz123.hohoplayer.support.message.HoHoMessage

class ControllerCoverAdapter(context: Context, private var isMute: Boolean) : BaseCoverAdapter(context) {
    private lateinit var muteView: ImageView
    private var isPlaying: Boolean = false

    override val key: String
        get() = "ControllerCoverAdapter"

    public override fun onCreateCoverView(context: Context): View {
        return View.inflate(context, R.layout.layout_player_controller_cover, null)
    }

    override fun getCoverLevel(): Int {
        return levelMedium(10)
    }

    override fun onCoverAttachedToWindow() {
        super.onCoverAttachedToWindow()
        val view = getView()
        muteView = view.findViewById(R.id.mute)
        setMuteImage()
        sendMuteEvent()
        muteView.setOnClickListener {
            isMute = !isMute
            setMuteImage()
            sendMuteEvent()
        }
    }

    fun handleMuteImage(boolean: Boolean) {
        if (boolean) {
            muteView.visibility = View.VISIBLE
        } else {
            muteView.visibility = View.INVISIBLE
        }
    }

    private fun setMuteImage() {
        if (isMute) {
            muteView.setBackgroundResource(R.drawable.ic_vec_mute)
        } else {
            muteView.setBackgroundResource(R.drawable.ic_vec_unmute)
        }
    }

    private fun sendMuteEvent() {
        sendOne2ManyAdapterEvent(HoHoMessage.obtain(what = PlayerEvent.EVENT_CODE_REQUEST_MUTE, argObj = isMute))
    }

    override fun onPlayerEvent(message: HoHoMessage) {
        when (message.what) {
            OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_RENDER_START,
            OnPlayerEventListener.PLAYER_EVENT_ON_START,
            OnPlayerEventListener.PLAYER_EVENT_ON_RESUME -> {
                isPlaying = true
            }
            OnPlayerEventListener.PLAYER_EVENT_ON_PAUSE,
            OnPlayerEventListener.PLAYER_EVENT_ON_STOP,
            OnPlayerEventListener.PLAYER_EVENT_ON_DESTROY -> {
                isPlaying = false
            }
        }
    }

    override fun onErrorEvent(message: HoHoMessage) {
        isPlaying = false
    }
}
