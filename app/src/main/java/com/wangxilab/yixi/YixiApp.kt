package com.wangxilab.yixi

import android.app.Application
import com.wangxilab.yixi.data.AppPreferences
import com.wangxilab.yixi.data.ServiceStatusStore
import com.wangxilab.yixi.data.StatsStore

class YixiApp : Application() {
    lateinit var preferences: AppPreferences
        private set
    lateinit var statsStore: StatsStore
        private set
    lateinit var serviceStatusStore: ServiceStatusStore
        private set

    override fun onCreate() {
        super.onCreate()
        preferences = AppPreferences(this)
        statsStore = StatsStore(this)
        serviceStatusStore = ServiceStatusStore(this)
        // A clean process start invalidates a previously persisted "connected" flag.
        serviceStatusStore.connected = false
    }
}
