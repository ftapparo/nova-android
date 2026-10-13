package br.com.novaresidence.feature.legal

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import br.com.novaresidence.core.ui.*
import kotlin.math.roundToInt

// Corpo do texto na folha de estilo das páginas públicas (src/v3/legal/legal.documents.ts na API).
private const val PageBodyPx = 16f

private fun fallbackTitle(document: String) = if (document == TermsDocument) "Termo de Uso" else "Política de Privacidade"

/**
 * Termo de Uso ou Política de Privacidade (Figma 13 e 14): a página oficial publicada pela API, para o
 * texto do app ser sempre o vigente. O PDF (documento oficial) abre fora do app.
 */
@Composable
fun LegalDocumentScreen(state: LegalState, document: String, back: () -> Unit, retry: () -> Unit) {
    val doc = state.find(document)
    Column(Modifier.fillMaxSize().background(Color.White)) {
        NovaTopBar(doc?.title ?: fallbackTitle(document), back)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                doc != null -> LegalWebView(doc.htmlUrl, Modifier.fillMaxSize())
                state.loading -> NovaLoading("Carregando documento…")
                else -> LegalError(state.error ?: "Não foi possível carregar o documento.", retry)
            }
        }
    }
}

@Composable
private fun LegalError(message: String, retry: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        NovaNotice(message)
        NovaButton("Tentar novamente", retry, style = NovaButtonStyle.Secondary)
    }
}

/**
 * WebView restrito: sem JavaScript (a página não usa), sem acesso a arquivos e só navegando no próprio
 * site; qualquer outro link (PDF, e-mail) abre no app do sistema.
 */
@Composable
private fun LegalWebView(url: String, modifier: Modifier) {
    var attempt by remember(url) { mutableIntStateOf(0) }
    var loading by remember(url, attempt) { mutableStateOf(true) }
    var failed by remember(url, attempt) { mutableStateOf(false) }
    val host = remember(url) { Uri.parse(url).host }
    Box(modifier) {
        if (failed) LegalError("Não foi possível carregar o documento. Verifique sua conexão.") { attempt++ }
        else key(url, attempt) {
            AndroidView(factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    // Sem cache: o endereço da página não muda entre versões, e a tela de aceite registra a versão
                    // vigente do catálogo — uma cópia guardada mostraria o texto anterior para ser aceito.
                    settings.cacheMode = WebSettings.LOAD_NO_CACHE
                    // A página usa o corpo padrão da web (16 px), que no WebView vira 16 dp e fica maior que o
                    // texto do app (NovaType.Body, 14 sp). A escala explícita também segue a fonte do sistema.
                    settings.textZoom = (NovaType.Body.fontSize.value / PageBodyPx * context.resources.configuration.fontScale * 100).roundToInt()
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            val target = request.url
                            if (target.host == host && target.scheme == "https" && !target.path.orEmpty().endsWith("/pdf")) return false
                            try { context.startActivity(Intent(Intent.ACTION_VIEW, target)) } catch (_: ActivityNotFoundException) { }
                            return true
                        }
                        override fun onPageFinished(view: WebView, url: String) { loading = false }
                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                            if (request.isForMainFrame) failed = true
                        }
                    }
                    loadUrl(url)
                }
            }, modifier = Modifier.fillMaxSize())
        }
        if (loading && !failed) Box(Modifier.fillMaxSize().background(Color.White), contentAlignment = Alignment.TopCenter) {
            NovaLoading("Carregando documento…")
        }
    }
}

/** Texto com "Termo de Uso" e "Política de Privacidade" tocáveis, que abrem os documentos. */
@Composable
fun LegalLinksText(prefix: String, openLegal: (String) -> Unit, modifier: Modifier = Modifier, style: TextStyle = NovaType.Supporting,
    textAlign: TextAlign = TextAlign.Start) {
    val link = TextLinkStyles(SpanStyle(color = NovaPrimary, fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline))
    val text = buildAnnotatedString {
        append(prefix)
        withLink(LinkAnnotation.Clickable(TermsDocument, link) { openLegal(TermsDocument) }) { append("Termo de Uso") }
        append(" e a ")
        withLink(LinkAnnotation.Clickable(PrivacyDocument, link) { openLegal(PrivacyDocument) }) { append("Política de Privacidade") }
        append(".")
    }
    Text(text, modifier, style = style, color = NovaOnSurfaceVariant, textAlign = textAlign)
}

/**
 * Aceite do Termo de Uso para conta antiga ou versão nova (`user.mustAcceptTerms`). Sem tela no Figma:
 * segue o padrão das telas de autenticação. Bloqueia o app até aceitar ou sair.
 */
@Composable
fun TermsAcceptanceScreen(state: LegalState, accept: () -> Unit, logout: () -> Unit, openLegal: (String) -> Unit, retry: () -> Unit) {
    val terms = state.find(TermsDocument)
    Column(Modifier.fillMaxSize().background(NovaBackground).systemBarsPadding()) {
        NovaTopBar("Termo de Uso")
        Column(Modifier.padding(horizontal = 28.dp).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // O app não sabe se a conta já aceitou uma versão anterior: o título serve à conta antiga e à versão nova.
            Text("Aceite do Termo de Uso", style = NovaType.ScreenTitle, color = NovaOnSurface)
            Text("Para continuar usando o aplicativo, leia e aceite o Termo de Uso" + (terms?.let { " (versão ${it.version})" } ?: "") + ".",
                style = NovaType.Body, color = NovaOnSurfaceVariant)
        }
        Box(Modifier.weight(1f).fillMaxWidth().background(Color.White)) {
            when {
                terms != null -> LegalWebView(terms.htmlUrl, Modifier.fillMaxSize())
                state.loading -> NovaLoading("Carregando documento…")
                else -> LegalError(state.error ?: "Não foi possível carregar o Termo de Uso.", retry)
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LegalLinksText("Ao aceitar, você concorda com o ", openLegal)
            state.acceptError?.let { NovaNotice(it) }
            NovaButton(if (state.accepting) "Registrando aceite…" else "Aceitar e continuar", accept, enabled = terms != null && !state.accepting)
            NovaTextLink("Sair", logout, enabled = !state.accepting)
        }
    }
}
