package org.succlz123.hohoplayer.app.adpater

import android.content.Context
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import android.widget.TextView
import org.succlz123.hohoplayer.app.R
import org.succlz123.hohoplayer.app.support.AppPlayerData
import org.succlz123.hohoplayer.app.support.PlayerKtx.activity
import org.succlz123.hohoplayer.core.adapter.BaseCoverAdapter
import org.succlz123.hohoplayer.core.adapter.bridge.IBridge
import org.succlz123.hohoplayer.core.adapter.event.PlayerAdapterKtx.requestSeek
import org.succlz123.hohoplayer.core.player.listener.OnPlayerEventListener
import org.succlz123.hohoplayer.core.touch.OnTouchGestureListener
import org.succlz123.hohoplayer.support.message.HoHoMessage
import org.succlz123.hohoplayer.support.utils.PlayerTimeUtil
import kotlin.math.abs

class GestureCoverAdapter(val context: Context) : BaseCoverAdapter(context), OnTouchGestureListener {
    private var volumeBox: View? = null
    private var volumeIcon: ImageView? = null
    private var volumeText: TextView? = null

    private var brightnessBox: View? = null
    private var brightnessText: TextView? = null

    private var fastForwardBox: View? = null
    private var fastForwardStepTime: TextView? = null
    private var fastForwardProgressTime: TextView? = null

    private var firstTouch = false

    private var seekProgress = -1

    private var width = 0

    private var height = 0

    private var newPosition: Long = 0

    private var mHorizontalSlide = false

    private var brightness = -1f

    private var volume = 0

    private var maxVolume = 0

    private var audioManager: AudioManager? = null

    private var isGestureEnable = true

    private var gestureMessage = HoHoMessage.obtain(what = AppPlayerData.PrivateEvent.EVENT_CODE_UPDATE_SEEK)

    private val gestureHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
            }
        }
    }

    private var horizontalSlide = false

    private var rightVerticalSlide = false

    override val key: String
        get() = "GestureCoverAdapter"

    override fun onAdapterBind() {
        super.onAdapterBind()
        volumeBox = getView().findViewById(R.id.cover_player_gesture_operation_volume_box)
        brightnessBox = getView().findViewById(R.id.cover_player_gesture_operation_brightness_box)
        volumeIcon = getView().findViewById(R.id.cover_player_gesture_operation_volume_icon)
        volumeText = getView().findViewById(R.id.cover_player_gesture_operation_volume_text)
        brightnessText = getView().findViewById(R.id.cover_player_gesture_operation_brightness_text)
        fastForwardBox = getView().findViewById(R.id.cover_player_gesture_operation_fast_forward_box)
        fastForwardStepTime = getView().findViewById(R.id.cover_player_gesture_operation_fast_forward_text_view_step_time)
        fastForwardProgressTime = getView().findViewById(R.id.cover_player_gesture_operation_fast_forward_text_view_progress_time)

        initAudioManager(context)
    }

    private fun initAudioManager(context: Context) {
        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        maxVolume = audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 0
    }

    private fun sendSeekEvent(progress: Int) {
        getDataCenter().putObject(AppPlayerData.Key.KEY_TIMER_UPDATE_ENABLE, false)
        seekProgress = progress
        gestureHandler.removeCallbacks(seekEventRunnable)
        gestureHandler.postDelayed(seekEventRunnable, 300)
    }

    private val seekEventRunnable = Runnable {
        if (seekProgress < 0) {
            return@Runnable
        }
        requestSeek(seekProgress)
    }

    private val onValueListener: IBridge.OnValueListener = object : IBridge.OnValueListener {
        override fun keys(): Array<String> {
            return arrayOf(AppPlayerData.Key.KEY_COMPLETE_SHOW, AppPlayerData.Key.KEY_IS_LANDSCAPE)
        }

        override fun onValueUpdate(key: String, value: Any) {
            if (AppPlayerData.Key.KEY_COMPLETE_SHOW == key) {
                setGestureEnable(value as Boolean)
            } else if (AppPlayerData.Key.KEY_IS_LANDSCAPE == key) {
                notifyWH()
            }
        }
    }

    override fun onCoverAttachedToWindow() {
        super.onCoverAttachedToWindow()
        getDataCenter().registerOnValueListener(onValueListener)
        notifyWH()
    }

    private fun notifyWH() {
        getView().viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                width = getView().width
                height = getView().height
                getView().viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    override fun onCoverDetachedToWindow() {
        super.onCoverDetachedToWindow()
        getDataCenter().unregisterOnValueListener(onValueListener)
    }

    private fun setVolumeBoxState(state: Boolean) {
        volumeBox?.visibility = if (state) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun setVolumeIcon(resId: Int) {
        volumeIcon?.setImageResource(resId)
    }

    private fun setVolumeText(text: String?) {
        volumeText?.text = text
    }

    private fun setBrightnessBoxState(state: Boolean) {
        brightnessBox?.visibility = if (state) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun setBrightnessText(text: String?) {
        brightnessText?.text = text
    }

    private fun setFastForwardState(state: Boolean) {
        fastForwardBox?.visibility = if (state) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun setFastForwardStepTime(text: String) {
        fastForwardStepTime?.text = text
    }

    private fun setFastForwardProgressTime(text: String) {
        fastForwardProgressTime?.text = text
    }

    fun setGestureEnable(gestureEnable: Boolean) {
        isGestureEnable = gestureEnable
    }

    public override fun onCreateCoverView(context: Context): View {
        return View.inflate(context, R.layout.layout_gesture_cover, null)
    }

    override fun getCoverLevel(): Int {
        return levelLow(0)
    }

    override fun onPlayerEvent(message: HoHoMessage) {
        when (message.what) {
            OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_RENDER_START -> {
                setGestureEnable(true)
            }
        }
    }

    override fun onDown(event: MotionEvent?): Boolean {
        mHorizontalSlide = false
        firstTouch = true
        volume = getVolume()
        return false
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        if (!isGestureEnable) {
            return false
        }
        val mOldX = e1.x
        val mOldY = e1.y
        val deltaY = mOldY - e2.y
        val deltaX = mOldX - e2.x
        if (firstTouch) {
            horizontalSlide = Math.abs(distanceX) >= Math.abs(distanceY)
            rightVerticalSlide = mOldX > width * 0.5f
            firstTouch = false
        }
        if (horizontalSlide) {
            onHorizontalSlide(-deltaX / width)
        } else {
            if (abs(deltaY) > height) {
                return false
            }
            if (rightVerticalSlide) {
                onRightVerticalSlide(deltaY / height)
            } else {
                onLeftVerticalSlide(deltaY / height)
            }
        }
        return false
    }

    private val duration: Int
        private get() {
//            return videoViewState?.getDuration() ?: 0
            return 0
        }

    private val currentPosition: Int
        private get() {
//            return videoViewState?.getCurrentPosition() ?: 0
            return 0
        }

    private fun onHorizontalSlide(percent: Float) {
        if (duration <= 0) {
            return
        }
        mHorizontalSlide = true
        if (getDataCenter().getBoolean(AppPlayerData.Key.KEY_TIMER_UPDATE_ENABLE)) {
            getDataCenter().putObject(AppPlayerData.Key.KEY_TIMER_UPDATE_ENABLE, false)
        }
        val position = currentPosition.toLong()
        val duration = duration.toLong()
        val deltaMax = Math.min(duration / 2.toLong(), duration - position)
        var delta = (deltaMax * percent).toLong()
        newPosition = delta + position
        if (newPosition > duration) {
            newPosition = duration
        } else if (newPosition <= 0) {
            newPosition = 0
            delta = -position
        }
        val showDelta = delta.toInt() / 1000
        if (showDelta != 0) {
            gestureMessage.getDataNoNone().apply {
                put("newPosition", newPosition.toInt())
                put("duration", duration.toInt())
            }
            sendOne2OneAdapterEvent(ControllerCoverAdapter.TAG, gestureMessage)
            setFastForwardState(true)
            val text = if (showDelta > 0) {
                "+$showDelta"
            } else {
                "" + showDelta
            }
            setFastForwardStepTime(text + "s")
            val progressText = PlayerTimeUtil.getTimeSmartFormat(newPosition) + "/" + PlayerTimeUtil.getTimeSmartFormat(duration)
            setFastForwardProgressTime(progressText)
        }
    }

    private fun onRightVerticalSlide(percent: Float) {
        mHorizontalSlide = false
        var index = (percent * maxVolume).toInt() + volume
        if (index > maxVolume) {
            index = maxVolume
        } else if (index < 0) {
            index = 0
        }
        // 变更声音
        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0)
        // 变更进度条
        val i = (index * 1.0 / maxVolume * 100).toInt()
        var s = "$i%"
        if (i == 0) {
            s = "OFF"
        }
        // 显示
        setVolumeIcon(if (i == 0) {
            R.mipmap.ic_volume_off_white
        } else {
            R.mipmap.ic_volume_up_white
        })
        setBrightnessBoxState(false)
        setFastForwardState(false)
        setVolumeBoxState(true)
        setVolumeText(s)
    }

    private fun onLeftVerticalSlide(percent: Float) {
        mHorizontalSlide = false
        val activity = context.activity() ?: return
        if (brightness < 0) {
            brightness = activity.window.attributes.screenBrightness
            if (brightness <= 0.00f) {
                brightness = 0.50f
            } else if (brightness < 0.01f) {
                brightness = 0.01f
            }
        }
        setVolumeBoxState(false)
        setFastForwardState(false)
        setBrightnessBoxState(true)
        val lpa = activity.window.attributes
        lpa.screenBrightness = brightness + percent
        if (lpa.screenBrightness > 1.0f) {
            lpa.screenBrightness = 1.0f
        } else if (lpa.screenBrightness < 0.01f) {
            lpa.screenBrightness = 0.01f
        }
        setBrightnessText((lpa.screenBrightness * 100).toInt().toString() + "%")
        activity.window.attributes = lpa
    }

    private fun getVolume(): Int {
        volume = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        if (volume < 0) {
            volume = 0
        }
        return volume
    }

    override fun onEndGesture(event: MotionEvent?) {
        volume = -1
        brightness = -1f
        setVolumeBoxState(false)
        setBrightnessBoxState(false)
        setFastForwardState(false)
        if (newPosition >= 0 && mHorizontalSlide) {
            sendSeekEvent(newPosition.toInt())
            newPosition = 0
        } else {
            getDataCenter().putObject(AppPlayerData.Key.KEY_TIMER_UPDATE_ENABLE, true)
        }
        mHorizontalSlide = false
    }
}