package org.succlz123.hohoplayer.core.player.ext

import android.content.ContentResolver
import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.util.SparseArray
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
import org.succlz123.hohoplayer.support.message.HoHoMessage
import tv.danmaku.ijk.media.player.IMediaPlayer
import tv.danmaku.ijk.media.player.IjkMediaPlayer

/**
 * Created by Taurus on 2018/4/18.
 */
class IjkPlayer : BasePlayer() {
    private val TAG = "IjkPlayer"
    private var mMediaPlayer: IjkMediaPlayer?
    private var mTargetState = Int.MAX_VALUE
    private var startSeekPos = 0
    private val mOptionArrays: SparseArray<Bundle>

    companion object {
        private val TAG = "IjkPlayer"
        const val PLAN_ID = 100

        fun addThis(setAsDefault: Boolean = false) {
            if (setAsDefault) {
                PlayerConfig.addAndSetDecoder(Decoder(IjkPlayer.TAG, IjkPlayer::class.java.name))
            } else {
                PlayerConfig.addDecoder(Decoder(IjkPlayer.TAG, IjkPlayer::class.java.name))
            }
        }

        init {
            IjkMediaPlayer.loadLibrariesOnce(null)
            IjkMediaPlayer.native_profileBegin("libijkplayer.so")
        }
    }

    /**
     * ijkplayer的配置项设置
     * ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1);
     *
     * @param code      对应setOption方法的第一个参数
     * @param bundle    对应第二个和第三个参数，key为配置项的名称，比如mediacodec，value为对应的配置值(long类型)
     */
    override fun option(message: HoHoMessage) {
    }

    private fun fillOption(code: Int, bundle: Bundle) {
        val keySet = bundle.keySet()
        for (key in keySet) {
            mMediaPlayer!!.setOption(code, key, bundle.getLong(key))
        }
    }

    private fun setOptions(ijkMediaPlayer: IjkMediaPlayer?) {
        if (ijkMediaPlayer == null) return
        //设置清除dns cache
        //IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1

        //open mediacodec
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec", 1)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-hevc", 1)

        //accurate seek
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "mediacodec-auto-rotate", 1)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "opensles", 1)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "framedrop", 1)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "start-on-prepared", 0)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "timeout", 10000000)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "reconnect", 1)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "http-detect-range-support", 0)
        ijkMediaPlayer.setOption(IjkMediaPlayer.OPT_CATEGORY_CODEC, "skip_loop_filter", 48)

        val size = mOptionArrays.size()
        for (i in 0 until size) {
            fillOption(mOptionArrays.keyAt(i), mOptionArrays.valueAt(i))
        }
    }

    override fun setDataSource(data: DataSource) {
        if (data != null) {
            openVideo(data)
        }
    }

    private fun openVideo(dataSource: DataSource) {
        try {
            if (mMediaPlayer == null) {
                mMediaPlayer = IjkMediaPlayer()
            } else {
                stop()
                reset()
                resetListener()
            }
            mTargetState = Int.MAX_VALUE
            setOptions(mMediaPlayer)
            // REMOVED: mAudioSession
            mMediaPlayer!!.setOnPreparedListener(mPreparedListener)
            mMediaPlayer!!.setOnVideoSizeChangedListener(mSizeChangedListener)
            mMediaPlayer!!.setOnCompletionListener(mCompletionListener)
            mMediaPlayer!!.setOnErrorListener(mErrorListener)
            mMediaPlayer!!.setOnInfoListener(mInfoListener)
            mMediaPlayer!!.setOnSeekCompleteListener(mOnSeekCompleteListener)
            mMediaPlayer!!.setOnBufferingUpdateListener(mBufferingUpdateListener)
            updateStatus(IPlayer.STATE_INITIALIZED)
            if (dataSource.timedTextSource != null) {
                Log.e(TAG, "ijkplayer not support timed text !")
            }
            val applicationContext: Context = PlayerContext.context()
            val data = dataSource.data
            val uri = dataSource.uri
            val assetsPath = dataSource.assetsPath
            val headers = dataSource.extra
            val rawId = dataSource.rawId
            if (data != null) {
                if (headers == null) mMediaPlayer!!.dataSource = data else mMediaPlayer!!.setDataSource(data, headers)
            } else if (uri != null) {
                if (uri.scheme == ContentResolver.SCHEME_ANDROID_RESOURCE) {
                    mMediaPlayer!!.setDataSource(RawDataSourceProvider.create(applicationContext, uri))
                } else {
                    if (headers == null) mMediaPlayer!!.setDataSource(applicationContext, uri) else mMediaPlayer!!.setDataSource(applicationContext, uri, headers)
                }
            } else if (!TextUtils.isEmpty(assetsPath)) {
                Log.e(TAG, "ijkplayer not support assets play, you can use raw play.")
            } else if (rawId > 0
                    && Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                val rawUri = buildRawPath(applicationContext.packageName, rawId)
                mMediaPlayer!!.setDataSource(RawDataSourceProvider.create(applicationContext, rawUri))
            }
            mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mMediaPlayer!!.setScreenOnWhilePlaying(true)
            mMediaPlayer!!.prepareAsync()

            //set looping indicator for IjkMediaPlayer
            mMediaPlayer!!.isLooping = isLooping()
            submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_DATA_SOURCE_SET, argObj = dataSource))
        } catch (e: Exception) {
            e.printStackTrace()
            updateStatus(IPlayer.STATE_ERROR)
            mTargetState = IPlayer.STATE_ERROR
            submitErrorEvent(HoHoMessage.obtain(what = OnErrorEventListener.ERROR_EVENT_IO))
        }
    }

    private fun available(): Boolean {
        return mMediaPlayer != null
    }

    override fun start() {
        if (available() &&
                (getState() == IPlayer.STATE_PREPARED || getState() == IPlayer.STATE_PAUSED || getState() == IPlayer.STATE_PLAYBACK_COMPLETE)) {
            mMediaPlayer!!.start()
            updateStatus(IPlayer.STATE_STARTED)
            submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_START))
        }
        mTargetState = IPlayer.STATE_STARTED
        Log.d(TAG, "start...")
    }

    override fun start(msc: Int) {
        if (getState() == IPlayer.STATE_PREPARED && msc > 0) {
            start()
            mMediaPlayer!!.seekTo(msc.toLong())
        } else {
            if (msc > 0) {
                startSeekPos = msc
            }
            if (available()) {
                start()
            }
        }
    }

    override fun pause() {
        try {
            val state = getState()
            if (available()
                    && state != IPlayer.STATE_END && state != IPlayer.STATE_ERROR && state != IPlayer.STATE_IDLE && state != IPlayer.STATE_INITIALIZED && state != IPlayer.STATE_PAUSED && state != IPlayer.STATE_STOPPED) {
                mMediaPlayer!!.pause()
                updateStatus(IPlayer.STATE_PAUSED)
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_PAUSE))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mTargetState = IPlayer.STATE_PAUSED
    }

    override fun resume() {
        try {
            if (available() && getState() == IPlayer.STATE_PAUSED) {
                mMediaPlayer!!.start()
                updateStatus(IPlayer.STATE_STARTED)
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_RESUME))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mTargetState = IPlayer.STATE_STARTED
    }

    override fun seekTo(msc: Int) {
        if (available() &&
                (getState() == IPlayer.STATE_PREPARED || getState() == IPlayer.STATE_STARTED || getState() == IPlayer.STATE_PAUSED || getState() == IPlayer.STATE_PLAYBACK_COMPLETE)) {
            mMediaPlayer!!.seekTo(msc.toLong())
            submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_TO, argInt = msc))
        }
    }

    override fun stop() {
        if (available() &&
                (getState() == IPlayer.STATE_PREPARED || getState() == IPlayer.STATE_STARTED || getState() == IPlayer.STATE_PAUSED || getState() == IPlayer.STATE_PLAYBACK_COMPLETE)) {
            mMediaPlayer!!.stop()
            updateStatus(IPlayer.STATE_STOPPED)
            submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_STOP))
        }
        mTargetState = IPlayer.STATE_STOPPED
    }

    override fun reset() {
        if (available()) {
            mMediaPlayer!!.reset()
            updateStatus(IPlayer.STATE_IDLE)
            submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_RESET))
        }
        mTargetState = IPlayer.STATE_IDLE
    }

    override fun isPlaying(): Boolean {
        return if (available() && getState() != IPlayer.STATE_ERROR) {
            mMediaPlayer!!.isPlaying
        } else false
    }

    override fun getCurrentPosition(): Int {
        return if (available() && (getState() == IPlayer.STATE_PREPARED || getState() == IPlayer.STATE_STARTED || getState() == IPlayer.STATE_PAUSED || getState() == IPlayer.STATE_PLAYBACK_COMPLETE)) {
            mMediaPlayer!!.currentPosition.toInt()
        } else 0
    }

    override fun getDuration(): Int {
        return if (available()
                && getState() != IPlayer.STATE_ERROR && getState() != IPlayer.STATE_INITIALIZED && getState() != IPlayer.STATE_IDLE) {
            mMediaPlayer!!.duration.toInt()
        } else 0
    }

    override fun getVideoWidth(): Int {
        return if (available()) {
            mMediaPlayer!!.videoWidth
        } else 0
    }

    override fun getVideoHeight(): Int {
        return if (available()) {
            mMediaPlayer!!.videoHeight
        } else 0
    }

    override fun destroy() {
        if (available()) {
            updateStatus(IPlayer.STATE_END)
            resetListener()
            mMediaPlayer!!.release()
            submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_DESTROY))
        }
    }

    override fun setDisplay(surfaceHolder: SurfaceHolder?) {
        try {
            if (available()) {
                mMediaPlayer!!.setDisplay(surfaceHolder)
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_SURFACE_HOLDER_UPDATE))
            }
        } catch (e: Exception) {
            submitErrorEvent(HoHoMessage.obtain(what = OnErrorEventListener.ERROR_EVENT_RENDER, extra = hashMapOf(
                    "errorMessage" to e.message, "causeMessage" to if (e.cause != null) e.cause!!.message else ""
            )))
        }
    }

    override fun setSurface(surface: Surface?) {
        try {
            if (available()) {
                mMediaPlayer!!.setSurface(surface)
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_SURFACE_UPDATE))
            }
        } catch (e: Exception) {
            submitErrorEvent(HoHoMessage.obtain(what = OnErrorEventListener.ERROR_EVENT_RENDER, extra = hashMapOf(
                    "errorMessage" to e.message, "causeMessage" to if (e.cause != null) e.cause!!.message else ""
            )))
        }
    }

    override fun setVolume(leftVolume: Float, rightVolume: Float) {
        if (available()) {
            mMediaPlayer!!.setVolume(leftVolume, rightVolume)
        }
    }

    override fun setSpeed(speed: Float) {
        if (available()) {
            mMediaPlayer!!.setSpeed(speed)
        }
    }

    override fun setLooping(looping: Boolean) {
        super.setLooping(looping)
        mMediaPlayer!!.isLooping = looping
    }

    override fun getAudioSessionId(): Int {
        return if (available()) {
            mMediaPlayer!!.audioSessionId
        } else 0
    }

    private fun resetListener() {
        if (mMediaPlayer == null) return
        mMediaPlayer!!.setOnPreparedListener(null)
        mMediaPlayer!!.setOnVideoSizeChangedListener(null)
        mMediaPlayer!!.setOnCompletionListener(null)
        mMediaPlayer!!.setOnErrorListener(null)
        mMediaPlayer!!.setOnInfoListener(null)
        mMediaPlayer!!.setOnBufferingUpdateListener(null)
    }

    var mPreparedListener = IMediaPlayer.OnPreparedListener { mp ->
        Log.d(TAG, "onPrepared...")
        updateStatus(IPlayer.STATE_PREPARED)
        mVideoWidth = mp.videoWidth
        mVideoHeight = mp.videoHeight
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_PREPARED, extra = hashMapOf(
                "videoWidth" to mVideoWidth, "videoHeight" to mVideoHeight
        )))
        val seekToPosition = startSeekPos // mSeekWhenPrepared may be changed after seekTo() call
        if (seekToPosition > 0 && mp.duration > 0) {
//            mMediaPlayer.seekTo(seekToPosition.toLong())
            startSeekPos = 0
        }

        // We don't know the video size yet, but should start anyway.
        // The video size might be reported to us later.
        Log.d(TAG, "mTargetState = $mTargetState")
        if (mTargetState == IPlayer.STATE_STARTED) {
            start()
        } else if (mTargetState == IPlayer.STATE_PAUSED) {
            pause()
        } else if (mTargetState == IPlayer.STATE_STOPPED
                || mTargetState == IPlayer.STATE_IDLE) {
            reset()
        }
    }
    private var mVideoWidth = 0
    private var mVideoHeight = 0
    var mSizeChangedListener = IMediaPlayer.OnVideoSizeChangedListener { mp, width, height, sarNum, sarDen ->
        mVideoWidth = mp.videoWidth
        mVideoHeight = mp.videoHeight
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_SIZE_CHANGE, extra = hashMapOf(
                "videoWidth" to mVideoWidth, "videoHeight" to mVideoHeight,
                "videoSarNum" to sarNum, "videoSarDen" to sarDen
        )))
    }
    private val mCompletionListener = IMediaPlayer.OnCompletionListener {
        updateStatus(IPlayer.STATE_PLAYBACK_COMPLETE)
        mTargetState = IPlayer.STATE_PLAYBACK_COMPLETE
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_PLAY_COMPLETE))
        if (!isLooping()) {
            stop()
        }
    }
    private val mInfoListener = IMediaPlayer.OnInfoListener { mp, arg1, arg2 ->
        when (arg1) {
            IMediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING -> Log.d(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING:")
            IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                Log.d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START")
                startSeekPos = 0
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_RENDER_START))
            }
            IMediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                Log.d(TAG, "MEDIA_INFO_BUFFERING_START:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_START))
            }
            IMediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                Log.d(TAG, "MEDIA_INFO_BUFFERING_END:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_END))
            }
            IMediaPlayer.MEDIA_INFO_NETWORK_BANDWIDTH -> {
            }
            IMediaPlayer.MEDIA_INFO_BAD_INTERLEAVING -> {
                Log.d(TAG, "MEDIA_INFO_BAD_INTERLEAVING:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_BAD_INTERLEAVING))
            }
            IMediaPlayer.MEDIA_INFO_NOT_SEEKABLE -> {
                Log.d(TAG, "MEDIA_INFO_NOT_SEEKABLE:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_NOT_SEEK_ABLE))
            }
            IMediaPlayer.MEDIA_INFO_METADATA_UPDATE -> {
                Log.d(TAG, "MEDIA_INFO_METADATA_UPDATE:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_METADATA_UPDATE))
            }
            IMediaPlayer.MEDIA_INFO_TIMED_TEXT_ERROR -> {
                Log.d(TAG, "MEDIA_INFO_TIMED_TEXT_ERROR:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_TIMED_TEXT_ERROR))
            }
            IMediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE -> {
                Log.d(TAG, "MEDIA_INFO_UNSUPPORTED_SUBTITLE:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_UNSUPPORTED_SUBTITLE))
            }
            IMediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT -> {
                Log.d(TAG, "MEDIA_INFO_SUBTITLE_TIMED_OUT:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_SUBTITLE_TIMED_OUT))
            }
            IMediaPlayer.MEDIA_INFO_VIDEO_ROTATION_CHANGED -> {
                Log.d(TAG, "MEDIA_INFO_VIDEO_ROTATION_CHANGED: $arg2")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_ROTATION_CHANGED, argInt = arg2))
            }
            IMediaPlayer.MEDIA_INFO_AUDIO_RENDERING_START -> {
                Log.d(TAG, "MEDIA_INFO_AUDIO_RENDERING_START:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_AUDIO_RENDER_START))
            }
            IMediaPlayer.MEDIA_INFO_AUDIO_DECODED_START -> {
                Log.d(TAG, "MEDIA_INFO_AUDIO_DECODED_START:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_AUDIO_DECODER_START))
            }
            IMediaPlayer.MEDIA_INFO_AUDIO_SEEK_RENDERING_START -> {
                Log.d(TAG, "MEDIA_INFO_AUDIO_SEEK_RENDERING_START:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_AUDIO_SEEK_RENDERING_START))
            }
        }
        true
    }
    private val mOnSeekCompleteListener = IMediaPlayer.OnSeekCompleteListener {
        Log.d(TAG, "EVENT_CODE_SEEK_COMPLETE")
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_COMPLETE))
    }
    private val mErrorListener = IMediaPlayer.OnErrorListener { mp, framework_err, impl_err ->
        Log.d(TAG, "Error: $framework_err,$impl_err")
        updateStatus(IPlayer.STATE_ERROR)
        mTargetState = IPlayer.STATE_ERROR
        when (framework_err) {
            100 -> {
            }
        }

        /* If an error handler has been supplied, use it and finish. */
        submitPlayerEvent(HoHoMessage.obtain(what = OnErrorEventListener.ERROR_EVENT_COMMON))
        true
    }
    private val mBufferingUpdateListener = IMediaPlayer.OnBufferingUpdateListener { mp, percent -> submitBufferingUpdate(percent) }

    init {
        // init player
        mMediaPlayer = IjkMediaPlayer()
        mOptionArrays = SparseArray()
    }
}