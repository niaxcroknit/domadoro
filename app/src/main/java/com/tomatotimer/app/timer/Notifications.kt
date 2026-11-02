package com.tomatotimer.app.timer

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tomatotimer.app.R
import com.tomatotimer.app.data.Prefs
import com.tomatotimer.app.ui.MainActivity

object Notifications {
    const val CHANNEL_TIMER = "timer"
    // 소리·진동은 앱이 직접(집중 끝/휴식 끝을 구분해서) 내고, 알림 자체는 조용히 화면에만 띄워요.
    const val CHANNEL_DONE = "harvest"
    const val ID_TIMER = 1
    private const val ID_DONE = 2
    private val focusArt by lazy { NotificationArt.timerFace(false) }
    private val restArt by lazy { NotificationArt.timerFace(true) }
    private const val TOMATO = 0xFFFF594B.toInt()

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.deleteNotificationChannel("done") // 이전 버전에서 만든 채널 정리
        val timer = NotificationChannel(CHANNEL_TIMER, "타이머 진행", NotificationManager.IMPORTANCE_LOW).apply {
            description = "집중하는 동안 남은 시간을 보여줘요"
            setShowBadge(false)
        }
        val done = NotificationChannel(CHANNEL_DONE, "토마토 수확", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "집중이나 휴식이 끝나면 화면 위에 알려줘요"
            setSound(null, null)
            enableVibration(false)
        }
        nm.createNotificationChannels(listOf(timer, done))
    }

    /** 타이머가 도는 동안(또는 잠깐 멈춘 동안) 보이는 알림 */
    fun timer(context: Context, s: TimerState): Notification {
        val title = "도마도로 · " + if (s.phase == Phase.FOCUS) "집중" else "휴식"
        val builder = NotificationCompat.Builder(context, CHANNEL_TIMER)
            .setSmallIcon(R.drawable.ic_notify)
            .setColor(TOMATO)
            .setContentTitle(title)
            .setContentIntent(openApp(context))
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setSubText(if (s.isRunning) "진행 중" else "일시정지")
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (s.isRunning) {
            // 시스템이 알아서 1초마다 줄어드는 시계를 그려줘서, 앱이 매초 알림을 고칠 필요가 없어요.
            builder.setOngoing(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setShowWhen(true)
                .setWhen(System.currentTimeMillis() + s.remainingMillis)
                .setContentText("남은 시간 · 눌러서 타이머 열기")
        } else {
            builder.setOngoing(false)
                .setShowWhen(false)
                .setContentText("잠깐 멈춤 · ${s.timeText} 남음")
        }
        val content = timerContent(context, s)
        builder.setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(content)
            .setCustomBigContentView(timerContent(context, s, expanded = true))
        return builder.build()
    }

    private fun timerContent(context: Context, s: TimerState, expanded: Boolean = false): RemoteViews =
        RemoteViews(context.packageName, if (expanded) R.layout.notification_timer_expanded else R.layout.notification_timer).apply {
            setImageViewResource(R.id.notification_toggle, if (s.isRunning) R.drawable.ic_pause else R.drawable.ic_play)
            setContentDescription(R.id.notification_toggle, if (s.isRunning) "잠깐 멈춤" else "계속하기")
            setOnClickPendingIntent(R.id.notification_toggle, TimerActionReceiver.pending(context, TimerActionReceiver.ACTION_TOGGLE))
            setOnClickPendingIntent(R.id.notification_skip, TimerActionReceiver.pending(context, TimerActionReceiver.ACTION_SKIP))
            setImageViewBitmap(R.id.notification_tomato, if (s.phase == Phase.BREAK) restArt else focusArt)
            setTextViewText(R.id.notification_phase,
                if (!s.isRunning) "잠깐 멈춤" else if (s.phase == Phase.BREAK) "휴식 중" else "집중 중")
            setViewVisibility(R.id.notification_countdown, if (s.isRunning) View.VISIBLE else View.GONE)
            setViewVisibility(R.id.notification_paused, if (s.isRunning) View.GONE else View.VISIBLE)
            setTextViewText(R.id.notification_paused, s.timeText)
            setBoolean(R.id.notification_countdown, "setCountDown", true)
            val deadline = if (s.endAtElapsedMillis > 0L) s.endAtElapsedMillis
                else SystemClock.elapsedRealtime() + s.remainingMillis
            setChronometer(R.id.notification_countdown, deadline, null, s.isRunning)
        }

    fun showFinished(context: Context, s: TimerState, autoStarted: Boolean) {
        if (!canNotify(context)) return
        val d = Prefs.durations.value
        val (title, text) = if (s.phase == Phase.FOCUS) {
            "토마토 하나 수확!" to when {
                d.restMin <= 0 -> "집중 끝. 수고했어요!"
                autoStarted -> "휴식 ${d.restMin}분을 시작했어요."
                else -> "집중 끝. ${d.restMin}분 쉬어가요."
            }
        } else {
            "휴식 끝!" to if (autoStarted) "다음 집중 ${d.focusMin}분을 시작했어요." else "다음 토마토를 따러 가볼까요?"
        }
        val n = NotificationCompat.Builder(context, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_notify)
            .setColor(TOMATO)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(ID_DONE, n)
    }

    fun notifyTimer(context: Context, s: TimerState) {
        if (!canNotify(context)) return
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(context).notify(ID_TIMER, timer(context, s))
    }

    fun cancelTimer(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_TIMER)
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun openApp(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            10,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
