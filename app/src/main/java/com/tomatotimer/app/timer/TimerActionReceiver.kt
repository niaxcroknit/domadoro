package com.tomatotimer.app.timer

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** PIP 창 버튼, 알림 버튼, 종료 알람을 받아서 타이머에 전달해요. */
class TimerActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_TOGGLE -> TimerEngine.toggle()
            ACTION_SKIP -> TimerEngine.skip()
            ACTION_ALARM -> TimerEngine.onAlarm()
        }
    }

    companion object {
        const val ACTION_TOGGLE = "com.tomatotimer.app.action.TOGGLE"
        const val ACTION_SKIP = "com.tomatotimer.app.action.SKIP"
        const val ACTION_ALARM = "com.tomatotimer.app.action.ALARM"

        fun pending(context: Context, action: String): PendingIntent {
            val requestCode = when (action) {
                ACTION_TOGGLE -> 1
                ACTION_SKIP -> 2
                else -> 3
            }
            val intent = Intent(context, TimerActionReceiver::class.java).setAction(action)
            return PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
