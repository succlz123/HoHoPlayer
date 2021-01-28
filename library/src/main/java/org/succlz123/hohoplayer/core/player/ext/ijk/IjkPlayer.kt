package org.succlz123.hohoplayer.core.player.ext.ijk

import android.content.ContentResolver
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.text.TextUtils
import android.view.Surface
import android.view.SurfaceHolder
import org.succlz123.hohoplayer.config.PlayerConfig
import org.succlz123.hohoplayer.config.PlayerContext
import org.succlz123.hohoplayer.core.player.base.BasePlayer
import org.succlz123.hohoplayer.core.player.base.IPlayer
import org.succlz123.hohoplayer.core.player.listener.OnErrorEventListener
import org.succlz123.hohoplayer.core.player.listener.OnPlayerEventListener
import org.succlz123.hohoplayer.core.source.DataSource
import org.succlz123.hohoplayer.core.source.DataSource.Companion.buildRawPath
import org.succlz123.hohoplayer.core.source.Decoder
import org.succlz123.hohoplayer.support.log.PlayerLog
import org.succlz123.hohoplayer.support.message.HoHoMessage
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer

class IjkPlayer : BasePlayer() {

    companion object {
        const val TAG = "IjkPlayer"

        init {
            IjkMediaPlayer.loadLibrariesOnce(null)
            IjkMediaPlayer.native_profileBegin("libijkplayer.so")
        }

        fun addThis(setAsDefault: Boolean = false) {
            if (setAsDefault) {
                PlayerConfig.addAndSetDecoder(Decoder(TAG, IjkPlayer::class.java.name))
            } else {
                PlayerConfig.addDecoder(Decoder(TAG, IjkPlayer::class.java.name))
            }
        }
    }

    private var internalPlayer = IjkMediaPlayer()

    private var targetState = Int.MAX_VALUE

    private var startSeekPos = 0

    private val optionArrays = arrayListOf<IjkOption>()

    override fun option(message: HoHoMessage) {
        val category = message.what
        val key = message.argString ?: return
        val value = message.argLong
        optionArrays.add(IjkOption(category, key, value))
        fillOption(category, key, value)
    }

    private fun fillOption(category: Int, key: String, value: Long) {
        internalPlayer.setOption(category, key, value)
    }

    private fun setOptions(ijkMediaPlayer: IjkMediaPlayer) {
        // open media codec
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-hevc", 1)

        // accurate seek
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "timeout", 10000000)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48)

        for (optionArray in optionArrays) {
            fillOption(optionArray.category, optionArray.key, optionArray.value)
        }
    }

    override fun setDataSource(dataSource: DataSource) {
        openVideo(dataSource)
    }

    private fun openVideo(dataSource: DataSource) {
        try {
            stop()
            reset()
            resetListener()
            targetState = Int.MAX_VALUE
            setOptions(internalPlayer)
            // REMOVED: mAudioSession
            internalPlayer.setOnPreparedListener(mPreparedListener)
            internalPlayer.setOnVideoSizeChangedListener(mSizeChangedListener)
            internalPlayer.setOnCompletionListener(mCompletionListener)
            internalPlayer.setOnErrorListener(mErrorListener)
            internalPlayer.setOnInfoListener(mInfoListener)
            internalPlayer.setOnSeekCompleteListener(mOnSeekCompleteListener)
            internalPlayer.setOnBufferingUpdateListener(mBufferingUpdateListener)
            updateStatus(IPlayer.STATE_INITIALIZED)

            if (dataSource.timedTextSource != null) {
                PlayerLog.e(TAG, "ijkplayer not support timed text !")
            }
            val applicationContext: Context = PlayerContext.context()
            val data = dataSource.data
            val uri = dataSource.uri
            val assetsPath = dataSource.assetsPath
            val headers = dataSource.extra
            val rawId = dataSource.rawId
            if (data != null) {
                if (headers == null) {
                    internalPlayer.dataSource = data
                } else {
                    internalPlayer.setDataSource(data, headers)
                }
            } else if (uri != null) {
                if (uri.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE) {
                    internalPlayer.setDataSource(RawDataSourceProvider.create(applicationContext, uri))
                } else {
                    if (headers == null) {
                        internalPlayer.setDataSource(applicationContext, uri)
                    } else {
                        internalPlayer.setDataSource(applicationContext, uri, headers)
                    }
                }
            } else if (!TextUtils.isEmpty(assetsPath)) {
                PlayerLog.e(TAG, "ijkplayer not support assets play, you can use raw play.")
            } else if (rawId > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                val rawUri = buildRawPath(applicationContext.packageName, rawId)
                internalPlayer.setDataSource(RawDataSourceProvider.create(applicationContext, rawUri))
            }
            internalPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
            internalPlayer.setScreenOnWhilePlaying(true)
            internalPlayer.prepareAsync()

            // set looping indicator for IjkMediaPlayer
            internalPlayer.isLooping = isLooping()
            submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_DATA_SOURCE_SET, argObj = dataSource))
        } catch (e: Exception) {
            PlayerLog.e(TAG, e.toString())
            updateStatus(IPlayer.STATE_ERROR)
            targetState = IPlayer.STATE_ERROR
            submitErrorEvent(HoHoMessage.obtain(what = OnErrorEventListener.ERROR_EVENT_IO))
        }
    }

    override fun start() {
        val state = getState()
        if (state == IPlayer.STATE_PREPARED
                || state == IPlayer.STATE_PAUSED
                || state == IPlayer.STATE_PLAYBACK_COMPLETE) {
            internalPlayer.start()
            updateStatus(IPlayer.STATE_STARTED)
            submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_START))
        }
        targetState = IPlayer.STATE_STARTED
        PlayerLog.d(TAG, "start...")
    }

    override fun start(msc: Int) {
        if (getState() == IPlayer.STATE_PREPARED && msc > 0) {
            start()
            internalPlayer.seekTo(msc.toLong())
        } else {
            if (msc > 0) {
                startSeekPos = msc
            }
            start()
        }
    }

    override fun pause() {
        try {
            val state = getState()
            if (state != IPlayer.STATE_END
                    && state != IPlayer.STATE_ERROR
                    && state != IPlayer.STATE_IDLE
                    && state != IPlayer.STATE_INITIALIZED
                    && state != IPlayer.STATE_PAUSED
                    && state != IPlayer.STATE_STOPPED) {
                internalPlayer.pause()
                updateStatus(IPlayer.STATE_PAUSED)
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_PAUSE))
            }
        } catch (e: Exception) {
            PlayerLog.e(TAG, e.toString())
        }
        targetState = IPlayer.STATE_PAUSED
    }

    override fun resume() {
        try {
            if (getState() == IPlayer.STATE_PAUSED) {
                internalPlayer.start()
                updateStatus(IPlayer.STATE_STARTED)
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_RESUME))
            }
        } catch (e: Exception) {
            PlayerLog.e(TAG, e.toString())
        }
        targetState = IPlayer.STATE_STARTED
    }

    override fun seekTo(msc: Int) {
        val state = getState()
        if (state == IPlayer.STATE_PREPARED
                || state == IPlayer.STATE_STARTED
                || state == IPlayer.STATE_PAUSED
                || state == IPlayer.STATE_PLAYBACK_COMPLETE) {
            internalPlayer.seekTo(msc.toLong())
            submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_TO, argInt = msc))
        }
    }

    override fun stop() {
        val state = getState()
        if (state == IPlayer.STATE_PREPARED
                || state == IPlayer.STATE_STARTED
                || state == IPlayer.STATE_PAUSED
                || state == IPlayer.STATE_PLAYBACK_COMPLETE) {
            internalPlayer.stop()
            updateStatus(IPlayer.STATE_STOPPED)
            submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_STOP))
        }
        targetState = IPlayer.STATE_STOPPED
    }

    override fun reset() {
        internalPlayer.reset()
        updateStatus(IPlayer.STATE_IDLE)
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_RESET))
        targetState = IPlayer.STATE_IDLE
    }

    override fun isPlaying(): Boolean {
        return if (getState() != IPlayer.STATE_ERROR) {
            internalPlayer.isPlaying
        } else {
            false
        }
    }

    override fun getCurrentPosition(): Int {
        val state = getState()
        return if (state == IPlayer.STATE_PREPARED
                || state == IPlayer.STATE_STARTED
                || state == IPlayer.STATE_PAUSED
                || state == IPlayer.STATE_PLAYBACK_COMPLETE) {
            internalPlayer.currentPosition.toInt()
        } else {
            0
        }
    }

    override fun getDuration(): Int {
        val state = getState()
        return if (state != IPlayer.STATE_ERROR
                && state != IPlayer.STATE_INITIALIZED
                && state != IPlayer.STATE_IDLE) {
            internalPlayer.duration.toInt()
        } else {
            0
        }
    }

    override fun getVideoWidth(): Int {
        return internalPlayer.videoWidth
    }

    override fun getVideoHeight(): Int {
        return internalPlayer.videoHeight
    }

    override fun destroy() {
        updateStatus(IPlayer.STATE_END)
        resetListener()
        internalPlayer.release()
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_DESTROY))
    }

    override fun setDisplay(surfaceHolder: SurfaceHolder?) {
        try {
            internalPlayer.setDisplay(surfaceHolder)
            submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_SURFACE_HOLDER_UPDATE))
        } catch (e: Exception) {
            submitErrorEvent(HoHoMessage.obtain(what = OnErrorEventListener.ERROR_EVENT_RENDER, extra = hashMapOf(
                    "errorMessage" to e.message,
                    "causeMessage" to e.cause?.message.orEmpty()
            )))
        }
    }

    override fun setSurface(surface: Surface?) {
        try {
            internalPlayer.setSurface(surface)
            submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_SURFACE_UPDATE))
        } catch (e: Exception) {
            submitErrorEvent(HoHoMessage.obtain(what = OnErrorEventListener.ERROR_EVENT_RENDER, extra = hashMapOf(
                    "errorMessage" to e.message,
                    "causeMessage" to e.cause?.message.orEmpty()
            )))
        }
    }

    override fun setVolume(left: Float, right: Float) {
        internalPlayer.setVolume(left, right)
    }

    override fun setSpeed(speed: Float) {
        internalPlayer.setSpeed(speed)
    }

    override fun setLooping(looping: Boolean) {
        super.setLooping(looping)
        internalPlayer.isLooping = looping
    }

    override fun getAudioSessionId(): Int {
        return internalPlayer.audioSessionId
    }

    private fun resetListener() {
        internalPlayer.setOnPreparedListener(null)
        internalPlayer.setOnVideoSizeChangedListener(null)
        internalPlayer.setOnCompletionListener(null)
        internalPlayer.setOnErrorListener(null)
        internalPlayer.setOnInfoListener(null)
        internalPlayer.setOnBufferingUpdateListener(null)
    }

    private val mPreparedListener = IMediaPlayer.OnPreparedListener { mp ->
        PlayerLog.d(TAG, "onPrepared...")
        updateStatus(IPlayer.STATE_PREPARED)
        mVideoWidth = mp.videoWidth
        mVideoHeight = mp.videoHeight
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_PREPARED, extra = hashMapOf(
                "videoWidth" to mVideoWidth,
                "videoHeight" to mVideoHeight
        )))
        val seekToPosition = startSeekPos // mSeekWhenPrepared may be changed after seekTo() call
        if (seekToPosition > 0 && mp.duration > 0) {
            internalPlayer.seekTo(seekToPosition.toLong())
            startSeekPos = 0
        }

        // We don't know the video size yet, but should start anyway.
        // The video size might be reported to us later.
        PlayerLog.d(TAG, "mTargetState = $targetState")
        if (targetState == IPlayer.STATE_STARTED) {
            start()
        } else if (targetState == IPlayer.STATE_PAUSED) {
            pause()
        } else if (targetState == IPlayer.STATE_STOPPED || targetState == IPlayer.STATE_IDLE) {
            reset()
        }
    }

    private var mVideoWidth = 0

    private var mVideoHeight = 0

    private val mSizeChangedListener = IMediaPlayer.OnVideoSizeChangedListener { mp, width, height, sarNum, sarDen ->
        mVideoWidth = mp.videoWidth
        mVideoHeight = mp.videoHeight
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_SIZE_CHANGE, extra = hashMapOf(
                "videoWidth" to mVideoWidth, "videoHeight" to mVideoHeight,
                "videoSarNum" to sarNum, "videoSarDen" to sarDen
        )))
    }

    private val mCompletionListener = IMediaPlayer.OnCompletionListener {
        updateStatus(IPlayer.STATE_PLAYBACK_COMPLETE)
        targetState = IPlayer.STATE_PLAYBACK_COMPLETE
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_PLAY_COMPLETE))
        if (!isLooping()) {
            stop()
        }
    }

    private val mInfoListener = IMediaPlayer.OnInfoListener { mp, arg1, arg2 ->
        when (arg1) {
            IMediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING -> {
                PlayerLog.d(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING:")
            }
            IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                PlayerLog.d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START")
                startSeekPos = 0
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_RENDER_START))
            }
            IMediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                PlayerLog.d(TAG, "MEDIA_INFO_BUFFERING_START:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_START))
            }
            IMediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                PlayerLog.d(TAG, "MEDIA_INFO_BUFFERING_END:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_END))
            }
            IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH -> {
            }
            IMediaPlayer.MEDIA_INFO_BAD_INTERLEAVING -> {
                PlayerLog.d(TAG, "MEDIA_INFO_BAD_INTERLEAVING:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_BAD_INTERLEAVING))
            }
            IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE -> {
                PlayerLog.d(TAG, "MEDIA_INFO_NOT_SEEKABLE:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_NOT_SEEK_ABLE))
            }
            IMediaPlayer.MEDIA_INFO_METADATA_UPDATE -> {
                PlayerLog.d(TAG, "MEDIA_INFO_METADATA_UPDATE:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_METADATA_UPDATE))
            }
            IMediaPlayer.MEDIA_INFO_TIMED_TEXT_ERROR -> {
                PlayerLog.d(TAG, "MEDIA_INFO_TIMED_TEXT_ERROR:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_TIMED_TEXT_ERROR))
            }
            IMediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE -> {
                PlayerLog.d(TAG, "MEDIA_INFO_UNSUPPORTED_SUBTITLE:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_UNSUPPORTED_SUBTITLE))
            }
            IMediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT -> {
                PlayerLog.d(TAG, "MEDIA_INFO_SUBTITLE_TIMED_OUT:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_SUBTITLE_TIMED_OUT))
            }
            IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED -> {
                PlayerLog.d(TAG, "MEDIA_INFO_VIDEO_ROTATION_CHANGED: $arg2")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_ROTATION_CHANGED, argInt = arg2))
            }
            IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START -> {
                PlayerLog.d(TAG, "MEDIA_INFO_AUDIO_RENDERING_START:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_AUDIO_RENDER_START))
            }
            IMediaPlayer.MEDIA_INFO_AUDIO_DECODED_START -> {
                PlayerLog.d(TAG, "MEDIA_INFO_AUDIO_DECODED_START:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_AUDIO_DECODER_START))
            }
            IMediaPlayer.MEDIA_INFO_AUDIO_SEEK_RENDERING_START -> {
                PlayerLog.d(TAG, "MEDIA_INFO_AUDIO_SEEK_RENDERING_START:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_AUDIO_SEEK_RENDERING_START))
            }
        }
        true
    }

    private val mOnSeekCompleteListener = IMediaPlayer.OnSeekCompleteListener {
        PlayerLog.d(TAG, "EVENT_CODE_SEEK_COMPLETE")
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_COMPLETE))
    }

    private val mErrorListener = IMediaPlayer.OnErrorListener { mp, framework_err, impl_err ->
        PlayerLog.d(TAG, "Error: $framework_err,$impl_err")
        updateStatus(IPlayer.STATE_ERROR)
        targetState = IPlayer.STATE_ERROR
        when (framework_err) {
            100 -> {
            }
        }

        /* If an error handler has been supplied, use it and finish. */
        submitPlayerEvent(HoHoMessage.obtain(what = OnErrorEventListener.ERROR_EVENT_COMMON))
        true
    }

    private val mBufferingUpdateListener = IMediaPlayer.OnBufferingUpdateListener { mp, percent -> submitBufferingUpdate(percent) }

    class IjkOption(val category: Int, val key: String, val value: Long)
}