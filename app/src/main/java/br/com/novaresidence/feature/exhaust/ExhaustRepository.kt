package br.com.novaresidence.feature.exhaust

import br.com.novaresidence.core.network.ApiClient
import br.com.novaresidence.core.network.ApiErrorKind
import br.com.novaresidence.core.network.ApiException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ExhaustRepository(private val api: ApiClient) {
    private companion object {
        const val DEFAULT_DURATION_MINUTES = 180
    }
    suspend fun list(): List<Exhaust> = api.get("exhausts")

    suspend fun activate(id: String): Exhaust {
        if (!Regex("[A-Za-z0-9_-]+").matches(id)) throw ApiException(ApiErrorKind.InvalidResponse)
        // A API controla o desligamento após a duração solicitada.
        return api.post("exhausts/$id/on", buildJsonObject { put("minutes", DEFAULT_DURATION_MINUTES) })
    }
}
