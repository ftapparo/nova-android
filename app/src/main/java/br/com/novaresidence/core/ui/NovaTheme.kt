package br.com.novaresidence.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
import br.com.novaresidence.R
import androidx.compose.ui.unit.sp

val NovaPrimary = Color(0xFF45207A)
val NovaHeader = Color(0xFF2E0F44)
val NovaBackground = Color(0xFFF5F6FA)
val NovaSecondaryContainer = Color(0xFFECEBF3)
val NovaPrimarySoft = Color(0xFFEEE6F7)
val NovaSuccess = Color(0xFF237A4B)
val NovaOutline = Color(0xFFE3E6ED)
val NovaAccent = Color(0xFFF2B705)
val NovaDanger = Color(0xFFB71C1C)
val NovaWarning = Color(0xFFA85F00)
val NovaDisabledContainer = Color(0xFFE7E9EE)
val NovaDisabledContent = Color(0xFFA7ADB6)
@OptIn(ExperimentalTextApi::class)
val NovaBrandFont = FontFamily(Font(R.font.inter, FontWeight.Bold, variationSettings = FontVariation.Settings(FontVariation.weight(700))))

private val Colors = lightColorScheme(
    primary = NovaPrimary, onPrimary = Color.White,
    primaryContainer = NovaPrimarySoft, onPrimaryContainer = NovaPrimary,
    secondaryContainer = NovaSecondaryContainer, onSecondaryContainer = NovaPrimary,
    background = NovaBackground, onBackground = Color(0xFF1F2933),
    surface = Color.White, onSurface = Color(0xFF1F2933),
    onSurfaceVariant = Color(0xFF6B7280), surfaceVariant = NovaSecondaryContainer,
    outline = Color(0xFF79747E), outlineVariant = NovaOutline,
)

private fun style(size: Int, height: Int, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f) = TextStyle(
    fontFamily = FontFamily.SansSerif, fontSize = size.sp, lineHeight = height.sp,
    fontWeight = weight, letterSpacing = tracking.sp,
)

private val Type = Typography(
    headlineMedium = style(20, 26, FontWeight.Medium),
    titleLarge = style(18, 24, FontWeight.Medium),
    titleMedium = style(16, 22, FontWeight.Medium),
    bodyLarge = style(16, 24),
    bodyMedium = style(14, 20),
    bodySmall = style(12, 16),
    labelLarge = style(14, 20, FontWeight.Medium),
    labelMedium = style(12, 16, FontWeight.Medium),
    labelSmall = style(11, 16, FontWeight.Medium),
)

@Composable
fun NovaResidenceTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colors, typography = Type) {
        CompositionLocalProvider(LocalContentColor provides Colors.onSurface, content = content)
    }
}
