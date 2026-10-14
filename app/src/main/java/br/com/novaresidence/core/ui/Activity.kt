package br.com.novaresidence.core.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/** Activity por trás do contexto do Compose; permissões do Android só são pedidas a partir dela. */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
