package org.succlz123.hohoplayer.app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import org.succlz123.hohoplayer.app.databinding.ActivityMainBinding
import org.succlz123.hohoplayer.config.PlayerConfig

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater, null, false)
        setContentView(binding.root)
        setSupportActionBar(findViewById(R.id.toolbar))
        updateDecoderInfo()
    }

    override fun onResume() {
        super.onResume()
        updateDecoderInfo()
    }

    private fun updateDecoderInfo() {
        binding.tvInfo.text = "Current Media Player: ${PlayerConfig.getDefaultDecoderName()}"
    }

    fun useBaseVideoView(view: View?) {
        startActivity(BaseVideoViewActivity::class.java)
    }

    fun recyclerviewListPlay(view: View?) {
        startActivity(RecyclerViewPlayActivity::class.java)
    }

    fun shareAnimationVideos(view: View?) {
    }

    fun viewPagerPlay(view: View?) {
    }

    fun useWindowVideoView(view: View?) {
    }

    fun useFloatWindow(view: View?) {
    }

    private fun startActivity(cls: Class<out Activity?>) {
        val intent = Intent(applicationContext, cls)
        startActivity(intent)
    }
}