package com.wangxilab.yixi

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.wangxilab.yixi.domain.LaunchableApp
import com.wangxilab.yixi.domain.DailyCount
import com.wangxilab.yixi.domain.RecentIntervention
import com.wangxilab.yixi.domain.StatisticsSummary
import com.wangxilab.yixi.platform.LaunchableAppProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val loading: Boolean = true,
    val apps: List<LaunchableApp> = emptyList(),
    val statistics: StatisticsSummary = StatisticsSummary(),
    val recent: List<RecentIntervention> = emptyList(),
    val dailyCounts: List<DailyCount> = emptyList(),
    val accessibilityEnabled: Boolean = false,
    val serviceConnected: Boolean = false,
    val lastEventAt: Long = 0L,
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as YixiApp
    private val launchableAppProvider = LaunchableAppProvider(application)
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            app.statsStore.prune()
            val enabled = isAccessibilityServiceEnabled()
            _uiState.value = AppUiState(
                loading = false,
                apps = launchableAppProvider.load(app.preferences.monitoredPackages()),
                statistics = app.statsStore.summary(),
                recent = app.statsStore.recent(),
                dailyCounts = app.statsStore.dailyCounts(),
                accessibilityEnabled = enabled,
                serviceConnected = enabled && app.serviceStatusStore.connected,
                lastEventAt = app.serviceStatusStore.lastEventAt,
            )
        }
    }

    fun setMonitored(packageName: String, monitored: Boolean) {
        app.preferences.setMonitored(packageName, monitored)
        _uiState.update { state ->
            state.copy(
                apps = state.apps.map { app ->
                    if (app.packageName == packageName) app.copy(monitored = monitored) else app
                },
            )
        }
    }

    fun clearStatistics() {
        viewModelScope.launch(Dispatchers.IO) {
            app.statsStore.clearAll()
            _uiState.update {
                it.copy(
                    statistics = StatisticsSummary(),
                    recent = emptyList(),
                    dailyCounts = app.statsStore.dailyCounts(),
                )
            }
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val manager = getApplication<Application>()
            .getSystemService(AccessibilityManager::class.java)
        return manager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { info ->
                info.resolveInfo.serviceInfo.packageName == getApplication<Application>().packageName &&
                    info.resolveInfo.serviceInfo.name.endsWith("AppWatchAccessibilityService")
            }
    }
}
