package br.com.novaresidence.core.ui

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

/** Idiomas com tradução completa. Português é o padrão; os textos ficam em `values/`, `values-en/` e `values-es/`. */
enum class AppLanguage(val tag: String) {
    Portuguese("pt-BR"), English("en"), Spanish("es");

    val locale: Locale get() = Locale.forLanguageTag(tag)

    companion object {
        fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }

        /** Regra da primeira abertura: português, inglês ou espanhol do aparelho; qualquer outro idioma vira inglês, o mais compreendido. */
        fun fromSystem(locale: Locale): AppLanguage = when (locale.language) {
            "pt" -> Portuguese
            "es" -> Spanish
            else -> English
        }
    }
}

/**
 * Idioma do app, sempre gravado. O idioma do Android só é lido na primeira abertura depois da instalação;
 * daí em diante vale apenas a escolha do usuário na tela 14, mesmo que o idioma do aparelho mude.
 */
class AppLanguageStore(private val context: Context) {
    private val preferences = context.getSharedPreferences("app_language", Context.MODE_PRIVATE)

    fun current(): AppLanguage = AppLanguage.fromTag(preferences.getString(KEY, null)) ?: firstRun()

    private fun firstRun(): AppLanguage {
        // Versões anteriores gravavam "pt-BR" fixo (não havia outra opção): quem atualiza passa pela detecção uma vez.
        context.deleteSharedPreferences("residence_preferences")
        return AppLanguage.fromSystem(Resources.getSystem().configuration.locales[0]).also { save(it) }
    }

    /** Grava de forma síncrona: a Activity é recriada logo em seguida e lê o valor em `attachBaseContext`. */
    fun save(language: AppLanguage): Boolean = preferences.edit().putString(KEY, language.tag).commit()

    companion object {
        private const val KEY = "language"

        /**
         * Aplica o idioma gravado antes do primeiro quadro (Application e Activity). `Locale.setDefault` cobre
         * os formatadores de data e número que usam o locale padrão.
         */
        fun wrap(base: Context): Context {
            val locale = AppLanguageStore(base).current().locale
            Locale.setDefault(locale)
            val configuration = Configuration(base.resources.configuration)
            configuration.setLocale(locale)
            return base.createConfigurationContext(configuration)
        }
    }
}

/**
 * Contexto no idioma atual do app. O serviço do FCM e o contexto da aplicação não são recriados quando o
 * usuário troca o idioma: quem monta texto fora da Activity (canais de notificação) passa por aqui.
 */
fun Context.localized(): Context = AppLanguageStore.wrap(this)
