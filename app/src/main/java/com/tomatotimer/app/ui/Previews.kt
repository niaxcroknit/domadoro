package com.tomatotimer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview

/*
 * Android Studio 미리보기용 화면들이에요.
 * 이 파일을 열고 오른쪽 위 'Split' 또는 'Design'을 누르면 보여요.
 * (실제 앱 동작에는 영향이 없어요.)
 */

@Composable
private fun PreviewFrame(palette: Palette, tab: Tab) {
    MaterialTheme {
        CompositionLocalProvider(LocalPalette provides palette) {
            TomatoAppContent(pipSupported = true, onEnterPip = {}, onPrimary = {}, startTab = tab)
        }
    }
}

@Preview(name = "타이머 · 낮", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun TimerDayPreview() = PreviewFrame(DayPalette, Tab.TIMER)

@Preview(name = "타이머 · 밤", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun TimerNightPreview() = PreviewFrame(NightPalette, Tab.TIMER)

@Preview(name = "바구니 · 낮", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun BasketDayPreview() = PreviewFrame(DayPalette, Tab.BASKET)

@Preview(name = "바구니 · 밤", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun BasketNightPreview() = PreviewFrame(NightPalette, Tab.BASKET)

@Preview(name = "설정 · 낮", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
private fun SettingsDayPreview() = PreviewFrame(DayPalette, Tab.SETTINGS)

@Preview(name = "작은 창(PIP) · 낮", widthDp = 200, heightDp = 200)
@Composable
private fun PipDayPreview() {
    CompositionLocalProvider(LocalPalette provides DayPalette) {
        Box(Modifier.fillMaxSize().background(DayPalette.bg)) { PipScreen() }
    }
}

@Preview(name = "작은 창(PIP) · 밤", widthDp = 200, heightDp = 200)
@Composable
private fun PipNightPreview() {
    CompositionLocalProvider(LocalPalette provides NightPalette) {
        Box(Modifier.fillMaxSize().background(NightPalette.bg)) { PipScreen() }
    }
}
