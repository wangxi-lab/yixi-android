package com.wangxilab.yixi.intervention

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GraceWindowManagerTest {
    @Test
    fun `allowed package remains open during grace window`() {
        var now = 1_000L
        val manager = GraceWindowManager(durationMillis = 90_000L) { now }

        manager.allow("example.social")
        now += 89_999L

        assertTrue(manager.isAllowed("example.social"))
    }

    @Test
    fun `grace window expires at boundary`() {
        var now = 1_000L
        val manager = GraceWindowManager(durationMillis = 90_000L) { now }

        manager.allow("example.social")
        now += 90_000L

        assertFalse(manager.isAllowed("example.social"))
    }
}
