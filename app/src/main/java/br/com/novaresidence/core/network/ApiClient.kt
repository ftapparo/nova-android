package br.com.novaresidence.core.network

import br.com.novaresidence.core.session.SessionCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

enum class ApiErrorKind {
    Validation, Unauthorized, Forbidden, NotFound, Conflict, RateLimited, Unavailable, Internal, Network, InvalidResponse,
}

data class FieldError(val path: String, val message: String)

class ApiException(
    val kind: ApiErrorKind,
    val detail: String? = null,
    val requestId: String? = null,
    val fieldErrors: List<FieldError> = emptyList(),
    val retryAfterSeconds: Long = 0,
) : Exception(kind.name) {
    fun userMessage(): String = when (kind) {
        ApiErrorKind.Validation -> "Confira os dados informados."
        ApiErrorKind.Unauthorized -> "E-mail ou senha incorretos."
        ApiErrorKind.Forbidden -> "Acesso negado."
        ApiErrorKind.NotFound -> "Não foi possível encontrar este recurso."
        ApiErrorKind.Conflict -> "Este e-mail já possui uma conta."
        ApiErrorKind.RateLimited -> "Muitas tentativas. Aguarde para tentar novamente."
        ApiErrorKind.Unavailable -> "Serviço indisponível. Tente novamente."
        ApiErrorKind.Network -> "Não foi possível conectar. Verifique sua conexão e tente novamente."
        ApiErrorKind.Internal, ApiErrorKind.InvalidResponse -> "Não foi possível concluir a solicitação. Tente novamente."
    }
}

val NovaJson = Json { ignoreUnknownKeys = true }

class ApiTransport(
    baseUrl: String,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(30, TimeUnit.SECONDS).retryOnConnectionFailure(false).build(),
) {
    private val apiUrl = baseUrl.trimEnd('/') + "/v3/api/"

    suspend fun request(path: String, body: JsonObject? = null, token: String? = null, post: Boolean = false): JsonElement =
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(apiUrl + path.trimStart('/')).apply {
                header("Accept", "application/json")
                if (token != null) header("Authorization", "Bearer $token")
                if (post) {
                    // Bodyless endpoints must not advertise an empty JSON document.
                    post(body?.toString()?.toRequestBody("application/json".toMediaType()) ?: ByteArray(0).toRequestBody())
                }
            }.build()
            try {
                client.newCall(request).execute().use { response ->
                    val envelope = try {
                        NovaJson.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
                    } catch (_: IllegalArgumentException) {
                        throw ApiException(ApiErrorKind.InvalidResponse)
                    }
                    val requestId = ((envelope["meta"] as? JsonObject)?.get("requestId") as? JsonPrimitive)?.contentOrNull
                    val success = (envelope["success"] as? JsonPrimitive)?.booleanOrNull
                    if (response.isSuccessful && success == true) {
                        return@withContext envelope["data"] ?: throw ApiException(ApiErrorKind.InvalidResponse, requestId = requestId)
                    }
                    val error = envelope["error"] as? JsonObject
                    val kind = when ((error?.get("type") as? JsonPrimitive)?.contentOrNull) {
                        "urn:nova-api:validation-error" -> ApiErrorKind.Validation
                        "urn:nova-api:unauthorized" -> ApiErrorKind.Unauthorized
                        "urn:nova-api:forbidden" -> ApiErrorKind.Forbidden
                        "urn:nova-api:not-found" -> ApiErrorKind.NotFound
                        "urn:nova-api:conflict" -> ApiErrorKind.Conflict
                        "urn:nova-api:rate-limited" -> ApiErrorKind.RateLimited
                        "urn:nova-api:upstream-error" -> ApiErrorKind.Unavailable
                        "urn:nova-api:internal-error" -> ApiErrorKind.Internal
                        else -> ApiErrorKind.InvalidResponse
                    }
                    if (success != false) throw ApiException(ApiErrorKind.InvalidResponse, requestId = requestId)
                    throw ApiException(
                        kind,
                        detail = (error?.get("detail") as? JsonPrimitive)?.contentOrNull,
                        requestId = requestId,
                        fieldErrors = (error?.get("validationErrors") as? JsonArray)?.mapNotNull { element ->
                            val field = element as? JsonObject ?: return@mapNotNull null
                            val fieldPath = (field["path"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
                            val message = (field["message"] as? JsonPrimitive)?.contentOrNull ?: return@mapNotNull null
                            FieldError(fieldPath, message)
                        }.orEmpty(),
                        retryAfterSeconds = response.header("Retry-After")?.toLongOrNull()?.coerceIn(0, 86400) ?: 0,
                    )
                }
            } catch (_: IOException) {
                throw ApiException(ApiErrorKind.Network)
            }
        }

    // Decoding large payloads (e.g. 50 fire panel logs per page) on the main thread froze the UI on slow phones.
    suspend inline fun <reified T> get(path: String, token: String? = null): T =
        request(path, token = token).let { data -> withContext(Dispatchers.Default) { decode<T>(data) } }
    suspend inline fun <reified T> post(path: String, body: JsonObject? = null, token: String? = null): T =
        request(path, body, token, post = true).let { data -> withContext(Dispatchers.Default) { decode<T>(data) } }

    inline fun <reified T> decode(data: JsonElement): T = try {
        NovaJson.decodeFromJsonElement<T>(data)
    } catch (_: SerializationException) {
        throw ApiException(ApiErrorKind.InvalidResponse)
    } catch (_: IllegalArgumentException) {
        throw ApiException(ApiErrorKind.InvalidResponse)
    }
}

class ApiClient(val transport: ApiTransport, val session: SessionCoordinator) {
    suspend inline fun <reified T> get(path: String): T = session.authorize { transport.get<T>(path, it) }
    suspend inline fun <reified T> post(path: String, body: JsonObject? = null): T =
        session.authorize { transport.post<T>(path, body, it) }
}
