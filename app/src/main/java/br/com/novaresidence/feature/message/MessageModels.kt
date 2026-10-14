package br.com.novaresidence.feature.message

import kotlinx.serialization.Serializable
import java.time.Instant

/** Comunicado da administração (`GET /messages`, APP-GUIDE §5.12). */
@Serializable
data class CondoMessage(
    val id: Long,
    val category: String,
    val label: String? = null,
    val title: String,
    val body: String,
    val publishedAt: String,
    val expiresAt: String? = null,
    val read: Boolean = false,
) {
    // Parsed once: the list groups and sorts by date on every recomposition.
    internal val publishedInstant: Instant? by lazy { runCatching { Instant.parse(publishedAt) }.getOrNull() }
}

@Serializable
data class MessageList(val items: List<CondoMessage> = emptyList(), val unreadCount: Int = 0)

/** Nome da categoria (linha da lista, Figma 17). */
fun CondoMessage.categoryLabel(): String = when (category) {
    "manutencao" -> "Manutenção"
    "comunicado" -> "Comunicado"
    "aviso" -> "Aviso"
    "urgente" -> "Urgente"
    else -> "Informativo"
}

/** Etiqueta do cartão e do detalhe (Figma 08d e 18): a da administração ou o nome da categoria. */
fun CondoMessage.badgeLabel(): String = label?.takeIf { it.isNotBlank() } ?: categoryLabel()

data class MessageState(
    /** Carrossel da home (08d), na ordem da API. */
    val latest: List<CondoMessage> = emptyList(),
    val unreadCount: Int = 0,
    val latestError: String? = null,
    /** Lista da tela 17 conforme o filtro; null = ainda não carregada. */
    val list: List<CondoMessage>? = null,
    val unreadOnly: Boolean = false,
    val listLoading: Boolean = false,
    val listError: String? = null,
)
