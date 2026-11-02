package com.tomatotimer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomatotimer.app.timer.TimerEngine

/**
 * PIP(작은 창)에 보이는 화면. 토마토와 남은 시간만.
 * 작은 창에서는 보조 글자가 읽히지 않을 만큼 작아져서 넣지 않아요.
 * 일시정지·건너뛰기 버튼은 창을 누르면 안드로이드가 위에 띄워줘요.
 */
@Composable
fun PipScreen() {
    val p = LocalPalette.current
    val s by TimerEngine.state.collectAsStateWithLifecycle()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(p.bg),
        contentAlignment = Alignment.Center,
    ) {
        val face = min(maxWidth, maxHeight * (300f / 330f)) * 0.94f
        TomatoTimerFace(state = s, size = face, label = null)
    }
}
