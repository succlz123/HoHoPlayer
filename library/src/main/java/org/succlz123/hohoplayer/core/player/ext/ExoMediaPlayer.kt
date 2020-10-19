package org.succlz123.hohoplayer.core.player.ext

import android.net.Uri
import android.text.TextUtils
import android.view.Surface
import android.view.SurfaceHolder
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.source.*
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.upstream.AssetDataSource.AssetDataSourceException
import com.google.android.exoplayer2.upstream.RawResourceDataSource.RawResourceDataSourceException
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.android.exoplayer2.video.VideoListener
import org.succlz123.hohoplayer.config.PlayerConfig
import org.succlz123.hohoplayer.config.PlayerContext
import org.succlz123.hohoplayer.core.player.base.BasePlayer
import org.succlz123.hohoplayer.core.player.base.IPlayer
import org.succlz123.hohoplayer.core.player.base.IPlayer.Companion.STATE_PREPARED
import org.succlz123.hohoplayer.core.player.listener.OnErrorEventListener
import org.succlz123.hohoplayer.core.player.listener.OnPlayerEventListener
import org.succlz123.hohoplayer.core.source.DataSource
import org.succlz123.hohoplayer.core.source.DataSource.Companion.buildAssetsUri
import org.succlz123.hohoplayer.core.source.Decoder
import org.succlz123.hohoplayer.support.log.PlayerLog.d
import org.succlz123.hohoplayer.support.log.PlayerLog.e
import org.succlz123.hohoplayer.support.message.HoHoMessage

class ExoMediaPlayer : BasePlayer() {

    companion object {
        const val TAG = "ExoMediaPlayer"

        fun addThis(setAsDefault: Boolean = false) {
            if (setAsDefault) {
                PlayerConfig.addAndSetDecoder(Decoder(TAG, ExoMediaPlayer::class.java.name))
            } else {
                PlayerConfig.addDecoder(Decoder(TAG, ExoMediaPlayer::class.java.name))
            }
        }
    }

    private val appContext = PlayerContext.context()

    private lateinit var internalPlayer: SimpleExoPlayer

    private var mVideoWidth = 0

    private var mVideoHeight = 0

    private var mStartPos = -1L

    private var isPreparing = true

    private var isBuffering = false

    private var isPendingSeek = false

    // Measures bandwidth during playback. Can be null if not required.
    private val bandwidthMeter: DefaultBandwidthMeter =
            DefaultBandwidthMeter.Builder(appContext).build()

    private var eventListener: Player.EventListener = object : Player.EventListener {

        override fun onTracksChanged(
                trackGroups: TrackGroupArray,
                trackSelections: TrackSelectionArray
        ) {
        }

        override fun onLoadingChanged(isLoading: Boolean) {
            val bufferPercentage = internalPlayer.bufferedPercentage
            if (!isLoading) {
                submitBufferingUpdate(bufferPercentage)
            }
            d(TAG, "onLoadingChanged : $isLoading, bufferPercentage = $bufferPercentage")
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            d(TAG, "onPlayerStateChanged : playWhenReady = $playWhenReady, playbackState = $playbackState")
            if (isPreparing) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        isPreparing = false
                        val format = internalPlayer.videoFormat
                        updateStatus(STATE_PREPARED)
                        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_PREPARED, extra = hashMapOf(
                                "videoWidth" to (format?.width
                                        ?: -1), "videoHeight" to (format?.height ?: -1)
                        )))

                        if (mStartPos > 0 && internalPlayer.duration > 0) {
                            internalPlayer.seekTo(mStartPos)
                            mStartPos = -1
                        }
                    }
                }
            }
            if (isBuffering) {
                when (playbackState) {
                    Player.STATE_READY, Player.STATE_ENDED -> {
                        val bitrateEstimate: Long = bandwidthMeter.bitrateEstimate
                        d(TAG, "buffer_end, BandWidth : $bitrateEstimate")
                        isBuffering = false
                        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_END, argLong = bitrateEstimate))
                    }
                }
            }

            if (isPendingSeek) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        isPendingSeek = false
                        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_COMPLETE))
                    }
                }
            }

            if (!isPreparing) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        val bitrateEstimate: Long = bandwidthMeter.bitrateEstimate
                        d(TAG, "buffer_start, BandWidth : $bitrateEstimate")
                        isBuffering = true
                        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_START, argLong = bitrateEstimate))
                    }
                    Player.STATE_ENDED -> {
                        updateStatus(IPlayer.STATE_PLAYBACK_COMPLETE)
                        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_PLAY_COMPLETE))
                    }
                }
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            d(TAG, "onPlayerStateChanged : playWhenReady = $playWhenReady, reason = $reason")
            if (!isPreparing) {
                if (playWhenReady) {
                    if (getState() == STATE_PREPARED) {
                        updateStatus(IPlayer.STATE_STARTED)
                        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_AUDIO_RENDER_START))
                    } else {
                        updateStatus(IPlayer.STATE_STARTED)
                        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_RESUME))
                    }
                } else {
                    updateStatus(IPlayer.STATE_PAUSED)
                    submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_PAUSE))
                }
            }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {}

        override fun onPlayerError(error: ExoPlaybackException) {
            val errorMessage = error.message ?: ""
            val causeMessage = error.cause?.message ?: ""
            e(TAG, "$errorMessage, causeMessage = $causeMessage")
            val message = HoHoMessage.obtain()
            message.getDataNoNone().apply {
                put("errorMessage", errorMessage)
                put("causeMessage", causeMessage)
            }
            when (error.type) {
                ExoPlaybackException.TYPE_SOURCE -> {
                    submitErrorEvent(message.apply { what = OnErrorEventListener.ERROR_EVENT_IO })
                }
                ExoPlaybackException.TYPE_RENDERER -> {
                    submitErrorEvent(message.apply { what = OnErrorEventListener.ERROR_EVENT_COMMON })
                }
                ExoPlaybackException.TYPE_UNEXPECTED -> {
                    submitErrorEvent(message.apply { what = OnErrorEventListener.ERROR_EVENT_UNKNOWN })
                }
                ExoPlaybackException.TYPE_OUT_OF_MEMORY -> {
                    submitErrorEvent(message.apply { what = OnErrorEventListener.ERROR_EVENT_OUT_OF_MEMORY })
                }
                ExoPlaybackException.TYPE_REMOTE -> {
                    submitErrorEvent(message.apply { what = OnErrorEventListener.ERROR_EVENT_REMOTE })
                }
                ExoPlaybackException.TYPE_TIMEOUT -> {
                    submitErrorEvent(message.apply { what = OnErrorEventListener.ERROR_EVENT_TIMED_OUT })
                }
            }
        }

        override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
            d(TAG, "onPlaybackParametersChanged : $playbackParameters")
        }
    }

    init {
        val renderersFactory: RenderersFactory = DefaultRenderersFactory(appContext)
        val trackSelector = DefaultTrackSelector(appContext)
        internalPlayer =
                SimpleExoPlayer.Builder(appContext, renderersFactory).setTrackSelector(trackSelector)
                        .build()
        internalPlayer.addListener(eventListener)
    }

    override fun setDataSource(dataSource: DataSource) {
        updateStatus(IPlayer.STATE_INITIALIZED)
        internalPlayer.addVideoListener(videoListener)
        val data = dataSource.data
        val uri = dataSource.uri
        val assetsPath = dataSource.assetsPath
        val rawId = dataSource.rawId
        var videoUri: Uri? = null
        if (!TextUtils.isEmpty(data)) {
            videoUri = Uri.parse(data)
        } else if (uri != null) {
            videoUri = uri
        } else if (!TextUtils.isEmpty(assetsPath)) {
            try {
                val dataSpec = DataSpec(buildAssetsUri(assetsPath!!))
                val assetDataSource = AssetDataSource(appContext)
                assetDataSource.open(dataSpec)
                videoUri = assetDataSource.uri
            } catch (e: AssetDataSourceException) {
                e.printStackTrace()
            }
        } else if (rawId > 0) {
            try {
                val dataSpec = DataSpec(RawResourceDataSource.buildRawResourceUri(dataSource.rawId))
                val rawResourceDataSource = RawResourceDataSource(appContext)
                rawResourceDataSource.open(dataSpec)
                videoUri = rawResourceDataSource.uri
            } catch (e: RawResourceDataSourceException) {
                e.printStackTrace()
            }
        }
        if (videoUri == null) {
            val message = HoHoMessage.obtain()
            message.getDataNoNone().apply {
                put("errorMessage", "Incorrect setting of playback data!")
                put("causeMessage", "Incorrect setting of playback data!")
            }
            submitErrorEvent(message.apply { what = OnErrorEventListener.ERROR_EVENT_IO })
            return
        }

        // if scheme is http or https and DataSource contain extra data, use DefaultHttpDataSourceFactory.
        val scheme = videoUri.scheme
        val extra = dataSource.extra
        // setting user-agent from extra data
        val settingUserAgent = extra?.get("User-Agent")
        // if not setting, use default user-agent
        val userAgent = if (settingUserAgent.isNullOrEmpty()) {
            Util.getUserAgent(appContext, appContext.packageName)
        } else {
            settingUserAgent
        }
        // create DefaultDataSourceFactory
        var dataSourceFactory: com.google.android.exoplayer2.upstream.DataSource.Factory =
                DefaultDataSourceFactory(appContext, userAgent, bandwidthMeter)
        if (extra != null && extra.size > 0
                && ("http".equals(scheme, ignoreCase = true) || "https".equals(
                        scheme,
                        ignoreCase = true
                ))
        ) {
            dataSourceFactory = DefaultHttpDataSourceFactory(
                    userAgent,
                    DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                    DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                    true
            )
            dataSourceFactory.defaultRequestProperties.set(extra)
        }

        // Prepare the player with the source
        isPreparing = true

        // Create MediaSource
        var mediaSource = getMediaSource(videoUri, dataSourceFactory)

        // Handle timed text source
        val timedTextSource = dataSource.timedTextSource
        if (timedTextSource != null) {
            val format = Format.createTextSampleFormat(
                    null,
                    timedTextSource.mimeType,
                    timedTextSource.flag,
                    null
            )
            val timedTextMediaSource =
                    SingleSampleMediaSource.Factory(DefaultDataSourceFactory(appContext, userAgent))
                            .createMediaSource(Uri.parse(timedTextSource.path), format, C.TIME_UNSET)
            // Merge MediaSource and timedTextMediaSource.
            mediaSource = MergingMediaSource(mediaSource, timedTextMediaSource)
        }
        internalPlayer.prepare(mediaSource)
        internalPlayer.playWhenReady = false

        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_DATA_SOURCE_SET, argObj = dataSource))
    }

    private fun getMediaSource(
            uri: Uri,
            dataSourceFactory: com.google.android.exoplayer2.upstream.DataSource.Factory
    ): MediaSource {
        val contentType = Util.inferContentType(uri)
        val mediaItem = MediaItem.Builder()
                .setUri(uri)
                .setMimeType(MimeTypes.APPLICATION_MPD)
                .build()
        return when (contentType) {
            C.TYPE_DASH -> DashMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            C.TYPE_SS -> SsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            C.TYPE_HLS -> HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
            C.TYPE_OTHER -> ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(mediaItem)
            // This is the MediaSource representing the media to be played.
            else -> ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        }
    }

    override fun option(message: HoHoMessage) {
    }

    override fun setDisplay(surfaceHolder: SurfaceHolder?) {
        internalPlayer.setVideoSurfaceHolder(surfaceHolder)
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_SURFACE_HOLDER_UPDATE))
    }

    override fun setSurface(surface: Surface?) {
        internalPlayer.setVideoSurface(surface)
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_SURFACE_UPDATE))
    }

    override fun setVolume(left: Float, right: Float) {
        internalPlayer.volume = left
    }

    override fun setSpeed(speed: Float) {
        internalPlayer.setPlaybackParameters(PlaybackParameters(speed, 1f))
    }

    override fun setLooping(looping: Boolean) {
        internalPlayer.repeatMode = if (looping) {
            Player.REPEAT_MODE_ALL
        } else {
            Player.REPEAT_MODE_OFF
        }
    }

    override fun isPlaying(): Boolean {
        val state = internalPlayer.playbackState
        return when (state) {
            Player.STATE_BUFFERING, Player.STATE_READY -> {
                internalPlayer.playWhenReady
            }
            Player.STATE_IDLE, Player.STATE_ENDED -> {
                false
            }
            else -> {
                false
            }
        }
    }

    override fun getCurrentPosition(): Int {
        return internalPlayer.currentPosition.toInt()
    }

    override fun getDuration(): Int {
        return internalPlayer.duration.toInt()
    }

    override fun getAudioSessionId(): Int {
        return internalPlayer.audioSessionId
    }

    override fun getVideoWidth(): Int {
        return mVideoWidth
    }

    override fun getVideoHeight(): Int {
        return mVideoHeight
    }

    override fun start() {
        internalPlayer.playWhenReady = true
    }

    override fun start(msc: Int) {
        if (getState() == STATE_PREPARED && msc > 0) {
            start()
            seekTo(msc)
        } else {
            mStartPos = msc.toLong()
            start()
        }
    }

    override fun pause() {
        val state = getState()
        if (isInPlaybackState
                && state != IPlayer.STATE_END
                && state != IPlayer.STATE_ERROR
                && state != IPlayer.STATE_IDLE
                && state != IPlayer.STATE_INITIALIZED
                && state != IPlayer.STATE_PAUSED
                && state != IPlayer.STATE_STOPPED
        ) {
            internalPlayer.playWhenReady = false
        }
    }

    override fun resume() {
        if (isInPlaybackState && getState() == IPlayer.STATE_PAUSED) {
            internalPlayer.playWhenReady = true
        }
    }

    override fun seekTo(msc: Int) {
        if (isInPlaybackState) {
            isPendingSeek = true
        }
        internalPlayer.seekTo(msc.toLong())
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_TO, argInt = msc))
    }

    override fun stop() {
        isPreparing = true
        isBuffering = false
        updateStatus(IPlayer.STATE_STOPPED)
        internalPlayer.stop()
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_STOP))
    }

    override fun reset() {
        stop()
    }

    override fun destroy() {
        isPreparing = true
        isBuffering = false
        updateStatus(IPlayer.STATE_END)
        internalPlayer.removeListener(eventListener)
        internalPlayer.removeVideoListener(videoListener)
        internalPlayer.release()
        submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_DESTROY))
    }

    private val isInPlaybackState: Boolean
        get() {
            val state = getState()
            return state != IPlayer.STATE_END
                    && state != IPlayer.STATE_ERROR
                    && state != IPlayer.STATE_INITIALIZED
                    && state != IPlayer.STATE_STOPPED
        }

    private val videoListener: VideoListener = object : VideoListener {
        override fun onVideoSizeChanged(
                width: Int, height: Int,
                unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float
        ) {
            mVideoWidth = width
            mVideoHeight = height
            submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_SIZE_CHANGE, extra = hashMapOf(
                    "videoWidth" to mVideoWidth, "videoHeight" to mVideoHeight,
                    "videoSarNum" to 0, "videoSarDen" to 0 // ijk ->
            )))
        }

        override fun onRenderedFirstFrame() {
            d(TAG, "onRenderedFirstFrame")
            updateStatus(IPlayer.STATE_STARTED)
            submitPlayerEvent(HoHoMessage.obtain(what = OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_RENDER_START))
        }
    }
}