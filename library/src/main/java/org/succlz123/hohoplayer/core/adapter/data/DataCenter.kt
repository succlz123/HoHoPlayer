package org.succlz123.hohoplayer.core.adapter.data

import org.succlz123.hohoplayer.core.adapter.bridge.IBridge.OnValueListener
import java.util.*

// key code
class DataCenter {
    private val onValueListeners = ArrayList<OnValueListener>()
    private val listenerKeys = HashMap<OnValueListener, Array<String>>()

    private val valueMap = HashMap<String, Any>()

    fun registerOnValueListener(listener: OnValueListener) {
        if (onValueListeners.contains(listener)) {
            return
        }
        onValueListeners.add(listener)
        listenerKeys[listener] = listener.keys()
        // preset value
        val iterator = valueMap.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            if (containsKey(listenerKeys[listener], next.key)) {
                listener.onValueUpdate(next.key, next.value)
            }
        }
    }

    fun unregisterOnValueListener(onValueListener: OnValueListener) {
        listenerKeys.remove(onValueListener)
        onValueListeners.remove(onValueListener)
    }

    fun clearOnValueListeners() {
        onValueListeners.clear()
        listenerKeys.clear()
    }

    fun clearValues() {
        valueMap.clear()
    }

    fun clear() {
        clearOnValueListeners()
        clearValues()
    }

    fun putObject(key: String, value: Any) {
        put(key, value)
    }

    private fun put(key: String, value: Any, notifyUpdate: Boolean = true) {
        valueMap[key] = value
        if (notifyUpdate) {
            callBackValueUpdate(key, value)
        }
    }

    private fun callBackValueUpdate(key: String, value: Any) {
        onValueListeners.filter {
            containsKey(listenerKeys[it], key)
        }.forEach { callback ->
            callback.onValueUpdate(key, value)
        }
    }

    private fun containsKey(keys: Array<String>?, nowKey: String): Boolean {
        return if (!keys.isNullOrEmpty()) {
            Arrays.binarySearch(keys, nowKey) >= 0
        } else {
            false
        }
    }

    operator fun <T> get(key: String): T? {
        return valueMap[key] as? T
    }

    fun getBoolean(key: String): Boolean {
        return getBoolean(key, false)
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return get<Boolean>(key) ?: return defaultValue
    }

    fun getInt(key: String): Int {
        return getInt(key, 0)
    }

    fun getInt(key: String, defaultValue: Int): Int {
        return get<Int>(key) ?: return defaultValue
    }

    fun getString(key: String): String? {
        return get<String>(key)
    }

    fun getFloat(key: String): Float {
        return getFloat(key, 0f)
    }

    fun getFloat(key: String, defaultValue: Float): Float {
        return get<Float>(key) ?: return defaultValue
    }

    fun getLong(key: String): Long {
        return getLong(key, 0)
    }

    fun getLong(key: String, defaultValue: Long): Long {
        return get<Long>(key) ?: return defaultValue
    }

    fun getDouble(key: String): Double {
        return getDouble(key, 0.0)
    }

    fun getDouble(key: String, defaultValue: Double): Double {
        return get<Double>(key) ?: return defaultValue
    }
}