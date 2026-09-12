package com.wangxilab.yixi.intervention

class GraceWindowManager(
    private val durationMillis: Long = 90_000L,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val allowedUntil = mutableMapOf<String, Long>()

    @Synchronized
    fun allow(packageName: String) {
        allowedUntil[packageName] = clock() + durationMillis
    }

    @Synchronized
    fun isAllowed(packageName: String): Boolean {
        val expiry = allowedUntil[packageName] ?: return false
        if (clock() >= expiry) {
            allowedUntil.remove(packageName)
            return false
        }
        return true
    }
}
