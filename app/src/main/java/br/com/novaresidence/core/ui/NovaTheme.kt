package br.com.novaresidence.core.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp

val NovaPrimary = Color(0xFF45207A)
val NovaHeader = Color(0xFF2E0F44)
val NovaBackground = Color(0xFFF5F6FA)
val NovaSecondaryContainer = Color(0xFFECEBF3)
val NovaContextSubtle = Color(0xFFF1F0F8)
val NovaPrimarySoft = Color(0xFFEEE6F7)
val NovaOnSurface = Color(0xFF1F2933)
val NovaOnSurfaceVariant = Color(0xFF6B7280)
val NovaSuccess = Color(0xFF237A4B)
val NovaSuccessContainer = Color(0xFFEAF7EF)
val NovaOutline = Color(0xFFE3E6ED)
val NovaAccent = Color(0xFFF2B705)
val NovaDanger = Color(0xFFB71C1C)
val NovaError = Color(0xFFB42318)
val NovaErrorContainer = Color(0xFFFDECEA)
val NovaWarning = Color(0xFFA85F00)
val NovaWarningContainer = Color(0xFFFFF4D6)
val NovaOffline = Color(0xFF667085)
val NovaDisabledContainer = Color(0xFFE7E9EE)
val NovaDisabledContent = Color(0xFFA7ADB6)

/** Largura dos frames do Figma (dp). */
const val FigmaFrameWidth = 412f

private val Colors = lightColorScheme(
    primary = NovaPrimary, onPrimary = Color.White,
    primaryContainer = NovaPrimarySoft, onPrimaryContainer = NovaPrimary,
    secondaryContainer = NovaSecondaryContainer, onSecondaryContainer = NovaPrimary,
    background = NovaBackground, onBackground = NovaOnSurface,
    surface = Color.White, onSurface = NovaOnSurface,
    onSurfaceVariant = NovaOnSurfaceVariant, surfaceVariant = NovaSecondaryContainer,
    outline = Color(0xFF79747E), outlineVariant = NovaOutline,
    error = NovaError,
)

private fun style(size: Int, height: Int, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f) = TextStyle(
    fontFamily = FontFamily.SansSerif, fontSize = size.sp, lineHeight = height.sp,
    fontWeight = weight, letterSpacing = tracking.sp,
)

// Estilos com os mesmos tamanhos, pesos e espaçamento entre letras dos estilos do Figma.
private val Type = Typography(
    headlineMedium = style(20, 26, FontWeight.Medium),
    titleLarge = style(18, 24),
    titleMedium = style(16, 22, FontWeight.Medium),
    bodyLarge = style(16, 24, tracking = 0.5f),
    bodyMedium = style(14, 20, tracking = 0.4f),
    bodySmall = style(12, 16, tracking = 0.4f),
    labelLarge = style(14, 20, FontWeight.Medium, 0.1f),
    labelMedium = style(12, 16, FontWeight.Medium, 0.5f),
    labelSmall = style(11, 16, FontWeight.Medium, 0.5f),
)

/**
 * Telas mais estreitas que o frame do Figma recebem densidade proporcional, para que a
 * composição (margens, quebras de linha e proporções) seja a mesma do Figma. Decisão do
 * produto em 01/10/2026; a escala de fonte do sistema continua aplicada sobre o resultado.
 * Telas iguais ou maiores mantêm a densidade real e usam largura máxima nos conteúdos.
 */
@Composable
private fun FigmaProportionalDensity(content: @Composable () -> Unit) {
    val base = LocalDensity.current
    val width = LocalConfiguration.current.screenWidthDp.toFloat()
    val density = if (width > 0f && width < FigmaFrameWidth) Density(base.density * width / FigmaFrameWidth, base.fontScale) else base
    CompositionLocalProvider(LocalDensity provides density, content = content)
}

@Composable
fun NovaResidenceTheme(content: @Composable () -> Unit) {
    FigmaProportionalDensity {
        MaterialTheme(colorScheme = Colors, typography = Type) {
            CompositionLocalProvider(LocalContentColor provides Colors.onSurface, content = content)
        }
    }
}
