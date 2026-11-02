package com.tomatotimer.app.ui

import android.Manifest
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.app.AppOpsManager
import android.graphics.Rect
import android.widget.Toast
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.tomatotimer.app.R
import com.tomatotimer.app.data.Prefs
import com.tomatotimer.app.data.ThemeMode
import com.tomatotimer.app.timer.TimerActionReceiver
import com.tomatotimer.app.timer.TimerEngine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var inPip by mutableStateOf(false)
    private var pipSource: Rect? = null

    private val askNotification = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) Toast.makeText(this,
            "알림을 허용하면 잠금화면에서도 남은 시간을 볼 수 있어요.", Toast.LENGTH_LONG).show()
    }

    /** 시작/멈춤 버튼. 처음 시작할 때 알림 권한도 같이 물어봐요. */
    private fun onPrimary() {
        if (!TimerEngine.state.value.isRunning &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            askNotification.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        TimerEngine.toggle()
    }

    private val pipSupported: Boolean
        get() = packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)

    private fun pipAllowed(): Boolean {
        if (!pipSupported) return false
        val ops = getSystemService(AppOpsManager::class.java) ?: return false
        return ops.checkOpNoThrow(AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
            android.os.Process.myUid(), packageName) == AppOpsManager.MODE_ALLOWED
    }

    override fun onStart() {
        super.onStart()
        if (TimerEngine.state.value.isRunning) com.tomatotimer.app.timer.TimerService.start(this)
        updatePipParams()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inPip = isInPictureInPictureMode
        addOnPictureInPictureModeChangedListener { info -> inPip = info.isInPictureInPictureMode }

        setContent {
            val mode by Prefs.theme.collectAsStateWithLifecycle()
            val dark = when (mode) {
                ThemeMode.AUTO -> isSystemInDarkTheme()
                ThemeMode.DAY -> false
                ThemeMode.NIGHT -> true
            }
            LaunchedEffect(dark) {
                // 위쪽은 빨강/검정 배경이라 항상 밝은 아이콘, 아래쪽은 시트 색에 맞춰요.
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                    navigationBarStyle = if (dark) {
                        SystemBarStyle.dark(Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                    },
                )
            }
            val palette = if (dark) NightPalette else DayPalette
            MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme()) {
                CompositionLocalProvider(LocalPalette provides palette) {
                    if (inPip) {
                        PipScreen()
                    } else {
                        TomatoAppContent(
                            pipSupported = pipSupported,
                            onEnterPip = ::enterPip,
                            onPrimary = ::onPrimary,
                            onFaceBounds = { if (pipSource != it) { pipSource = it; updatePipParams() } },
                        )
                    }
                }
            }
        }

        // 타이머가 돌 때만 '홈으로 나가면 자동으로 작은 창'이 켜지도록, 그리고 버튼 모양을 맞춰요.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.CREATED) {
                TimerEngine.state
                    .map { it.isRunning to it.phase }
                    .distinctUntilChanged()
                    .collect { updatePipParams() }
            }
        }
    }

    private fun pipParams(): PictureInPictureParams {
        val running = TimerEngine.state.value.isRunning
        val toggle = RemoteAction(
            Icon.createWithResource(this, if (running) R.drawable.ic_pause else R.drawable.ic_play),
            if (running) "잠깐 멈춤" else "시작",
            if (running) "타이머 잠깐 멈춤" else "타이머 시작",
            TimerActionReceiver.pending(this, TimerActionReceiver.ACTION_TOGGLE),
        )
        // 작은 창은 단순하게: 조작 버튼은 재생/멈춤 하나만.
        // Android 작은 창은 앱이 터치를 직접 받지 못해서, 토마토를 누르면 시스템이 이 버튼 하나를 토마토 위에 띄워요.
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(1, 1))
            .setActions(listOf(toggle))
        pipSource?.takeUnless { it.isEmpty }?.let { builder.setSourceRectHint(it) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(running && pipAllowed())
            builder.setSeamlessResizeEnabled(false)
        }
        return builder.build()
    }

    private fun updatePipParams() {
        if (!pipSupported) return
        try {
            setPictureInPictureParams(pipParams())
        } catch (_: IllegalStateException) {
            // 일부 기기에서 PIP가 꺼져 있을 때
        }
    }

    private fun enterPip() {
        if (!pipAllowed()) {
            Toast.makeText(this, "작은 창이 허용되지 않았어요. 알림에서 남은 시간을 확인할 수 있어요.", Toast.LENGTH_LONG).show()
            return
        }
        try {
            if (!enterPictureInPictureMode(pipParams())) {
                Toast.makeText(this, "작은 창을 열 수 없어요. 알림에서 타이머를 확인해 주세요.", Toast.LENGTH_LONG).show()
            }
        } catch (_: IllegalStateException) {
            Toast.makeText(this, "알림에서 타이머를 확인해 주세요.", Toast.LENGTH_SHORT).show()
        } catch (_: SecurityException) {
            Toast.makeText(this, "작은 창 권한을 확인해 주세요.", Toast.LENGTH_SHORT).show()
        }
    }

    /** Android 11 이하: 홈 버튼을 누를 때 직접 작은 창으로 들어가요. (12 이상은 자동) */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && TimerEngine.state.value.isRunning) {
            enterPip()
        }
    }
}

@Composable
fun TomatoAppContent(
    pipSupported: Boolean,
    onEnterPip: () -> Unit,
    onPrimary: () -> Unit,
    startTab: Tab = Tab.TIMER,
    onFaceBounds: (Rect) -> Unit = {},
) {
    var tab by rememberSaveable { mutableStateOf(startTab) }
    val p = LocalPalette.current
    val bottomBar: @Composable () -> Unit = { BottomNav(current = tab, onSelect = { tab = it }) }

    Box(
        Modifier
            .fillMaxSize()
            .background(p.bg),
    ) {
        Backdrop()
        when (tab) {
            Tab.TIMER -> TimerScreen(
                pipSupported = pipSupported,
                onEnterPip = onEnterPip,
                onPrimary = onPrimary,
                bottomBar = bottomBar,
                onFaceBounds = onFaceBounds,
            )
            Tab.BASKET -> BasketScreen(bottomBar = bottomBar)
            Tab.SETTINGS -> SettingsScreen(bottomBar = bottomBar)
        }
    }
}

