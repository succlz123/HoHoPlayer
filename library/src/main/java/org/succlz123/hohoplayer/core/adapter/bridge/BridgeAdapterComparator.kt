package org.succlz123.hohoplayer.core.adapter.bridge

import org.succlz123.hohoplayer.core.adapter.BaseCoverAdapter
import org.succlz123.hohoplayer.core.adapter.IAdapter
import java.util.*

class BridgeAdapterComparator : Comparator<IAdapter> {

    override fun compare(o1: IAdapter, o2: IAdapter): Int {
        var x = 0
        var y = 0
        if (o1 is BaseCoverAdapter) {
            x = o1.getCoverLevel()
        }
        if (o2 is BaseCoverAdapter) {
            y = o2.getCoverLevel()
        }
        return if (x < y) -1 else if (x == y) 0 else 1
    }
}