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

// Variáveis "color/…" do Figma (12/10/2026).
val NovaPrimary = Color(0xFF4C1D95)
val NovaHeader = Color(0xFF2B0F44)
val NovaHeaderControl = Color(0xFF3A145F)
val NovaBackground = Color(0xFFF5F6FA)
val NovaServiceBackground = Color(0xFFF7F8FC)
val NovaSecondaryContainer = Color(0xFFEBEBF1)
val NovaPrimarySoft = Color(0xFFEEE8F8)
val NovaPrimaryTint = Color(0xFFF2EFFC)
val NovaInfoSurface = Color(0xFFEEECF6)
val NovaChevronSurface = Color(0xFFF7F7F9)
val NovaOnSurface = Color(0xFF1F2933)
val NovaInk = Color(0xFF151B34)
val NovaOnSurfaceVariant = Color(0xFF6B7280)
val NovaOutline = Color(0xFFE3E6ED)
val NovaNeutralOutline = Color(0xFFE2E2E7)

// Cores de estado. O verde, o cinza e o amarelo do Figma (#248E4E, #667080, #BD9700) ficam abaixo de
// 4,5:1 nos selos e textos; usamos tons mais escuros da mesma família (DESIGN_GUIDE §4).
// Texto secundário sobre as superfícies tingidas (faixa informativa #EEECF6): #6B7280 cairia para 4,1:1.
val NovaOnTint = Color(0xFF5F6675)
val NovaSuccess = Color(0xFF237A4B)
val NovaSuccessContainer = Color(0xFFE8F7ED)
val NovaWarning = Color(0xFF8A6A00)
val NovaWarningContainer = Color(0xFFF8F4E3)
val NovaAlarm = Color(0xFFC23030)
val NovaAlarmContainer = Color(0xFFFFECEC)
val NovaError = Color(0xFFB42318)
val NovaErrorContainer = Color(0xFFFFF1F0)
val NovaOffline = Color(0xFF5B6473)
val NovaOfflineContainer = Color(0xFFEEF0F4)
val NovaNeutralContainer = Color(0xFFF2F4F8)
val NovaDisabledContainer = Color(0xFFE7E9EE)
val NovaDisabledContent = Color(0xFFA7ADB6)

/** Largura dos frames do Figma (dp). */
const val FigmaFrameWidth = 412f

private val Colors = lightColorScheme(
    primary = NovaPrimary, onPrimary = Color.White,
    primaryContainer = NovaPrimarySoft, onPrimaryContainer = NovaPrimary,
    secondaryContainer = NovaSecondaryContainer, onSecondaryContainer = NovaOnSurface,
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

/** Estilos "Nova / Typography" do Figma, pelo papel de cada texto. */
object NovaType {
    val Hero = style(32, 38, FontWeight.SemiBold)
    val HeadlineHome = style(22, 28, FontWeight.SemiBold)
    val ScreenTitle = style(20, 26, FontWeight.Medium)
    val TopBar = style(18, 24)
    val Section = style(18, 24, FontWeight.SemiBold)
    val HeroSupporting = style(18, 22)
    val Highlight = style(16, 22, FontWeight.SemiBold)
    val Field = style(16, 24)
    val SectionTitle = style(15, 20, FontWeight.SemiBold)
    val ItemTitle = style(15, 20, FontWeight.Medium)
    val Body = style(14, 20)
    val BodyEmphasis = style(14, 20, FontWeight.Medium)
    val Button = style(14, 20, FontWeight.Medium)
    val Supporting = style(13, 18)
    val Caption = style(12, 16)
    val Badge = style(12, 16, FontWeight.Medium)
    val NavLabel = style(11, 16, FontWeight.Medium, 0.5f)
}

// Os componentes Material (campos, botões, menus) leem estes papéis.
private val Type = Typography(
    headlineMedium = NovaType.ScreenTitle,
    titleLarge = NovaType.TopBar,
    titleMedium = NovaType.ItemTitle,
    bodyLarge = NovaType.Field,
    bodyMedium = NovaType.Body,
    bodySmall = NovaType.Caption,
    labelLarge = NovaType.Button,
    labelMedium = NovaType.Badge,
    labelSmall = NovaType.NavLabel,
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
