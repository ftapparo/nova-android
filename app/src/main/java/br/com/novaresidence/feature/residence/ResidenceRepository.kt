package br.com.novaresidence.feature.residence

import br.com.novaresidence.core.network.ApiClient

class ResidenceRepository(private val api: ApiClient) {
    suspend fun me(): Residence = api.get("residence/me")
}
