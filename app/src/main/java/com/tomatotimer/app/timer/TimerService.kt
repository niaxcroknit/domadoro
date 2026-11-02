package com.tomatotimer.app.timer

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 타이머가 도는 동안 앱이 꺼지지 않게 붙잡아 두는 서비스.
 * 상단 알림에 남은 시간을 보여주고, 멈추거나 끝나면 스스로 내려가요.
 */
class TimerService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var watchJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!TimerEngine.state.value.isRunning) {
            stopSelf()
            return START_NOT_STICKY
        }
        goForeground(TimerEngine.state.value)

        if (watchJob == null) {
            watchJob = scope.launch {
                // 매 틱마다가 아니라, 상태가 '바뀔 때만' 알림을 고쳐요.
                TimerEngine.state
                    .map { Triple(it.phase, it.isRunning, it.endAtElapsedMillis) }
                    .distinctUntilChanged()
                    .collect {
                        val s = TimerEngine.state.value
                        if (s.isRunning) {
                            goForeground(s)
                        } else {
                            ServiceCompat.stopForeground(this@TimerService, ServiceCompat.STOP_FOREGROUND_DETACH)
                            if (s.isMidway) {
                                Notifications.notifyTimer(this@TimerService, s)
                            } else {
                                Notifications.cancelTimer(this@TimerService)
                            }
                            stopSelf()
                        }
                    }
            }
        }
        return START_STICKY
    }

    private fun goForeground(s: TimerState) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        ServiceCompat.startForeground(this, Notifications.ID_TIMER, Notifications.timer(this, s), type)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        fun start(context: Context) {
            try {
                ContextCompat.startForegroundService(context, Intent(context, TimerService::class.java))
            } catch (_: Exception) {
                // 백그라운드 시작이 막힌 드문 경우: 타이머 자체는 계속 돌고, 알람으로 끝을 알려줘요.
            }
        }
    }
}
