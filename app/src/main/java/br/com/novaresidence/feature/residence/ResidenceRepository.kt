package br.com.novaresidence.feature.residence

import br.com.novaresidence.core.network.ApiClient
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

class ResidenceRepository(private val api: ApiClient) {
    suspend fun me(): Residence = api.get("residence/me")

    /** Confere CPF e celular com o cadastro do condomínio; a API não grava nenhum dos dois. */
    suspend fun checkSelfLink(cpf: String, ddi: String, phone: String): SelfLinkCheck =
        api.post("residence/link/self/check", buildJsonObject {
            put("cpf", cpf)
            putJsonObject("phone") {
                put("ddi", ddi)
                put("number", phone)
            }
        })

    suspend fun confirmSelfLink(confirmation: String): Residence =
        api.post("residence/link/self/confirm", buildJsonObject { put("confirmation", confirmation) })
}
