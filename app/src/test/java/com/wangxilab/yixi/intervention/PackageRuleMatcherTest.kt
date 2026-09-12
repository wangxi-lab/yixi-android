package com.wangxilab.yixi.intervention

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageRuleMatcherTest {
    private val matcher = PackageRuleMatcher("com.wangxilab.yixi") {
        it == "example.social" || it == "com.android.systemui"
    }

    @Test
    fun `matches selected third party app`() {
        assertTrue(matcher.shouldIntervene("example.social"))
    }

    @Test
    fun `never matches own or protected system package`() {
        assertFalse(matcher.shouldIntervene("com.wangxilab.yixi"))
        assertFalse(matcher.shouldIntervene("com.android.systemui"))
        assertFalse(matcher.shouldIntervene("com.android.settings"))
    }

    @Test
    fun `does not match unselected app`() {
        assertFalse(matcher.shouldIntervene("example.notes"))
    }
}
