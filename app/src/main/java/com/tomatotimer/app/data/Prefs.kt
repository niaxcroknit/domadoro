package com.tomatotimer.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate

enum class ThemeMode { AUTO, DAY, NIGHT }

data class Durations(
    val focusMin: Int,
    val restMin: Int,
    /** 긴 휴식 길이(분) */
    val longRestMin: Int = 15,
    /** 집중을 몇 번 마칠 때마다 긴 휴식을 줄지. 0이면 긴 휴식 없음 */
    val longEvery: Int = 4,
) {
    companion object {
        val Default = Durations(focusMin = 25, restMin = 5, longRestMin = 15, longEvery = 4)
    }
}

/** 수확한 토마토 하나 = 끝까지 마친 집중 한 번. label은 무엇에 집중했는지(없으면 null). */
data class Session(val epochDay: Long, val minutes: Int, val label: String? = null)

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
    // 아래 상태들보다 먼저 만들어져야 해서 맨 위에 둬요.
    private val DEFAULT_LABELS = listOf("공부", "업무", "독서")
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

    /** 하루 목표 토마토 개수. 0이면 목표 없음 */
    private val _goal = MutableStateFlow(DEFAULT_GOAL)
    val goal: StateFlow<Int> = _goal.asStateFlow()

    /** 목표 달성 축하 화면을 이미 보여준 날(epochDay) */
    private val _celebratedDay = MutableStateFlow(-1L)
    val celebratedDay: StateFlow<Long> = _celebratedDay.asStateFlow()

    /** 집중 라벨 목록과 지금 고른 라벨 */
    private val _labels = MutableStateFlow(DEFAULT_LABELS)
    val labels: StateFlow<List<String>> = _labels.asStateFlow()

    private val _currentLabel = MutableStateFlow<String?>(null)
    val currentLabel: StateFlow<String?> = _currentLabel.asStateFlow()

    /** 집중하는 동안 방해 금지 켜기 */
    private val _focusDnd = MutableStateFlow(false)
    val focusDnd: StateFlow<Boolean> = _focusDnd.asStateFlow()

    fun init(context: Context) {
        if (::sp.isInitialized) return
        sp = context.getSharedPreferences("tomato", Context.MODE_PRIVATE)
        _theme.value = ThemeMode.entries.getOrElse(sp.getInt(KEY_THEME, 0)) { ThemeMode.AUTO }
        _durations.value = Durations(
            focusMin = sp.getInt(KEY_FOCUS, Durations.Default.focusMin),
            restMin = sp.getInt(KEY_REST, Durations.Default.restMin),
            longRestMin = sp.getInt(KEY_LONG_REST, Durations.Default.longRestMin),
            longEvery = sp.getInt(KEY_LONG_EVERY, Durations.Default.longEvery),
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
        _goal.value = sp.getInt(KEY_GOAL, DEFAULT_GOAL)
        _celebratedDay.value = sp.getLong(KEY_CELEBRATED, -1L)
        _labels.value = sp.getString(KEY_LABELS, null)
            ?.split("\n")?.map { it.trim() }?.filter { it.isNotEmpty() }
            ?: DEFAULT_LABELS
        _currentLabel.value = sp.getString(KEY_CURRENT_LABEL, null)?.takeIf { it in _labels.value }
        _focusDnd.value = sp.getBoolean(KEY_DND, false)
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
        if (::sp.isInitialized) {
            sp.edit()
                .putInt(KEY_FOCUS, d.focusMin)
                .putInt(KEY_REST, d.restMin)
                .putInt(KEY_LONG_REST, d.longRestMin)
                .putInt(KEY_LONG_EVERY, d.longEvery)
                .apply()
        }
        _durations.value = d
    }

    fun setGoal(goal: Int) {
        val g = goal.coerceIn(0, MAX_GOAL)
        if (::sp.isInitialized) sp.edit().putInt(KEY_GOAL, g).apply()
        _goal.value = g
    }

    fun markCelebrated(epochDay: Long) {
        if (::sp.isInitialized) sp.edit().putLong(KEY_CELEBRATED, epochDay).apply()
        _celebratedDay.value = epochDay
    }

    /** 라벨 이름 정리: 앞뒤 공백, 저장에 쓰는 기호(| 줄바꿈) 빼고 최대 10자 */
    fun cleanLabel(raw: String): String =
        raw.replace("|", " ").replace("\n", " ").trim().take(MAX_LABEL_LENGTH)

    fun addLabel(raw: String): Boolean {
        val name = cleanLabel(raw)
        if (name.isEmpty() || name in _labels.value || _labels.value.size >= MAX_LABELS) return false
        saveLabels(_labels.value + name)
        return true
    }

    fun removeLabel(name: String) {
        saveLabels(_labels.value - name)
        if (_currentLabel.value == name) setCurrentLabel(null)
    }

    fun setCurrentLabel(name: String?) {
        val value = name?.takeIf { it in _labels.value }
        if (::sp.isInitialized) sp.edit().putString(KEY_CURRENT_LABEL, value).apply()
        _currentLabel.value = value
    }

    private fun saveLabels(list: List<String>) {
        if (::sp.isInitialized) sp.edit().putString(KEY_LABELS, list.joinToString("\n")).apply()
        _labels.value = list
    }

    fun setFocusDnd(on: Boolean) {
        if (::sp.isInitialized) sp.edit().putBoolean(KEY_DND, on).apply()
        _focusDnd.value = on
    }

    fun addSession(minutes: Int, label: String? = null) {
        val today = LocalDate.now().toEpochDay()
        val kept = (_sessions.value + Session(today, minutes, label?.let { cleanLabel(it) }?.ifEmpty { null }))
            .filter { it.epochDay >= today - KEEP_DAYS }
        if (::sp.isInitialized) {
            sp.edit().putString(KEY_SESSIONS, kept.joinToString("\n") { "${it.epochDay}|${it.minutes}|${it.label.orEmpty()}" }).apply()
        }
        _sessions.value = kept
    }

    fun todayCount(): Int {
        val today = LocalDate.now().toEpochDay()
        return _sessions.value.count { it.epochDay == today }
    }

    /** 예전 형식 "날짜|분"과 새 형식 "날짜|분|라벨"을 모두 읽어요. */
    private fun parse(raw: String): List<Session> = raw.lineSequence()
        .mapNotNull { line ->
            val parts = line.split("|")
            if (parts.size < 2) return@mapNotNull null
            val day = parts[0].toLongOrNull() ?: return@mapNotNull null
            val min = parts[1].toIntOrNull() ?: return@mapNotNull null
            Session(day, min, parts.getOrNull(2)?.trim()?.ifEmpty { null })
        }
        .toList()

    const val MAX_GOAL = 20
    const val MAX_LABELS = 8
    const val MAX_LABEL_LENGTH = 10
    private const val DEFAULT_GOAL = 6

    private const val KEY_THEME = "theme"
    private const val KEY_FOCUS = "focus_min"
    private const val KEY_REST = "rest_min"
    private const val KEY_LONG_REST = "long_rest_min"
    private const val KEY_LONG_EVERY = "long_every"
    private const val KEY_SESSIONS = "sessions"
    private const val KEY_GOAL = "daily_goal"
    private const val KEY_CELEBRATED = "goal_celebrated_day"
    private const val KEY_LABELS = "labels"
    private const val KEY_CURRENT_LABEL = "current_label"
    private const val KEY_DND = "focus_dnd"
    private const val KEEP_DAYS = 365L
}
