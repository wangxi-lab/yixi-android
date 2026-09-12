package com.wangxilab.yixi.data

import android.content.Context

class ServiceStatusStore(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var connected: Boolean
        get() = preferences.getBoolean(KEY_CONNECTED, false)
        set(value) = preferences.edit().putBoolean(KEY_CONNECTED, value).apply()

    var lastEventAt: Long
        get() = preferences.getLong(KEY_LAST_EVENT_AT, 0L)
        set(value) = preferences.edit().putLong(KEY_LAST_EVENT_AT, value).apply()

    companion object {
        private const val FILE_NAME = "yixi_service_status"
        private const val KEY_CONNECTED = "connected"
        private const val KEY_LAST_EVENT_AT = "last_event_at"
    }
}
