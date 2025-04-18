package org.succlz123.hohoplayer.support.log

import org.succlz123.hohoplayer.core.player.listener.OnErrorEventListener
import org.succlz123.hohoplayer.core.player.listener.OnPlayerEventListener
import org.succlz123.hohoplayer.support.log.PlayerLog.d
import org.succlz123.hohoplayer.support.log.PlayerLog.e
import org.succlz123.hohoplayer.support.message.HoHoMessage

object PlayerEventLog {
    private const val EVENT_TAG_PLAY_EVENT = "hoho_event_play"
    private const val EVENT_TAG_ERROR_EVENT = "hoho_event_error"

    fun onPlayEventLog(message: HoHoMessage) {
        if (!PlayerLog.LOG_OPEN) {
            return
        }
        val value: String = when (message.what) {
            OnPlayerEventListener.PLAYER_EVENT_ON_DATA_SOURCE_SET -> "PLAYER_EVENT_ON_DATA_SOURCE_SET"
            OnPlayerEventListener.PLAYER_EVENT_ON_SURFACE_HOLDER_UPDATE -> "PLAYER_EVENT_ON_SURFACE_HOLDER_UPDATE"
            OnPlayerEventListener.PLAYER_EVENT_ON_SURFACE_UPDATE -> "PLAYER_EVENT_ON_SURFACE_UPDATE"
            OnPlayerEventListener.PLAYER_EVENT_ON_START -> "PLAYER_EVENT_ON_START"
            OnPlayerEventListener.PLAYER_EVENT_ON_PAUSE -> "PLAYER_EVENT_ON_PAUSE"
            OnPlayerEventListener.PLAYER_EVENT_ON_RESUME -> "PLAYER_EVENT_ON_RESUME"
            OnPlayerEventListener.PLAYER_EVENT_ON_STOP -> "PLAYER_EVENT_ON_STOP"
            OnPlayerEventListener.PLAYER_EVENT_ON_RESET -> "PLAYER_EVENT_ON_RESET"
            OnPlayerEventListener.PLAYER_EVENT_ON_DESTROY -> "PLAYER_EVENT_ON_DESTROY"
            OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_START -> "PLAYER_EVENT_ON_BUFFERING_START"
            OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_END -> "PLAYER_EVENT_ON_BUFFERING_END"
            OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_UPDATE -> "PLAYER_EVENT_ON_BUFFERING_UPDATE"
            OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_TO -> "PLAYER_EVENT_ON_SEEK_TO"
            OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_COMPLETE -> "PLAYER_EVENT_ON_SEEK_COMPLETE"
            OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_RENDER_START -> "PLAYER_EVENT_ON_VIDEO_RENDER_START"
            OnPlayerEventListener.PLAYER_EVENT_ON_PLAY_COMPLETE -> "PLAYER_EVENT_ON_PLAY_COMPLETE"
            OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_SIZE_CHANGE -> "PLAYER_EVENT_ON_VIDEO_SIZE_CHANGE"
            OnPlayerEventListener.PLAYER_EVENT_ON_PREPARED -> "PLAYER_EVENT_ON_PREPARED"
            OnPlayerEventListener.PLAYER_EVENT_ON_TIMER_UPDATE -> {
                val current = message.getIntFromExtra("current", 0)
                val duration = message.getIntFromExtra("duration", 0)
                val bufferPercentage = message.getIntFromExtra("bufferPercentage", 0)
                "PLAYER_EVENT_ON_TIMER_UPDATE, current = $current,duration = $duration,bufferPercentage = $bufferPercentage"
            }
            OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_ROTATION_CHANGED -> "PLAYER_EVENT_ON_VIDEO_ROTATION_CHANGED"
            OnPlayerEventListener.PLAYER_EVENT_ON_AUDIO_DECODER_START -> "PLAYER_EVENT_ON_AUDIO_DECODER_START"
            OnPlayerEventListener.PLAYER_EVENT_ON_AUDIO_RENDER_START -> "PLAYER_EVENT_ON_AUDIO_RENDER_START"
            OnPlayerEventListener.PLAYER_EVENT_ON_AUDIO_SEEK_RENDERING_START -> "PLAYER_EVENT_ON_AUDIO_SEEK_RENDERING_START"
            OnPlayerEventListener.PLAYER_EVENT_ON_NETWORK_BANDWIDTH -> "PLAYER_EVENT_ON_NETWORK_BANDWIDTH"
            OnPlayerEventListener.PLAYER_EVENT_ON_BAD_INTERLEAVING -> "PLAYER_EVENT_ON_BAD_INTERLEAVING"
            OnPlayerEventListener.PLAYER_EVENT_ON_NOT_SEEK_ABLE -> "PLAYER_EVENT_ON_NOT_SEEK_ABLE"
            OnPlayerEventListener.PLAYER_EVENT_ON_METADATA_UPDATE -> "PLAYER_EVENT_ON_METADATA_UPDATE"
            OnPlayerEventListener.PLAYER_EVENT_ON_TIMED_TEXT_ERROR -> "PLAYER_EVENT_ON_TIMED_TEXT_ERROR"
            OnPlayerEventListener.PLAYER_EVENT_ON_UNSUPPORTED_SUBTITLE -> "PLAYER_EVENT_ON_UNSUPPORTED_SUBTITLE"
            OnPlayerEventListener.PLAYER_EVENT_ON_SUBTITLE_TIMED_OUT -> "PLAYER_EVENT_ON_SUBTITLE_TIMED_OUT"
            OnPlayerEventListener.PLAYER_EVENT_ON_STATUS_CHANGE -> "PLAYER_EVENT_ON_STATUS_CHANGE"
            OnPlayerEventListener.PLAYER_EVENT_ON_PROVIDER_DATA_START -> "PLAYER_EVENT_ON_PROVIDER_DATA_START"
            OnPlayerEventListener.PLAYER_EVENT_ON_PROVIDER_DATA_SUCCESS -> "PLAYER_EVENT_ON_PROVIDER_DATA_SUCCESS"
            OnPlayerEventListener.PLAYER_EVENT_ON_PROVIDER_DATA_ERROR -> "PLAYER_EVENT_ON_PROVIDER_DATA_ERROR"
            else -> "UNKNOWN EVENT, maybe from provider, maybe from user custom code."
        }
        d(EVENT_TAG_PLAY_EVENT, value)
    }

    fun onErrorEventLog(message: HoHoMessage) {
        if (!PlayerLog.LOG_OPEN) {
            return
        }
        var value: String = when (message.what) {
            OnErrorEventListener.ERROR_EVENT_DATA_PROVIDER_ERROR -> "ERROR_EVENT_DATA_PROVIDER_ERROR"
            OnErrorEventListener.ERROR_EVENT_COMMON -> "ERROR_EVENT_COMMON"
            OnErrorEventListener.ERROR_EVENT_UNKNOWN -> "ERROR_EVENT_UNKNOWN"
            OnErrorEventListener.ERROR_EVENT_SERVER_DIED -> "ERROR_EVENT_SERVER_DIED"
            OnErrorEventListener.ERROR_EVENT_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> "ERROR_EVENT_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK"
            OnErrorEventListener.ERROR_EVENT_IO -> "ERROR_EVENT_IO"
            OnErrorEventListener.ERROR_EVENT_MALFORMED -> "ERROR_EVENT_MALFORMED"
            OnErrorEventListener.ERROR_EVENT_UNSUPPORTED -> "ERROR_EVENT_UNSUPPORTED"
            OnErrorEventListener.ERROR_EVENT_TIMED_OUT -> "ERROR_EVENT_TIMED_OUT"
            else -> "unKnow code error, maybe user custom errorCode"
        }
        value += ", $message"
        e(EVENT_TAG_ERROR_EVENT, value)
    }
}