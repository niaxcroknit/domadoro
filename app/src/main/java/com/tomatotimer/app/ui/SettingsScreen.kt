package com.tomatotimer.app.ui

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.net.Uri
import android.provider.Settings
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomatotimer.app.R
import com.tomatotimer.app.data.Durations
import com.tomatotimer.app.data.NoiseType
import com.tomatotimer.app.data.Prefs
import com.tomatotimer.app.data.ThemeMode
import com.tomatotimer.app.sound.Alerts
import com.tomatotimer.app.timer.Phase
import com.tomatotimer.app.timer.TimerEngine

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(bottomBar: @Composable () -> Unit) {
    val base = LocalPalette.current
    val colors = if (base.isDark) base else base.copy(bg = Color(0xFFFFF7F4), ink = base.sheetInk, sub = base.sheetSub)
    CompositionLocalProvider(LocalPalette provides colors) {
        Box(Modifier.fillMaxSize().background(colors.bg)) { SettingsContent(bottomBar) }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsContent(bottomBar: @Composable () -> Unit) {
    val p = LocalPalette.current
    val context = LocalContext.current
    val theme by Prefs.theme.collectAsStateWithLifecycle()
    val d by Prefs.durations.collectAsStateWithLifecycle()
    val alerts by Prefs.alerts.collectAsStateWithLifecycle()
    val noise by Prefs.noise.collectAsStateWithLifecycle()

    ScreenFrame(
        sheetFills = true,
        bottomBar = bottomBar,
        top = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("설정", color = p.ink, fontFamily = BlackHanSans, fontWeight = FontWeight.Bold, fontSize = 28.sp)
                    VSpace(6.dp)
                    Text("나에게 맞는 속도로, 한 알씩.", color = p.sub, fontSize = 13.sp)
                }
                TomatoIcon(p.full, p.fullLeaf, 48.dp)
            }
        },
        sheet = {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                // ── 테마
                SectionTitle("화면 테마")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("자동", selected = theme == ThemeMode.AUTO) { Prefs.setTheme(ThemeMode.AUTO) }
                    Pill("낮 (토마토)", selected = theme == ThemeMode.DAY) { Prefs.setTheme(ThemeMode.DAY) }
                    Pill("밤", selected = theme == ThemeMode.NIGHT) { Prefs.setTheme(ThemeMode.NIGHT) }
                }
                Hint("자동은 휴대폰의 다크 모드를 따라가요.")

                Divider()
                // ── 시간
                SectionTitle("시간")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(25 to 5, 50 to 10, 90 to 15).forEach { (f, r) ->
                        Pill("${f}분 · ${r}분", selected = d.focusMin == f && d.restMin == r) {
                            change(Durations(f, r))
                        }
                    }
                }
                VSpace(6.dp)
                StepperRow(
                    title = "집중",
                    value = d.focusMin,
                    canMinus = d.focusMin > 1,
                    canPlus = d.focusMin < 120,
                    onMinus = { change(d.copy(focusMin = if (d.focusMin > 5) d.focusMin - 5 else d.focusMin - 1)) },
                    onPlus = { change(d.copy(focusMin = if (d.focusMin >= 5) d.focusMin + 5 else d.focusMin + 1)) },
                )
                StepperRow(
                    title = "휴식",
                    value = d.restMin,
                    canMinus = d.restMin > 0,
                    canPlus = d.restMin < 30,
                    onMinus = { change(d.copy(restMin = d.restMin - 1)) },
                    onPlus = { change(d.copy(restMin = d.restMin + 1)) },
                )
                Hint("타이머가 멈춰 있을 때 바로 반영돼요.")

                Divider()
                // ── 끝날 때 알려주기
                SectionTitle("끝날 때 알려주기")
                ToggleRow(
                    title = "소리",
                    hint = "집중 끝은 올라가는 '딩-동-댕', 휴식 끝은 '띵띵-띠링'. 두 번씩 울려요.",
                    checked = alerts.sound,
                ) { Prefs.setAlerts(alerts.copy(sound = it)) }
                ToggleRow(
                    title = "진동",
                    hint = "집중 끝은 짧게 두 번, 휴식 끝은 길게 세 번. 주머니 속에서도 구분돼요.",
                    checked = alerts.vibrate,
                ) { Prefs.setAlerts(alerts.copy(vibrate = it)) }
                ToggleRow(
                    title = "자동으로 이어서",
                    hint = "집중이 끝나면 휴식이, 휴식이 끝나면 집중이 바로 시작돼요.",
                    checked = alerts.autoNext,
                ) { Prefs.setAlerts(alerts.copy(autoNext = it)) }
                VSpace(4.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SoftButton("집중 끝 들어보기") {
                        Alerts.chime(Phase.FOCUS)
                        Alerts.vibrate(context, Phase.FOCUS)
                    }
                    SoftButton("휴식 끝 들어보기") {
                        Alerts.chime(Phase.BREAK)
                        Alerts.vibrate(context, Phase.BREAK)
                    }
                }
                Hint("화면이 꺼져 있어도 정해진 시각에 소리·진동과 함께 알림이 떠요.")

                Divider()
                // ── 백색소음
                SectionTitle("백색소음")
                ToggleRow(
                    title = "켜기",
                    hint = "타이머 화면의 스피커 버튼으로도 켜고 끌 수 있어요.",
                    checked = noise.on,
                ) { Prefs.setNoise(noise.copy(on = it)) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoiseType.entries.forEach { t ->
                        Pill(t.label, selected = noise.type == t) { Prefs.setNoise(noise.copy(type = t)) }
                    }
                }
                VSpace(8.dp)
                Text("소리 크기", color = p.sheetInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Slider(
                    value = noise.volume,
                    onValueChange = { Prefs.setNoise(noise.copy(volume = it)) },
                    colors = SliderDefaults.colors(
                        thumbColor = p.primary,
                        activeTrackColor = p.primary,
                        inactiveTrackColor = p.barTrack,
                    ),
                )
                ToggleRow(
                    title = "휴식 땐 멈추기",
                    hint = "소리가 멈추면 쉬는 시간이라는 신호예요.",
                    checked = noise.breakOff,
                ) { Prefs.setNoise(noise.copy(breakOff = it)) }

                Divider()
                // ── 작은 창
                SectionTitle("앱 밖에서도 이어서")
                Hint("진행 중에 홈으로 나가면 작은 창으로 이어져요. 토마토를 누르면 재생·멈춤 버튼이 떠요.")
                Hint("화면을 잠그면 시스템 알림으로 남은 시간을 확인해요. 표시 위치와 AOD 표시는 기기·잠금화면 설정에 따라 달라요.")
                VSpace(10.dp)
                if (context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                    SoftButton("작은 창 허용 설정") {
                        val intent = Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS",
                            Uri.parse("package:" + context.packageName))
                        runCatching { context.startActivity(intent) }.onFailure {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + context.packageName)))
                        }
                    }
                    VSpace(8.dp)
                }
                SoftButton("잠금화면 알림 설정") {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                }

                Divider()
                // ── 정확한 시간 · 배터리 (설정에서 돌아올 때마다 상태를 다시 확인)
                var resumeTick by remember { mutableIntStateOf(0) }
                LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { resumeTick++ }
                val exactOk = remember(resumeTick) { canScheduleExact(context) }
                SectionTitle("정확한 시간에 알림 받기")
                if (exactOk) {
                    Hint("켜져 있어요. 화면이 오래 꺼져 있어도 끝나는 시각에 바로 알려요.")
                } else {
                    Hint("지금은 꺼져 있어요. 화면이 오래 꺼져 있으면 종료 알림이 몇 분 늦을 수 있어요.")
                    VSpace(10.dp)
                    SoftButton("정확한 알림 허용하기") {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            runCatching {
                                context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:" + context.packageName)))
                            }
                        }
                    }
                }
                VSpace(4.dp)
                Hint("삼성·샤오미 등 일부 폰은 배터리 절약 기능이 타이머를 멈출 수 있어요. 도마도로를 '제한 없음'(최적화 안 함)으로 두면 안전해요.")
                VSpace(10.dp)
                SoftButton("배터리 절약 예외 설정") {
                    runCatching { context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
                        .onFailure {
                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.parse("package:" + context.packageName)))
                        }
                }
                VSpace(16.dp)
            }
        },
    )
}

private fun canScheduleExact(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
        context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true

private fun change(d: Durations) {
    Prefs.setDurations(d)
    TimerEngine.refreshIfIdle()
}

@Composable
private fun SectionTitle(text: String) {
    val p = LocalPalette.current
    Text(text, color = p.sheetInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    VSpace(10.dp)
}

@Composable
private fun Hint(text: String) {
    val p = LocalPalette.current
    VSpace(6.dp)
    Text(text, color = p.sheetSub, fontSize = 13.sp, lineHeight = 19.sp)
}

@Composable
private fun Divider() {
    val p = LocalPalette.current
    VSpace(18.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(p.barTrack),
    )
    VSpace(18.dp)
}

@Composable
private fun SoftButton(text: String, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(p.barTrack)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
    ) {
        Text(text, color = p.sheetInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ToggleRow(title: String, hint: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val p = LocalPalette.current
    Row(
        modifier = Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = p.sheetInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(hint, color = p.sheetSub, fontSize = 13.sp, lineHeight = 19.sp)
        }
        HSpace(12.dp)
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = p.primaryInk,
                checkedTrackColor = p.primary,
                uncheckedThumbColor = p.sheet,
                uncheckedTrackColor = p.empty,
                uncheckedBorderColor = p.empty,
            ),
        )
    }
}

@Composable
private fun StepperRow(
    title: String,
    value: Int,
    canMinus: Boolean,
    canPlus: Boolean,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
) {
    val p = LocalPalette.current
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = p.sheetInk, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        StepButton(R.drawable.ic_minus, "$title 시간 줄이기", canMinus, onMinus)
        Text(
            text = "${value}분",
            color = p.sheetInk,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(64.dp),
        )
        StepButton(R.drawable.ic_plus, "$title 시간 늘리기", canPlus, onPlus)
    }
}

@Composable
private fun StepButton(icon: Int, description: String, enabled: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        modifier = Modifier
            .size(48.dp)
            .alpha(if (enabled) 1f else 0.35f)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(p.chipOnBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(icon), contentDescription = description, tint = p.chipOnInk, modifier = Modifier.size(18.dp))
        }
    }
}

