package com.tomatotimer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import android.graphics.Rect
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomatotimer.app.R
import com.tomatotimer.app.data.Prefs
import com.tomatotimer.app.data.ThemeMode
import com.tomatotimer.app.timer.Phase
import com.tomatotimer.app.timer.TimerEngine
import java.time.LocalDate

@Composable
fun TimerScreen(
    pipSupported: Boolean,
    onEnterPip: () -> Unit,
    onPrimary: () -> Unit,
    bottomBar: @Composable () -> Unit,
    onFaceBounds: (Rect) -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    val p = LocalPalette.current
    val s by TimerEngine.state.collectAsStateWithLifecycle()
    val sessions by Prefs.sessions.collectAsStateWithLifecycle()
    val durations by Prefs.durations.collectAsStateWithLifecycle()
    val noise by Prefs.noise.collectAsStateWithLifecycle()
    val goal by Prefs.goal.collectAsStateWithLifecycle()
    val labels by Prefs.labels.collectAsStateWithLifecycle()
    val currentLabel by Prefs.currentLabel.collectAsStateWithLifecycle()
    val todayCount = sessions.count { it.epochDay == LocalDate.now().toEpochDay() }
    val resting = s.phase == Phase.BREAK
    val primaryLabel = when {
        s.isRunning -> "잠깐 멈추기"
        s.isMidway -> "이어서 하기"
        resting -> "${s.phaseName} 시작"
        else -> "집중 시작"
    }
    // 긴 휴식까지 남은 집중 수 (긴 휴식을 쓰지 않으면 null)
    val untilLong = if (durations.longEvery > 0 && durations.longRestMin > 0) {
        (durations.longEvery - s.cycleDone).coerceAtLeast(1)
    } else {
        null
    }
    ScreenFrame(
        sheetFills = false,
        bottomBar = bottomBar,
        top = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("도마도로", color = p.ink, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1).sp)
                }
                CircleIconButton(
                    if (p.isDark) R.drawable.ic_sun else R.drawable.ic_moon,
                    if (p.isDark) "낮 모드로 바꾸기" else "밤 모드로 바꾸기",
                    p.iconBg, p.iconInk,
                ) { Prefs.setTheme(if (p.isDark) ThemeMode.DAY else ThemeMode.NIGHT) }
                if (pipSupported) {
                    HSpace(8.dp)
                    CircleIconButton(R.drawable.ic_pip, "작은 창으로 보기", p.iconBg, p.iconInk, onEnterPip)
                }
            }
            VSpace(22.dp)
            Text(when {
                    s.longBreak -> "여기까지 잘 왔어요.\n푹 쉬어가요."
                    resting -> "잠깐 쉬어도,\n괜찮으니까."
                    else -> "딱 하나에,\n푹 빠질 시간."
                },
                color = p.ink, fontSize = 30.sp, lineHeight = 39.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-1.5).sp)
            VSpace(8.dp)
            // 빨간 배경 위 연한 작은 글자는 잘 안 읽혀서, 조금 키우고 은은한 칩 위에 흰 글자로.
            Row(
                Modifier.clip(RoundedCornerShape(50))
                    .background(if (p.isDark) Color.White.copy(alpha = 0.08f) else Color(0x24500A10))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(6.dp).clip(RoundedCornerShape(50)).background(p.leaf))
                HSpace(7.dp)
                Text(when {
                        s.longBreak -> "긴 휴식 · ${durations.longRestMin}분"
                        resting -> "지금은 충전 중 · 휴식"
                        currentLabel != null -> "오늘의 ${todayCount + 1}번째 몰입 · $currentLabel"
                        else -> "오늘의 ${todayCount + 1}번째 몰입 · 집중"
                    },
                    color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
            BoxWithConstraints(Modifier.fillMaxWidth().weight(1f).onGloballyPositioned {
                val b = it.boundsInWindow()
                onFaceBounds(Rect(b.left.toInt(), b.top.toInt(), b.right.toInt(), b.bottom.toInt()))
            }, contentAlignment = Alignment.Center) {
                val faceWidth = min(min(maxWidth, maxHeight * (300f / 330f)), 340.dp)
                // 토마토 안에는 시간만. 작은 보조 문구는 읽히지 않아서 넣지 않아요.
                TomatoTimerFace(s, faceWidth, label = null)
            }
            VSpace(6.dp)
        },
        sheet = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (goal > 0) "오늘의 수확 · 목표 ${goal}개" else "오늘의 수확", color = p.sheetSub, fontSize = 13.sp)
                    VSpace(3.dp)
                    Text(
                        if (goal > 0) "$todayCount / ${goal}개" else "${todayCount}개의 토마토",
                        color = p.sheetInk, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                    )
                }
                TomatoBasketRow(todayCount, slots = if (goal > 0) goal else 5)
            }
            VSpace(10.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    buildString {
                        append("집중 ${durations.focusMin}분 · 휴식 ${durations.restMin}분")
                        if (untilLong != null) append("\n긴 휴식까지 ${untilLong}번")
                    },
                    color = p.sheetSub, fontSize = 13.sp, lineHeight = 18.sp,
                    modifier = Modifier.weight(1f),
                )
                HSpace(8.dp)
                LabelPicker(
                    labels = labels,
                    current = currentLabel,
                    onPick = { Prefs.setCurrentLabel(it) },
                    onEdit = onOpenSettings,
                )
            }
            VSpace(10.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(60.dp).clip(RoundedCornerShape(20.dp)).background(p.barTrack)
                        .clickable(role = Role.Button, onClick = { TimerEngine.reset() }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_reset), "처음부터", tint = p.sheetInk, modifier = Modifier.size(23.dp))
                }
                HSpace(12.dp)
                Row(
                    Modifier.weight(1f).height(60.dp).shadow(3.dp, RoundedCornerShape(20.dp))
                        .clip(RoundedCornerShape(20.dp)).background(Brush.verticalGradient(
                            if (p.isDark) listOf(Color(0xFFC4FFE8), p.primary)
                            else listOf(Color(0xFFFF7862), p.primary)))
                        .clickable(role = Role.Button, onClick = onPrimary),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(painterResource(if (s.isRunning) R.drawable.ic_pause else R.drawable.ic_play),
                        null, tint = p.primaryInk, modifier = Modifier.size(20.dp))
                    HSpace(10.dp)
                    Text(primaryLabel, color = p.primaryInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
            VSpace(14.dp)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(p.barTrack)
                    .clickable(role = Role.Button, onClick = { Prefs.setNoise(noise.copy(on = !noise.on)) })
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(if (noise.on) R.drawable.ic_volume_on else R.drawable.ic_volume_off),
                    if (noise.on) "백색소음 끄기" else "백색소음 켜기", tint = p.sheetInk, modifier = Modifier.size(18.dp))
                HSpace(9.dp)
                Text(if (noise.on) noise.type.label else "백색소음", color = p.sheetInk, fontSize = 14.sp,
                    modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text(if (noise.on) "ON" else "OFF", color = p.sheetSub, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            VSpace(5.dp)
        },
    )
}

/** 무엇에 집중하는지 고르는 작은 버튼. 누르면 라벨 목록이 내려와요. */
@Composable
private fun LabelPicker(
    labels: List<String>,
    current: String?,
    onPick: (String?) -> Unit,
    onEdit: () -> Unit,
) {
    val p = LocalPalette.current
    var open by remember { mutableStateOf(false) }
    Box {
        Row(
            Modifier
                .height(40.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (current != null) p.chipOnBg else p.barTrack)
                .clickable(role = Role.Button, onClickLabel = "집중 라벨 고르기") { open = true }
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                current?.let { "# $it" } ?: "# 라벨",
                color = if (current != null) p.chipOnInk else p.sheetInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            HSpace(4.dp)
            Text("▾", color = if (current != null) p.chipOnInk else p.sheetSub, fontSize = 13.sp)
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = p.sheet,
        ) {
            DropdownMenuItem(
                text = { Text("라벨 없음", color = p.sheetInk, fontSize = 15.sp,
                    fontWeight = if (current == null) FontWeight.Bold else FontWeight.Normal) },
                onClick = { onPick(null); open = false },
            )
            labels.forEach { label ->
                DropdownMenuItem(
                    text = { Text("# $label", color = p.sheetInk, fontSize = 15.sp,
                        fontWeight = if (label == current) FontWeight.Bold else FontWeight.Normal) },
                    onClick = { onPick(label); open = false },
                )
            }
            DropdownMenuItem(
                text = { Text("라벨 추가·편집", color = p.sheetSub, fontSize = 14.sp) },
                onClick = { open = false; onEdit() },
            )
        }
    }
}

