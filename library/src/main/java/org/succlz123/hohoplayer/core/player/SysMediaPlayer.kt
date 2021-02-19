package org.succlz123.hohoplayer.core.player

import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.TrackInfo
import android.media.PlaybackParams
import android.os.Build
import android.text.TextUtils
import android.view.Surface
import android.view.SurfaceHolder
import org.succlz123.hohoplayer.config.PlayerContext
import org.succlz123.hohoplayer.core.player.base.BasePlayer
import org.succlz123.hohoplayer.core.player.base.IPlayer
import org.succlz123.hohoplayer.core.player.listener.OnErrorEventListener
import org.succlz123.hohoplayer.core.player.listener.OnPlayerEventListener
import org.succlz123.hohoplayer.core.source.DataSource
import org.succlz123.hohoplayer.core.source.DataSource.Companion.buildRawPath
import org.succlz123.hohoplayer.core.source.DataSource.Companion.getAssetsFileDescriptor
import org.succlz123.hohoplayer.support.log.PlayerLog.d
import org.succlz123.hohoplayer.support.log.PlayerLog.e
import org.succlz123.hohoplayer.support.message.HoHoMessage

class SysMediaPlayer : BasePlayer() {

    companion object {
        const val TAG = "SysMediaPlayer"
        private const val MEDIA_INFO_NETWORK_BANDWIDTH = 703
    }

    private var mediaPlayer: MediaPlayer = MediaPlayer()

    private var targetState: Int = Integer.MAX_VALUE

    private var bandWidth: Long = 0

    private var curDataSource: DataSource? = null

    override fun setDataSource(dataSource: DataSource) {
        try {
            stop()
            reset()
            resetListener()
            targetState = Integer.MAX_VALUE
            // REMOVED: mAudioSession
            mediaPlayer.setOnPreparedListener(preparedListener)
            mediaPlayer.setOnVideoSizeChangedListener(sizeChangedListener)
            mediaPlayer.setOnCompletionListener(completionListener)
            mediaPlayer.setOnErrorListener(errorListener)
            mediaPlayer.setOnInfoListener(infoListener)
            mediaPlayer.setOnSeekCompleteListener(onSeekCompleteListener)
            mediaPlayer.setOnBufferingUpdateListener(bufferingUpdateListener)
            updateStatus(IPlayer.STATE_INITIALIZED)

            curDataSource = dataSource
            val applicationContext = PlayerContext.context()
            val data = dataSource.data
            val uri = dataSource.uri
            val assetsPath = dataSource.assetsPath
            val headers = dataSource.extra
            val rawId = dataSource.rawId
            if (data != null) {
                mediaPlayer.setDataSource(data)
            } else if (uri != null) {
                if (headers == null) {
                    mediaPlayer.setDataSource(applicationContext, uri)
                } else {
                    mediaPlayer.setDataSource(applicationContext, uri, headers)
                }
            } else if (!TextUtils.isEmpty(assetsPath)) {
                val fileDescriptor = getAssetsFileDescriptor(
                        applicationContext, dataSource.assetsPath
                )
                if (fileDescriptor != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        mediaPlayer.setDataSource(fileDescriptor)
                    } else {
                        mediaPlayer.setDataSource(
                                fileDescriptor.fileDescriptor,
                                fileDescriptor.startOffset, fileDescriptor.length
                        )
                    }
                }
            } else if (rawId > 0) {
                val rawUri = buildRawPath(applicationContext.packageName, rawId)
                mediaPlayer.setDataSource(applicationContext, rawUri)
            }
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mediaPlayer.setScreenOnWhilePlaying(true)
            mediaPlayer.prepareAsync()

            // set looping indicator for MediaPlayer
            mediaPlayer.isLooping = isLooping()

            submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_DATA_SOURCE_SET, argObj = dataSource))
        } catch (e: Exception) {
            e.printStackTrace()
            updateStatus(IPlayer.STATE_ERROR)
            targetState = IPlayer.STATE_ERROR
        }
    }

    override fun getDataSource(): DataSource? {
        return curDataSource
    }

    override fun option(message: HoHoMessage) {
    }

    override fun setDisplay(surfaceHolder: SurfaceHolder?) {
        try {
            mediaPlayer.setDisplay(surfaceHolder)
            submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_SURFACE_HOLDER_UPDATE))
        } catch (e: Exception) {
            handleException(e)
        }
    }

    override fun setSurface(surface: Surface?) {
        try {
            mediaPlayer.setSurface(surface)
            submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_SURFACE_UPDATE))
        } catch (e: Exception) {
            handleException(e)
        }
    }

    override fun setVolume(left: Float, right: Float) {
        mediaPlayer.setVolume(left, right)
    }

    override fun setSpeed(speed: Float) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val playbackParams = mediaPlayer.playbackParams
                playbackParams.speed = speed
                /**
                 * Sets playback rate using [PlaybackParams]. The object sets its internal
                 * PlaybackParams to the input, except that the object remembers previous speed
                 * when input speed is zero. This allows the object to resume at previous speed
                 * when start() is called. Calling it before the object is prepared does not change
                 * the object state. After the object is prepared, calling it with zero speed is
                 * equivalent to calling pause(). After the object is prepared, calling it with
                 * non-zero speed is equivalent to calling start().
                 */
                mediaPlayer.playbackParams = playbackParams
                if (speed <= 0) {
                    pause()
                } else if (speed > 0 && getState() == IPlayer.STATE_PAUSED) {
                    resume()
                }
            } else {
                e(TAG, "not support play speed setting.")
            }
        } catch (e: Exception) {
            e(
                    TAG,
                    "IllegalStateException，if the internal player engine has not been initialized " +
                            "or has been released."
            )
        }
    }

    override fun setLooping(looping: Boolean) {
        super.setLooping(looping)
        mediaPlayer.isLooping = looping
    }

    override fun isPlaying(): Boolean {
        return if (getState() != IPlayer.STATE_ERROR) {
            mediaPlayer.isPlaying
        } else {
            false
        }
    }

    override fun getCurrentPosition(): Int {
        return if ((getState() == IPlayer.STATE_PREPARED
                        || getState() == IPlayer.STATE_STARTED
                        || getState() == IPlayer.STATE_PAUSED
                        || getState() == IPlayer.STATE_PLAYBACK_COMPLETE)
        ) {
            mediaPlayer.currentPosition
        } else {
            0
        }
    }

    override fun getDuration(): Int {
        return if (getState() != IPlayer.STATE_ERROR
                && getState() != IPlayer.STATE_INITIALIZED
                && getState() != IPlayer.STATE_IDLE
        ) {
            mediaPlayer.duration
        } else {
            0
        }
    }

    override fun getAudioSessionId(): Int {
        return mediaPlayer.audioSessionId
    }

    override fun getVideoWidth(): Int {
        return mediaPlayer.videoWidth
    }

    override fun getVideoHeight(): Int {
        return mediaPlayer.videoHeight
    }

    override fun start() {
        try {
            if (getState() == IPlayer.STATE_PREPARED ||
                    getState() == IPlayer.STATE_PAUSED ||
                    getState() == IPlayer.STATE_PLAYBACK_COMPLETE
            ) {
                mediaPlayer.start()
                updateStatus(IPlayer.STATE_STARTED)
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_START))
            }
        } catch (e: Exception) {
            handleException(e)
        }
        targetState = IPlayer.STATE_STARTED
    }

    override fun start(msc: Int) {
        if (getState() == IPlayer.STATE_PREPARED && msc > 0) {
            start()
            mediaPlayer.seekTo(msc)
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
                    && state != IPlayer.STATE_STOPPED
            ) {
                mediaPlayer.pause()
                updateStatus(IPlayer.STATE_PAUSED)
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_PAUSE))
            }
        } catch (e: Exception) {
            handleException(e)
        }
        targetState = IPlayer.STATE_PAUSED
    }

    override fun resume() {
        try {
            if (getState() == IPlayer.STATE_PAUSED) {
                mediaPlayer.start()
                updateStatus(IPlayer.STATE_STARTED)
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_RESUME))
            }
        } catch (e: Exception) {
            handleException(e)
        }
        targetState = IPlayer.STATE_STARTED
    }

    override fun seekTo(msc: Int) {
        if (getState() == IPlayer.STATE_PREPARED
                || getState() == IPlayer.STATE_STARTED
                || getState() == IPlayer.STATE_PAUSED
                || getState() == IPlayer.STATE_PLAYBACK_COMPLETE
        ) {
            mediaPlayer.seekTo(msc)
            submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_TO, argInt = msc))
        }
    }

    override fun stop() {
        try {
            if (getState() == IPlayer.STATE_PREPARED
                    || getState() == IPlayer.STATE_STARTED
                    || getState() == IPlayer.STATE_PAUSED
                    || getState() == IPlayer.STATE_PLAYBACK_COMPLETE
            ) {
                mediaPlayer.stop()
                updateStatus(IPlayer.STATE_STOPPED)
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_STOP))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        targetState = IPlayer.STATE_STOPPED
    }

    override fun reset() {
        mediaPlayer.reset()
        updateStatus(IPlayer.STATE_IDLE)
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_RESET))
        targetState = IPlayer.STATE_IDLE
    }

    override fun destroy() {
        updateStatus(IPlayer.STATE_END)
        resetListener()
        mediaPlayer.release()
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_DESTROY))
    }

    private fun handleException(e: Exception?) {
        e?.printStackTrace()
        reset()
    }

    private fun resetListener() {
        mediaPlayer.setOnPreparedListener(null)
        mediaPlayer.setOnVideoSizeChangedListener(null)
        mediaPlayer.setOnCompletionListener(null)
        mediaPlayer.setOnErrorListener(null)
        mediaPlayer.setOnInfoListener(null)
        mediaPlayer.setOnBufferingUpdateListener(null)
    }

    var preparedListener = MediaPlayer.OnPreparedListener { mp ->
        d(TAG, "onPrepared...")

        updateStatus(IPlayer.STATE_PREPARED)
        videoWidth = mp.videoWidth
        videoHeight = mp.videoHeight
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_PREPARED, extra = hashMapOf(
                "videoWidth" to videoWidth, "videoHeight" to videoHeight
        )))

        val seekToPosition = startSeekPos // mSeekWhenPrepared may be changed after seekTo() call
        if (seekToPosition > 0 && mp.duration > 0) {
            //seek to start position
            mediaPlayer.seekTo(seekToPosition)
            startSeekPos = 0
        }

        // We don't know the video size yet, but should start anyway.
        // The video size might report to us later.
        d(TAG, "mTargetState = $targetState")
        if (targetState == IPlayer.STATE_STARTED) {
            start()
        } else if (targetState == IPlayer.STATE_PAUSED) {
            pause()
        } else if (targetState == IPlayer.STATE_STOPPED || targetState == IPlayer.STATE_IDLE) {
            reset()
        }
        attachTimedTextSource()
    }

    private fun attachTimedTextSource() {
        val timedTextSource = curDataSource?.timedTextSource ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mediaPlayer.addTimedTextSource(timedTextSource.path, timedTextSource.mimeType)
                val trackInfo = mediaPlayer.trackInfo
                if (!trackInfo.isNullOrEmpty()) {
                    for (i in trackInfo.indices) {
                        val info = trackInfo[i]
                        if (info.trackType == TrackInfo.MEDIA_TRACK_TYPE_TIMEDTEXT) {
                            mediaPlayer.selectTrack(i)
                            break
                        }
                    }
                }
            } else {
                e(TAG, "not support setting timed text source !")
            }
        } catch (e: Exception) {
            e(TAG, "addTimedTextSource error !")
            e.printStackTrace()
        }
    }

    private var videoWidth = 0

    private var videoHeight = 0

    var sizeChangedListener = MediaPlayer.OnVideoSizeChangedListener { mp, width, height ->
        videoWidth = mp.videoWidth
        videoHeight = mp.videoHeight
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_SIZE_CHANGE, extra = hashMapOf(
                "videoWidth" to videoWidth, "videoHeight" to videoHeight
        )))
    }

    private val completionListener = MediaPlayer.OnCompletionListener {
        updateStatus(IPlayer.STATE_PLAYBACK_COMPLETE)
        targetState = IPlayer.STATE_PLAYBACK_COMPLETE
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_PLAY_COMPLETE))
        if (!isLooping()) {
            stop()
        }
    }

    private var startSeekPos = 0

    private val infoListener = MediaPlayer.OnInfoListener { mp, arg1, arg2 ->
        when (arg1) {
            MediaPlayer.MEDIA_INFO_VIDEO_TRACK_LAGGING -> {
                d(TAG, "MEDIA_INFO_VIDEO_TRACK_LAGGING:")
            }
            MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START -> {
                d(TAG, "MEDIA_INFO_VIDEO_RENDERING_START")
                startSeekPos = 0
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_RENDER_START))
            }
            MediaPlayer.MEDIA_INFO_BUFFERING_START -> {
                d(TAG, "MEDIA_INFO_BUFFERING_START:$arg2")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_START, argLong = bandWidth))
            }
            MediaPlayer.MEDIA_INFO_BUFFERING_END -> {
                d(TAG, "MEDIA_INFO_BUFFERING_END:$arg2")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_END, argLong = bandWidth))
            }
            MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING -> {
                d(TAG, "MEDIA_INFO_BAD_INTERLEAVING:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_BAD_INTERLEAVING))
            }
            MediaPlayer.MEDIA_INFO_NOT_SEEKABLE -> {
                d(TAG, "MEDIA_INFO_NOT_SEEKABLE:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_NOT_SEEK_ABLE))
            }
            MediaPlayer.MEDIA_INFO_METADATA_UPDATE -> {
                d(TAG, "MEDIA_INFO_METADATA_UPDATE:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_METADATA_UPDATE))
            }
            MediaPlayer.MEDIA_INFO_UNSUPPORTED_SUBTITLE -> {
                d(TAG, "MEDIA_INFO_UNSUPPORTED_SUBTITLE:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_UNSUPPORTED_SUBTITLE))
            }
            MediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT -> {
                d(TAG, "MEDIA_INFO_SUBTITLE_TIMED_OUT:")
                submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_SUBTITLE_TIMED_OUT))
            }
            MEDIA_INFO_NETWORK_BANDWIDTH -> {
                d(TAG, "band_width : $arg2")
                bandWidth = arg2 * 1000.toLong()
            }
        }
        true
    }

    private val onSeekCompleteListener = MediaPlayer.OnSeekCompleteListener {
        d(TAG, "EVENT_CODE_SEEK_COMPLETE")
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_COMPLETE))
    }

    private val errorListener = MediaPlayer.OnErrorListener { mp, frameworkErr, implErr ->
        d(TAG, "Error: $frameworkErr,$implErr")
        updateStatus(IPlayer.STATE_ERROR)
        targetState = IPlayer.STATE_ERROR
        var eventCode = OnErrorEventListener.ERROR_EVENT_COMMON
        when (frameworkErr) {
            MediaPlayer.MEDIA_ERROR_IO -> {
                eventCode = OnErrorEventListener.ERROR_EVENT_IO
            }
            MediaPlayer.MEDIA_ERROR_MALFORMED -> {
                eventCode = OnErrorEventListener.ERROR_EVENT_MALFORMED
            }
            MediaPlayer.MEDIA_ERROR_TIMED_OUT -> {
                eventCode = OnErrorEventListener.ERROR_EVENT_TIMED_OUT
            }
            MediaPlayer.MEDIA_ERROR_UNKNOWN -> {
                eventCode = OnErrorEventListener.ERROR_EVENT_UNKNOWN
            }
            MediaPlayer.MEDIA_ERROR_UNSUPPORTED -> {
                eventCode = OnErrorEventListener.ERROR_EVENT_UNSUPPORTED
            }
            MediaPlayer.MEDIA_ERROR_SERVER_DIED -> {
                eventCode = OnErrorEventListener.ERROR_EVENT_SERVER_DIED
            }
            MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK -> {
                eventCode = OnErrorEventListener.ERROR_EVENT_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK
            }
        }
        submitErrorEvent(HoHoMessage.obtain(what = eventCode))
        true
    }

    private val bufferingUpdateListener = MediaPlayer.OnBufferingUpdateListener { mp, percent ->
        submitBufferingUpdate(percent)
    }
}