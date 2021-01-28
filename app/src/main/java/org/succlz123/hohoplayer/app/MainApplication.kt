package org.succlz123.hohoplayer.app

import android.app.Application
import org.succlz123.hohoplayer.config.PlayerConfig
import org.succlz123.hohoplayer.core.player.ext.exo.ExoMediaPlayer
import org.succlz123.hohoplayer.core.player.ext.ijk.IjkPlayer
import org.succlz123.hohoplayer.support.log.PlayerLog

class MainApplication : Application() {

    companion object {
        lateinit var instance: MainApplication

        var ignoreMobile = false
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        PlayerLog.LOG_OPEN = true
        PlayerConfig.init(this, true)
        IjkPlayer.addThis(true)
//        ExoMediaPlayer.addThis(false)
        PlayerConfig.isUseDefaultNetworkEventProducer = true
    }
}