package br.com.novaresidence.core.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.com.novaresidence.R
import br.com.novaresidence.core.ui.localized

/**
 * Canais de notificação, com os mesmos ids que a API envia. O som de um canal não pode mudar depois de
 * criado: para trocar o do alarme é preciso um id novo (ex.: `cie_alarm_v2`).
 */
object PushChannels {
    const val FireAlarm = "cie_alarm"
    const val FireStatus = "cie_status"
    const val FireInfo = "cie_info"
    const val Exhaust = "exhaust"
    const val Messages = "messages"
    const val General = "general"
    val All = setOf(FireAlarm, FireStatus, FireInfo, Exhaust, Messages, General)

    private fun Context.channel(id: String): NotificationChannel = when (id) {
        FireAlarm -> NotificationChannel(id, getString(R.string.channel_fire_alarm), NotificationManager.IMPORTANCE_HIGH).apply {
            description = getString(R.string.channel_fire_alarm_description)
            // Som de alarme no volume de alarme: o toque comum pode estar baixo ou no silencioso.
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 800, 400, 800, 400, 800)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }
        FireStatus -> NotificationChannel(id, getString(R.string.channel_fire_status), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = getString(R.string.channel_fire_status_description)
        }
        FireInfo -> NotificationChannel(id, getString(R.string.channel_fire_info), NotificationManager.IMPORTANCE_LOW).apply {
            description = getString(R.string.channel_fire_info_description)
        }
        Exhaust -> NotificationChannel(id, getString(R.string.channel_exhaust), NotificationManager.IMPORTANCE_HIGH).apply {
            description = getString(R.string.channel_exhaust_description)
        }
        Messages -> NotificationChannel(id, getString(R.string.channel_messages), NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = getString(R.string.channel_messages_description)
        }
        else -> NotificationChannel(General, getString(R.string.channel_general), NotificationManager.IMPORTANCE_DEFAULT)
    }

    /** Canais de todos os perfis. Os da equipe só aparecem no Android quando o primeiro aviso deles chega. */
    fun createBase(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val localized = context.localized()
        manager.createNotificationChannels(listOf(FireAlarm, Exhaust, Messages, General).map { localized.channel(it) })
    }

    /** Recriar um canal existente só atualiza nome e descrição (no idioma atual do app); as escolhas do usuário ficam. */
    fun ensure(context: Context, id: String) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(context.localized().channel(id))
    }
}

/** Monta e exibe a notificação de um aviso. O toque abre o app no destino do aviso. */
object PushNotifier {
    fun canNotify(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled() &&
            (Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)

    fun show(context: Context, push: PushMessage) {
        if (!canNotify(context)) return
        PushChannels.ensure(context, push.channel)
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        launch.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        launch.putExtras(push.open().toBundle())
        val pending = PendingIntent.getActivity(context, push.tag.hashCode(), launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val icon = when (push.target) {
            PushTarget.Fire -> R.drawable.nova_alarm_nav
            PushTarget.Exhaust -> R.drawable.nova_exhaust_nav
            else -> R.drawable.nova_bell
        }
        val notification = NotificationCompat.Builder(context, push.channel)
            .setSmallIcon(icon)
            .setContentTitle(push.title)
            .setContentText(push.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(push.body))
            .setColor(ContextCompat.getColor(context, if (push.isFireAlarm) R.color.nova_alarm else R.color.nova_primary))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .apply {
                if (push.isFireAlarm) {
                    setCategory(NotificationCompat.CATEGORY_ALARM)
                    setPriority(NotificationCompat.PRIORITY_MAX)
                    setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                } else if (push.target == PushTarget.Message) {
                    // Comunicado é interno do condomínio: na tela bloqueada aparece só que chegou um comunicado.
                    setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                }
            }
            .build()
        try {
            NotificationManagerCompat.from(context).notify(push.tag, 0, notification)
        } catch (_: SecurityException) {
            // Permissão revogada entre a checagem e o envio: o aviso não é exibido, como com notificações desligadas.
        }
    }
}
