package com.desodre.meuar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocation
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusDenied
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLAuthorizationStatusRestricted
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSArray
import platform.Foundation.NSDictionary
import platform.Foundation.NSError
import platform.Foundation.NSHTTPURLResponse
import platform.Foundation.NSJSONSerialization
import platform.Foundation.NSNumber
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.Foundation.NSURLSession
import platform.darwin.NSObject
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val baseUrl = "https://monitorar-backend.mma.gov.br/v1/estacao"

actual suspend fun loadAllStations(): List<Station> {
    val parsed = fetchJson("$baseUrl/todas")
    val records = parsed.asItems() ?: error("Resposta de estações inválida")
    return records.mapNotNull { it.toStation() }
}

actual suspend fun loadStationById(id: Long): Station? {
    val parsed = fetchJson("$baseUrl/por-ids?ids=$id")
    return (parsed.asItems()?.firstOrNull() ?: parsed).toStation()
}

private suspend fun fetchJson(address: String): Any = suspendCoroutine { continuation ->
    val url = NSURL(string = address)
    if (url == null) {
        continuation.resumeWithException(IllegalArgumentException("URL inválida"))
        return@suspendCoroutine
    }
    NSURLSession.sharedSession.dataTaskWithURL(url) { data, response, requestError ->
        try {
            if (requestError != null) throw IllegalStateException(requestError.localizedDescription)
            val status = (response as? NSHTTPURLResponse)?.statusCode?.toInt() ?: 0
            if (status !in 200..299) error("HTTP $status")
            val jsonData = data ?: error("Resposta vazia")
            continuation.resume(NSJSONSerialization.JSONObjectWithData(jsonData, 0uL, null) ?: error("JSON inválido"))
        } catch (failure: Exception) {
            continuation.resumeWithException(failure)
        }
    }.resume()
}

private fun Any?.asItems(): List<Any?>? = when (this) {
    is NSArray -> List(count.toInt()) { objectAtIndex(it.toULong()) }
    is List<*> -> this
    else -> null
}

private fun Any?.isObject(): Boolean = this is NSDictionary || this is Map<*, *>
private fun Any?.value(key: String): Any? = when (this) {
    is NSDictionary -> objectForKey(key)
    is Map<*, *> -> this[key]
    else -> null
}
private fun Any?.text(key: String): String? = value(key)?.toString()?.takeIf { it.isNotBlank() && it != "<null>" }
private fun Any?.number(key: String): Double? = when (val raw = value(key)) {
    is NSNumber -> raw.doubleValue
    is Number -> raw.toDouble()
    else -> raw?.toString()?.toDoubleOrNull()
}

private fun Any?.toStation(): Station? {
    if (!isObject()) return null
    val id = number("idEstacao")?.toLong() ?: return null
    val point = GeoPoint(number("nuLatitude") ?: return null, number("nuLongitude") ?: return null)
    if (!point.isValid()) return null
    val municipality = value("municipio")
    val air = value("indiceQualidadeArAtual")
    val measurement = air.value("medicao")
    val rawIndex = measurement.value("indiceQualidadeAr")
    val index = when (rawIndex) {
        is NSNumber -> rawIndex.doubleValue
        is Number -> rawIndex.toDouble()
        else -> rawIndex.number("parsedValue") ?: rawIndex.number("source")
    }
    val reading = if (air.isObject()) AirReading(index, air.value("classificacaoIqAr").text("noClassificacao"),
        air.value("poluente").text("noPoluente"), measurement.text("dtMedicao")) else null
    return Station(id, text("noEstacao")?.trim().orEmpty().ifBlank { "Estação $id" }, point,
        municipality?.text("noMunicipio"), municipality?.text("sgUf"), text("noFonteDados"),
        reading, value("semInfoMaisDeTresDias")?.toString()?.lowercase() in setOf("1", "true"))
}

@OptIn(ExperimentalForeignApi::class)
private class IosLocationController : NSObject(), CLLocationManagerDelegateProtocol {
    private val manager = CLLocationManager()
    var state by mutableStateOf(UserLocationState())
        private set

    fun start() {
        state = UserLocationState()
        manager.delegate = this
        if (!CLLocationManager.locationServicesEnabled()) {
            state = UserLocationState(error = "Ative a localização do iPhone para encontrar a estação mais próxima.")
            return
        }
        updateAuthorization()
    }

    fun stop() {
        manager.stopUpdatingLocation()
        manager.delegate = null
    }

    override fun locationManagerDidChangeAuthorization(manager: CLLocationManager) {
        updateAuthorization()
    }

    private fun updateAuthorization() {
        when (CLLocationManager.authorizationStatus()) {
            kCLAuthorizationStatusNotDetermined -> manager.requestWhenInUseAuthorization()
            kCLAuthorizationStatusAuthorizedAlways, kCLAuthorizationStatusAuthorizedWhenInUse -> manager.requestLocation()
            kCLAuthorizationStatusDenied, kCLAuthorizationStatusRestricted ->
                state = UserLocationState(error = "Permita o acesso à localização para encontrar a estação mais próxima.")
        }
    }

    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
        val location = didUpdateLocations.lastOrNull() as? CLLocation ?: return
        location.coordinate.useContents {
            state = UserLocationState(GeoPoint(latitude, longitude))
        }
    }

    override fun locationManager(manager: CLLocationManager, didFailWithError: NSError) {
        state = UserLocationState(error = "Não foi possível obter sua localização.")
    }
}

@Composable
actual fun rememberUserLocation(retryKey: Int): UserLocationState {
    val controller = remember { IosLocationController() }
    DisposableEffect(controller, retryKey) {
        controller.start()
        onDispose { controller.stop() }
    }
    return controller.state
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun StationMap(point: GeoPoint, modifier: Modifier) {
    val url = remember(point) { stationMapUrl(point) }
    key(point) {
        UIKitView(
            factory = {
                WKWebView(frame = CGRectZero.readValue(), configuration = WKWebViewConfiguration()).apply {
                    loadRequest(NSURLRequest.requestWithURL(NSURL(string = url)!!))
                }
            },
            modifier = modifier,
        )
    }
}
