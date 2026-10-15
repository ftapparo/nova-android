package br.com.novaresidence.feature.residence

import br.com.novaresidence.R
import br.com.novaresidence.core.network.NovaJson
import br.com.novaresidence.core.ui.uiText
import org.junit.Assert.*
import org.junit.Test

class ResidenceTest {
    private fun unit(id: Int, blocked: Boolean = false, reason: String? = null) = ResidenceUnit(id, "A", "124", "A-124", true, false, true, blocked, reason)
    @Test fun `selection keeps existing unit then prefers first unblocked server unit`() {
        val residence = Residence("ativo", false, null, listOf(unit(1, true), unit(2), unit(3)))
        assertEquals(2, selectResidenceUnit(residence, null))
        assertEquals(3, selectResidenceUnit(residence, 3))
        assertEquals(2, selectResidenceUnit(residence, 999))
        assertNull(selectResidenceUnit(residence.copy(units = emptyList()), 2))
    }
    @Test fun `resident permissions require active unblocked link and staff roles work without unit`() {
        val roles = listOf<String?>(null, "morador", "porteiro", "sindico", "admin")
        roles.forEach { assertTrue(Residence("ativo", false, null, listOf(unit(1))).canSeeServices(it)) }
        listOf<String?>(null, "morador").forEach {
            assertFalse(Residence("pendente", false, null, emptyList()).canSeeServices(it))
            assertFalse(Residence("ativo", true, null, listOf(unit(1, true))).canSeeServices(it))
        }
        listOf("porteiro", "sindico", "admin").forEach {
            assertTrue(Residence("sem-unidade", true, null, emptyList()).canSeeServices(it))
        }
    }
    @Test fun `rotulos da home e do seletor seguem o Figma`() {
        val value = unit(1).copy(quadra = "A", lote = "124")
        assertEquals(uiText(R.string.unit_home_label, "124", "A"), value.homeLabel())
        assertEquals(uiText(R.string.unit_selector_label, "124", "A"), value.selectorLabel())
    }
    @Test fun `pending and missing unit messages and blocked reasons match contract`() {
        assertEquals(uiText(R.string.residence_pending), Residence("pendente", true, null, emptyList()).notice())
        assertEquals(uiText(R.string.residence_no_unit), Residence("sem-unidade", true, null, emptyList()).notice())
        assertEquals(uiText(R.string.unit_blocked_not_resident), unit(1, true, "nao-morador").blockedMessage())
        assertEquals(uiText(R.string.unit_blocked_tenant), unit(1, true, "unidade-com-inquilino").blockedMessage())
        assertEquals(uiText(R.string.unit_blocked_inconsistent), unit(1, true, "cadastro-inconsistente").blockedMessage())
        assertNull(unit(1).blockedMessage())
    }
    @Test fun `nullable person and real labels decode from OpenAPI envelope data`() {
        val result = NovaJson.decodeFromString<Residence>("""{"status":"pendente","blocked":true,"person":null,"units":[]}""")
        assertNull(result.person)
        assertTrue(result.units.isEmpty())
        val linked = NovaJson.decodeFromString<Residence>(
            """{"status":"sem-unidade","blocked":true,"person":{"sequencia":91,"nome":"FULANO","cpfMasked":"•••.456.789-••"},"units":[]}""")
        assertEquals("•••.456.789-••", linked.person!!.cpfMasked)
        // Servidor anterior ao campo: a tela de dados pessoais só omite a linha do CPF.
        assertNull(NovaJson.decodeFromString<ResidencePerson>("""{"sequencia":91,"nome":"FULANO"}""").cpfMasked)
    }
}
