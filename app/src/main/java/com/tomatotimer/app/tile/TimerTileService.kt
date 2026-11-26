package com.tomatotimer.app.tile

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.tomatotimer.app.R
import com.tomatotimer.app.timer.TimerEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 알림창을 내렸을 때 보이는 빠른 설정 버튼.
 * 누르면 바로 시작·멈춤, 버튼 아래에 '집중 중 · 12분 남음'처럼 보여줘요.
 */
class TimerTileService : TileService() {
    private var scope: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        scope?.cancel()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { sc ->
            sc.launch {
                // 1분 단위로만 다시 그려요.
                TimerEngine.state
                    .map { Triple(it.phase to it.longBreak, it.isRunning, (it.remainingSeconds + 59) / 60) }
                    .distinctUntilChanged()
                    .collect { render() }
            }
        }
    }

    override fun onStopListening() {
        scope?.cancel()
        scope = null
        super.onStopListening()
    }

    override fun onClick() {
        super.onClick()
        TimerEngine.toggle()
        render()
    }

    private fun render() {
        val tile = qsTile ?: return
        val s = TimerEngine.state.value
        val minutesLeft = (s.remainingSeconds + 59) / 60
        tile.state = if (s.isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = getString(R.string.tile_label)
        val sub = when {
            s.isRunning -> "${s.phaseName} 중 · ${minutesLeft}분 남음"
            s.isMidway -> "멈춤 · ${s.timeText}"
            else -> "눌러서 ${s.phaseName} 시작"
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = sub
        }
        tile.contentDescription = "도마도로, $sub"
        tile.icon = Icon.createWithResource(this, R.drawable.ic_notify)
        tile.updateTile()
    }

    companion object {
        /** 타이머 상태가 바뀌면 빠른 설정 버튼도 다시 그리게 해요. */
        fun refresh(context: Context) {
            try {
                TileService.requestListeningState(context, ComponentName(context, TimerTileService::class.java))
            } catch (_: Exception) {
            }
        }

        /** Android 13부터: 빠른 설정에 버튼 추가를 바로 요청할 수 있어요. */
        fun canRequestAdd(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

        fun requestAdd(context: Context, onResult: (Boolean) -> Unit) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                onResult(false)
                return
            }
            val sbm = context.getSystemService(StatusBarManager::class.java)
            if (sbm == null) {
                onResult(false)
                return
            }
            try {
                sbm.requestAddTileService(
                    ComponentName(context, TimerTileService::class.java),
                    context.getString(R.string.tile_label),
                    Icon.createWithResource(context, R.drawable.ic_notify),
                    context.mainExecutor,
                ) { result ->
                    onResult(
                        result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ||
                            result == StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED,
                    )
                }
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }
}
