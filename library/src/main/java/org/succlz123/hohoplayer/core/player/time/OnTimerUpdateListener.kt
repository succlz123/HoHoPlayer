package org.succlz123.hohoplayer.core.player.time

/**
 * in AVPlayer default open timer proxy, you can use update callback to refresh UI.
 * if you close timer proxy[AVPlayer.setUseTimerProxy],
 * you will not receive this timer update callback.
 * if timer open , the call back called per second.
 * in some scene, you can close it to improve battery performance.
 */
interface OnTimerUpdateListener {
    fun onTimerUpdate(curr: Int, duration: Int, bufferPercentage: Int)
}