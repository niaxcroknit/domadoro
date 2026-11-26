package com.tomatotimer.app.timer

import android.app.NotificationManager
import android.content.Context
import com.tomatotimer.app.data.Prefs

/**
 * 집중하는 동안만 방해 금지(중요한 알림만 받기)를 켜고, 휴식·멈춤·끝나면 다시 풀어요.
 *
 * - 사용자가 설정에서 '방해 금지 접근'을 허용해야 동작해요.
 * - 원래 방해 금지가 꺼져 있을 때만 켜요. 사용자가 직접 켜 둔 방해 금지는 건드리지 않아요.
 * - 도마도로가 켠 방해 금지만 도마도로가 꺼요. (앱이 꺼졌다 켜져도 기억해요)
 */
object FocusDnd {
    private const val FILE = "focus_dnd"
    private const val KEY_OURS = "engaged_by_us"

    fun hasAccess(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted == true

    /** 지금 타이머 상태에 맞게 켜거나 꺼요. */
    fun sync(context: Context, s: TimerState) {
        val want = Prefs.focusDnd.value && s.isRunning && s.phase == Phase.FOCUS
        if (want) engage(context) else release(context)
    }

    private fun engage(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        if (!nm.isNotificationPolicyAccessGranted) return
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (sp.getBoolean(KEY_OURS, false)) return
        if (nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL) return
        try {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            sp.edit().putBoolean(KEY_OURS, true).apply()
        } catch (_: SecurityException) {
        }
    }

    /** 도마도로가 켠 방해 금지를 풀어요. 실제로 풀었으면 true. */
    fun release(context: Context): Boolean {
        val sp = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        if (!sp.getBoolean(KEY_OURS, false)) return false
        sp.edit().putBoolean(KEY_OURS, false).apply()
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        if (!nm.isNotificationPolicyAccessGranted) return false
        // 그 사이 사용자가 다른 방해 금지 모드로 바꿨다면 그대로 둬요.
        if (nm.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_PRIORITY) return false
        return try {
            nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            true
        } catch (_: SecurityException) {
            false
        }
    }
}
