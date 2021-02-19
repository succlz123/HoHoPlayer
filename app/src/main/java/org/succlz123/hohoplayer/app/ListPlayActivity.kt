package org.succlz123.hohoplayer.app

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_list.*
import org.succlz123.hohoplayer.ui.list.ListPlayer
import org.succlz123.hohoplayer.app.support.DataProvider
import org.succlz123.hohoplayer.app.support.OrientationSensor

class RecyclerViewPlayActivity : AppCompatActivity() {
    private lateinit var listAdapter: ListAdapter

    private lateinit var orientationSensor: OrientationSensor

    private var isLandScape = false

    private var listPlayer = ListPlayer(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        listAdapter = ListAdapter(listPlayer, DataProvider.videoList)
        recycler.adapter = listAdapter

        listPlayer.bindLifecycleOwner(this)
        listPlayer.bindRecyclerView(recycler)

        orientationSensor = OrientationSensor(this, onOrientationListener)
        orientationSensor.enable()
    }

    private val onOrientationListener = object : OrientationSensor.OnOrientationListener {

        override fun onLandScape(orientation: Int) {
            if (listPlayer.isInPlaybackState()) {
                requestedOrientation = orientation
            }
        }

        override fun onPortrait(orientation: Int) {
            if (listPlayer.isInPlaybackState()) {
                requestedOrientation = orientation
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        isLandScape = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        } else {
        }
    }

    private fun toggleScreen() {
        requestedOrientation = if (isLandScape) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
    }

    override fun onStart() {
        super.onStart()
        orientationSensor.enable()
    }

    override fun onStop() {
        super.onStop()
        orientationSensor.disable()
    }

    override fun onBackPressed() {
        if (isLandScape) {
            toggleScreen()
            return
        }
        super.onBackPressed()
    }

    override fun onDestroy() {
        super.onDestroy()
        orientationSensor.disable()
    }
}