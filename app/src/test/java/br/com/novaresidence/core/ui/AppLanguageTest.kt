package br.com.novaresidence.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AppLanguageTest {
    @Test
    fun firstRunFollowsSupportedSystemLanguagesAndFallsBackToEnglish() {
        assertEquals(AppLanguage.Portuguese, AppLanguage.fromSystem(Locale.forLanguageTag("pt-BR")))
        assertEquals(AppLanguage.Portuguese, AppLanguage.fromSystem(Locale.forLanguageTag("pt-PT")))
        assertEquals(AppLanguage.English, AppLanguage.fromSystem(Locale.forLanguageTag("en-US")))
        assertEquals(AppLanguage.English, AppLanguage.fromSystem(Locale.forLanguageTag("en-GB")))
        assertEquals(AppLanguage.Spanish, AppLanguage.fromSystem(Locale.forLanguageTag("es-ES")))
        assertEquals(AppLanguage.Spanish, AppLanguage.fromSystem(Locale.forLanguageTag("es-419")))
        assertEquals(AppLanguage.English, AppLanguage.fromSystem(Locale.FRENCH))
        assertEquals(AppLanguage.English, AppLanguage.fromSystem(Locale.JAPANESE))
    }

    @Test
    fun storedTagsRoundTrip() {
        AppLanguage.entries.forEach { assertEquals(it, AppLanguage.fromTag(it.tag)) }
        assertEquals(null, AppLanguage.fromTag("fr"))
    }
}
