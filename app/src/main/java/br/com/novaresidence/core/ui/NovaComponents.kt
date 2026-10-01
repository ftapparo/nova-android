package br.com.novaresidence.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.novaresidence.R

/** Botão em pílula do Figma: altura mínima 48, texto 14 Medium. */
@Composable
fun NovaButton(
    label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, secondary: Boolean = false, outlined: Boolean = false, icon: Int? = null,
) {
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Icon(painterResource(icon), null, Modifier.size(20.dp), tint = if (enabled) NovaPrimary else NovaDisabledContent)
            Spacer(Modifier.width(8.dp))
            Text(label, Modifier.weight(1f).padding(end = 28.dp), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        } else Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
    }
    val padding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    val shape = RoundedCornerShape(999.dp)
    if (outlined) OutlinedButton(onClick, modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled, shape = shape, contentPadding = padding,
        border = BorderStroke(1.dp, if (enabled) NovaPrimary else NovaDisabledContent),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = NovaPrimary, disabledContentColor = NovaDisabledContent), content = content)
    else Button(onClick, modifier.fillMaxWidth().heightIn(min = 48.dp), enabled = enabled, shape = shape, contentPadding = padding,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (secondary) NovaSecondaryContainer else NovaPrimary,
            contentColor = if (secondary) NovaPrimary else Color.White,
            // Primário desabilitado mantém o tom roxo (Figma 15, "Ligando ..."); secundário fica cinza (Figma 05).
            disabledContainerColor = if (secondary) NovaDisabledContainer else NovaPrimary.copy(alpha = 0.6f),
            disabledContentColor = if (secondary) NovaDisabledContent else Color.White.copy(alpha = 0.85f),
        ), content = content)
}

/** Link de texto (ex.: "Esqueci minha senha"): 20 dp visíveis dentro de um alvo de toque de 48 dp. */
@Composable
fun NovaTextLink(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = NovaPrimary) {
    Box(modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (enabled) color else NovaDisabledContent, textAlign = TextAlign.Center)
    }
}

/** Barra superior do Figma: 56 dp, fundo da tela, chevron em alvo de 48 dp e título 18/24 Regular. */
@Composable
fun NovaTopBar(title: String, back: (() -> Unit)? = null, enabled: Boolean = true, background: Color = NovaBackground) {
    Row(Modifier.fillMaxWidth().background(background).height(56.dp).padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) IconButton(onClick = back, enabled = enabled, modifier = Modifier.size(48.dp)) {
            Icon(painterResource(R.drawable.nova_back), "Voltar", Modifier.size(24.dp), tint = NovaOnSurface)
        } else Spacer(Modifier.width(8.dp))
        Text(title, Modifier.weight(1f).semantics { heading() }, style = MaterialTheme.typography.titleLarge, maxLines = 1)
    }
}

/** Faixa "Unidade ativa" das telas de serviço (exaustor e central de incêndio). */
@Composable
fun NovaUnitContext(label: String?, background: Color = NovaContextSubtle, onClick: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().background(background).heightIn(min = 48.dp)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("▦", Modifier.width(18.dp), fontSize = 12.sp, lineHeight = 16.sp, color = NovaPrimary, textAlign = TextAlign.Center)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Unidade ativa", style = MaterialTheme.typography.bodySmall, color = NovaOnSurfaceVariant)
            Text(label ?: "Nenhuma unidade ativa", style = MaterialTheme.typography.labelMedium, color = NovaHeader)
        }
    }
}

@Composable
fun NovaIntroduction(title: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineMedium)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = NovaOnSurfaceVariant)
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
        Image(painterResource(R.drawable.nova_info), null, Modifier.size(24.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.bodySmall, color = NovaPrimary)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = NovaOnSurface)
            if (requestId != null) Text("Referência para suporte: $requestId", style = MaterialTheme.typography.bodySmall, color = NovaOnSurfaceVariant)
        }
    }
}

/** Selo de estado em pílula (Figma 09–13): altura 32, texto 12 Medium. */
@Composable
fun NovaStatusChip(label: String, color: Color, container: Color, leading: @Composable () -> Unit) {
    Row(Modifier.background(container, CircleShape).heightIn(min = 32.dp).padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        CompositionLocalProvider(LocalContentColor provides color) { leading() }
        Text(label, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

/** Nota de rodapé com marcador (✓) usada nas telas do exaustor. */
@Composable
fun NovaFootnote(text: String, mark: String = "✓") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(mark, Modifier.width(18.dp), fontSize = 12.sp, lineHeight = 16.sp, color = NovaOnSurfaceVariant, textAlign = TextAlign.Center)
        Text(text, fontSize = 13.sp, lineHeight = 16.sp, color = NovaOnSurfaceVariant)
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
fun NovaLoading(text: String = "Aguarde…") {
    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(Modifier.size(24.dp), color = NovaPrimary, strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
