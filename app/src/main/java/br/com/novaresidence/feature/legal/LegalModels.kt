package br.com.novaresidence.feature.legal

import kotlinx.serialization.Serializable

const val TermsDocument = "termo-de-uso"
const val PrivacyDocument = "politica-de-privacidade"

/** Documento vigente (`GET /legal`, público). O PDF é o oficial; o HTML é a leitura no celular. */
@Serializable
data class LegalDocument(
    val document: String,
    val title: String,
    val version: String,
    val effectiveDate: String,
    val htmlUrl: String,
    val pdfUrl: String,
)

data class LegalState(
    val documents: List<LegalDocument>? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val accepting: Boolean = false,
    val acceptError: String? = null,
) {
    fun find(document: String): LegalDocument? = documents?.firstOrNull { it.document == document }
}
