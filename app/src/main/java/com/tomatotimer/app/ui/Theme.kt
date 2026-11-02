package com.tomatotimer.app.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.tomatotimer.app.R

/** 시안의 낮(토마토) / 밤(나이트 팝) 색을 한 곳에 모아둔 팔레트 */
@Immutable
data class Palette(
    val isDark: Boolean,
    val bg: Color,
    val blob: Color,
    val ink: Color,
    val sub: Color,
    val tomato: Color,
    val track: Color,
    val ring: Color,
    val breakRing: Color,
    val leaf: Color,
    val leafDark: Color,
    val sheet: Color,
    val sheetInk: Color,
    val sheetSub: Color,
    val primary: Color,
    val primaryInk: Color,
    val chipOnBg: Color,
    val chipOnInk: Color,
    val chipBg: Color,
    val chipInk: Color,
    val iconBg: Color,
    val iconInk: Color,
    val navOn: Color,
    val navOff: Color,
    val full: Color,
    val fullLeaf: Color,
    val empty: Color,
    val emptyLeaf: Color,
    val barTrack: Color,
)

val DayPalette = Palette(
    isDark = false,
    bg = Color(0xFFFF594B),
    blob = Color(0xFFFF7464),
    ink = Color(0xFFFFFDFC),
    sub = Color(0xFFFFE8E3),
    tomato = Color(0xFFF04438),
    track = Color(0xFFFF8E80),
    ring = Color(0xFFFFFCF7),
    breakRing = Color(0xFFBDF2D5),
    leaf = Color(0xFFBBF1B9),
    leafDark = Color(0xFF86CD9A),
    sheet = Color(0xFFFFFCFA),
    sheetInk = Color(0xFF303449),
    sheetSub = Color(0xFF6B7084),
    primary = Color(0xFFFF594B),
    primaryInk = Color(0xFFFFFCFA),
    chipOnBg = Color(0xFFFF594B),
    chipOnInk = Color(0xFFFFFCFA),
    chipBg = Color(0xFFF3F4F8),
    chipInk = Color(0xFF777D90),
    iconBg = Color(0xFFFF7567),
    iconInk = Color(0xFFFFFDFC),
    navOn = Color(0xFFEF4B40),
    navOff = Color(0xFF9095A6),
    full = Color(0xFFFF594B),
    fullLeaf = Color(0xFF409B6D),
    empty = Color(0xFFDDC6C0),
    emptyLeaf = Color(0xFFDDC6C0),
    barTrack = Color(0xFFF2F3F7),
)

val NightPalette = Palette(
    isDark = true,
    bg = Color(0xFF171C2E),
    blob = Color(0xFF202840),
    ink = Color(0xFFF8FAFF),
    sub = Color(0xFFB5C1DD),
    tomato = Color(0xFF252E49),
    track = Color(0xFFFF8E80),
    ring = Color(0xFFFFFCF7),
    breakRing = Color(0xFFA6EFD7),
    leaf = Color(0xFFA6EFD7),
    leafDark = Color(0xFF78CDB8),
    sheet = Color(0xFF20273E),
    sheetInk = Color(0xFFF8FAFF),
    sheetSub = Color(0xFFB1BCD6),
    primary = Color(0xFFA6EFD7),
    primaryInk = Color(0xFF1D3441),
    chipOnBg = Color(0xFFFF9696),
    chipOnInk = Color(0xFF171C2E),
    chipBg = Color(0xFF2D3753),
    chipInk = Color(0xFFD4DDF1),
    iconBg = Color(0xFF252E49),
    iconInk = Color(0xFFF8FAFF),
    navOn = Color(0xFFA6EFD7),
    navOff = Color(0xFF9AAAC8),
    full = Color(0xFFFF9696),
    fullLeaf = Color(0xFF71C398),
    empty = Color(0xFF65738E),
    emptyLeaf = Color(0xFF63877F),
    barTrack = Color(0xFF2D3753),
)

val LocalPalette = staticCompositionLocalOf { DayPalette }

/** 제목과 큰 숫자에 쓰는 굵은 글씨체 */
val BlackHanSans = FontFamily.SansSerif
