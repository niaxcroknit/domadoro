package com.tomatotimer.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.tomatotimer.app.R
import com.tomatotimer.app.data.Prefs
import com.tomatotimer.app.timer.NotificationArt
import com.tomatotimer.app.timer.Phase
import com.tomatotimer.app.timer.TimerActionReceiver
import com.tomatotimer.app.timer.TimerEngine
import com.tomatotimer.app.ui.MainActivity

/**
 * 홈 화면 위젯: 토마토, 남은 시간, 오늘 수확, 재생·멈춤 버튼.
 * 남은 시간은 시스템 시계(Chronometer)가 1초마다 줄여 그려서, 앱이 매초 위젯을 고치지 않아요.
 * 타이머 상태가 바뀔 때만 TimerEngine이 [updateAll]을 불러요.
 */
class TimerWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, appWidgetIds: IntArray) {
        if (appWidgetIds.isEmpty()) return
        manager.updateAppWidget(appWidgetIds, views(context))
    }

    companion object {
        private var focusArt: Bitmap? = null
        private var restArt: Bitmap? = null

        fun updateAll(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val ids = manager.getAppWidgetIds(ComponentName(context, TimerWidget::class.java))
                if (ids.isEmpty()) return
                manager.updateAppWidget(ids, views(context))
            } catch (_: Exception) {
                // 위젯을 지원하지 않는 기기 등
            }
        }

        /** 이 폰 홈 화면에 위젯을 바로 추가할 수 있는지 */
        fun canPin(context: Context): Boolean =
            AppWidgetManager.getInstance(context)?.isRequestPinAppWidgetSupported == true

        /** 홈 화면에 위젯 추가를 요청해요. (런처가 확인 창을 띄워요) */
        fun requestPin(context: Context): Boolean = try {
            AppWidgetManager.getInstance(context)
                ?.requestPinAppWidget(ComponentName(context, TimerWidget::class.java), null, null) == true
        } catch (_: Exception) {
            false
        }

        private fun art(resting: Boolean): Bitmap {
            val cached = if (resting) restArt else focusArt
            if (cached != null) return cached
            // 알림과 같은 토마토 그림을 위젯 크기에 맞게 줄여서(전달 용량을 줄이려고) 써요.
            val full = NotificationArt.timerFace(resting)
            val small = Bitmap.createScaledBitmap(full, 150, 165, true)
            if (small != full) full.recycle()
            if (resting) restArt = small else focusArt = small
            return small
        }

        internal fun views(context: Context): RemoteViews {
            val s = TimerEngine.state.value
            val goal = Prefs.goal.value
            val today = Prefs.todayCount()
            return RemoteViews(context.packageName, R.layout.widget_timer).apply {
                setImageViewBitmap(R.id.widget_tomato, art(s.phase == Phase.BREAK))
                setTextViewText(R.id.widget_phase, when {
                    s.isRunning -> "${s.phaseName} 중"
                    s.isMidway -> "잠깐 멈춤 · ${s.phaseName}"
                    else -> s.phaseName
                })
                setTextViewText(R.id.widget_today,
                    if (goal > 0) "오늘 $today / ${goal}개" else "오늘 ${today}개 수확")

                setViewVisibility(R.id.widget_countdown, if (s.isRunning) View.VISIBLE else View.GONE)
                setViewVisibility(R.id.widget_paused, if (s.isRunning) View.GONE else View.VISIBLE)
                setTextViewText(R.id.widget_paused, s.timeText)
                setChronometerCountDown(R.id.widget_countdown, true)
                val deadline = if (s.endAtElapsedMillis > 0L) s.endAtElapsedMillis
                    else SystemClock.elapsedRealtime() + s.remainingMillis
                setChronometer(R.id.widget_countdown, deadline, null, s.isRunning)

                setImageViewResource(R.id.widget_toggle, if (s.isRunning) R.drawable.ic_pause else R.drawable.ic_play)
                setContentDescription(R.id.widget_toggle, if (s.isRunning) "잠깐 멈춤" else "${s.phaseName} 시작")
                setOnClickPendingIntent(R.id.widget_toggle,
                    TimerActionReceiver.pending(context, TimerActionReceiver.ACTION_TOGGLE))
                setOnClickPendingIntent(R.id.widget_root, openApp(context))
            }
        }

        private fun openApp(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            return PendingIntent.getActivity(
                context, 20, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
    }
}
