package com.jpenner.vibetuner.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography
import com.jpenner.vibetuner.R

val Sora = FontFamily(
    Font(R.font.sora_regular, FontWeight.Normal),
    Font(R.font.sora_medium, FontWeight.Medium),
    Font(R.font.sora_semibold, FontWeight.SemiBold),
    Font(R.font.sora_bold, FontWeight.Bold),
    Font(R.font.sora_extrabold, FontWeight.ExtraBold),
)
val Mono = FontFamily(Font(R.font.jetbrainsmono_regular), Font(R.font.jetbrainsmono_medium, FontWeight.Medium))

val MonoMeta = TextStyle(
    fontFamily = Mono,
    fontWeight = FontWeight.Medium,
    fontSize = 14.sp,
    letterSpacing = 0.5.sp,
)

val MonoLabel = TextStyle(
    fontFamily = Mono,
    fontWeight = FontWeight.SemiBold,
    fontSize = 13.sp,
    letterSpacing = 1.2.sp,
)
val AerialTypography = Typography(
    displayLarge  = TextStyle(fontFamily = Sora, fontWeight = FontWeight.ExtraBold, fontSize = 64.sp, letterSpacing = (-1).sp),
    headlineMedium = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 40.sp),
    titleMedium   = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyLarge     = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 17.sp),
    labelSmall    = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 14.sp),
)