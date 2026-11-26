package com.tomatotimer.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tomatotimer.app.data.Prefs
import com.tomatotimer.app.data.Session
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/** 딴 토마토를 모아 보는 '내 바구니'. 최근 7일과 한 달 달력 두 가지로 봐요. */
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
    val goal by Prefs.goal.collectAsStateWithLifecycle()
    // 0 = 최근 7일, 1 = 한 달
    var mode by rememberSaveable { mutableIntStateOf(0) }
    // 한 달 보기에서 몇 달 전을 보는지 (0 = 이번 달)
    var monthsBack by rememberSaveable { mutableIntStateOf(0) }

    val today = LocalDate.now()
    val month = YearMonth.from(today).minusMonths(monthsBack.toLong())
    val (from, to) = if (mode == 0) {
        today.minusDays(6) to today
    } else {
        month.atDay(1) to month.atEndOfMonth()
    }
    val inRange = sessions.filter { it.epochDay in from.toEpochDay()..to.toEpochDay() }
    val totalMinutes = inRange.sumOf { it.minutes }

    ScreenFrame(
        sheetFills = true,
        bottomBar = bottomBar,
        top = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("내 바구니", color = p.ink, fontFamily = BlackHanSans, fontWeight = FontWeight.Bold,
                    fontSize = 28.sp, modifier = Modifier.weight(1f))
                Pill("7일", selected = mode == 0) { mode = 0 }
                HSpace(6.dp)
                Pill("한 달", selected = mode == 1) { mode = 1 }
            }
            Text(
                if (mode == 0) "최근 7일 · ${from.monthValue}.${from.dayOfMonth} – ${to.monthValue}.${to.dayOfMonth}"
                else "${month.year}년 ${month.monthValue}월",
                color = p.sub,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            VSpace(14.dp)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${inRange.size}개", color = p.ink, fontFamily = BlackHanSans, fontWeight = FontWeight.Light, fontSize = 64.sp, lineHeight = 72.sp)
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
                if (mode == 0) {
                    WeekView(sessions, today, goal)
                } else {
                    MonthView(
                        sessions = sessions,
                        month = month,
                        today = today,
                        goal = goal,
                        canGoBack = monthsBack < 11,
                        canGoForward = monthsBack > 0,
                        onBack = { monthsBack++ },
                        onForward = { monthsBack-- },
                    )
                }
                LabelBreakdown(inRange)
                if (inRange.isEmpty()) {
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

// ───────────────────────── 최근 7일 ─────────────────────────

@Composable
private fun WeekView(sessions: List<Session>, today: LocalDate, goal: Int) {
    val p = LocalPalette.current
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val perDay = days.map { d -> sessions.count { it.epochDay == d.toEpochDay() } }
    val weekTotal = perDay.sum()
    val bestIndex = perDay.indices.maxByOrNull { perDay[it] } ?: 0
    val streak = streakDays(sessions, today)
    val goalDays = if (goal > 0) perDay.count { it >= goal } else 0

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
            DayColumn(count = perDay[i], label = dayName(d), isToday = i == days.lastIndex,
                reachedGoal = goal > 0 && perDay[i] >= goal)
        }
    }

    VSpace(20.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(
            title = "하루 평균",
            value = "%.1f개".format(weekTotal / 7f),
            modifier = Modifier.weight(1f),
        )
        if (goal > 0) {
            StatTile(
                title = "목표 달성",
                value = "${goalDays}일",
                modifier = Modifier.weight(1f),
            )
        } else {
            StatTile(
                title = "최고의 날",
                value = if (perDay[bestIndex] > 0) "${dayName(days[bestIndex])} ${perDay[bestIndex]}개" else "-",
                modifier = Modifier.weight(1f),
            )
        }
        StatTile(
            title = "연속 기록",
            value = "${streak}일",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DayColumn(count: Int, label: String, isToday: Boolean, reachedGoal: Boolean) {
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
        // 목표를 채운 날은 요일 이름 아래에 초록 점
        Text(
            text = label,
            color = if (isToday) p.navOn else p.sheetSub,
            fontSize = 13.sp,
            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Medium,
        )
        VSpace(4.dp)
        Box(
            Modifier
                .size(6.dp)
                .alpha(if (reachedGoal) 1f else 0f)
                .clip(CircleShape)
                .background(p.fullLeaf),
        )
    }
}

// ───────────────────────── 한 달 달력 ─────────────────────────

@Composable
private fun MonthView(
    sessions: List<Session>,
    month: YearMonth,
    today: LocalDate,
    goal: Int,
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBack: () -> Unit,
    onForward: () -> Unit,
) {
    val p = LocalPalette.current
    val counts = sessions.groupingBy { it.epochDay }.eachCount()
    val first = month.atDay(1)
    val days = month.lengthOfMonth()
    // 일요일부터 시작하는 달력
    val lead = first.dayOfWeek.value % 7
    val perDay = (1..days).map { counts[month.atDay(it).toEpochDay()] ?: 0 }
    // 진하기 기준: 목표가 있으면 목표 개수, 없으면 이 달의 최고 개수
    val scale = (if (goal > 0) goal else perDay.maxOrNull() ?: 1).coerceAtLeast(1)

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("날짜별 수확", color = p.sheetInk, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        ArrowButton("‹", "이전 달", canGoBack, onBack)
        HSpace(4.dp)
        ArrowButton("›", "다음 달", canGoForward, onForward)
    }
    VSpace(10.dp)
    Row(Modifier.fillMaxWidth()) {
        listOf("일", "월", "화", "수", "목", "금", "토").forEach {
            Text(it, color = p.sheetSub, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        }
    }
    VSpace(6.dp)
    val cells = lead + days
    val rows = (cells + 6) / 7
    for (r in 0 until rows) {
        Row(Modifier.fillMaxWidth()) {
            for (c in 0 until 7) {
                val index = r * 7 + c - lead
                Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                    if (index in 0 until days) {
                        val date = month.atDay(index + 1)
                        DayCell(
                            day = index + 1,
                            count = perDay[index],
                            level = perDay[index].toFloat() / scale,
                            reachedGoal = goal > 0 && perDay[index] >= goal,
                            isToday = date == today,
                            isFuture = date.isAfter(today),
                        )
                    }
                }
            }
        }
    }

    val active = perDay.count { it > 0 }
    val best = perDay.maxOrNull() ?: 0
    VSpace(14.dp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatTile(title = "토마토 딴 날", value = "${active}일", modifier = Modifier.weight(1f))
        if (goal > 0) {
            StatTile(title = "목표 달성", value = "${perDay.count { it >= goal }}일", modifier = Modifier.weight(1f))
        }
        StatTile(title = "가장 많이", value = if (best > 0) "${best}개" else "-", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun DayCell(day: Int, count: Int, level: Float, reachedGoal: Boolean, isToday: Boolean, isFuture: Boolean) {
    val p = LocalPalette.current
    // 많이 딸수록 진한 토마토색
    val fill = if (count == 0) p.barTrack else lerp(p.full.copy(alpha = 0.28f), p.full, level.coerceIn(0f, 1f))
    val ink = if (count > 0 && level >= 0.5f) Color.White else p.sheetInk
    Box(
        Modifier
            .fillMaxSize()
            .alpha(if (isFuture) 0.4f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .background(fill),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "$day",
                color = ink,
                fontSize = 13.sp,
                fontWeight = if (isToday || count > 0) FontWeight.Bold else FontWeight.Medium,
            )
            if (reachedGoal) {
                Box(Modifier.size(5.dp).clip(CircleShape).background(if (level >= 0.5f) Color.White else p.fullLeaf))
            }
        }
        if (isToday) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 3.dp)
                    .width(14.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (level >= 0.5f) Color.White else p.navOn),
            )
        }
    }
}

@Composable
private fun ArrowButton(symbol: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Box(
        Modifier
            .size(40.dp)
            .alpha(if (enabled) 1f else 0.3f)
            .clip(CircleShape)
            .background(p.barTrack)
            .clickable(enabled = enabled, role = Role.Button, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(symbol, color = p.sheetInk, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

// ───────────────────────── 라벨별 ─────────────────────────

@Composable
private fun LabelBreakdown(sessions: List<Session>) {
    val p = LocalPalette.current
    if (sessions.none { it.label != null }) return
    val groups = sessions.groupBy { it.label ?: "라벨 없음" }
        .map { (label, list) -> Triple(label, list.size, list.sumOf { it.minutes }) }
        .sortedByDescending { it.second }
    val max = groups.maxOf { it.second }.coerceAtLeast(1)

    VSpace(22.dp)
    Text("무엇에 집중했나요", color = p.sheetInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    VSpace(10.dp)
    groups.forEach { (label, count, minutes) ->
        Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = p.sheetInk, fontSize = 14.sp, fontWeight = FontWeight.Bold,
                maxLines = 1, modifier = Modifier.width(76.dp))
            Box(
                Modifier
                    .weight(1f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(p.barTrack),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth(count.toFloat() / max)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (label == "라벨 없음") p.empty else p.full),
                )
            }
            HSpace(10.dp)
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(64.dp)) {
                Text("${count}개", color = p.sheetInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(formatMinutes(minutes), color = p.sheetSub, fontSize = 12.sp)
            }
        }
    }
}

// ───────────────────────── 공통 ─────────────────────────

private fun dayName(d: LocalDate): String = d.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)

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
