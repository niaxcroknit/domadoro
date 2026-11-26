package com.tomatotimer.app.ui

import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.takahirom.roborazzi.captureRoboImage
import com.tomatotimer.app.data.Prefs
import com.tomatotimer.app.data.Session
import com.tomatotimer.app.widget.TimerWidget
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/**
 * 실제 폰 없이 화면을 PNG로 그려 보는 테스트. 결과는 app/build/screens/ 에 생겨요.
 * (GitHub Actions에서도 돌려서 'domadoro-screens'로 올려요)
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w390dp-h844dp-xxhdpi")
class ScreenshotTest {
    @get:Rule
    val compose = createComposeRule()

    private val today = LocalDate.now().toEpochDay()

    @Before
    fun seed() {
        val labels = listOf("공부", "업무", "독서", null)
        val counts = mapOf(0 to 3, 1 to 6, 2 to 2, 3 to 4, 5 to 7, 6 to 1, 8 to 3, 9 to 6, 12 to 2, 15 to 5, 16 to 6, 20 to 1)
        val list = counts.flatMap { (back, n) ->
            (0 until n).map { i -> Session(today - back, 25, labels[(back + i) % labels.size]) }
        }
        Prefs.replaceSessionsForTest(list)
        Prefs.setGoal(6)
        Prefs.setCurrentLabel("공부")
        Prefs.markCelebrated(today)
    }

    private fun app(palette: Palette, tab: Tab) {
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalPalette provides palette) {
                    TomatoAppContent(pipSupported = true, onEnterPip = {}, onPrimary = {}, startTab = tab)
                }
            }
        }
    }

    private fun shot(name: String) = compose.onRoot().captureRoboImage("build/screens/$name.png")

    @Test fun timerDay() { app(DayPalette, Tab.TIMER); shot("01-timer-day") }

    @Test fun timerNight() { app(NightPalette, Tab.TIMER); shot("02-timer-night") }

    @Test fun basketWeek() { app(DayPalette, Tab.BASKET); shot("03-basket-week") }

    @Test fun basketMonth() {
        app(DayPalette, Tab.BASKET)
        compose.onNodeWithText("한 달").performClick()
        compose.waitForIdle()
        shot("04-basket-month")
        compose.onNodeWithText("무엇에 집중했나요").performScrollTo()
        shot("05-basket-labels")
    }

    @Test fun basketMonthNight() {
        app(NightPalette, Tab.BASKET)
        compose.onNodeWithText("한 달").performClick()
        compose.waitForIdle()
        shot("06-basket-month-night")
    }

    @Test fun settings() {
        app(DayPalette, Tab.SETTINGS)
        compose.onNodeWithText("긴 휴식 간격").performScrollTo()
        shot("07-settings-time")
        compose.onNodeWithText("집중 라벨").performScrollTo()
        shot("08-settings-goal-labels")
        compose.onNodeWithText("앱을 열지 않고 시작하기").performScrollTo()
        shot("09-settings-dnd-widget")
    }

    @Test fun celebration() {
        Prefs.markCelebrated(-1L)
        app(DayPalette, Tab.TIMER)
        compose.waitForIdle()
        shot("10-goal-celebration")
    }

    @Test fun labelMenu() {
        app(DayPalette, Tab.TIMER)
        compose.onNodeWithText("# 공부").performClick()
        compose.waitForIdle()
        shot("11-label-menu")
    }

    @Test fun widget() {
        compose.setContent {
            Box(Modifier.background(Color(0xFF9AA3B5)).padding(16.dp)) {
                AndroidView(
                    factory = { ctx -> TimerWidget.views(ctx).apply(ctx, FrameLayout(ctx)) },
                    modifier = Modifier.size(width = 330.dp, height = 96.dp),
                )
            }
        }
        shot("12-widget")
    }
}
