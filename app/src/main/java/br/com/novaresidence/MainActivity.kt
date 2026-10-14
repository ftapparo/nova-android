package br.com.novaresidence

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import br.com.novaresidence.core.push.PushOpen

class MainActivity : ComponentActivity() {
    private val model: AppViewModel by viewModels { viewModelFactory { initializer { AppViewModel(application as NovaApplication) } } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Em aparelhos com botões de navegação fixos (ex.: Galaxy J4), o padrão pinta uma película
        // cinza-clara atrás dos botões. Preto com ícones claros integra os botões à moldura do aparelho,
        // do splash nativo (tema) até as telas do app.
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.dark(Color.BLACK))
        // Recriação (ex.: rotação) reaproveita o intent original: o destino só vale na primeira abertura.
        if (savedInstanceState == null) openFromNotification(intent)
        setContent { App(model) }
    }

    // launchMode singleTop: tocar numa notificação com o app aberto chega aqui, sem empilhar outra tela.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        openFromNotification(intent)
    }

    private fun openFromNotification(intent: Intent?) {
        PushOpen.from(intent?.extras)?.let { model.pendingOpen.value = it }
    }
}
