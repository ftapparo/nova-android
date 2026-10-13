package br.com.novaresidence.core.ui

import android.content.res.Resources
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Ilustrações do exaustor e da central (WebP por densidade, até 1316 px em xxxhdpi). `painterResource`
 * as decodificava na thread principal a cada entrada na tela, travando a navegação por até 2 s no J4;
 * aqui a decodificação roda em segundo plano e o resultado fica em memória para as visitas seguintes.
 */
object NovaIllustrations {
    private val cache = object : LruCache<Int, ImageBitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
        override fun sizeOf(key: Int, value: ImageBitmap) = value.width * value.height * 4
    }

    fun cached(id: Int): ImageBitmap? = cache.get(id)

    suspend fun load(resources: Resources, id: Int): ImageBitmap = cache.get(id) ?: withContext(Dispatchers.IO) {
        BitmapFactory.decodeResource(resources, id).asImageBitmap().also { cache.put(id, it) }
    }

    /** Antecipa as ilustrações mais prováveis para que a primeira abertura das abas não espere a decodificação. */
    suspend fun preload(resources: Resources, ids: List<Int>) = ids.forEach { load(resources, it) }
}

/** O espaço da ilustração é reservado pelo [modifier]; a imagem aparece assim que estiver decodificada. */
@Composable
fun NovaIllustration(id: Int, modifier: Modifier, contentScale: ContentScale = ContentScale.Fit, colorFilter: ColorFilter? = null) {
    val resources = LocalContext.current.resources
    val bitmap by produceState(NovaIllustrations.cached(id), id) { value = NovaIllustrations.load(resources, id) }
    Box(modifier) {
        bitmap?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = contentScale, colorFilter = colorFilter) }
    }
}
