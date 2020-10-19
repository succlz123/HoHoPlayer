package org.succlz123.hohoplayer.support.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Handler
import android.os.Looper
import android.os.Message
import org.succlz123.hohoplayer.core.producer.AbsProducer
import org.succlz123.hohoplayer.support.message.HoHoMessage
import org.succlz123.hohoplayer.support.log.PlayerLog.d
import java.lang.ref.WeakReference

class NetworkProducer(context: Context) : AbsProducer() {

    companion object {
        private const val TAG = "NetworkEventProducer"
        private const val MSG_CODE_NETWORK_CHANGE = 100

        var KEY_NETWORK_STATE = 100866
    }

    private val appContext = context.applicationContext

    private var broadcastReceiver: NetChangeBroadcastReceiver? = null

    private var state = 0

    private val handler: Handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                MSG_CODE_NETWORK_CHANGE -> {
                    val state = msg.obj as Int
                    if (this@NetworkProducer.state == state) {
                        return
                    }
                    this@NetworkProducer.state = state
                    sender?.sendEvent(HoHoMessage.obtain(what = KEY_NETWORK_STATE, argInt = this@NetworkProducer.state))
                    d(TAG, "onNetworkChange : ${this@NetworkProducer.state}")
                }
            }
        }
    }

    override fun onAdded() {
        state = NetworkUtils.getNetworkState(appContext)
        registerNetChangeReceiver()
    }

    override fun onRemoved() {
        destroy()
    }

    override fun destroy() {
        broadcastReceiver?.destroy()
        unregisterNetChangeReceiver()
        handler.removeMessages(MSG_CODE_NETWORK_CHANGE)
    }

    private fun registerNetChangeReceiver() {
        unregisterNetChangeReceiver()
        broadcastReceiver =
                NetChangeBroadcastReceiver(
                        appContext,
                        handler
                )
        val intentFilter = IntentFilter()
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        appContext.registerReceiver(broadcastReceiver, intentFilter)
    }

    private fun unregisterNetChangeReceiver() {
        try {
            if (broadcastReceiver != null) {
                appContext.unregisterReceiver(broadcastReceiver)
                broadcastReceiver = null
            }
        } catch (e: Exception) {
        }
    }

    class NetChangeBroadcastReceiver(context: Context?, private val handler: Handler) : BroadcastReceiver() {
        private val contextRefer = WeakReference(context)

        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (ConnectivityManager.CONNECTIVITY_ACTION == action) {
                handler.removeCallbacks(mDelayRunnable)
                handler.postDelayed(mDelayRunnable, 1000)
            }
        }

        private val mDelayRunnable = Runnable {
            contextRefer.get()?.let {
                val networkState = NetworkUtils.getNetworkState(it)
                val message = Message.obtain()
                message.what =
                        MSG_CODE_NETWORK_CHANGE
                message.obj = networkState
                handler.sendMessage(message)
            }
        }

        fun destroy() {
            handler.removeCallbacks(mDelayRunnable)
        }
    }
}
