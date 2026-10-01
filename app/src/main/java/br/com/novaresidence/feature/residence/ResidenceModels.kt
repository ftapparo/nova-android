package br.com.novaresidence.feature.residence

import kotlinx.serialization.Serializable

@Serializable
data class ResidencePerson(val sequencia: Int, val nome: String)

@Serializable
data class ResidenceUnit(
    val sequencia: Int, val quadra: String, val lote: String, val label: String,
    val morador: Boolean, val proprietario: Boolean, val locatario: Boolean,
    val blocked: Boolean, val blockedReason: String?,
)

@Serializable
data class Residence(val status: String, val blocked: Boolean, val person: ResidencePerson?, val units: List<ResidenceUnit>) {
    fun notice(): String? = when (status) {
        "pendente" -> "Cadastro em análise pela administração"
        "sem-unidade" -> "Seu cadastro não possui unidade ativa"
        "ativo" -> if (blocked) "Seu acesso está bloqueado. Fale com a administração." else null
        else -> "Não foi possível verificar seu vínculo. Fale com a administração."
    }
    fun canSeeServices(role: String?): Boolean = role in setOf("porteiro", "sindico", "admin") ||
        (status == "ativo" && !blocked && units.any { !it.blocked })
}

fun ResidenceUnit.blockedMessage(): String? = if (!blocked) null else when (blockedReason) {
    "nao-morador" -> "Você não consta como morador desta unidade."
    "unidade-com-inquilino" -> "Esta unidade possui inquilino cadastrado; o acesso é do inquilino."
    "cadastro-inconsistente" -> "Seu cadastro precisa ser regularizado na administração."
    else -> "O acesso a esta unidade está bloqueado. Fale com a administração."
}

private val NameParticles = setOf("da", "das", "de", "do", "dos", "e")

/** "FLAVIO EDUARDO DE SOUZA" (cadastro do ERP) → "Flavio Eduardo de Souza". */
fun String.personName(): String = trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }.mapIndexed { index, word ->
    if (index > 0 && word in NameParticles) word else word.replaceFirstChar { it.titlecase() }
}.joinToString(" ")

/** Formato da home e do exaustor (Figma 08a/14): "Apto. 124 - Torre A". */
fun ResidenceUnit.homeLabel(): String = "Apto. ${lote.trim()} - Torre ${quadra.trim()}"

/** Formato do seletor de unidades (Figma 22): "Torre A · Apto. 124". */
fun ResidenceUnit.selectorLabel(): String = "Torre ${quadra.trim()} · Apto. ${lote.trim()}"

fun selectResidenceUnit(residence: Residence, selected: Int?): Int? =
    residence.units.firstOrNull { it.sequencia == selected }?.sequencia
        ?: residence.units.firstOrNull { !it.blocked }?.sequencia
        ?: residence.units.firstOrNull()?.sequencia
