package br.com.novaresidence.core.push

import android.os.Bundle

/** Tela que o toque na notificação abre (campo `target` do aviso). */
enum class PushTarget { Fire, Exhaust, Message, Home }

/** Destino pendente de uma notificação tocada, aplicado depois do login e com o acesso liberado. */
data class PushOpen(val target: PushTarget, val messageId: Long? = null) {
    fun toBundle() = Bundle().apply {
        putString(ExtraTarget, target.name)
        messageId?.let { putLong(ExtraMessageId, it) }
    }

    companion object {
        private const val ExtraTarget = "br.com.novaresidence.push.target"
        private const val ExtraMessageId = "br.com.novaresidence.push.messageId"

        fun from(extras: Bundle?): PushOpen? {
            val target = extras?.getString(ExtraTarget)?.let { name -> PushTarget.entries.firstOrNull { it.name == name } } ?: return null
            return PushOpen(target, extras.getLong(ExtraMessageId, 0L).takeIf { it > 0 })
        }
    }
}

/**
 * Aviso recebido da API (mensagem só de dados do FCM; ver `/notifications` no AGENTS.md). Os textos vêm
 * prontos do servidor; o app escolhe canal, ícone e destino e nunca interpreta o conteúdo.
 */
data class PushMessage(
    val type: String,
    val title: String,
    val body: String,
    val channel: String,
    val target: PushTarget,
    val exhaustId: String? = null,
    val messageId: Long? = null,
) {
    val isFireAlarm get() = type == "cie.alarm"

    /** Chave da notificação: um aviso novo do mesmo assunto substitui o anterior (ex.: alarme encerrado substitui o alarme). */
    val tag: String get() = when {
        type.startsWith("cie.alarm") -> "cie-alarm"
        type.startsWith("cie.") -> "cie-status"
        type.startsWith("exhaust.") -> "exhaust:${exhaustId.orEmpty()}"
        type == "message.published" -> "message:${messageId ?: 0}"
        else -> type
    }

    fun open() = PushOpen(target, messageId)

    /**
     * A notificação só aparece se a pessoa não estiver olhando a tela do aviso ([visible] = tela aberta, nulo com
     * o app em segundo plano); nesse caso a própria tela se atualiza. O alarme de incêndio aparece sempre: na
     * tela da central ele não teria som nem vibração.
     */
    fun shouldNotify(visible: PushTarget?): Boolean = isFireAlarm || target != visible

    companion object {
        /** Lê o payload do FCM; devolve null se faltar o essencial (aviso de outra origem ou corrompido). */
        fun from(data: Map<String, String>): PushMessage? {
            val type = data["type"]?.takeIf { it.isNotBlank() } ?: return null
            val title = data["title"]?.takeIf { it.isNotBlank() } ?: return null
            val target = when (data["target"]) {
                "fire" -> PushTarget.Fire
                "exhaust" -> PushTarget.Exhaust
                "message" -> PushTarget.Message
                else -> PushTarget.Home
            }
            // Canal desconhecido (API mais nova que o app) cai em "Avisos gerais"; o alarme nunca depende do texto do servidor.
            val channel = if (type == "cie.alarm") PushChannels.FireAlarm
                else data["channel"]?.takeIf { it in PushChannels.All } ?: PushChannels.General
            return PushMessage(
                type, title, data["body"].orEmpty(), channel, target,
                exhaustId = data["exhaustId"]?.takeIf { it.isNotBlank() },
                messageId = data["messageId"]?.toLongOrNull()?.takeIf { it > 0 },
            )
        }
    }
}
