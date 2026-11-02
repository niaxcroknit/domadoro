package com.tomatotimer.app.timer

/** Monotonic time is authoritative within a boot; wall time is a restart fallback. */
internal data class TimerCheckpoint(
    val totalMillis: Long,
    val remainingMillis: Long,
    val running: Boolean,
    val bootCount: Int,
    val elapsedDeadline: Long,
    val wallDeadline: Long,
) {
    fun remainingAt(currentBoot: Int, elapsedNow: Long, wallNow: Long): Long {
        val left = when {
            !running -> remainingMillis
            bootCount == currentBoot -> elapsedDeadline - elapsedNow
            else -> wallDeadline - wallNow
        }
        return left.coerceIn(0L, totalMillis.coerceAtLeast(1L))
    }
}
