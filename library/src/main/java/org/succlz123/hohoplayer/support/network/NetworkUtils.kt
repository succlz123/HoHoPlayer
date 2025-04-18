package org.succlz123.hohoplayer.support.network

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import org.succlz123.hohoplayer.support.network.NetworkConst.NETWORK_STATE_2G
import org.succlz123.hohoplayer.support.network.NetworkConst.NETWORK_STATE_3G
import org.succlz123.hohoplayer.support.network.NetworkConst.NETWORK_STATE_4G
import org.succlz123.hohoplayer.support.network.NetworkConst.NETWORK_STATE_CONNECTING
import org.succlz123.hohoplayer.support.network.NetworkConst.NETWORK_STATE_MOBILE_UNKNOWN
import org.succlz123.hohoplayer.support.network.NetworkConst.NETWORK_STATE_NONE
import org.succlz123.hohoplayer.support.network.NetworkConst.NETWORK_STATE_WIFI

object NetworkUtils {

    fun getNetworkState(context: Activity): Int {
        val connManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return NETWORK_STATE_NONE
        val networkInfo = connManager.activeNetworkInfo
        // none or connecting
        if (networkInfo == null) {
            return NETWORK_STATE_NONE
        } else {
            val networkInfoState = networkInfo.state
            if (networkInfoState == NetworkInfo.State.CONNECTING) {
                return NETWORK_STATE_CONNECTING
            }
            if (!networkInfo.isAvailable) {
                return NETWORK_STATE_NONE
            }
        }
        // wifi
        val wifiInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)
        if (wifiInfo != null) {
            val state = wifiInfo.state
            if (null != state) {
                if (state == NetworkInfo.State.CONNECTED || state == NetworkInfo.State.CONNECTING) {
                    return NETWORK_STATE_WIFI
                }
            }
        }
        // mobile, bluetooth or ethernet
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        return if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            val caps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                connManager.getNetworkCapabilities(connManager.activeNetwork)
            } else {
                null
            }
            val isMobile = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            val isBluetooth = caps?.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)
            val isEthernet = caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            when {
                isMobile == true -> {
                    NETWORK_STATE_MOBILE_UNKNOWN
                }
                isEthernet == true || isBluetooth == true -> {
                    NETWORK_STATE_WIFI
                }
                else -> {
                    // haha, I don't know - Manifest.permission.READ_PHONE_STATE
                    NETWORK_STATE_MOBILE_UNKNOWN
                }
            }
        } else {
            val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                telephonyManager.dataNetworkType
            } else {
                telephonyManager.networkType
            }
            when (type) {
                TelephonyManager.NETWORK_TYPE_GPRS,
                TelephonyManager.NETWORK_TYPE_CDMA,
                TelephonyManager.NETWORK_TYPE_EDGE,
                TelephonyManager.NETWORK_TYPE_1xRTT,
                TelephonyManager.NETWORK_TYPE_IDEN -> {
                    NETWORK_STATE_2G
                }
                TelephonyManager.NETWORK_TYPE_EVDO_A,
                TelephonyManager.NETWORK_TYPE_UMTS,
                TelephonyManager.NETWORK_TYPE_EVDO_0,
                TelephonyManager.NETWORK_TYPE_HSDPA,
                TelephonyManager.NETWORK_TYPE_HSUPA,
                TelephonyManager.NETWORK_TYPE_HSPA,
                TelephonyManager.NETWORK_TYPE_EVDO_B,
                TelephonyManager.NETWORK_TYPE_EHRPD,
                TelephonyManager.NETWORK_TYPE_HSPAP -> {
                    NETWORK_STATE_3G
                }
                TelephonyManager.NETWORK_TYPE_LTE -> {
                    NETWORK_STATE_4G
                }
                else -> {
                    NETWORK_STATE_MOBILE_UNKNOWN
                }
            }
        }
    }

    fun isMobile(networkState: Int): Boolean {
        return networkState > NETWORK_STATE_WIFI
    }

    fun isNetConnected(context: Context): Boolean {
        val connectivity =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivity != null) {
            @SuppressLint("MissingPermission") val info = connectivity.activeNetworkInfo
            if (info != null && info.isConnected) {
                if (info.state == NetworkInfo.State.CONNECTED) {
                    return true
                }
            }
        }
        return false
    }

    @Synchronized
    fun isWifiConnected(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (connectivityManager != null) {
            val networkInfo = connectivityManager.activeNetworkInfo
            if (networkInfo != null) {
                val networkInfoType = networkInfo.type
                if (networkInfoType == ConnectivityManager.TYPE_WIFI || networkInfoType == ConnectivityManager.TYPE_ETHERNET) {
                    return networkInfo.isConnected
                }
            }
        }
        return false
    }
}