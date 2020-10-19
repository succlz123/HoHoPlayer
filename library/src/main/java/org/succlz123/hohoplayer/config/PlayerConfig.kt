package org.succlz123.hohoplayer.config

import android.content.Context
import org.succlz123.hohoplayer.core.player.SysMediaPlayer
import org.succlz123.hohoplayer.core.source.Decoder

object PlayerConfig {
    const val SYS_MEDIA_PLAYER = SysMediaPlayer.TAG

    private var defaultDecoder = SYS_MEDIA_PLAYER

    private var sources = hashMapOf<String, Decoder>()

    // whether use the default NetworkEventProducer.
    var isUseDefaultNetworkEventProducer = false

    var enableVideoCache = false

    init {
        addDecoder(Decoder(SYS_MEDIA_PLAYER, SysMediaPlayer::class.java.name))
        defaultDecoder = SYS_MEDIA_PLAYER
    }

    fun init(context: Context, enableVideoCache: Boolean, cacheSize: Long = 1024 * 1024 * 256) {
        PlayerContext.appContext = context
        PlayerConfig.enableVideoCache = enableVideoCache
        if (enableVideoCache) {
            PlayerCacher.init(context, cacheSize)
        }
    }

    fun addDecoder(decoder: Decoder) {
        sources[decoder.name] = decoder
    }

    fun addAndSetDecoder(decoder: Decoder) {
        sources[decoder.name] = decoder
        defaultDecoder = decoder.name
    }

    fun getDecoder(name: String): Decoder? {
        return sources[name]
    }

    fun getDefaultDecoderName(): String {
        return defaultDecoder
    }
}
