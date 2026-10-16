package br.com.novaresidence.feature.residence

import br.com.novaresidence.core.session.isStaffRole
import br.com.novaresidence.core.ui.uiText
import br.com.novaresidence.core.ui.UiText
import br.com.novaresidence.R
import kotlinx.serialization.Serializable

@Serializable
data class ResidencePerson(
    val sequencia: Int, val nome: String,
    /** "•••.456.789-••" (a API nunca envia o CPF completo); nulo se ausente no cadastro ou em servidor antigo. */
    val cpfMasked: String? = null,
)

@Serializable
data class ResidenceUnit(
    val sequencia: Int, val quadra: String, val lote: String, val label: String,
    val morador: Boolean, val proprietario: Boolean, val locatario: Boolean,
    val blocked: Boolean, val blockedReason: String?,
)

@Serializable
data class Residence(val status: String, val blocked: Boolean, val person: ResidencePerson?, val units: List<ResidenceUnit>) {
    fun notice(): UiText? = when (status) {
        "pendente" -> uiText(R.string.residence_pending)
        "sem-unidade" -> uiText(R.string.residence_no_unit)
        "ativo" -> if (blocked) uiText(R.string.residence_blocked) else null
        else -> uiText(R.string.residence_unknown)
    }
    fun canSeeServices(role: String?): Boolean = isStaffRole(role) ||
        (status == "ativo" && !blocked && units.any { !it.blocked })
}

fun ResidenceUnit.blockedMessage(): UiText? = if (!blocked) null else uiText(when (blockedReason) {
    "nao-morador" -> R.string.unit_blocked_not_resident
    "unidade-com-inquilino" -> R.string.unit_blocked_tenant
    "cadastro-inconsistente" -> R.string.unit_blocked_inconsistent
    else -> R.string.unit_blocked_other
})

private val NameParticles = setOf("da", "das", "de", "do", "dos", "e")

/** "FLAVIO EDUARDO DE SOUZA" (cadastro do ERP) → "Flavio Eduardo de Souza". */
fun String.personName(): String = trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }.mapIndexed { index, word ->
    if (index > 0 && word in NameParticles) word else word.replaceFirstChar { it.titlecase() }
}.joinToString(" ")

/** Formato da home (Figma 08a): "Apto. 124 - Torre A". */
fun ResidenceUnit.homeLabel(): UiText = uiText(R.string.unit_home_label, lote.trim(), quadra.trim())

/** Formato do cartão do exaustor (Figma 19): "Apto. 124 · Torre A". */
fun ResidenceUnit.cardLabel(): UiText = uiText(R.string.unit_card_label, lote.trim(), quadra.trim())

/** Formato do seletor de unidades e dos dados pessoais (Figma 10 e 15): "Torre A · Apto. 124". */
fun ResidenceUnit.selectorLabel(): UiText = uiText(R.string.unit_selector_label, lote.trim(), quadra.trim())

fun selectResidenceUnit(residence: Residence, selected: Int?): Int? =
    residence.units.firstOrNull { it.sequencia == selected }?.sequencia
        ?: residence.units.firstOrNull { !it.blocked }?.sequencia
        ?: residence.units.firstOrNull()?.sequencia
