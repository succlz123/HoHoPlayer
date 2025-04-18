package org.succlz123.hohoplayer.core.adapter.event

import android.view.KeyEvent

interface OnAdapterKeyEventListener {

    fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean

    fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean

    fun onKeyLongPress(keyCode: Int, event: KeyEvent?): Boolean

    fun dispatchKeyEvent(event: KeyEvent?): Boolean
}