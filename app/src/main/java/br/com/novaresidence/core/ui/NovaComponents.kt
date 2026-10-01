package br.com.novaresidence.core.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import br.com.novaresidence.R

@Composable
fun NovaButton(
    label: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, secondary: Boolean = false, quiet: Boolean = false, outlined: Boolean = false,
) {
    if (outlined) {
        OutlinedButton(
            onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
            shape = RoundedCornerShape(999.dp), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            border = BorderStroke(1.dp, if (enabled) NovaPrimary else NovaDisabledContent),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NovaPrimary, disabledContentColor = NovaDisabledContent),
        ) { Text(label, style = MaterialTheme.typography.labelLarge) }
        return
    }
    if (quiet) {
        TextButton(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 48.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
        return
    }
    Button(
        onClick = onClick, enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(999.dp), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (secondary) NovaSecondaryContainer else NovaPrimary,
            contentColor = if (secondary) NovaPrimary else Color.White,
            disabledContainerColor = NovaDisabledContainer, disabledContentColor = NovaDisabledContent,
        ),
    ) { Text(label, style = MaterialTheme.typography.labelLarge) }
}

@Composable
fun NovaTopBar(title: String, back: (() -> Unit)? = null, enabled: Boolean = true) {
    Row(Modifier.fillMaxWidth().background(NovaBackground).heightIn(min = 56.dp).padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (back != null) IconButton(onClick = back, enabled = enabled) {
            Image(painterResource(R.drawable.nova_back), "Voltar", Modifier.size(24.dp))
        } else Spacer(Modifier.width(8.dp))
        Text(title, Modifier.weight(1f).padding(horizontal = 8.dp), style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.width(8.dp))
    }
}

@Composable
fun NovaIntroduction(title: String, description: String) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun NovaNotice(message: String, requestId: String? = null, title: String? = null) {
    Row(
        Modifier.fillMaxWidth().background(NovaPrimarySoft).semantics { liveRegion = LiveRegionMode.Polite }
            .drawBehind { if (title != null) drawRect(NovaPrimary, size = Size(4.dp.toPx(), size.height)) }
            .padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.nova_info), null, Modifier.size(24.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (title != null) Text(title, style = MaterialTheme.typography.bodySmall, color = NovaPrimary)
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (requestId != null) Text("Referência para suporte: $requestId", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun NovaSplash() {
    Box(Modifier.fillMaxSize().background(NovaHeader).systemBarsPadding(), contentAlignment = Alignment.Center) {
        Image(painterResource(R.drawable.nova_logo_white), "Nova Residence", Modifier.size(244.dp, 98.dp))
    }
}

@Composable
fun NovaLoading() {
    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.Center) {
        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        Spacer(Modifier.width(12.dp))
        Text("Aguarde…", style = MaterialTheme.typography.bodyMedium)
    }
}
