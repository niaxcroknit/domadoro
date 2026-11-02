package com.tomatotimer.app.ui

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomatotimer.app.R
import com.tomatotimer.app.timer.Phase
import com.tomatotimer.app.timer.TimerState

// ───────────────────────── 토마토 그림 ─────────────────────────

/** 숫자들을 받아 cubic 곡선 경로를 만들어요. (시안의 SVG 좌표를 그대로 씀) */
private fun curvePath(scale: Float, vararg v: Float, close: Boolean = true): Path = Path().apply {
    moveTo(v[0] * scale, v[1] * scale)
    var i = 2
    while (i + 5 < v.size) {
        cubicTo(v[i] * scale, v[i + 1] * scale, v[i + 2] * scale, v[i + 3] * scale, v[i + 4] * scale, v[i + 5] * scale)
        i += 6
    }
    if (close) close()
}

/**
 * 꼭지 달린 토마토 + 남은 시간 링.
 * 그림 비율은 300 x 330 (시안과 동일).
 */
@Composable
fun TomatoRing(fraction: Float, ringColor: Color, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Canvas(modifier) {
        val s = size.width / 300f
        val center = Offset(150f * s, 180f * s)
        val r = 113f * s
        val sw = 7f * s
        val body = Path().apply {
            moveTo(150f*s, 68f*s)
            cubicTo(205f*s, 42f*s, 290f*s, 81f*s, 289f*s, 174f*s)
            cubicTo(294f*s, 269f*s, 220f*s, 322f*s, 150f*s, 307f*s)
            cubicTo(80f*s, 322f*s, 6f*s, 269f*s, 11f*s, 174f*s)
            cubicTo(10f*s, 81f*s, 95f*s, 42f*s, 150f*s, 68f*s)
            close()
        }
        drawOval(p.ink.copy(alpha = 0.06f), Offset(61f*s, 300f*s), Size(188f*s, 20f*s))
        for (offset in 2..10 step 2) {
            translate(top = offset*s) { drawPath(body, Color.Black.copy(alpha = 0.012f)) }
        }
        drawPath(body, Brush.linearGradient(
            // 낮·밤 모두 같은 밝은 빨강 토마토 (밤의 회색·남색 토마토는 상한 과일처럼 보여서 쓰지 않음).
            colors = listOf(Color(0xFFFF8B6B), Color(0xFFF64C3E), Color(0xFFE33B43)),
            start = Offset(30f*s, 60f*s), end = Offset(260f*s, 300f*s)))
        drawPath(body, Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.20f), Color.Transparent),
            center = Offset(73f*s, 90f*s), radius = 200f*s))
        drawPath(body, Color.White.copy(alpha = 0.12f), style = Stroke(1f*s))
        val shine = Path().apply {
            moveTo(36f*s, 138f*s)
            quadraticBezierTo(39f*s, 105f*s, 63f*s, 91f*s)
        }
        // Broad diffuse highlight instead of a hard white reflection.
        drawCircle(Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
            center = Offset(48f*s, 118f*s), radius = 28f*s),
            28f*s, Offset(48f*s, 118f*s))
        sparkle(Offset(20f*s, 39f*s), 10f*s, Color(0xFFFFF1B7))
        sparkle(Offset(277f*s, 62f*s), 10f*s, p.leaf)
        drawCircle(Color(0xFFCFC4FF), 4f*s, Offset(289f*s, 243f*s))
        drawArc(p.track, -70f, 320f, false, Offset(center.x-r, center.y-r), Size(r*2, r*2), style = Stroke(sw, cap = StrokeCap.Round))
        if (fraction > 0f) {
            drawArc(ringColor.copy(alpha = 0.80f), -70f, 320f * fraction, false,
                Offset(center.x-r, center.y-r), Size(r*2, r*2),
                style = Stroke(sw, cap = StrokeCap.Round))
        }

        drawLeaves(s, p.leaf, p.leafDark)
    }
}

private fun DrawScope.drawLeaves(s: Float, leaf: Color, leafDark: Color) {
    val crown = Path().apply {
        moveTo(150f*s,63f*s)
        cubicTo(132f*s,44f*s,115f*s,40f*s,99f*s,45f*s)
        cubicTo(109f*s,60f*s,131f*s,63f*s,148f*s,65f*s)
        cubicTo(132f*s,68f*s,121f*s,78f*s,120f*s,90f*s)
        cubicTo(138f*s,89f*s,148f*s,78f*s,151f*s,69f*s)
        cubicTo(160f*s,84f*s,173f*s,89f*s,184f*s,87f*s)
        cubicTo(179f*s,72f*s,165f*s,65f*s,155f*s,65f*s)
        cubicTo(175f*s,63f*s,195f*s,57f*s,203f*s,43f*s)
        cubicTo(180f*s,38f*s,162f*s,47f*s,150f*s,63f*s)
        close()
    }
    drawPath(crown, Brush.linearGradient(listOf(leaf, leafDark), Offset(110f*s,40f*s), Offset(185f*s,90f*s)))
    drawPath(curvePath(s,150f,62f,148f,50f,150f,36f,165f,24f,close=false),
        leaf,style=Stroke(7f*s,cap=StrokeCap.Round))

}

/** 바구니·기록에 쓰는 작은 토마토 아이콘 */
@Composable
fun TomatoIcon(fill: Color, leaf: Color, size: Dp, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Canvas(modifier.size(size)) {
        val s = this.size.width / 24f
        val empty = fill == p.empty
        val body = Path().apply {
            moveTo(12f*s, 8f*s)
            cubicTo(8f*s, 5f*s, 2f*s, 8f*s, 2f*s, 15f*s)
            cubicTo(2f*s, 21f*s, 7f*s, 24f*s, 12f*s, 22.5f*s)
            cubicTo(17f*s, 24f*s, 22f*s, 21f*s, 22f*s, 15f*s)
            cubicTo(22f*s, 8f*s, 16f*s, 5f*s, 12f*s, 8f*s)
            close()
        }
        if (empty) drawPath(body, fill, style = Stroke(1.3f*s))
        else drawPath(body, fill)
        val crown = Path().apply {
            moveTo(12f*s, 8f*s)
            lineTo(6f*s, 5.5f*s); lineTo(9.5f*s, 5f*s)
            lineTo(10f*s, 2.5f*s); lineTo(12.5f*s, 5f*s)
            lineTo(17.5f*s, 3.8f*s); lineTo(15.5f*s, 7f*s)
            lineTo(18f*s, 9f*s); lineTo(13f*s, 8.1f*s)
            lineTo(10f*s, 10f*s); close()
        }
        drawPath(crown, leaf)
        drawLine(leaf, Offset(12f*s, 6f*s), Offset(13.5f*s, 1.8f*s),
            strokeWidth = 1.3f*s, cap = StrokeCap.Round)
    }
}

/** 토마토 링 + 큰 숫자. 메인 화면과 PIP 창이 함께 써요. */
@Composable
fun TomatoTimerFace(state: TimerState, size: Dp, label: String?) {
    val p = LocalPalette.current
    val fraction by animateFloatAsState(
        targetValue = state.remainingFraction,
        animationSpec = tween(durationMillis = 250, easing = LinearEasing),
        label = "ring",
    )
    val density = LocalDensity.current
    // 글자 크기를 그림 크기에 맞춰 고정 (시스템 글자 크기 설정에 밀려 넘치지 않게)
    val timeSize = with(density) { (size * 0.22f).toSp() }
    val labelSize = with(density) { (size * 0.054f).coerceAtLeast(13.dp).toSp() }

    Box(Modifier.size(width = size, height = size * (330f / 300f))) {
        TomatoRing(
            fraction = fraction,
            ringColor = if (state.phase == Phase.BREAK) p.breakRing else p.ring,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = size * (52f / 300f))
                .height(size * (256f / 300f)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = state.timeText,
                color = p.ink,
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
                fontSize = timeSize,
                lineHeight = timeSize,
                maxLines = 1,
            )
            if (label != null) {
                Text(
                    text = label,
                    color = if (p.isDark) Color(0xFFFFE1DA) else p.sub,
                    fontSize = labelSize,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                )
            }
        }
    }
}

/** 오늘 딴 토마토 줄. 새로 따면 마지막 토마토가 톡 튀어나와요. */
@Composable
fun TomatoBasketRow(count: Int) {
    val p = LocalPalette.current
    val slots = 5
    val pop = remember { Animatable(1f) }
    var lastCount by remember { mutableIntStateOf(count) }
    LaunchedEffect(count) {
        if (count > lastCount) {
            pop.snapTo(0.3f)
            pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = 320f))
        }
        lastCount = count
    }
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until slots) {
            val on = i < count
            val m = if (on && i == count - 1) {
                Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }
            } else {
                Modifier
            }
            TomatoIcon(
                fill = if (on) p.full else p.empty,
                leaf = if (on) p.fullLeaf else p.emptyLeaf,
                size = 22.dp,
                modifier = m,
            )
        }
        if (count > slots) {
            Text("+${count - slots}", color = p.sheetSub, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ───────────────────────── 배경·틀 ─────────────────────────

/** 배경의 동그란 무늬. 밤에는 작은 별도 뿌려요. */
@Composable
fun Backdrop(modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    Canvas(modifier.fillMaxSize()) {
        val u = size.width / 390f
        drawRect(Brush.radialGradient(
            listOf(p.blob.copy(alpha = 0.6f), Color.Transparent),
            center = Offset(20f*u, 50f*u), radius = 470f*u))
        drawCircle(p.ink.copy(alpha = 0.025f), 85f*u, Offset(400f*u, 150f*u), style = Stroke(18f*u))
        sparkle(Offset(323f*u, 173f*u), 18f*u, Color(0xFFFFF0BA))
        sparkle(Offset(345f*u, 146f*u), 6f*u, p.leaf)
        drawCircle(Color(0xFFCFC4FF), 3f*u, Offset(346f*u, 201f*u))
    }
}

private fun DrawScope.sparkle(c: Offset, r: Float, color: Color) {
    val k = r * 0.32f
    val path = Path().apply {
        moveTo(c.x, c.y - r)
        lineTo(c.x + k, c.y - k)
        lineTo(c.x + r, c.y)
        lineTo(c.x + k, c.y + k)
        lineTo(c.x, c.y + r)
        lineTo(c.x - k, c.y + k)
        lineTo(c.x - r, c.y)
        lineTo(c.x - k, c.y - k)
        close()
    }
    drawPath(path, color)
}

/**
 * 모든 화면의 공통 틀: 위쪽(배경색 위) + 아래쪽 크림/어두운 시트 + 하단 탭.
 * sheetFills = true 이면 시트가 남은 공간을 다 차지해요(기록·설정 화면).
 */
@Composable
fun ScreenFrame(
    sheetFills: Boolean,
    bottomBar: @Composable () -> Unit,
    top: @Composable ColumnScope.() -> Unit,
    sheet: @Composable ColumnScope.() -> Unit,
) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxSize()) {
        val topModifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = if (sheetFills) 24.dp else 0.dp)
        Column(if (sheetFills) topModifier else topModifier.weight(1f), content = top)

        val sheetModifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(p.sheet)
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 20.dp)
        Column(if (sheetFills) sheetModifier.weight(1f) else sheetModifier) {
            sheet()
            bottomBar()
        }
    }
}

// ───────────────────────── 버튼·탭 ─────────────────────────

@Composable
fun CircleIconButton(
    @DrawableRes icon: Int,
    description: String,
    background: Color,
    tint: Color,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), contentDescription = description, tint = tint, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun Pill(
    text: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    Box(
        modifier = Modifier
            .alpha(if (enabled || selected) 1f else 0.55f)
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) p.primary else p.barTrack)
            .selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (selected) p.primaryInk else p.sheetInk,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

enum class Tab(val label: String, @DrawableRes val icon: Int) {
    TIMER("타이머", R.drawable.ic_timer),
    BASKET("바구니", R.drawable.ic_basket),
    SETTINGS("설정", R.drawable.ic_settings),
}

@Composable
fun BottomNav(current: Tab, onSelect: (Tab) -> Unit) {
    val p = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { tab ->
            val selected = tab == current
            val color = if (selected) p.navOn else p.navOff
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (selected) p.barTrack else Color.Transparent)
                    .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(tab) })
                    .padding(horizontal = 18.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(painterResource(tab.icon), contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                Spacer(Modifier.height(4.dp))
                Text(
                    text = tab.label,
                    color = color,
                    fontSize = 12.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
fun HSpace(width: Dp) = Spacer(Modifier.width(width))

@Composable
fun VSpace(height: Dp) = Spacer(Modifier.height(height))




