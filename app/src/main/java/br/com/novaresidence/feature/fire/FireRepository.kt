package br.com.novaresidence.feature.fire

import br.com.novaresidence.core.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.net.URLEncoder

/** Somente leitura: o app nunca envia comandos à central de incêndio. */
class FireRepository(private val api: ApiClient) {
    private val pretty = Json { prettyPrint = true }

    suspend fun panel(): CiePanel = api.get("cie/panel")

    suspend fun alarms(): CieAlarms {
        val data: JsonObject = api.get("cie/alarms/active")
        return withContext(Dispatchers.Default) {
            val alarms = api.transport.decode<CieAlarms>(data)
            alarms.copy(
                latestAlarmLogs = alarms.latestAlarmLogs.withRaw(data["latestAlarmLogs"]),
                activeAlarms = alarms.activeAlarms.withRaw(data["activeAlarms"]),
            )
        }
    }

    /** Falhas e supervisões ativas agora, com o local de cada uma (Figma 27). */
    suspend fun activeEvents(): CieActiveEvents = api.get("cie/events/active")

    suspend fun logs(cursor: String?, limit: Int = 50): CieLogPage {
        val data: JsonObject = api.get("cie/logs?limit=$limit" + (cursor?.let { "&cursor=" + URLEncoder.encode(it, "UTF-8") } ?: ""))
        return withContext(Dispatchers.Default) {
            val page = api.transport.decode<CieLogPage>(data)
            page.copy(items = page.items.withRaw(data["items"]))
        }
    }

    // The decoded model drops fields the app does not use; the detail screen shows the record as received.
    private fun List<CieLog>.withRaw(source: JsonElement?): List<CieLog> {
        val items = source as? JsonArray ?: return this
        return mapIndexed { index, log -> items.getOrNull(index)?.let { log.copy(raw = pretty.encodeToString(JsonElement.serializer(), it)) } ?: log }
    }
}
