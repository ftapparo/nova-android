package br.com.novaresidence.feature.residence

import android.content.Context

/**
 * Unidade ativa escolhida na tela 10, para o app reabrir nela. Guarda só o número da unidade (não é dado pessoal
 * nem token). Se a unidade deixou de existir ou não pertence mais à conta, [selectResidenceUnit] cai na regra inicial.
 */
class SelectedUnitStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("selected_unit", Context.MODE_PRIVATE)

    fun read(): Int? = if (preferences.contains(KEY)) preferences.getInt(KEY, 0) else null

    fun save(unit: Int?) {
        preferences.edit().apply { if (unit == null) remove(KEY) else putInt(KEY, unit) }.apply()
    }

    private companion object {
        const val KEY = "unit"
    }
}
