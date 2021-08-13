package org.succlz123.hohoplayer.app.adpater

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.TextView
import org.succlz123.hohoplayer.app.MainApplication
import org.succlz123.hohoplayer.app.R
import org.succlz123.hohoplayer.app.support.AppPlayerData
import org.succlz123.hohoplayer.core.adapter.BaseCoverAdapter
import org.succlz123.hohoplayer.core.adapter.event.PlayerAdapterKtx.requestResume
import org.succlz123.hohoplayer.core.adapter.event.PlayerAdapterKtx.requestRetry
import org.succlz123.hohoplayer.core.player.listener.OnPlayerEventListener
import org.succlz123.hohoplayer.support.message.HoHoMessage
import org.succlz123.hohoplayer.support.network.NetworkConst
import org.succlz123.hohoplayer.support.network.NetworkProducer.Companion.KEY_NETWORK_STATE
import org.succlz123.hohoplayer.support.network.NetworkUtils

class ErrorCover(val context: Activity) : BaseCoverAdapter(context) {

    companion object {
        const val STATUS_ERROR = -1
        const val STATUS_UNDEFINE = 0
        const val STATUS_MOBILE = 1
        const val STATUS_NETWORK_ERROR = 2
    }

    private var mStatus = STATUS_UNDEFINE

    var mInfo: TextView? = null
    var mRetry: TextView? = null

    private var mErrorShow = false
    private var mCurrPosition = 0

    override val key: String
        get() = "ErrorCover"

    override fun onCreateCoverView(context: Context): View {
        return View.inflate(context, R.layout.layout_error_cover, null)
    }

    override fun getCoverLevel(): Int {
        return levelHigh(0)
    }

    override fun onCoverAttachedToWindow() {
        super.onCoverAttachedToWindow()
        handleStatusUI(NetworkUtils.getNetworkState(context))
    }

    override fun onAdapterBind() {
        super.onAdapterBind()
        mInfo = getView().findViewById(R.id.tv_error_info)
        mRetry = getView().findViewById(R.id.tv_retry)
        mRetry?.setOnClickListener {
            handleStatus()
        }
    }

    private fun handleStatus() {
        when (mStatus) {
            STATUS_ERROR, STATUS_NETWORK_ERROR -> {
                setErrorState(false)
                requestRetry(mCurrPosition)
            }
            STATUS_MOBILE -> {
                MainApplication.ignoreMobile = true
                setErrorState(false)
                requestResume()
            }
        }
    }

    override fun receiveProducerEvent(message: HoHoMessage) {
        super.receiveProducerEvent(message)
        if (KEY_NETWORK_STATE == message.what) {
            val networkState = message.argInt
            if (networkState == NetworkConst.NETWORK_STATE_WIFI && mErrorShow) {
                requestRetry(mCurrPosition)
            }
            handleStatusUI(networkState)
        }
    }

    private fun handleStatusUI(networkState: Int) {
        if (!getDataCenter().getBoolean(AppPlayerData.Key.KEY_NETWORK_RESOURCE, true)) {
            return
        }
        if (networkState < 0) {
            mStatus = STATUS_NETWORK_ERROR
            setErrorInfo("无网络！")
            setHandleInfo("重试")
            setErrorState(true)
        } else {
            if (networkState == NetworkConst.NETWORK_STATE_WIFI) {
                if (mErrorShow) {
                    setErrorState(false)
                }
            } else {
                if (MainApplication.ignoreMobile) return
                mStatus = STATUS_MOBILE
                setErrorInfo("您正在使用移动网络！")
                setHandleInfo("继续")
                setErrorState(true)
            }
        }
    }

    private fun setErrorInfo(text: String) {
        mInfo!!.text = text
    }

    private fun setHandleInfo(text: String) {
        mRetry!!.text = text
    }

    private fun setErrorState(state: Boolean) {
        mErrorShow = state
        setCoverVisibility(if (state) View.VISIBLE else View.GONE)
        if (!state) {
            mStatus = STATUS_UNDEFINE
        } else {
            sendOne2ManyAdapterEvent(HoHoMessage.obtain(what = AppPlayerData.Event.EVENT_CODE_ERROR_SHOW))
        }
        getDataCenter().putObject(AppPlayerData.Key.KEY_ERROR_SHOW, state)
    }

    override fun onPlayerEvent(message: HoHoMessage) {
        when (message.what) {
            OnPlayerEventListener.PLAYER_EVENT_ON_DATA_SOURCE_SET -> {
                mCurrPosition = 0
                handleStatusUI(NetworkUtils.getNetworkState(context))
            }
            OnPlayerEventListener.PLAYER_EVENT_ON_TIMER_UPDATE -> {
                mCurrPosition = message.getIntFromExtra("current", 0)
            }
        }
    }

    override fun onErrorEvent(message: HoHoMessage) {
        mStatus = STATUS_ERROR
        if (!mErrorShow) {
            setErrorInfo("出错了！")
            setHandleInfo("重试")
            setErrorState(true)
        }
    }
}
