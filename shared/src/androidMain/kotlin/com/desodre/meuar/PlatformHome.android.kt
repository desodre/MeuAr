package com.desodre.meuar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val baseUrl = "https://monitorar-backend.mma.gov.br/v1/estacao"

actual suspend fun loadAllStations(): List<Station> = fetchJson("$baseUrl/todas") { body ->
    val array = JSONArray(body)
    buildList {
        for (index in 0 until array.length()) {
            array.optJSONObject(index)?.toStation()?.let(::add)
        }
    }
}

actual suspend fun loadStationById(id: Long): Station? = fetchJson("$baseUrl/por-ids?ids=$id") { body ->
    val parsed = org.json.JSONTokener(body).nextValue()
    val station = when (parsed) {
        is JSONObject -> parsed
        is JSONArray -> parsed.optJSONObject(0)
        else -> null
    }
    station?.toStation()
}

private suspend fun <T> fetchJson(url: String, parse: (String) -> T): T = suspendCoroutine { continuation ->
    Thread {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 20_000
                setRequestProperty("Accept", "application/json")
            }
            if (connection.responseCode !in 200..299) error("HTTP ${connection.responseCode}")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            continuation.resume(parse(body))
        } catch (error: Exception) {
            continuation.resumeWithException(error)
        } finally {
            connection?.disconnect()
        }
    }.start()
}

private fun JSONObject.toStation(): Station? {
    val id = optLong("idEstacao", -1)
    val point = GeoPoint(optString("nuLatitude").toDoubleOrNull() ?: return null,
        optString("nuLongitude").toDoubleOrNull() ?: return null)
    if (id < 0 || !point.isValid()) return null
    val municipality = optJSONObject("municipio")
    val air = optJSONObject("indiceQualidadeArAtual")
    val measurement = air?.optJSONObject("medicao")
    val rawIndex = measurement?.opt("indiceQualidadeAr")
    val index = when (rawIndex) {
        is Number -> rawIndex.toDouble()
        is JSONObject -> rawIndex.optDouble("parsedValue").takeUnless { it.isNaN() }
            ?: rawIndex.optString("source").toDoubleOrNull()
        else -> null
    }
    val reading = air?.let {
        AirReading(index, it.optJSONObject("classificacaoIqAr")?.textOrNull("noClassificacao"),
            it.optJSONObject("poluente")?.textOrNull("noPoluente"),
            measurement?.textOrNull("dtMedicao"))
    }
    return Station(id, textOrNull("noEstacao")?.trim().orEmpty().ifBlank { "Estação $id" }, point,
        municipality?.textOrNull("noMunicipio"), municipality?.textOrNull("sgUf"),
        textOrNull("noFonteDados"), reading, optBoolean("semInfoMaisDeTresDias", false))
}

private fun JSONObject.textOrNull(key: String): String? = if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

@Composable
actual fun rememberUserLocation(retryKey: Int): UserLocationState {
    val context = LocalContext.current
    var state by remember { mutableStateOf(UserLocationState()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        if (permissions.values.any { it }) requestPosition(context) { state = it }
        else state = UserLocationState(error = "Permita o acesso à localização para encontrar a estação mais próxima.")
    }
    LaunchedEffect(retryKey) {
        state = UserLocationState()
        val allowed = context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (allowed) requestPosition(context) { state = it }
        else launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }
    return state
}

@Suppress("DEPRECATION")
private fun requestPosition(context: Context, onResult: (UserLocationState) -> Unit) {
    try {
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val provider = when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> null
        }
        if (provider == null) {
            onResult(UserLocationState(error = "Ative a localização do aparelho para ver a estação mais próxima."))
            return
        }
        val handler = Handler(Looper.getMainLooper())
        var finished = false
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                if (finished) return
                finished = true
                onResult(UserLocationState(GeoPoint(location.latitude, location.longitude)))
            }
        }
        manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        handler.postDelayed({
            if (!finished) {
                finished = true
                manager.removeUpdates(listener)
                onResult(UserLocationState(error = "Tempo esgotado ao obter a localização. Verifique o GPS e tente novamente."))
            }
        }, 20_000)
    } catch (_: SecurityException) {
        onResult(UserLocationState(error = "Acesso à localização negado."))
    } catch (_: Exception) {
        onResult(UserLocationState(error = "Não foi possível obter sua localização."))
    }
}

@Composable
actual fun StationMap(point: GeoPoint, modifier: Modifier) {
    val url = remember(point) { stationMapUrl(point) }
    key(point) {
        AndroidView(
            factory = { context -> WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                loadUrl(url)
            } },
            modifier = modifier,
            onRelease = { it.destroy() },
        )
    }
}
