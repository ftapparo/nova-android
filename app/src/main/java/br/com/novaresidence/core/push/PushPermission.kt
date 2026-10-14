package br.com.novaresidence.core.push

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import br.com.novaresidence.core.ui.*

/**
 * Ação "Ativar notificações": pede a permissão do Android 13+ enquanto o sistema ainda mostra o pedido;
 * depois (ou em versões anteriores), abre as configurações de notificação do app, ou as do canal do alarme
 * quando só ele está bloqueado. [changed] roda na volta, para reconferir e registrar o aparelho.
 */
@Composable
fun rememberNotificationEnabler(registration: PushRegistration, changed: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { changed() }
    return remember(registration, context) {
        {
            val device = PushDevice.current(context)
            val activity = context.findActivity()
            when {
                !device.notificationsEnabled && Build.VERSION.SDK_INT >= 33 && activity != null && (!registration.permissionAsked() ||
                    ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)) -> {
                    registration.markPermissionAsked()
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                !device.notificationsEnabled -> context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
                !device.alarmChannelEnabled -> context.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    .putExtra(Settings.EXTRA_CHANNEL_ID, PushChannels.FireAlarm))
                else -> changed()
            }
        }
    }
}

/** Abre as configurações do canal do alarme (som e vibração são escolhas do Android, não do app). */
fun openAlarmChannelSettings(context: Context) {
    context.startActivity(Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .putExtra(Settings.EXTRA_CHANNEL_ID, PushChannels.FireAlarm))
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Cartão da home que pede para ativar as notificações (uma ação principal e uma para dispensar). */
@Composable
fun NotificationPrompt(enable: () -> Unit, dismiss: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(NovaPrimarySoft, RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Ative as notificações", Modifier.semantics { heading() }, style = NovaType.ItemTitle, color = NovaOnSurface)
            Text("Receba o alarme de incêndio, o aviso antes de o exaustor desligar e os comunicados do condomínio.",
                style = NovaType.Body, color = NovaOnTint)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            NovaButton("Ativar notificações", enable, Modifier.weight(1f))
            NovaTextLink("Agora não", dismiss, Modifier.width(112.dp), color = NovaOnTint)
        }
    }
}
