package org.succlz123.hohoplayer.app.adpater

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import org.succlz123.hohoplayer.app.R
import org.succlz123.hohoplayer.app.support.AppPlayerData
import org.succlz123.hohoplayer.core.adapter.BaseCoverAdapter
import org.succlz123.hohoplayer.core.adapter.bridge.IBridge
import org.succlz123.hohoplayer.core.adapter.event.PlayerAdapterKtx.requestPause
import org.succlz123.hohoplayer.core.adapter.event.PlayerAdapterKtx.requestResume
import org.succlz123.hohoplayer.core.adapter.event.PlayerAdapterKtx.requestSeek
import org.succlz123.hohoplayer.core.player.base.IPlayer
import org.succlz123.hohoplayer.core.player.listener.OnPlayerEventListener
import org.succlz123.hohoplayer.core.player.time.OnTimerUpdateListener
import org.succlz123.hohoplayer.core.source.DataSource
import org.succlz123.hohoplayer.core.touch.OnTouchGestureListener
import org.succlz123.hohoplayer.support.log.PlayerLog.d
import org.succlz123.hohoplayer.support.message.HoHoMessage
import org.succlz123.hohoplayer.support.utils.PlayerTimeUtil

class ControllerCoverAdapter(context: Context) : BaseCoverAdapter(context), OnTimerUpdateListener, OnTouchGestureListener {

    companion object {
        const val TAG = "ControllerCoverAdapter"

        const val MSG_CODE_DELAY_HIDDEN_CONTROLLER = 101
    }

    private var topContainer: View? = null
    private var backIcon: ImageView? = null
    private var topTitle: TextView? = null

    private var bottomContainer: View? = null
    private var stateIcon: ImageView? = null
    private var currTime: TextView? = null
    private var totalTime: TextView? = null
    private var switchScreen: ImageView? = null
    private var controllerSeekBar: SeekBar? = null
    private var bottomSeekBar: SeekBar? = null

    private var bufferPercentage = 0

    private var seekProgress = -1

    private var timerUpdateProgressEnable = true

    private val controllerHandler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                MSG_CODE_DELAY_HIDDEN_CONTROLLER -> {
                    d(TAG, "msg_delay_hidden...")
                    setControllerState(false)
                }
            }
        }
    }

    private var isGestureEnable = true

    private var timeFormat: String? = null

    private var controllerTopEnable = true

    private val isControllerShow: Boolean
        get() = bottomContainer?.visibility == View.VISIBLE

    override val key: String
        get() = TAG

    public override fun onCreateCoverView(context: Context): View {
        return View.inflate(context, R.layout.layout_controller_cover, null)
    }

    override fun getCoverLevel(): Int {
        return levelLow(1)
    }

    override fun onAdapterBind() {
        super.onAdapterBind()
        getDataCenter().registerOnValueListener(onValueListener)
    }

    override fun onCoverAttachedToWindow() {
        super.onCoverAttachedToWindow()
        val view = getView()

        topContainer = view.findViewById(R.id.cover_player_controller_top_container)
        bottomContainer = view.findViewById(R.id.cover_player_controller_bottom_container)
        backIcon = view.findViewById(R.id.cover_player_controller_image_view_back_icon)
        topTitle = view.findViewById(R.id.cover_player_controller_text_view_video_title)
        stateIcon = view.findViewById(R.id.cover_player_controller_image_view_play_state)
        currTime = view.findViewById(R.id.cover_player_controller_text_view_curr_time)
        totalTime = view.findViewById(R.id.cover_player_controller_text_view_total_time)
        switchScreen = view.findViewById(R.id.cover_player_controller_image_view_switch_screen)
        controllerSeekBar = view.findViewById(R.id.cover_player_controller_seek_bar)
        bottomSeekBar = view.findViewById(R.id.cover_bottom_seek_bar)
        controllerSeekBar?.setOnSeekBarChangeListener(onSeekBarChangeListener)

        backIcon?.setOnClickListener {
            sendOne2ManyAdapterEvent(HoHoMessage.obtain(what = AppPlayerData.Event.EVENT_CODE_REQUEST_BACK))
        }
        stateIcon?.setOnClickListener {
            val selected = stateIcon?.isSelected ?: false
            if (selected) {
                requestResume()
            } else {
                requestPause()
            }
            stateIcon?.isSelected = !selected
        }
        switchScreen?.setOnClickListener {
            sendOne2ManyAdapterEvent(HoHoMessage.obtain(what = AppPlayerData.Event.EVENT_CODE_REQUEST_TOGGLE_SCREEN))
        }

        val dataSource = getDataCenter().get<DataSource>(AppPlayerData.Key.KEY_DATA_SOURCE)
        setTitle(dataSource)

        val topEnable = getDataCenter().getBoolean(AppPlayerData.Key.KEY_CONTROLLER_TOP_ENABLE, false)
        controllerTopEnable = topEnable
        if (!topEnable) {
            setTopContainerState(false)
        }
        val screenSwitchEnable = getDataCenter().getBoolean(AppPlayerData.Key.KEY_CONTROLLER_SCREEN_SWITCH_ENABLE, true)
        setScreenSwitchEnable(screenSwitchEnable)
    }

    override fun onCoverDetachedToWindow() {
        super.onCoverDetachedToWindow()
        topContainer?.visibility = View.GONE
        bottomContainer?.visibility = View.GONE
        removeDelayHiddenMessage()
    }

    override fun onAdapterUnBind() {
        super.onAdapterUnBind()
        cancelTopAnimation()
        cancelBottomAnimation()
        getDataCenter().unregisterOnValueListener(onValueListener)
        removeDelayHiddenMessage()
        controllerHandler.removeCallbacks(seekEventRunnable)
    }

    private val onValueListener: IBridge.OnValueListener = object : IBridge.OnValueListener {

        override fun keys(): Array<String> {
            return arrayOf(AppPlayerData.Key.KEY_COMPLETE_SHOW,
                    AppPlayerData.Key.KEY_TIMER_UPDATE_ENABLE,
                    AppPlayerData.Key.KEY_DATA_SOURCE,
                    AppPlayerData.Key.KEY_IS_LANDSCAPE,
                    AppPlayerData.Key.KEY_CONTROLLER_TOP_ENABLE)
        }

        override fun onValueUpdate(key: String, value: Any) {
            if (key == AppPlayerData.Key.KEY_COMPLETE_SHOW) {
                val show = value as Boolean
                if (show) {
                    setControllerState(false)
                }
                setGestureEnable(!show)
            } else if (key == AppPlayerData.Key.KEY_CONTROLLER_TOP_ENABLE) {
                controllerTopEnable = value as Boolean
                if (!controllerTopEnable) {
                    setTopContainerState(false)
                }
            } else if (key == AppPlayerData.Key.KEY_IS_LANDSCAPE) {
                setSwitchScreenIcon(value as Boolean)
            } else if (key == AppPlayerData.Key.KEY_TIMER_UPDATE_ENABLE) {
                timerUpdateProgressEnable = value as Boolean
            } else if (key == AppPlayerData.Key.KEY_DATA_SOURCE) {
                val dataSource = value as DataSource
                setTitle(dataSource)
            }
        }
    }

    private val onSeekBarChangeListener: OnSeekBarChangeListener = object : OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser) updateUI(progress, seekBar.max)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {}

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            sendSeekEvent(seekBar.progress)
        }
    }

    private fun sendSeekEvent(progress: Int) {
        timerUpdateProgressEnable = false
        seekProgress = progress
        controllerHandler.removeCallbacks(seekEventRunnable)
        controllerHandler.postDelayed(seekEventRunnable, 300)
    }

    private val seekEventRunnable = Runnable {
        if (seekProgress < 0) {
            return@Runnable
        }
        requestSeek(seekProgress)
    }

    private fun setTitle(dataSource: DataSource?) {
        if (dataSource != null) {
            val title = dataSource.title
            if (!TextUtils.isEmpty(title)) {
                setTitle(title)
                return
            }
            val data = dataSource.data
            if (!TextUtils.isEmpty(data)) {
                setTitle(data)
            }
        }
    }

    private fun setTitle(text: String?) {
        topTitle?.text = text
    }

    private fun setSwitchScreenIcon(isFullScreen: Boolean) {
        switchScreen?.setImageResource(if (isFullScreen) {
            R.mipmap.icon_exit_full_screen
        } else {
            R.mipmap.icon_full_screen
        })
    }

    private fun setScreenSwitchEnable(screenSwitchEnable: Boolean) {
        switchScreen?.visibility = if (screenSwitchEnable) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun setBottomSeekBarState(state: Boolean) {
        bottomSeekBar?.visibility = if (state) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun setGestureEnable(gestureEnable: Boolean) {
        this.isGestureEnable = gestureEnable
    }

    private fun setTopContainerState(state: Boolean) {
        if (controllerTopEnable) {
            val tc = topContainer ?: return
            cancelTopAnimation()
            val ap = if (state) {
                1f
            } else {
                0f
            }
            tc.animate().alpha(ap).setListener(object : AnimatorListenerAdapter() {

                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    if (state) {
                        topContainer?.visibility = View.VISIBLE
                    }
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    if (!state) {
                        topContainer?.visibility = View.GONE
                    }
                }
            }).start()
        } else {
            topContainer?.visibility = View.GONE
        }
    }

    private fun cancelTopAnimation() {
        val tc = topContainer ?: return
        tc.clearAnimation()
        tc.animate().cancel()
        tc.animate().setListener(null)
    }

    private fun setBottomContainerState(state: Boolean) {
        val bc = bottomContainer ?: return
        cancelBottomAnimation()
        val ap = if (state) {
            1f
        } else {
            0f
        }
        bc.animate().alpha(ap).setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                if (state) {
                    bottomContainer?.visibility = View.VISIBLE
                }
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                if (!state) {
                    bottomContainer?.visibility = View.GONE
                }
            }
        }).start()
        setBottomSeekBarState(!state)
    }

    private fun cancelBottomAnimation() {
        val bc = bottomContainer ?: return
        bc.clearAnimation()
        bc.animate().cancel()
        bc.animate().setListener(null)
    }

    private fun setControllerState(state: Boolean) {
        if (state) {
            sendDelayHiddenMessage()
        } else {
            removeDelayHiddenMessage()
        }
        setTopContainerState(state)
        setBottomContainerState(state)
    }

    private fun toggleController() {
        if (isControllerShow) {
            setControllerState(false)
        } else {
            setControllerState(true)
        }
    }

    private fun sendDelayHiddenMessage() {
        removeDelayHiddenMessage()
        controllerHandler.sendEmptyMessageDelayed(MSG_CODE_DELAY_HIDDEN_CONTROLLER, 5000)
    }

    private fun removeDelayHiddenMessage() {
        controllerHandler.removeMessages(MSG_CODE_DELAY_HIDDEN_CONTROLLER)
    }

    private fun setCurrTime(curr: Int) {
        currTime?.text = PlayerTimeUtil.getTime(timeFormat, curr.toLong())
    }

    private fun setTotalTime(duration: Int) {
        totalTime?.text = PlayerTimeUtil.getTime(timeFormat, duration.toLong())
    }

    private fun setSeekProgress(curr: Int, duration: Int) {
        controllerSeekBar?.max = duration
        controllerSeekBar?.progress = curr
        val secondProgress = bufferPercentage * 1.0f / 100 * duration
        setSecondProgress(secondProgress.toInt())
    }

    private fun setSecondProgress(secondProgress: Int) {
        controllerSeekBar?.secondaryProgress = secondProgress
    }

    private fun setBottomSeekProgress(curr: Int, duration: Int) {
        bottomSeekBar?.max = duration
        bottomSeekBar?.progress = curr
        val secondProgress = bufferPercentage * 1.0f / 100 * duration
        bottomSeekBar?.secondaryProgress = secondProgress.toInt()
    }

    override fun onTimerUpdate(curr: Int, duration: Int, bufferPercentage: Int) {
        if (!timerUpdateProgressEnable) return
        if (timeFormat == null || duration != controllerSeekBar?.max) {
            timeFormat = PlayerTimeUtil.getFormat(duration.toLong())
        }
        this.bufferPercentage = bufferPercentage
        updateUI(curr, duration)
    }

    private fun updateUI(curr: Int, duration: Int) {
        setSeekProgress(curr, duration)
        setBottomSeekProgress(curr, duration)
        setCurrTime(curr)
        setTotalTime(duration)
    }

    override fun onPlayerEvent(message: HoHoMessage) {
        when (message.what) {
            OnPlayerEventListener.PLAYER_EVENT_ON_DATA_SOURCE_SET -> {
                bufferPercentage = 0
                timeFormat = null
                updateUI(0, 0)
                setBottomSeekBarState(true)
                val data = message.argObj as? DataSource ?: return
                getDataCenter().putObject(AppPlayerData.Key.KEY_DATA_SOURCE, data)
                setTitle(data)
            }
            OnPlayerEventListener.PLAYER_EVENT_ON_STATUS_CHANGE -> {
                val status = message.argInt
                if (status == IPlayer.STATE_PAUSED) {
                    stateIcon?.isSelected = true
                } else if (status == IPlayer.STATE_STARTED) {
                    stateIcon?.isSelected = false
                }
            }
            OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_RENDER_START, OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_COMPLETE -> timerUpdateProgressEnable = true
        }
    }

    override fun receiveOne2OneAdapterEvent(message: HoHoMessage): HoHoMessage? {
        when (message.what) {
            AppPlayerData.PrivateEvent.EVENT_CODE_UPDATE_SEEK -> {
                val curr = message.getIntFromExtra("newPosition", 0)
                val duration = message.getIntFromExtra("duration", 0)
                updateUI(curr, duration)
            }
        }
        return null
    }

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        if (!isGestureEnable) {
            return false
        }
        toggleController()
        return false
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        return isGestureEnable
    }
}