package br.com.novaresidence.feature.exhaust

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.core.location.LocationManagerCompat
import br.com.novaresidence.core.ui.findActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.Task
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Localização enviada no `POST /exhausts/{id}/on`; só valida a presença no condomínio e nunca vai para log. */
data class CommandLocation(val latitude: Double, val longitude: Double, val accuracy: Float, val mock: Boolean)

sealed interface LocationResult {
    data class Fix(val location: CommandLocation) : LocationResult
    data object PermissionNeeded : LocationResult
    data object ApproximateOnly : LocationResult
    data object Disabled : LocationResult
    data object Unavailable : LocationResult
}

private val LocationPermissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)

/**
 * Lê a localização uma vez, no toque em ligar ou reiniciar (só "durante o uso", sem segundo plano).
 * O provedor do Google Play Services combina GPS, Wi-Fi e rede, o que acerta melhor dentro do prédio.
 */
class ExhaustLocator(context: Context) {
    private val context = context.applicationContext

    suspend fun current(): LocationResult {
        val fine = granted(Manifest.permission.ACCESS_FINE_LOCATION)
        if (!fine) return if (granted(Manifest.permission.ACCESS_COARSE_LOCATION)) LocationResult.ApproximateOnly else LocationResult.PermissionNeeded
        val manager = context.getSystemService(LocationManager::class.java)
        if (manager == null || !LocationManagerCompat.isLocationEnabled(manager)) return LocationResult.Disabled
        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            // Uma leitura recente basta; mais antiga que isso pode ser de antes de a pessoa chegar.
            .setMaxUpdateAgeMillis(30_000)
            .setDurationMillis(15_000)
            .build()
        val location = try {
            withTimeoutOrNull(20_000) { currentLocation(request) }
        } catch (_: SecurityException) {
            return LocationResult.PermissionNeeded
        } catch (_: Exception) {
            null
        } ?: return LocationResult.Unavailable
        return LocationResult.Fix(CommandLocation(location.latitude, location.longitude, location.accuracy, location.simulated()))
    }

    private fun granted(permission: String) = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission") // conferida em current()
    private suspend fun currentLocation(request: CurrentLocationRequest): Location? {
        val cancellation = CancellationTokenSource()
        return try {
            LocationServices.getFusedLocationProviderClient(context).getCurrentLocation(request, cancellation.token).await()
        } finally {
            cancellation.cancel()
        }
    }
}

private fun Location.simulated(): Boolean =
    if (Build.VERSION.SDK_INT >= 31) isMock else @Suppress("DEPRECATION") isFromMockProvider

private suspend fun <T> Task<T>.await(): T? = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        val error = task.exception
        when {
            task.isCanceled -> continuation.resume(null)
            error != null -> continuation.resumeWithException(error)
            else -> continuation.resume(task.result)
        }
    }
}

/** Ações dos avisos de localização (Figma 25c–25f). Nada é enviado na volta: a pessoa toca de novo no botão. */
class LocationEnabler internal constructor(
    private val context: Context,
    private val askPermission: () -> Unit,
    private val resolveSettings: (IntentSenderRequest) -> Unit,
    private val done: () -> Unit,
) {
    private val preferences = context.applicationContext.getSharedPreferences("exhaust_location", Context.MODE_PRIVATE)

    /** O Android não mostra mais o pedido de permissão: só as configurações do app resolvem. */
    fun permissionBlocked(): Boolean {
        val activity = context.findActivity() ?: return false
        return preferences.getBoolean("permission_asked", false) &&
            LocationPermissions.none { ActivityCompat.shouldShowRequestPermissionRationale(activity, it) }
    }

    fun resolve(issue: LocationIssue) {
        when (issue) {
            LocationIssue.Permission, LocationIssue.Approximate ->
                if (permissionBlocked()) openAppSettings() else {
                    preferences.edit { putBoolean("permission_asked", true) }
                    askPermission()
                }
            LocationIssue.Disabled -> enableLocation()
            else -> done()
        }
    }

    private fun openAppSettings() {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
        done()
    }

    // O diálogo do Play Services liga a localização sem sair do app; sem ele, as configurações do Android.
    private fun enableLocation() {
        val request = LocationSettingsRequest.Builder()
            .addLocationRequest(LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000).build()).build()
        LocationServices.getSettingsClient(context).checkLocationSettings(request).addOnCompleteListener { task ->
            val error = task.exception
            when {
                error is ResolvableApiException && context.findActivity() != null ->
                    resolveSettings(IntentSenderRequest.Builder(error.resolution).build())
                error != null -> {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                    done()
                }
                else -> done()
            }
        }
    }
}

@Composable
fun rememberLocationEnabler(done: () -> Unit): LocationEnabler {
    val context = LocalContext.current
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { done() }
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { done() }
    val currentDone by rememberUpdatedState(done)
    return remember(context) {
        LocationEnabler(context, { permission.launch(LocationPermissions) }, { settings.launch(it) }) { currentDone() }
    }
}
