package com.tomatotimer.app.timer

import android.app.AlarmManager
import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import android.os.Build
import android.os.SystemClock
import com.tomatotimer.app.data.Prefs
import com.tomatotimer.app.sound.Alerts
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class Phase { FOCUS, BREAK }

data class TimerState(
    val phase: Phase,
    val totalMillis: Long,
    val remainingMillis: Long,
    val isRunning: Boolean,
    val endAtElapsedMillis: Long = 0L,
) {
    /** 남은 비율(1 = 막 시작, 0 = 끝) */
    val remainingFraction: Float
        get() = if (totalMillis <= 0L) 0f else (remainingMillis.toFloat() / totalMillis).coerceIn(0f, 1f)

    val remainingSeconds: Long get() = (remainingMillis + 999L) / 1000L

    val timeText: String
        get() {
            val s = remainingSeconds
            return "%02d:%02d".format(s / 60, s % 60)
        }

    /** 시작했다가 멈춘 상태인지 */
    val isMidway: Boolean get() = !isRunning && remainingMillis in 1 until totalMillis
}

/**
 * 타이머의 '진짜' 상태를 들고 있는 곳.
 * 화면, PIP 창, 알림이 모두 여기 상태를 보고 그려요.
 */
object TimerEngine {
    private var app: Context? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tickJob: Job? = null
    private var endAtElapsed = 0L
    private lateinit var saved: SharedPreferences

    private val _state = MutableStateFlow(fresh(Phase.FOCUS))
    val state: StateFlow<TimerState> = _state.asStateFlow()

    fun init(context: Context) {
        app = context.applicationContext
        saved = context.getSharedPreferences("active_timer", Context.MODE_PRIVATE)
        val fallback = fresh(Phase.FOCUS)
        val phase = runCatching { Phase.valueOf(saved.getString("phase", "FOCUS")!!) }.getOrDefault(Phase.FOCUS)
        val total = saved.getLong("total", fallback.totalMillis).coerceAtLeast(1L)
        val running = saved.getBoolean("running", false)
        val now = SystemClock.elapsedRealtime()
        val boot = Settings.Global.getInt(context.contentResolver, Settings.Global.BOOT_COUNT, 0)
        val left = TimerCheckpoint(total, saved.getLong("remaining", total), running,
            saved.getInt("boot", -1), saved.getLong("end_elapsed", now),
            saved.getLong("end_wall", System.currentTimeMillis()))
            .remainingAt(boot, now, System.currentTimeMillis())
        endAtElapsed = now + left.coerceAtLeast(0L)
        _state.value = TimerState(phase, total, left.coerceIn(0L, total), running, if (running) endAtElapsed else 0L)
        if (running) {
            scope.launch {
                if (endAtElapsed <= SystemClock.elapsedRealtime()) finish()
                else {
                    scheduleAlarm(context, endAtElapsed)
                    launchTicker()
                }
            }
        }
    }

    /** 설정에서 시간을 바꿨을 때, 아직 시작 전이면 바로 반영 */
    fun refreshIfIdle() {
        val s = _state.value
        if (!s.isRunning && !s.isMidway) {
            _state.value = fresh(s.phase)
            save()
        }
    }

    fun toggle() {
        if (_state.value.isRunning) pause() else start()
    }

    fun start() {
        val ctx = app ?: return
        val s = _state.value
        if (s.isRunning || s.totalMillis <= 0L) return
        val remaining = if (s.remainingMillis <= 0L) s.totalMillis else s.remainingMillis
        endAtElapsed = SystemClock.elapsedRealtime() + remaining
        _state.value = s.copy(remainingMillis = remaining, isRunning = true, endAtElapsedMillis = endAtElapsed)
        save()
        scheduleAlarm(ctx, endAtElapsed)
        TimerService.start(ctx)

        launchTicker()
    }

    private fun launchTicker() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val left = endAtElapsed - SystemClock.elapsedRealtime()
                if (left <= 0L) {
                    finish()
                    break
                }
                _state.value = _state.value.copy(remainingMillis = left)
                delay(left.coerceAtMost(200L))
            }
        }
    }

    fun pause() {
        val s = _state.value
        if (!s.isRunning) return
        tickJob?.cancel()
        cancelAlarm()
        val left = (endAtElapsed - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
        _state.value = s.copy(remainingMillis = left, isRunning = false, endAtElapsedMillis = 0L)
        save()
        app?.let { Notifications.notifyTimer(it, _state.value) }
    }

    fun reset() {
        tickJob?.cancel()
        cancelAlarm()
        _state.value = fresh(_state.value.phase)
        save()
        app?.let { Notifications.cancelTimer(it) }
    }

    /** 지금 단계를 건너뛰고 다음 단계(집중 → 휴식 → 집중)로. 건너뛴 집중은 수확하지 않아요. */
    fun skip() {
        tickJob?.cancel()
        cancelAlarm()
        _state.value = next(_state.value)
        save()
        app?.let { Notifications.cancelTimer(it) }
    }

    /** 화면이 꺼져 있을 때 알람이 울리면 여기로 와요. */
    fun onAlarm() {
        val s = _state.value
        if (s.isRunning && SystemClock.elapsedRealtime() >= endAtElapsed - 500L) {
            tickJob?.cancel()
            finish()
        }
    }

    private fun finish() {
        val s = _state.value
        if (!s.isRunning) return
        cancelAlarm()
        if (s.phase == Phase.FOCUS) {
            Prefs.addSession((s.totalMillis / 60_000L).toInt())
        }
        // 화면을 안 보고 있어도 알 수 있게: 소리 + 진동 + 알림
        val alerts = Prefs.alerts.value
        if (alerts.sound) Alerts.chime(s.phase)
        app?.let { ctx ->
            if (alerts.vibrate) Alerts.vibrate(ctx, s.phase)
            Notifications.showFinished(ctx, s, autoStarted = alerts.autoNext)
        }
        _state.value = next(s)
        save()
        if (alerts.autoNext) start()
    }


    /** Persist only user/phase transitions, never every 200 ms tick. */
    private fun save() {
        val ctx = app ?: return
        if (!::saved.isInitialized) return
        val s = _state.value
        saved.edit()
            .putString("phase", s.phase.name)
            .putLong("total", s.totalMillis)
            .putLong("remaining", s.remainingMillis)
            .putBoolean("running", s.isRunning)
            .putLong("end_elapsed", endAtElapsed)
            .putLong("end_wall", System.currentTimeMillis() + s.remainingMillis)
            .putInt("boot", Settings.Global.getInt(ctx.contentResolver, Settings.Global.BOOT_COUNT, 0))
            .commit()
    }

    private fun next(s: TimerState): TimerState =
        if (s.phase == Phase.FOCUS && Prefs.durations.value.restMin > 0) fresh(Phase.BREAK) else fresh(Phase.FOCUS)

    private fun fresh(phase: Phase): TimerState {
        val d = Prefs.durations.value
        val minutes = if (phase == Phase.FOCUS) d.focusMin else d.restMin
        val total = minutes * 60_000L
        return TimerState(phase, total, total, isRunning = false)
    }

    private fun scheduleAlarm(ctx: Context, atElapsed: Long) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val pi = TimerActionReceiver.pending(ctx, TimerActionReceiver.ACTION_ALARM)
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, atElapsed, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, atElapsed, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, atElapsed, pi)
        }
    }

    private fun cancelAlarm() {
        val ctx = app ?: return
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(TimerActionReceiver.pending(ctx, TimerActionReceiver.ACTION_ALARM))
    }
}
