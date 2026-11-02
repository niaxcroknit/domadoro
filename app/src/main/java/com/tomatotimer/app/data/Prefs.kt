package com.tomatotimer.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

enum class ThemeMode { AUTO, DAY, NIGHT }

data class Durations(val focusMin: Int, val restMin: Int) {
    companion object {
        val Default = Durations(focusMin = 25, restMin = 5)
    }
}

/** 수확한 토마토 하나 = 끝까지 마친 집중 한 번 */
data class Session(val epochDay: Long, val minutes: Int)

enum class NoiseType(val label: String) {
    WHITE("백색소음"),
    RAIN("빗소리"),
    BROWN("깊은 소음"),
    WAVE("파도"),
}

data class NoiseSettings(
    val on: Boolean = false,
    val type: NoiseType = NoiseType.RAIN,
    val volume: Float = 0.5f,
    /** 휴식 시간엔 소리를 멈춰서 '쉬는 시간' 신호로 쓰기 */
    val breakOff: Boolean = true,
)

data class AlertSettings(
    val sound: Boolean = true,
    val vibrate: Boolean = true,
    /** 끝나면 다음 단계를 자동으로 시작 */
    val autoNext: Boolean = false,
)

/** 설정과 기록을 기기에 저장해요. */
object Prefs {
    private lateinit var sp: SharedPreferences

    private val _theme = MutableStateFlow(ThemeMode.AUTO)
    val theme: StateFlow<ThemeMode> = _theme.asStateFlow()

    private val _durations = MutableStateFlow(Durations.Default)
    val durations: StateFlow<Durations> = _durations.asStateFlow()

    private val _sessions = MutableStateFlow<List<Session>>(emptyList())
    val sessions: StateFlow<List<Session>> = _sessions.asStateFlow()

    private val _noise = MutableStateFlow(NoiseSettings())
    val noise: StateFlow<NoiseSettings> = _noise.asStateFlow()

    private val _alerts = MutableStateFlow(AlertSettings())
    val alerts: StateFlow<AlertSettings> = _alerts.asStateFlow()

    fun init(context: Context) {
        if (::sp.isInitialized) return
        sp = context.getSharedPreferences("tomato", Context.MODE_PRIVATE)
        _theme.value = ThemeMode.entries.getOrElse(sp.getInt(KEY_THEME, 0)) { ThemeMode.AUTO }
        _durations.value = Durations(
            focusMin = sp.getInt(KEY_FOCUS, Durations.Default.focusMin),
            restMin = sp.getInt(KEY_REST, Durations.Default.restMin),
        )
        _sessions.value = parse(sp.getString(KEY_SESSIONS, "").orEmpty())
        _noise.value = NoiseSettings(
            on = sp.getBoolean("noise_on", false),
            type = NoiseType.entries.getOrElse(sp.getInt("noise_type", NoiseType.RAIN.ordinal)) { NoiseType.RAIN },
            volume = sp.getFloat("noise_volume", 0.5f),
            breakOff = sp.getBoolean("noise_break_off", true),
        )
        _alerts.value = AlertSettings(
            sound = sp.getBoolean("alert_sound", true),
            vibrate = sp.getBoolean("alert_vibrate", true),
            autoNext = sp.getBoolean("auto_next", false),
        )
    }

    fun setNoise(n: NoiseSettings) {
        if (::sp.isInitialized) {
            sp.edit()
                .putBoolean("noise_on", n.on)
                .putInt("noise_type", n.type.ordinal)
                .putFloat("noise_volume", n.volume)
                .putBoolean("noise_break_off", n.breakOff)
                .apply()
        }
        _noise.value = n
    }

    fun setAlerts(a: AlertSettings) {
        if (::sp.isInitialized) {
            sp.edit()
                .putBoolean("alert_sound", a.sound)
                .putBoolean("alert_vibrate", a.vibrate)
                .putBoolean("auto_next", a.autoNext)
                .apply()
        }
        _alerts.value = a
    }

    fun setTheme(mode: ThemeMode) {
        if (::sp.isInitialized) sp.edit().putInt(KEY_THEME, mode.ordinal).apply()
        _theme.value = mode
    }

    fun setDurations(d: Durations) {
        if (::sp.isInitialized) sp.edit().putInt(KEY_FOCUS, d.focusMin).putInt(KEY_REST, d.restMin).apply()
        _durations.value = d
    }

    fun addSession(minutes: Int) {
        val today = LocalDate.now().toEpochDay()
        val kept = (_sessions.value + Session(today, minutes)).filter { it.epochDay >= today - KEEP_DAYS }
        if (::sp.isInitialized) {
            sp.edit().putString(KEY_SESSIONS, kept.joinToString("\n") { "${it.epochDay}|${it.minutes}" }).apply()
        }
        _sessions.value = kept
    }

    private fun parse(raw: String): List<Session> = raw.lineSequence()
        .mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size < 2) return@mapNotNull null
            val day = parts.first().toLongOrNull() ?: return@mapNotNull null
            val min = parts.last().toIntOrNull() ?: return@mapNotNull null
            Session(day, min)
        }
        .toList()

    private const val KEY_THEME = "theme"
    private const val KEY_FOCUS = "focus_min"
    private const val KEY_REST = "rest_min"
    private const val KEY_SESSIONS = "sessions"
    private const val KEEP_DAYS = 365L
}
