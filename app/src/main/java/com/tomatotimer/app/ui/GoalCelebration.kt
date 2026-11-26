package com.tomatotimer.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 하루 목표를 채웠을 때 한 번 뜨는 축하 화면 */
@Composable
fun GoalCelebration(goal: Int, onDismiss: () -> Unit) {
    val p = LocalPalette.current
    val pop = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 260f)) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .padding(horizontal = 28.dp)
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .graphicsLayer { scaleX = pop.value; scaleY = pop.value }
                .clip(RoundedCornerShape(32.dp))
                .background(p.sheet)
                // 카드 안을 눌러도 닫히지 않게
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Confetti(Modifier.size(width = 260.dp, height = 86.dp))
                TomatoIcon(p.full, p.fullLeaf, 72.dp)
            }
            VSpace(12.dp)
            Text("오늘 목표 달성!", color = p.sheetInk, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
            VSpace(8.dp)
            Text(
                "토마토 ${goal}개를 모두 땄어요.\n오늘 정말 잘했어요.",
                color = p.sheetSub,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
            )
            VSpace(16.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                for (i in 0 until goal.coerceAtMost(10)) TomatoIcon(p.full, p.fullLeaf, 20.dp)
            }
            VSpace(22.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(p.primary)
                    .clickable(role = Role.Button, onClick = onDismiss),
                contentAlignment = Alignment.Center,
            ) {
                Text("고마워요", color = p.primaryInk, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** 토마토 주위로 흩어지는 반짝이 */
@Composable
private fun Confetti(modifier: Modifier) {
    val p = LocalPalette.current
    val colors = listOf(Color(0xFFFFD66B), p.leaf, Color(0xFFCFC4FF), p.full, Color(0xFFFFB0A3))
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val spots = listOf(
            0.08f to 0.30f, 0.20f to 0.75f, 0.30f to 0.12f, 0.70f to 0.15f,
            0.80f to 0.78f, 0.92f to 0.35f, 0.14f to 0.52f, 0.86f to 0.55f,
        )
        spots.forEachIndexed { i, (x, y) ->
            val c = Offset(w * x, h * y)
            val r = if (i % 3 == 0) 9.dp.toPx() else 5.dp.toPx()
            val color = colors[i % colors.size]
            if (i % 2 == 0) {
                val k = r * 0.32f
                drawPath(Path().apply {
                    moveTo(c.x, c.y - r); lineTo(c.x + k, c.y - k); lineTo(c.x + r, c.y)
                    lineTo(c.x + k, c.y + k); lineTo(c.x, c.y + r); lineTo(c.x - k, c.y + k)
                    lineTo(c.x - r, c.y); lineTo(c.x - k, c.y - k); close()
                }, color)
            } else {
                drawCircle(color, r * 0.6f, c)
            }
        }
    }
}
