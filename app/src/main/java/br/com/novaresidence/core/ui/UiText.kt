package br.com.novaresidence.core.ui

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Texto produzido fora da tela (ViewModel, modelo, erro da API). Guarda o recurso, não a frase: a tela resolve
 * no idioma atual e os testes JVM comparam o id sem precisar de `Context`. [Raw] é texto que já veio pronto da API.
 */
sealed interface UiText {
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : UiText
    data class Plural(@PluralsRes val id: Int, val count: Int, val args: List<Any> = emptyList()) : UiText
    data class Raw(val text: String) : UiText
}

fun uiText(@StringRes id: Int, vararg args: Any): UiText = UiText.Res(id, args.toList())

fun uiPlural(@PluralsRes id: Int, count: Int, vararg args: Any): UiText = UiText.Plural(id, count, args.toList())

fun String.asUiText(): UiText = UiText.Raw(this)

/** Argumentos também podem ser [UiText] (ex.: o nome de uma categoria dentro de uma frase). */
fun UiText.resolve(context: Context): String = when (this) {
    is UiText.Res -> context.getString(id, *args.map { (it as? UiText)?.resolve(context) ?: it }.toTypedArray())
    is UiText.Plural -> context.resources.getQuantityString(id, count, *args.map { (it as? UiText)?.resolve(context) ?: it }.toTypedArray())
    is UiText.Raw -> text
}

@Composable
@ReadOnlyComposable
fun UiText.text(): String {
    // Ler a configuração inscreve a composição nas mudanças dela, como faz o stringResource.
    LocalConfiguration.current
    return resolve(LocalContext.current)
}
