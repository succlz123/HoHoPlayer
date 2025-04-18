package org.succlz123.hohoplayer.app

import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.succlz123.hohoplayer.app.adpater.CompleteCoverAdapter
import org.succlz123.hohoplayer.app.adpater.ControllerCoverAdapter
import org.succlz123.hohoplayer.app.adpater.ErrorCover
import org.succlz123.hohoplayer.app.adpater.LoadingCoverAdapter
import org.succlz123.hohoplayer.app.databinding.ActivityBaseVideoViewBinding
import org.succlz123.hohoplayer.app.support.AppPlayerData
import org.succlz123.hohoplayer.app.support.DataProvider
import org.succlz123.hohoplayer.app.support.OnItemClickListener
import org.succlz123.hohoplayer.app.support.PlayerKtx.dp2px
import org.succlz123.hohoplayer.app.support.PlayerKtx.isTopActivity
import org.succlz123.hohoplayer.app.support.PlayerKtx.screenWidth
import org.succlz123.hohoplayer.core.adapter.bridge.Bridge
import org.succlz123.hohoplayer.core.adapter.event.PlayerAdapterKtx
import org.succlz123.hohoplayer.core.player.SysMediaPlayer
import org.succlz123.hohoplayer.core.player.base.IPlayer
import org.succlz123.hohoplayer.core.player.ext.exo.ExoMediaPlayer
import org.succlz123.hohoplayer.core.player.listener.OnPlayerEventListener
import org.succlz123.hohoplayer.core.render.AspectRatio
import org.succlz123.hohoplayer.core.render.IRender
import org.succlz123.hohoplayer.core.source.DataSource
import org.succlz123.hohoplayer.support.message.HoHoMessage
import org.succlz123.hohoplayer.widget.videoview.VideoView
import org.succlz123.hohoplayer.widget.videoview.VideoViewAdapterEventHandler

class BaseVideoViewActivity : AppCompatActivity(), OnItemClickListener<SettingAdapter.SettingItemHolder, SettingItem> {
    private lateinit var binding: ActivityBaseVideoViewBinding
    private lateinit var bridge: Bridge
    private var userPause = false
    private var isLandscape = false
    private var hasStart = false

    private var adapter: SettingAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBaseVideoViewBinding.inflate(layoutInflater, null, false)
        setContentView(binding.root)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        updateVideo(false)

        bridge = Bridge().apply {
            addAdapter(LoadingCoverAdapter(this@BaseVideoViewActivity))
            addAdapter(ControllerCoverAdapter(this@BaseVideoViewActivity))
            addAdapter(CompleteCoverAdapter(this@BaseVideoViewActivity))
            addAdapter(ErrorCover(this@BaseVideoViewActivity))
        }

        bridge.getDataCenter().putObject(AppPlayerData.Key.KEY_CONTROLLER_TOP_ENABLE, true)

        binding.baseVideoView.setBridge(bridge)
        binding.baseVideoView.videoViewEventHandler = videoViewEventHandler
        binding.baseVideoView.onPlayerEventListener = playerEventListener
    }

    private fun initPlayAfterResume() {
        if (!hasStart) {
            val dataSource = DataSource(DataProvider.VIDEO_URL_08)
            dataSource.title = "音乐和艺术如何改变世界"
            binding.baseVideoView.setDataSource(dataSource)
            binding.baseVideoView.start()
            hasStart = true
        }
    }

    private val playerEventListener = object : OnPlayerEventListener {
        override fun onPlayerEvent(message: HoHoMessage) {
            when (message.what) {
                OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_RENDER_START -> if (adapter == null) {
                    binding.settingRecycler.layoutManager = LinearLayoutManager(this@BaseVideoViewActivity, LinearLayoutManager.VERTICAL, false)
                    adapter = SettingAdapter(this@BaseVideoViewActivity, SettingItem.initSettingList()).apply {
                        onItemClickListener = this@BaseVideoViewActivity
                        binding.settingRecycler.adapter = this
                    }
                }
            }
        }
    }

    private val videoViewEventHandler = object : VideoViewAdapterEventHandler() {

        override fun onHandle(t: VideoView, message: HoHoMessage) {
            super.onHandle(t, message)
            when (message.what) {
                PlayerAdapterKtx.CODE_REQUEST_PAUSE -> userPause = true
                AppPlayerData.Event.EVENT_CODE_REQUEST_BACK -> if (isLandscape) {
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    finish()
                }
                AppPlayerData.Event.EVENT_CODE_REQUEST_TOGGLE_SCREEN -> {
                    requestedOrientation = if (isLandscape) {
                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    } else {
                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                    }
                }
                AppPlayerData.Event.EVENT_CODE_ERROR_SHOW -> {
                    binding.baseVideoView.stop()
                }
            }
        }

        override fun requestRetry(t: VideoView, message: HoHoMessage) {
            if (isTopActivity()) {
                super.requestRetry(t, message)
            }
        }
    }

    private fun replay() {
        binding.baseVideoView.setDataSource(DataSource(DataProvider.VIDEO_URL_07))
        binding.baseVideoView.start()
    }

    override fun onItemClick(holder: SettingAdapter.SettingItemHolder, item: SettingItem, position: Int) {
        when (item.code) {
            SettingItem.CODE_RENDER_SURFACE_VIEW -> {
                binding.baseVideoView.setRenderType(IRender.RENDER_TYPE_SURFACE_VIEW, false)
            }
            SettingItem.CODE_RENDER_TEXTURE_VIEW -> {
                binding.baseVideoView.setRenderType(IRender.RENDER_TYPE_TEXTURE_VIEW, false)
            }
            SettingItem.CODE_STYLE_ROUND_RECT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.baseVideoView.setRoundRectShape(dp2px(25f).toFloat())
            } else {
                Toast.makeText(this, "not support", Toast.LENGTH_SHORT).show()
            }
            SettingItem.CODE_STYLE_OVAL_RECT -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.baseVideoView.setOvalRectShape()
            } else {
                Toast.makeText(this, "not support", Toast.LENGTH_SHORT).show()
            }
            SettingItem.CODE_STYLE_RESET -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                binding.baseVideoView.clearShapeStyle()
            } else {
                Toast.makeText(this, "not support", Toast.LENGTH_SHORT).show()
            }
            SettingItem.CODE_ASPECT_16_9 -> {
                binding.baseVideoView.setAspectRatio(AspectRatio.AspectRatio_16_9)
            }
            SettingItem.CODE_ASPECT_4_3 -> {
                binding.baseVideoView.setAspectRatio(AspectRatio.AspectRatio_4_3)
            }
            SettingItem.CODE_ASPECT_FILL -> {
                binding.baseVideoView.setAspectRatio(AspectRatio.AspectRatio_FILL_PARENT)
            }
            SettingItem.CODE_ASPECT_MATCH -> {
                binding.baseVideoView.setAspectRatio(AspectRatio.AspectRatio_MATCH_PARENT)
            }
            SettingItem.CODE_ASPECT_FIT -> {
                binding.baseVideoView.setAspectRatio(AspectRatio.AspectRatio_FIT_PARENT)
            }
            SettingItem.CODE_ASPECT_ORIGIN -> {
                binding.baseVideoView.setAspectRatio(AspectRatio.AspectRatio_ORIGIN)
            }
            SettingItem.CODE_PLAYER_MEDIA_PLAYER -> if (binding.baseVideoView.switchDecoder(SysMediaPlayer.TAG)) {
                replay()
            }
            SettingItem.CODE_PLAYER_EXO_PLAYER -> if (binding.baseVideoView.switchDecoder(ExoMediaPlayer.TAG)) {
                replay()
            }
            SettingItem.CODE_SPEED_0_5 -> {
                binding.baseVideoView.setSpeed(0.5f)
            }
            SettingItem.CODE_SPEED_2 -> {
                binding.baseVideoView.setSpeed(2f)
            }
            SettingItem.CODE_SPEED_1 -> {
                binding.baseVideoView.setSpeed(1f)
            }
            SettingItem.CODE_VOLUME_SILENT -> {
                binding.baseVideoView.setVolume(0f, 0f)
            }
            SettingItem.CODE_VOLUME_RESET -> {
                binding.baseVideoView.setVolume(1f, 1f)
            }
            SettingItem.CODE_CONTROLLER_REMOVE -> {
                bridge.removeAdapter(ControllerCoverAdapter.TAG)
                Toast.makeText(this, "已移除", Toast.LENGTH_SHORT).show()
            }
            SettingItem.CODE_CONTROLLER_RESET -> {
                val receiver = bridge.getAdapter(ControllerCoverAdapter.TAG)
                if (receiver == null) {
                    bridge.addAdapter(ControllerCoverAdapter(this))
                    Toast.makeText(this, "已添加", Toast.LENGTH_SHORT).show()
                }
            }
            SettingItem.CODE_TEST_UPDATE_RENDER -> binding.baseVideoView.refreshRender()
        }
    }

    private fun updateVideo(landscape: Boolean) {
        val margin = dp2px(2f)
        val layoutParams = binding.baseVideoView.layoutParams as LinearLayout.LayoutParams
        if (landscape) {
            layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            layoutParams.setMargins(0, 0, 0, 0)
        } else {
            layoutParams.width = screenWidth() - margin * 2
            layoutParams.height = layoutParams.width * 3 / 4
            layoutParams.setMargins(margin, margin, margin, margin)
        }
        binding.baseVideoView.layoutParams = layoutParams
    }

    override fun onBackPressed() {
        if (isLandscape) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            return
        }
        super.onBackPressed()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            isLandscape = true
            updateVideo(true)
        } else {
            isLandscape = false
            updateVideo(false)
        }
        bridge.getDataCenter().putObject(AppPlayerData.Key.KEY_IS_LANDSCAPE, isLandscape)
    }

    override fun onPause() {
        super.onPause()
        val state = binding.baseVideoView.getState()
        if (state == IPlayer.STATE_PLAYBACK_COMPLETE) {
            return
        }
        if (binding.baseVideoView.isInPlaybackState()) {
            binding.baseVideoView.pause()
        } else {
            binding.baseVideoView.stop()
        }
    }

    override fun onResume() {
        super.onResume()
        val state = binding.baseVideoView.getState()
        if (state == IPlayer.STATE_PLAYBACK_COMPLETE) {
            return
        }
        if (binding.baseVideoView.isInPlaybackState()) {
            if (!userPause) binding.baseVideoView.resume()
        } else {
            binding.baseVideoView.rePlay(0)
        }
        initPlayAfterResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.baseVideoView.stopPlayback()
    }
}

class SettingAdapter(private val context: Context, private val items: List<SettingItem>?) : RecyclerView.Adapter<SettingAdapter.SettingItemHolder>() {
    var onItemClickListener: OnItemClickListener<SettingItemHolder, SettingItem>? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingItemHolder {
        return SettingItemHolder(View.inflate(context, R.layout.item_setting, null))
    }

    override fun onBindViewHolder(holder: SettingItemHolder, position: Int) {
        items ?: return
        val item = items[position]
        holder.itemView.setBackgroundColor(if (item.code / 100 % 2 == 0) {
            Color.WHITE
        } else {
            Color.parseColor("#EEEEEE")
        })
        holder.settingText.text = item.itemText
        holder.itemView.setOnClickListener { onItemClickListener?.onItemClick(holder, item, position) }
    }

    override fun getItemCount(): Int {
        return items?.size ?: 0
    }

    class SettingItemHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var settingText: TextView = itemView.findViewById(R.id.settingText)
    }
}

class SettingItem(var itemText: String, var code: Int) {

    companion object {
        const val CODE_RENDER_SURFACE_VIEW = 100
        const val CODE_RENDER_TEXTURE_VIEW = 101
        const val CODE_STYLE_ROUND_RECT = 200
        const val CODE_STYLE_OVAL_RECT = 201
        const val CODE_STYLE_RESET = 202
        const val CODE_ASPECT_16_9 = 300
        const val CODE_ASPECT_4_3 = 301
        const val CODE_ASPECT_FILL = 302
        const val CODE_ASPECT_MATCH = 303
        const val CODE_ASPECT_FIT = 304
        const val CODE_ASPECT_ORIGIN = 305
        const val CODE_PLAYER_MEDIA_PLAYER = 400
        const val CODE_PLAYER_IJK_PLAYER = 401
        const val CODE_PLAYER_EXO_PLAYER = 402
        const val CODE_SPEED_0_5 = 500
        const val CODE_SPEED_2 = 501
        const val CODE_SPEED_1 = 502
        const val CODE_VOLUME_SILENT = 600
        const val CODE_VOLUME_RESET = 601
        const val CODE_CONTROLLER_REMOVE = 700
        const val CODE_CONTROLLER_RESET = 701
        const val CODE_TEST_UPDATE_RENDER = 801

        fun initSettingList(): List<SettingItem> {
            val items: MutableList<SettingItem> = ArrayList()
            items.add(SettingItem("SurfaceView Render", CODE_RENDER_SURFACE_VIEW))
            items.add(SettingItem("TextureView Render", CODE_RENDER_TEXTURE_VIEW))
            items.add(SettingItem("Style Round Rect", CODE_STYLE_ROUND_RECT))
            items.add(SettingItem("Style Oval Rect", CODE_STYLE_OVAL_RECT))
            items.add(SettingItem("Style Rest", CODE_STYLE_RESET))
            items.add(SettingItem("Aspect 16:9", CODE_ASPECT_16_9))
            items.add(SettingItem("Aspect 4:3", CODE_ASPECT_4_3))
            items.add(SettingItem("Aspect FILL", CODE_ASPECT_FILL))
            items.add(SettingItem("Aspect MATCH", CODE_ASPECT_MATCH))
            items.add(SettingItem("Aspect FIT", CODE_ASPECT_FIT))
            items.add(SettingItem("Aspect ORIGIN", CODE_ASPECT_ORIGIN))
            items.add(SettingItem("MediaPlayer", CODE_PLAYER_MEDIA_PLAYER))
            items.add(SettingItem("IjkPlayer", CODE_PLAYER_IJK_PLAYER))
            items.add(SettingItem("ExoPlayer", CODE_PLAYER_EXO_PLAYER))
            items.add(SettingItem("Speed 0.5x", CODE_SPEED_0_5))
            items.add(SettingItem("Speed 2x", CODE_SPEED_2))
            items.add(SettingItem("Speed 1x", CODE_SPEED_1))
            items.add(SettingItem("Mute", CODE_VOLUME_SILENT))
            items.add(SettingItem("Unmute", CODE_VOLUME_RESET))
            items.add(SettingItem("Remove Controller Component", CODE_CONTROLLER_REMOVE))
            items.add(SettingItem("Add Controller Component", CODE_CONTROLLER_RESET))
            items.add(SettingItem("Refresh Render", CODE_TEST_UPDATE_RENDER))
            return items
        }
    }
}
