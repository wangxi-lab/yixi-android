package com.wangxilab.yixi.platform

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.wangxilab.yixi.domain.LaunchableApp

class LaunchableAppProvider(private val context: Context) {
    fun load(monitoredPackages: Set<String>): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0),
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.queryIntentActivities(intent, 0)
        }

        return resolved
            .asSequence()
            .mapNotNull { info ->
                val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
                if (packageName == context.packageName) return@mapNotNull null
                LaunchableApp(
                    packageName = packageName,
                    label = info.loadLabel(context.packageManager).toString().ifBlank { packageName },
                    icon = runCatching { info.loadIcon(context.packageManager) }.getOrNull(),
                    monitored = packageName in monitoredPackages,
                )
            }
            .distinctBy { it.packageName }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            .toList()
    }
}
