package org.succlz123.hohoplayer.core.adapter.bridge

import org.succlz123.hohoplayer.core.adapter.IAdapter
import org.succlz123.hohoplayer.core.adapter.bridge.IBridge.OnAdapterFilter
import org.succlz123.hohoplayer.core.adapter.data.DataCenter
import org.succlz123.hohoplayer.core.player.base.IPlayer
import java.lang.ref.WeakReference
import java.util.*

class Bridge(dataCenter: DataCenter = DataCenter()) : IBridge {
    private val adapterMap: HashMap<String, IAdapter> = hashMapOf()

    private val adapterList: MutableList<IAdapter> = arrayListOf()

    private val onAdapterChangeListeners: MutableList<IBridge.OnAdapterChange> = arrayListOf()

    private var currentDataCenter = dataCenter

    private var weakRfPlayer: WeakReference<IPlayer>? = null

    override fun getDataCenter(): DataCenter {
        return currentDataCenter
    }

    override fun addChangeListener(onAdapterChange: IBridge.OnAdapterChange) {
        if (onAdapterChangeListeners.contains(onAdapterChange)) {
            return
        }
        onAdapterChangeListeners.add(onAdapterChange)
    }

    override fun removeChangeListener(onAdapterChange: IBridge.OnAdapterChange) {
        onAdapterChangeListeners.remove(onAdapterChange)
    }

    override fun addAdapter(adapter: IAdapter) {
        adapter.bindBridge(this)
        adapter.onAdapterBind()
        adapterMap[adapter.key] = adapter
        adapterList.add(adapter)
        for (listener in onAdapterChangeListeners) {
            listener.onAdapterAdd(adapter.key, adapter)
        }
    }

    override fun removeAdapter(key: String) {
        val adapter = adapterMap.remove(key)
        adapterList.remove(adapter)
        onAdapterRemove(key, adapter)
    }

    private fun onAdapterRemove(key: String, adapter: IAdapter?) {
        if (adapter != null) {
            for (listener in onAdapterChangeListeners) {
                listener.onAdapterRemove(key, adapter)
            }
            adapter.onAdapterUnBind()
        }
    }

    override fun sort(comparator: Comparator<IAdapter>) {
        Collections.sort(adapterList, comparator)
    }

    override fun forEach(lopper: (adapter: IAdapter) -> Unit) {
        forEach(null, lopper)
    }

    override fun forEach(filter: OnAdapterFilter?, lopper: (adapter: IAdapter) -> Unit) {
        for (iAdapter in adapterList) {
            if (filter == null || filter.filter(iAdapter)) {
                lopper.invoke(iAdapter)
            }
        }
    }

    override fun setPlayer(player: IPlayer) {
        weakRfPlayer = WeakReference(player)
    }

    override fun getPlayer(): IPlayer? {
        return weakRfPlayer?.get()
    }

    override fun getAdapter(key: String): IAdapter? {
        return adapterMap[key]
    }

    override fun clear() {
        weakRfPlayer?.clear()
        weakRfPlayer = null
        for (iAdapter in adapterList) {
            onAdapterRemove(iAdapter.key, iAdapter)
        }
        currentDataCenter.clear()
        adapterList.clear()
        adapterMap.clear()
    }
}
