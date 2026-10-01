package br.com.novaresidence

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Em aparelhos com botões de navegação fixos (ex.: Galaxy J4), o padrão pinta uma película
        // cinza-clara atrás dos botões. Preto com ícones claros integra os botões à moldura do aparelho,
        // do splash nativo (tema) até as telas do app.
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.dark(Color.BLACK))
        setContent {
            val model: AppViewModel = viewModel { AppViewModel(applicationContext) }
            App(model)
        }
    }
}
