package br.com.novaresidence.feature.fire

import br.com.novaresidence.core.network.ApiClient
import java.net.URLEncoder

/** Somente leitura: o app nunca envia comandos à central de incêndio. */
class FireRepository(private val api: ApiClient) {
    suspend fun panel(): CiePanel = api.get("cie/panel")
    suspend fun alarms(): CieAlarms = api.get("cie/alarms/active")
    suspend fun logs(cursor: String?, limit: Int = 50): CieLogPage =
        api.get("cie/logs?limit=$limit" + (cursor?.let { "&cursor=" + URLEncoder.encode(it, "UTF-8") } ?: ""))
}
