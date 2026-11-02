package com.tomatotimer.app.timer
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerCheckpointTest {
    private val active = TimerCheckpoint(60_000, 60_000, true, 5, 100_000, 1_000_000)
    @Test fun processRestorationCountsTimeSpentAway() {
        assertEquals(12_000L, active.remainingAt(5, 88_000, 999_999))
    }
    @Test fun changingSystemClockDoesNotChangeActiveTimer() {
        assertEquals(12_000L, active.remainingAt(5, 88_000, 9_999_999))
    }
    @Test fun rebootUsesWallDeadlineInsteadOfOldMonotonicClock() {
        assertEquals(10_000L, active.remainingAt(6, 1_000, 990_000))
    }
    @Test fun pausedTimerDoesNotConsumeTimeWhileProcessIsGone() {
        assertEquals(17_000L, active.copy(running = false, remainingMillis = 17_000)
            .remainingAt(5, 999_999, 9_999_999))
    }
    @Test fun expiredTimerRestoresAtZero() {
        assertEquals(0L, active.remainingAt(5, 120_000, 1_000_000))
    }
    @Test fun fallbackNeverExceedsOriginalDuration() {
        assertEquals(60_000L, active.remainingAt(6, 1_000, 0))
    }
}
