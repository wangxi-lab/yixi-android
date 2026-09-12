package com.wangxilab.yixi.data

import android.content.Context

class AppPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun monitoredPackages(): Set<String> =
        preferences.getStringSet(KEY_MONITORED_PACKAGES, emptySet())?.toSet().orEmpty()

    fun isMonitored(packageName: String): Boolean = packageName in monitoredPackages()

    fun setMonitored(packageName: String, monitored: Boolean) {
        val updated = monitoredPackages().toMutableSet()
        if (monitored) updated += packageName else updated -= packageName
        preferences.edit().putStringSet(KEY_MONITORED_PACKAGES, updated).apply()
    }

    companion object {
        private const val FILE_NAME = "yixi_preferences"
        private const val KEY_MONITORED_PACKAGES = "monitored_packages"
    }
}
