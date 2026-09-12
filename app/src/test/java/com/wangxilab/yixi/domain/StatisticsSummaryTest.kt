package com.wangxilab.yixi.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsSummaryTest {
    @Test
    fun `abandon rate ignores non-decisions`() {
        val summary = StatisticsSummary(
            total = 12,
            proceeded = 3,
            abandoned = 7,
            totalWaitSeconds = 100,
        )

        assertEquals(70, summary.abandonRate)
    }

    @Test
    fun `empty decisions have zero rate`() {
        assertEquals(0, StatisticsSummary().abandonRate)
    }
}
