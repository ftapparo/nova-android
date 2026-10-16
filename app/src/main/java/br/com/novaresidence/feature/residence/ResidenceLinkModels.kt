package br.com.novaresidence.feature.residence

import kotlinx.serialization.Serializable
import java.util.Locale

/** Resposta de `POST /residence/link/self/check` (Figma 07b → 07c). */
@Serializable
data class SelfLinkCheck(val confirmation: String, val person: SelfLinkPerson, val units: List<ResidenceUnit>)

@Serializable
data class SelfLinkPerson(val nome: String, val cpfMasked: String? = null)

/** Dígito verificador do CPF, conferido no app antes de enviar (a API confere de novo). */
fun isValidCpf(digits: String): Boolean {
    if (digits.length != 11 || digits.any { !it.isDigit() } || digits.all { it == digits[0] }) return false
    fun check(length: Int): Int {
        val sum = (0 until length).sumOf { (digits[it] - '0') * (length + 1 - it) }
        return (sum * 10 % 11).let { if (it == 10) 0 else it }
    }
    return check(9) == digits[9] - '0' && check(10) == digits[10] - '0'
}

/**
 * País do seletor de DDI. Bandeira em emoji gerada do código ISO e nome no idioma do app
 * ([Locale.getDisplayCountry]), sem tradução manual.
 */
data class PhoneCountry(val iso: String, val ddi: String) {
    val flag: String get() = iso.uppercase().map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")
    fun name(locale: Locale = Locale.getDefault()): String = Locale("", iso).getDisplayCountry(locale)
    val isBrazil: Boolean get() = ddi == "55"
}

/**
 * Países oferecidos no seletor (Brasil primeiro). Lista curta e fixa: os de onde vêm moradores e
 * proprietários do condomínio e os mais comuns na região; outro país pode ser incluído aqui.
 */
val PhoneCountries = listOf(
    PhoneCountry("BR", "55"), PhoneCountry("PT", "351"), PhoneCountry("US", "1"), PhoneCountry("CA", "1"),
    PhoneCountry("AR", "54"), PhoneCountry("UY", "598"), PhoneCountry("PY", "595"), PhoneCountry("CL", "56"),
    PhoneCountry("BO", "591"), PhoneCountry("PE", "51"), PhoneCountry("CO", "57"), PhoneCountry("VE", "58"),
    PhoneCountry("MX", "52"), PhoneCountry("ES", "34"), PhoneCountry("IT", "39"), PhoneCountry("FR", "33"),
    PhoneCountry("DE", "49"), PhoneCountry("GB", "44"), PhoneCountry("IE", "353"), PhoneCountry("NL", "31"),
    PhoneCountry("BE", "32"), PhoneCountry("CH", "41"), PhoneCountry("AT", "43"), PhoneCountry("JP", "81"),
    PhoneCountry("CN", "86"), PhoneCountry("AU", "61"), PhoneCountry("AO", "244"), PhoneCountry("MZ", "258"),
    PhoneCountry("IL", "972"), PhoneCountry("AE", "971"),
)

/** Celular aceito para o DDI: no Brasil, DDD + 8 ou 9 dígitos; fora, de 6 a 14 dígitos (limite do E.164). */
fun isPlausiblePhone(country: PhoneCountry, digits: String): Boolean =
    if (country.isBrazil) digits.length == 10 || digits.length == 11 else digits.length in 6..14
