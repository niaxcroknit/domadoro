package com.tomatotimer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomatotimer.app.data.Prefs
import com.tomatotimer.app.data.Session
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** 최근 7일 동안 딴 토마토를 보여주는 '내 바구니' */
@Composable
fun BasketScreen(bottomBar: @Composable () -> Unit) {
    val base = LocalPalette.current
    val basket = if (base.isDark) base else base.copy(
        bg = Color(0xFFFFF0ED),
        ink = base.sheetInk,
        sub = Color(0xFF85869A),
    )
    CompositionLocalProvider(LocalPalette provides basket) {
        Box(Modifier.fillMaxSize().background(basket.bg)) {
            BasketContent(bottomBar)
        }
    }
}

@Composable
private fun BasketContent(bottomBar: @Composable () -> Unit) {
    val p = LocalPalette.current
    val sessions by Prefs.sessions.collectAsStateWithLifecycle()

    val today = LocalDate.now()
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val from = days.first().toEpochDay()
    val week = sessions.filter { it.epochDay >= from }
    val totalMinutes = week.sumOf { it.minutes }
    val perDay = days.map { d -> week.count { it.epochDay == d.toEpochDay() } }
    val bestIndex = perDay.indices.maxByOrNull { perDay[it] } ?: 0
    val streak = streakDays(sessions, today)
    val dayName = { d: LocalDate -> d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN) }

    ScreenFrame(
        sheetFills = true,
        bottomBar = bottomBar,
        top = {
            Text("내 바구니", color = p.ink, fontFamily = BlackHanSans, fontWeight = FontWeight.Bold, fontSize = 28.sp)
            Text(
                "최근 7일 · ${days.first().monthValue}.${days.first().dayOfMonth} – ${today.monthValue}.${today.dayOfMonth}",
                color = p.sub,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            VSpace(14.dp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${week.size}개", color = p.ink, fontFamily = BlackHanSans, fontWeight = FontWeight.Light, fontSize = 64.sp, lineHeight = 72.sp)
                HSpace(12.dp)
                Column(Modifier.padding(bottom = 8.dp)) {
                    Text("토마토 수확", color = p.ink, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("집중 ${formatMinutes(totalMinutes)}", color = p.sub, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        sheet = {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("요일별 수확", color = p.sheetInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                VSpace(12.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    days.forEachIndexed { i, d ->
                        DayColumn(count = perDay[i], label = dayName(d), isToday = i == days.lastIndex)
                    }
                }

                VSpace(20.dp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(
                        title = "하루 평균",
                        value = "%.1f개".format(week.size / 7f),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        title = "최고의 날",
                        value = if (perDay[bestIndex] > 0) "${dayName(days[bestIndex])} ${perDay[bestIndex]}개" else "-",
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        title = "연속 기록",
                        value = "${streak}일",
                        modifier = Modifier.weight(1f),
                    )
                }
                if (week.isEmpty()) {
                    VSpace(16.dp)
                    Text(
                        "아직 딴 토마토가 없어요.\n첫 토마토를 따러 가볼까요?",
                        color = p.sheetSub,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                    )
                }
                VSpace(12.dp)
            }
        },
    )
}

/** 오늘(오늘 아직 안 했으면 어제)부터 거꾸로, 하루도 빠짐없이 토마토를 딴 날 수 */
private fun streakDays(sessions: List<Session>, today: LocalDate): Int {
    val days = sessions.map { it.epochDay }.toHashSet()
    var d = today.toEpochDay()
    if (d !in days) d -= 1
    var count = 0
    while (d in days) {
        count++
        d--
    }
    return count
}

@Composable
private fun DayColumn(count: Int, label: String, isToday: Boolean) {
    val p = LocalPalette.current
    val shown = count.coerceAtMost(5)
    Column(
        modifier = Modifier
            .width(38.dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        if (count > shown) {
            Text("+${count - shown}", color = p.sheetSub, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        for (i in 0 until shown) {
            TomatoIcon(fill = p.full, leaf = p.fullLeaf, size = 24.dp)
        }
        if (count == 0) {
            TomatoIcon(fill = p.empty, leaf = p.emptyLeaf, size = 24.dp)
        }
        VSpace(8.dp)
        Text(
            text = label,
            color = if (isToday) p.navOn else p.sheetSub,
            fontSize = 13.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
private fun StatTile(title: String, value: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(p.barTrack)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(title, color = p.sheetSub, fontSize = 13.sp, fontWeight = FontWeight.Medium)
        VSpace(4.dp)
        Text(value, color = p.sheetInk, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

fun formatMinutes(total: Int): String {
    val h = total / 60
    val m = total % 60
    return when {
        h > 0 && m > 0 -> "${h}시간 ${m}분"
        h > 0 -> "${h}시간"
        else -> "${m}분"
    }
}
