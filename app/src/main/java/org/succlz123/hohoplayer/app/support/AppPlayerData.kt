package org.succlz123.hohoplayer.app.support

object AppPlayerData {

    object Event {
        const val EVENT_CODE_REQUEST_BACK = -100
        const val EVENT_CODE_REQUEST_CLOSE = -101
        const val EVENT_CODE_REQUEST_TOGGLE_SCREEN = -104
        const val EVENT_CODE_ERROR_SHOW = -111
    }

    object Key {
        const val KEY_IS_LANDSCAPE = "isLandscape"
        const val KEY_DATA_SOURCE = "data_source"
        const val KEY_ERROR_SHOW = "error_show"
        const val KEY_COMPLETE_SHOW = "complete_show"
        const val KEY_CONTROLLER_TOP_ENABLE = "controller_top_enable"
        const val KEY_CONTROLLER_SCREEN_SWITCH_ENABLE = "screen_switch_enable"
        const val KEY_TIMER_UPDATE_ENABLE = "timer_update_enable"
        const val KEY_NETWORK_RESOURCE = "network_resource"
    }

    object PrivateEvent {
        const val EVENT_CODE_UPDATE_SEEK = -201
    }
}

