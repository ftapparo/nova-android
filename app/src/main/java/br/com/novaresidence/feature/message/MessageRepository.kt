package br.com.novaresidence.feature.message

import br.com.novaresidence.core.network.ApiClient

/** Comunicados do condomínio: só leitura e marcação de lido; quem publica é a administração. */
class MessageRepository(private val api: ApiClient) {
    suspend fun list(unreadOnly: Boolean = false, limit: Int = 50): MessageList =
        api.get("messages?filter=${if (unreadOnly) "unread" else "all"}&limit=$limit")

    suspend fun markRead(id: Long): CondoMessage = api.post("messages/$id/read")
}
