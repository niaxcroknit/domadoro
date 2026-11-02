package com.tomatotimer.app

import android.app.Application
import com.tomatotimer.app.data.Prefs
import com.tomatotimer.app.sound.NoisePlayer
import com.tomatotimer.app.timer.Notifications
import com.tomatotimer.app.timer.Phase
import com.tomatotimer.app.timer.TimerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TomatoApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        Notifications.createChannels(this)
        TimerEngine.init(this)

        // 백색소음: 켜져 있으면 재생하고, '휴식 땐 멈추기'면 휴식 동안 조용히.
        appScope.launch {
            combine(
                Prefs.noise,
                TimerEngine.state.map { it.phase }.distinctUntilChanged(),
            ) { noise, phase ->
                val play = noise.on && !(noise.breakOff && phase == Phase.BREAK)
                Triple(play, noise.type, noise.volume)
            }
                .distinctUntilChanged()
                .collect { (play, type, volume) -> NoisePlayer.update(play, type, volume) }
        }
    }
}
