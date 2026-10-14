package br.com.novaresidence.core.push

import br.com.novaresidence.NovaApplication
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Recebe os avisos do FCM, inclusive com o app fechado (o Android sobe o processo só para isto). Roda sem
 * Activity, por isso usa o grafo do processo ([NovaApplication]).
 */
class NovaMessagingService : FirebaseMessagingService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        val graph = (application as NovaApplication).graph
        scope.launch {
            graph.restoreSession()
            graph.registerPush(force = true)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val graph = (application as NovaApplication).graph
        val push = PushMessage.from(message.data) ?: return
        // Já roda fora da thread principal; ler a sessão aqui é rápido e evita exibir aviso a quem saiu.
        graph.restoreSession()
        if (graph.session.user.value == null) {
            // Sessão encerrada sem passar pelo logout (ex.: renovação recusada): o token antigo deixa de valer
            // e a API o invalida no próximo envio.
            FirebaseMessaging.getInstance().deleteToken()
            graph.push.forget()
            return
        }
        if (push.shouldNotify(graph.visibleScreen)) PushNotifier.show(this, push)
        graph.pushEvents.tryEmit(push)
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
