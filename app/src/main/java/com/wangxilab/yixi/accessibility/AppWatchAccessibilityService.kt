package com.wangxilab.yixi.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.wangxilab.yixi.YixiApp
import com.wangxilab.yixi.domain.InterventionResult
import com.wangxilab.yixi.intervention.BreathingOverlayController
import com.wangxilab.yixi.intervention.GraceWindowManager
import com.wangxilab.yixi.intervention.PackageRuleMatcher

class AppWatchAccessibilityService : AccessibilityService() {
    private val app: YixiApp get() = application as YixiApp
    private val graceWindow = GraceWindowManager()
    private lateinit var matcher: PackageRuleMatcher
    private lateinit var overlay: BreathingOverlayController
    private var activeRecordId: Long? = null
    private var activePackage: String? = null
    private val suppressUntil = mutableMapOf<String, Long>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        matcher = PackageRuleMatcher(packageName) { app.preferences.isMonitored(it) }
        overlay = BreathingOverlayController(this, ::onOverlayResolved)
        app.serviceStatusStore.connected = true
        app.statsStore.prune()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val foregroundPackage = event.packageName?.toString() ?: return
        app.serviceStatusStore.lastEventAt = System.currentTimeMillis()

        if (overlay.isShowing) {
            val target = activePackage
            if (foregroundPackage != target && foregroundPackage !in EVENT_PACKAGES_TO_IGNORE) {
                overlay.resolve(InterventionResult.ABANDONED)
            }
            return
        }

        if (!matcher.shouldIntervene(foregroundPackage)) return
        if (graceWindow.isAllowed(foregroundPackage)) return
        val suppressedUntil = suppressUntil[foregroundPackage] ?: 0L
        if (System.currentTimeMillis() < suppressedUntil) return
        suppressUntil.remove(foregroundPackage)

        val label = runCatching {
            packageManager.getApplicationLabel(
                packageManager.getApplicationInfo(foregroundPackage, 0),
            ).toString()
        }.getOrDefault(foregroundPackage)

        val recordId = runCatching { app.statsStore.begin(foregroundPackage) }.getOrNull()
        activeRecordId = recordId
        activePackage = foregroundPackage
        if (!overlay.show(label)) {
            recordId?.let { app.statsStore.resolve(it, InterventionResult.FAIL_OPEN, 0) }
            activeRecordId = null
            activePackage = null
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (::overlay.isInitialized) overlay.destroy()
        app.serviceStatusStore.connected = false
        super.onDestroy()
    }

    private fun onOverlayResolved(result: InterventionResult, waitSeconds: Int) {
        val target = activePackage
        if (result == InterventionResult.PROCEEDED && target != null) {
            graceWindow.allow(target)
        }
        if (result == InterventionResult.ABANDONED && target != null) {
            // Removing the overlay may briefly reveal the target before HOME is shown.
            suppressUntil[target] = System.currentTimeMillis() + 2_000L
        }
        activeRecordId?.let { recordId ->
            runCatching { app.statsStore.resolve(recordId, result, waitSeconds) }
        }
        activeRecordId = null
        activePackage = null
    }

    private companion object {
        val EVENT_PACKAGES_TO_IGNORE = setOf(
            "com.wangxilab.yixi",
            "com.android.systemui",
        )
    }
}
