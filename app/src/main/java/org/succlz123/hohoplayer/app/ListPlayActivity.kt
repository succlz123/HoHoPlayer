package org.succlz123.hohoplayer.app

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import org.succlz123.hohoplayer.app.databinding.ActivityListBinding
import org.succlz123.hohoplayer.ui.list.ListPlayer
import org.succlz123.hohoplayer.app.support.DataProvider
import org.succlz123.hohoplayer.app.support.OrientationSensor

class RecyclerViewPlayActivity : AppCompatActivity() {
    private lateinit var listAdapter: ListAdapter
    private lateinit var binding: ActivityListBinding
    private lateinit var orientationSensor: OrientationSensor

    private var isLandScape = false

    private var listPlayer = ListPlayer(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListBinding.inflate(layoutInflater, null, false)
        setContentView(binding.root)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        listAdapter = ListAdapter(listPlayer, DataProvider.videoList)
        binding.recycler.adapter = listAdapter

        listPlayer.bindLifecycleOwner(this)
        listPlayer.bindRecyclerView(binding.recycler)

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