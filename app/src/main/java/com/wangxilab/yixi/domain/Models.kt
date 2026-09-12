package com.wangxilab.yixi.domain

enum class InterventionResult {
    PROCEEDED,
    ABANDONED,
    TIMEOUT,
    FAIL_OPEN,
}

data class LaunchableApp(
    val packageName: String,
    val label: String,
    val monitored: Boolean,
)

data class StatisticsSummary(
    val total: Int = 0,
    val proceeded: Int = 0,
    val abandoned: Int = 0,
    val totalWaitSeconds: Int = 0,
) {
    val abandonRate: Int
        get() {
            val decided = proceeded + abandoned
            return if (decided == 0) 0 else abandoned * 100 / decided
        }
}

data class RecentIntervention(
    val appLabel: String,
    val packageName: String,
    val triggeredAt: Long,
    val waitSeconds: Int,
    val result: InterventionResult,
)
