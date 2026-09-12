package com.wangxilab.yixi.intervention

class PackageRuleMatcher(
    private val ownPackageName: String,
    private val monitored: (String) -> Boolean,
) {
    fun shouldIntervene(packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false
        if (packageName == ownPackageName || packageName in NEVER_INTERVENE) return false
        return monitored(packageName)
    }

    companion object {
        private val NEVER_INTERVENE = setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.phone",
            "com.android.server.telecom",
        )
    }
}
