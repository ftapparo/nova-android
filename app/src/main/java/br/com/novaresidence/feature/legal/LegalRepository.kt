package br.com.novaresidence.feature.legal

import br.com.novaresidence.core.network.ApiClient
import br.com.novaresidence.core.network.ApiErrorKind
import br.com.novaresidence.core.network.ApiException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Termo de Uso e Política de Privacidade (APP-GUIDE §5.13). Compartilhado por cadastro, configurações e aceite. */
class LegalRepository(private val api: ApiClient) {
    private val mutex = Mutex()
    private var cached: List<LegalDocument>? = null

    /** Catálogo público (sem token). Fica em memória: muda só com nova versão publicada. */
    suspend fun documents(refresh: Boolean = false): List<LegalDocument> = mutex.withLock {
        cached?.takeUnless { refresh } ?: api.transport.get<List<LegalDocument>>("legal").also { cached = it }
    }

    /**
     * Versão vigente do Termo, sempre consultada na hora: o processo do app pode durar dias, e uma versão
     * publicada nesse meio-tempo faria o cadastro mandar a anterior (a API recusa com 400).
     */
    suspend fun termsVersion(): String = documents(refresh = true).firstOrNull { it.document == TermsDocument }?.version
        ?: throw ApiException(ApiErrorKind.InvalidResponse)

    /** Aceite da versão mostrada; 409 = o Termo mudou e a versão nova precisa ser lida antes. */
    suspend fun acceptTerms(version: String) {
        api.post<JsonElement>("legal/terms/accept", buildJsonObject { put("version", version) })
    }
}
