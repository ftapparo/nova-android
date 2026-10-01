package br.com.novaresidence.feature.residence

import android.content.Context
import java.io.IOException

class ResidencePreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("residence_preferences", Context.MODE_PRIVATE)

    fun languageTag(): String = preferences.getString("language", "pt-BR") ?: "pt-BR"

    fun savePortuguese() {
        if (!preferences.edit().putString("language", "pt-BR").commit()) throw IOException("Não foi possível salvar o idioma.")
    }
}
