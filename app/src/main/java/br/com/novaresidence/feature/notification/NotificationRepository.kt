package br.com.novaresidence.feature.notification

import br.com.novaresidence.core.network.ApiClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Preferências de aviso da conta (`/notifications/preferences`). O alarme de incêndio não é desligável aqui. */
@Serializable
data class NotificationPreferences(
    val exhaustExpiring: Boolean = true,
    /** Minutos antes do desligamento automático; fixo no servidor. */
    val exhaustExpiringMinutes: Int = 15,
    val exhaustOff: Boolean = true,
    val messages: Boolean = true,
    /** Alertas da central além do alarme (falha, reinício, sem comunicação…); só vale para a equipe. */
    val cieStaffAlerts: Boolean = true,
)

enum class NotificationOption(val field: String) {
    ExhaustExpiring("exhaustExpiring"), ExhaustOff("exhaustOff"), Messages("messages"), CieStaffAlerts("cieStaffAlerts"),
}

fun NotificationPreferences.with(option: NotificationOption, enabled: Boolean) = when (option) {
    NotificationOption.ExhaustExpiring -> copy(exhaustExpiring = enabled)
    NotificationOption.ExhaustOff -> copy(exhaustOff = enabled)
    NotificationOption.Messages -> copy(messages = enabled)
    NotificationOption.CieStaffAlerts -> copy(cieStaffAlerts = enabled)
}

@Serializable
data class TestResult(val targets: Int, val sent: Int, val failed: Int)

class NotificationRepository(private val api: ApiClient) {
    suspend fun preferences(): NotificationPreferences = api.get("notifications/preferences")

    suspend fun update(option: NotificationOption, enabled: Boolean): NotificationPreferences =
        api.put("notifications/preferences", buildJsonObject { put(option.field, enabled) })

    /** Push de teste para os aparelhos desta conta. */
    suspend fun sendTest(): TestResult = api.post("notifications/test")
}
