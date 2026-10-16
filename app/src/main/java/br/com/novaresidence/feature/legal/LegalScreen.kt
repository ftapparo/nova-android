package br.com.novaresidence.feature.legal

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.annotation.StringRes
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
import br.com.novaresidence.R
import br.com.novaresidence.core.ui.*
import kotlin.math.roundToInt

// Corpo do texto na folha de estilo das páginas públicas (src/v3/legal/legal.documents.ts na API).
private const val PageBodyPx = 16f

/** Título no idioma do app; o documento oficial (e o título enviado pela API) é sempre em português. */
@Composable
private fun documentTitle(document: String) = stringResource(if (document == TermsDocument) R.string.legal_terms else R.string.legal_privacy)

/**
 * Termo de Uso ou Política de Privacidade (Figma 13 e 14): a página oficial publicada pela API, para o
 * texto do app ser sempre o vigente. O PDF (documento oficial) abre fora do app.
 */
@Composable
fun LegalDocumentScreen(state: LegalState, document: String, back: () -> Unit, retry: () -> Unit) {
    val doc = state.find(document)
    NovaPage(documentTitle(document), back = back, background = NovaSurface) {
        PortugueseOnlyNotice()
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                doc != null -> LegalWebView(doc.htmlUrl, Modifier.fillMaxSize())
                state.loading -> NovaLoading(stringResource(R.string.legal_loading))
                else -> LegalError(state.error?.text() ?: stringResource(R.string.legal_load_failed), retry)
            }
        }
    }
}

@Composable
private fun LegalError(message: String, retry: () -> Unit) {
    NovaRetryNotice(message, stringResource(R.string.legal_try_again), retry, Modifier.fillMaxWidth().padding(NovaSpacing.Margin))
}

/** Fora do português, avisa que o texto exibido é o oficial, em português (o documento jurídico não é traduzido). */
@Composable
private fun PortugueseOnlyNotice() {
    if (LocalConfiguration.current.locales[0].language == "pt") return
    Box(Modifier.padding(start = NovaSpacing.Margin, end = NovaSpacing.Margin, bottom = 12.dp)) { NovaInfoStrip(stringResource(R.string.legal_portuguese_only)) }
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
        if (failed) LegalError(stringResource(R.string.legal_load_failed_connection)) { attempt++ }
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
        if (loading && !failed) Box(Modifier.fillMaxSize().background(NovaSurface), contentAlignment = Alignment.TopCenter) {
            NovaLoading(stringResource(R.string.legal_loading))
        }
    }
}

// Posições dos links no modelo: %1$s = Termo de Uso, %2$s = Política de Privacidade.
private val LinkSlot = Regex("%([12])\\\$s")

/**
 * Frase com "Termo de Uso" e "Política de Privacidade" tocáveis, que abrem os documentos. [template] tem `%1$s`
 * (Termo) e `%2$s` (Política), para cada idioma posicionar os links na própria ordem.
 */
@Composable
fun LegalLinksText(@StringRes template: Int, openLegal: (String) -> Unit, modifier: Modifier = Modifier, style: TextStyle = NovaType.Supporting,
    textAlign: TextAlign = TextAlign.Start) {
    val link = TextLinkStyles(SpanStyle(color = NovaPrimary, fontWeight = FontWeight.Medium, textDecoration = TextDecoration.Underline))
    val sentence = stringResource(template)
    val terms = stringResource(R.string.legal_terms)
    val privacy = stringResource(R.string.legal_privacy)
    val text = buildAnnotatedString {
        var plain = 0
        LinkSlot.findAll(sentence).forEach { slot ->
            append(sentence.substring(plain, slot.range.first))
            if (slot.groupValues[1] == "1") withLink(LinkAnnotation.Clickable(TermsDocument, link) { openLegal(TermsDocument) }) { append(terms) }
            else withLink(LinkAnnotation.Clickable(PrivacyDocument, link) { openLegal(PrivacyDocument) }) { append(privacy) }
            plain = slot.range.last + 1
        }
        append(sentence.substring(plain))
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
    NovaPage(stringResource(R.string.legal_terms), modifier = Modifier.systemBarsPadding()) {
        Column(Modifier.padding(horizontal = NovaSpacing.Margin).padding(bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // O app não sabe se a conta já aceitou uma versão anterior: o título serve à conta antiga e à versão nova.
            Text(stringResource(R.string.legal_acceptance_title), style = NovaType.ScreenTitle, color = NovaOnSurface)
            Text(terms?.let { stringResource(R.string.legal_acceptance_body_version, it.version) } ?: stringResource(R.string.legal_acceptance_body),
                style = NovaType.Body, color = NovaOnSurfaceVariant)
        }
        PortugueseOnlyNotice()
        Box(Modifier.weight(1f).fillMaxWidth().background(NovaSurface)) {
            when {
                terms != null -> LegalWebView(terms.htmlUrl, Modifier.fillMaxSize())
                state.loading -> NovaLoading(stringResource(R.string.legal_loading))
                else -> LegalError(state.error?.text() ?: stringResource(R.string.legal_terms_load_failed), retry)
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = NovaSpacing.Margin, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LegalLinksText(R.string.legal_accept_agreement, openLegal)
            state.acceptError?.let { NovaNotice(it.text()) }
            NovaButton(stringResource(if (state.accepting) R.string.legal_registering else R.string.legal_accept), accept, enabled = terms != null && !state.accepting)
            NovaTextLink(stringResource(R.string.legal_sign_out), logout, enabled = !state.accepting)
        }
    }
}
