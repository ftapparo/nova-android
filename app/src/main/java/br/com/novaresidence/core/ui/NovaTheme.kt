package br.com.novaresidence.core.ui

import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
/** Superfície de cartões, menu inferior e área de leitura dos documentos. */
val NovaSurface = Color.White
/** Texto e ícones sobre o roxo, o cabeçalho da home e o vermelho de alarme. */
val NovaOnPrimary = Color.White
/** Ícone de maior contraste (chevron dos cartões de serviço da home). */
val NovaIconStrong = Color(0xFF1D1B20)
/** Indicador de página inativo do carrossel de comunicados. */
val NovaIndicatorInactive = Color(0xFFC7C9D0)
/** Sombra projetada para cima pelo menu inferior (rgba(64,52,80,.1) do Figma). */
val NovaNavShadow = Color(0x1A403450)

// Campos de texto (Material 3 do Figma): contorno e rótulo/placeholder fora de foco.
val NovaFieldOutline = Color(0xFF79747E)
val NovaFieldVariant = Color(0xFF49454F)

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
    primary = NovaPrimary, onPrimary = NovaOnPrimary,
    primaryContainer = NovaPrimarySoft, onPrimaryContainer = NovaPrimary,
    secondaryContainer = NovaSecondaryContainer, onSecondaryContainer = NovaOnSurface,
    background = NovaBackground, onBackground = NovaOnSurface,
    surface = NovaSurface, onSurface = NovaOnSurface,
    onSurfaceVariant = NovaOnSurfaceVariant, surfaceVariant = NovaSecondaryContainer,
    outline = NovaFieldOutline, outlineVariant = NovaOutline,
    error = NovaError,
)

private fun style(size: Int, height: Int, weight: FontWeight = FontWeight.Normal, tracking: Float = 0f) = TextStyle(
    fontFamily = FontFamily.SansSerif, fontSize = size.sp, lineHeight = height.sp,
    fontWeight = weight, letterSpacing = tracking.sp,
)

/** Estilos "Nova / Typography" do Figma, pelo papel de cada texto. */
object NovaType {
    val EquipmentState = style(28, 38, FontWeight.SemiBold)
    val EquipmentSupporting = style(16, 22)
    /** "ALARME DE INCÊNDIO" (Figma 21d). */
    val AlarmTitle = style(28, 28, FontWeight.Bold)
    /** Local do alarme, logo abaixo do título. */
    val AlarmPlace = style(28, 28, FontWeight.SemiBold)
    val HeadlineHome = style(22, 28, FontWeight.SemiBold)
    val ScreenTitle = style(20, 26, FontWeight.Medium)
    /** Título de página com mais peso (Excluir sua conta, Figma 20a). */
    val PageTitle = style(20, 26, FontWeight.SemiBold)
    val TopBar = style(18, 24)
    val Section = style(18, 24, FontWeight.SemiBold)
    /** Unidade no cartão do exaustor ("Apto. 124 · Torre A"). */
    val UnitLabel = style(18, 22, FontWeight.Bold)
    val Highlight = style(16, 22, FontWeight.SemiBold)
    /** Destaque dos cartões de serviço da home, na altura da linha do título. */
    val ServiceHighlight = style(16, 20, FontWeight.SemiBold)
    val Field = style(16, 24)
    val SectionTitle = style(15, 20, FontWeight.SemiBold)
    val ItemTitle = style(15, 20, FontWeight.Medium)
    val Body = style(14, 20)
    /** Texto corrido de leitura (corpo do comunicado e da exclusão de conta). */
    val Reading = style(14, 21)
    val BodyEmphasis = style(14, 20, FontWeight.Medium)
    /** Título de linha expansível ("Dados técnicos"). */
    val DisclosureTitle = style(14, 18, FontWeight.Medium)
    val Button = style(14, 20, FontWeight.Medium)
    val Supporting = style(13, 18)
    val SupportingEmphasis = style(13, 18, FontWeight.Medium)
    /** Letra do "i" informativo de 22 dp. */
    val InfoMark = style(13, 18, FontWeight.SemiBold)
    val Caption = style(12, 16)
    val Badge = style(12, 16, FontWeight.Medium)
    /** Selo compacto de 24 dp (categoria do comunicado e do evento). */
    val BadgeCompact = style(11, 16, FontWeight.Medium)
    val NavLabel = style(11, 16, FontWeight.Medium, 0.5f)
    /** Dados técnicos do evento, como vieram da API. */
    val Code = style(11, 15).copy(fontFamily = FontFamily.Monospace)
    /** Contador de não lidas sobre o sino. */
    val Counter = style(10, 10, FontWeight.SemiBold)
    /** Seta de texto (› ⌄) da linha expansível. */
    val Glyph = style(20, 20)
}

/** Medidas de layout repetidas entre telas. */
object NovaSpacing {
    /** Margem lateral do conteúdo em todas as telas (DESIGN_GUIDE §5). */
    val Margin = 28.dp
}

/** Cantos do Figma, pelo papel de cada superfície. */
object NovaShapes {
    /** Botões, filtros, selos e controles circulares. */
    val Pill = CircleShape
    /** Cartões (home, histórico, mensagens, detalhes). */
    val Card = RoundedCornerShape(16.dp)
    /** Cartão "Unidade ativa · Equipamento" do exaustor. */
    val ContextCard = RoundedCornerShape(18.dp)
    /** Cartão informativo, aviso de exclusão, selos de estado e ícone em quadrado do exaustor. */
    val Strip = RoundedCornerShape(14.dp)
    /** Caixas do código, ícone do cartão de serviço e aviso de preparo da recuperação. */
    val Control = RoundedCornerShape(12.dp)
    /** Campos de texto (Material 3). */
    val Field = RoundedCornerShape(4.dp)
    /** Folha da home, que sobe sobre o cabeçalho roxo. */
    val Sheet = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
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
