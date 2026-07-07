package com.jpenner.vibetuner.phone.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.jpenner.vibetuner.phone.R

val Sora = FontFamily(
    Font(R.font.sora_regular, FontWeight.Normal),
    Font(R.font.sora_medium, FontWeight.Medium),
    Font(R.font.sora_semibold, FontWeight.SemiBold),
    Font(R.font.sora_bold, FontWeight.Bold),
    Font(R.font.sora_extrabold, FontWeight.ExtraBold),
)
val Mono = FontFamily(
    Font(R.font.jetbrainsmono_regular),
    Font(R.font.jetbrainsmono_medium, FontWeight.Medium),
)

// Phone-scale sizes — the TV kit's numbers (18-64sp) are tuned for 10-foot viewing;
// these are the same type ramp at ordinary handheld reading distance.
val PhoneTypography = Typography(
    displayLarge  = TextStyle(fontFamily = Sora, fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 24.sp),
    titleMedium   = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge     = TextStyle(fontFamily = Sora, fontWeight = FontWeight.Normal, fontSize = 15.sp),
    labelSmall    = TextStyle(fontFamily = Mono, fontWeight = FontWeight.Normal, fontSize = 12.sp),
)

val MonoLabel = TextStyle(
    fontFamily = Mono,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    letterSpacing = 1.sp,
)
