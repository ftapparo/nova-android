package br.com.novaresidence.core.ui

import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.novaresidence.R

enum class NovaButtonStyle { Primary, Secondary, Outlined, Neutral, Danger }

/**
 * Botão em pílula: 48 dp e texto 14 Medium em todo o app (DESIGN_GUIDE); o Figma varia 48/52 dp e 14/15 sp nas telas de serviço.
 * O ícone opcional fica centralizado junto ao texto, como em "Ligar exaustor".
 */
@Composable
fun NovaButton(
    label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, style: NovaButtonStyle = NovaButtonStyle.Primary,
    icon: Int? = null,
) {
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(painterResource(icon), null, Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(label, style = NovaType.Button, textAlign = TextAlign.Center)
    }
    val sized = modifier.fillMaxWidth().heightIn(min = 48.dp)
    val padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    when (style) {
        NovaButtonStyle.Outlined, NovaButtonStyle.Neutral -> {
            val color = if (style == NovaButtonStyle.Outlined) NovaPrimary else NovaOnSurface
            OutlinedButton(onClick, sized, enabled = enabled, shape = CircleShape, contentPadding = padding,
                border = BorderStroke(1.dp, if (!enabled) NovaDisabledContainer else if (style == NovaButtonStyle.Outlined) NovaPrimary else NovaNeutralOutline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = color, disabledContentColor = NovaDisabledContent), content = content)
        }
        else -> {
            val (container, text) = when (style) {
                NovaButtonStyle.Secondary -> NovaSecondaryContainer to NovaPrimary
                NovaButtonStyle.Danger -> NovaError to Color.White
                else -> NovaPrimary to Color.White
            }
            Button(onClick, sized, enabled = enabled, shape = CircleShape, contentPadding = padding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = container, contentColor = text,
                    // Primário desabilitado mantém o tom roxo (Figma 20/22, "Ligando..."); os demais ficam cinza (Figma 05).
                    disabledContainerColor = if (style == NovaButtonStyle.Primary) NovaPrimary.copy(alpha = 0.55f) else NovaDisabledContainer,
                    disabledContentColor = if (style == NovaButtonStyle.Primary) Color.White else NovaDisabledContent,
                ), content = content)
        }
    }
}

/** Link de texto (ex.: "Esqueci minha senha", "Desconectar") dentro de um alvo de toque de 48 dp. */
@Composable
fun NovaTextLink(
    label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    color: Color = NovaPrimary, style: TextStyle = NovaType.Button, alignment: Alignment = Alignment.Center, underline: Boolean = false,
) {
    Box(modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick), contentAlignment = alignment) {
        Text(label, style = style, color = if (enabled) color else NovaDisabledContent,
            textDecoration = if (underline) TextDecoration.Underline else null)
    }
}

/**
 * Barra superior do Figma: 56 dp e título 18/24 Regular. Com [back], chevron em alvo de 48 dp;
 * o traço do chevron (que tem respiro interno no vetor) e, sem ele, o título alinham à margem de 28 dp.
 */
@Composable
fun NovaTopBar(title: String, back: (() -> Unit)? = null, enabled: Boolean = true, background: Color = NovaBackground, color: Color = NovaOnSurface) {
    Row(Modifier.fillMaxWidth().background(background).height(56.dp).padding(start = if (back != null) 8.dp else 28.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) IconButton(onClick = back, enabled = enabled, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(R.drawable.nova_back), stringResource(R.string.common_back), Modifier.size(24.dp), tint = NovaOnSurface)
        }
        Text(title, Modifier.weight(1f).semantics { heading() }, style = NovaType.TopBar, color = color, maxLines = 1)
    }
}

@Composable
fun NovaIntroduction(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, Modifier.semantics { heading() }, style = NovaType.ScreenTitle)
        Text(description, style = NovaType.Body, color = NovaOnSurfaceVariant)
    }
}

/** Faixa de aviso com borda roxa à esquerda (Figma 07). */
@Composable
fun NovaNotice(message: String, requestId: String? = null, title: String? = null) {
    Row(
        Modifier.fillMaxWidth().background(NovaPrimarySoft).semantics { liveRegion = LiveRegionMode.Polite }
            .drawBehind { drawRect(NovaPrimary, size = Size(4.dp.toPx(), size.height)) }
            .padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        NovaInfoMark(NovaPrimary, border = 1.5.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (title != null) Text(title, style = NovaType.BodyEmphasis, color = NovaPrimary)
            Text(message, style = NovaType.Body, color = NovaOnSurface)
            if (requestId != null) Text(stringResource(R.string.common_support_reference, requestId), style = NovaType.Caption, color = NovaOnSurfaceVariant)
        }
    }
}

/** Selo de estado com texto colorido e centralizado ("Disponível", "Central online", "Alarme"). */
@Composable
fun NovaStatusBadge(label: String, color: Color, container: Color, modifier: Modifier = Modifier, height: Dp = 28.dp, textStyle: TextStyle = NovaType.Badge) {
    Row(modifier.background(container, RoundedCornerShape(14.dp)).heightIn(min = height).padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = textStyle, color = color, maxLines = 1)
    }
}

/**
 * Cartão informativo único do app ("Info strip" do Figma): "i" contornado, texto 13/18 centralizado,
 * mínimo de 64 dp que cresce com o texto. Só a cor muda com o estado (central, manutenção).
 */
@Composable
fun NovaInfoStrip(text: String, modifier: Modifier = Modifier, accent: Color = NovaPrimary, container: Color = NovaInfoSurface) {
    Row(modifier.fillMaxWidth().background(container, RoundedCornerShape(14.dp)).heightIn(min = 64.dp).padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        NovaInfoMark(accent)
        Text(text, Modifier.weight(1f), style = NovaType.Supporting, color = NovaOnTint, textAlign = TextAlign.Center)
    }
}

/** "i" contornado de 22 dp do Figma (cartão informativo e [NovaNotice]). */
@Composable
fun NovaInfoMark(accent: Color, border: Dp = 1.6.dp) {
    Box(Modifier.size(22.dp).border(border, accent, CircleShape), contentAlignment = Alignment.Center) {
        Text("i", fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, color = accent)
    }
}

/** Superfície com contorno de 1 dp e cantos arredondados (cartões da home, histórico e detalhes). */
@Composable
fun NovaCard(modifier: Modifier = Modifier, radius: Dp = 16.dp, background: Color = Color.White, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(radius)
    Column(modifier.clip(shape).background(background).border(1.dp, NovaOutline, shape), content = content)
}

/**
 * Trecho de cartão de grupo desenhado por linha (histórico 29 e mensagens 17), para a lista continuar
 * preguiçosa: a primeira e a última linha levam os cantos; as do meio, só as laterais e o divisor superior.
 * Já recorta o toque no formato do trecho.
 */
fun Modifier.novaCardSegment(first: Boolean, last: Boolean, corner: Dp = 16.dp): Modifier {
    val shape = RoundedCornerShape(topStart = if (first) corner else 0.dp, topEnd = if (first) corner else 0.dp,
        bottomStart = if (last) corner else 0.dp, bottomEnd = if (last) corner else 0.dp)
    return drawBehind {
        val radius = corner.toPx()
        val stroke = 1.dp.toPx()
        val top = if (first) 0f else -radius * 2
        val bottom = if (last) size.height else size.height + radius * 2
        clipRect {
            drawRoundRect(Color.White, Offset(0f, top), Size(size.width, bottom - top), CornerRadius(radius))
            drawRoundRect(NovaOutline, Offset(stroke / 2, top + stroke / 2), Size(size.width - stroke, bottom - top - stroke), CornerRadius(radius), style = Stroke(stroke))
        }
        if (!first) drawLine(NovaOutline, Offset(0f, stroke / 2), Offset(size.width, stroke / 2), stroke)
    }.clip(shape)
}

/** Círculo de 48 dp com chevron, usado nas linhas de configurações (Figma 09, 12 e 15). */
@Composable
fun NovaChevronAction() {
    Box(Modifier.size(48.dp).background(NovaChevronSurface, CircleShape), contentAlignment = Alignment.Center) {
        Icon(painterResource(R.drawable.nova_chevron_forward), null, Modifier.size(20.dp), tint = NovaOnSurfaceVariant)
    }
}

/** Barra de filtros em pílula de 44 dp com linha inferior (histórico de eventos e mensagens, Figma 17 e 29). */
@Composable
fun NovaFilterBar(options: List<String>, selected: Int, height: Dp = 44.dp, onSelect: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth()
        .drawBehind { drawRect(NovaOutline, topLeft = Offset(0f, size.height - 1.dp.toPx()), size = Size(size.width, 1.dp.toPx())) }
        .padding(vertical = 15.dp).horizontalScroll(rememberScrollState()).padding(horizontal = 28.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEachIndexed { index, label ->
            val active = index == selected
            Box(Modifier.heightIn(min = height).widthIn(min = if (height < 44.dp) 0.dp else 83.dp).clip(CircleShape)
                .background(if (active) NovaPrimarySoft else NovaSecondaryContainer)
                .selectable(active, role = Role.Tab) { onSelect(index) }.padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center) {
                Text(label, style = NovaType.BodyEmphasis, color = if (active) NovaPrimary else NovaOnSurface)
            }
        }
    }
}

@Composable
fun NovaSplash() {
    // Mesmo tamanho e mesmo centro (tela inteira) do splash nativo do tema, para a troca não ser percebida.
    Box(Modifier.fillMaxSize().background(NovaHeader), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.nova_logo_white), "Nova Residence",
            Modifier.size(dimensionResource(R.dimen.nova_splash_logo_width), dimensionResource(R.dimen.nova_splash_logo_height)))
    }
}

@Composable
fun NovaLoading(text: String = stringResource(R.string.common_wait)) {
    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(24.dp), color = NovaPrimary, strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text(text, style = NovaType.Body)
    }
}
