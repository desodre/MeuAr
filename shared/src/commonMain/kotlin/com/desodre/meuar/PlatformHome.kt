package com.desodre.meuar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class UserLocationState(val point: GeoPoint? = null, val error: String? = null)

@Composable
expect fun rememberUserLocation(retryKey: Int): UserLocationState

@Composable
expect fun StationMap(point: GeoPoint, modifier: Modifier = Modifier)

fun stationMapUrl(point: GeoPoint): String {
    val latitude = point.latitude
    val longitude = point.longitude
    val latitudeSpan = 0.025
    val longitudeSpan = 0.035
    return "https://www.openstreetmap.org/export/embed.html?bbox=" +
        "${longitude - longitudeSpan}%2C${latitude - latitudeSpan}%2C" +
        "${longitude + longitudeSpan}%2C${latitude + latitudeSpan}" +
        "&layer=mapnik&marker=$latitude%2C$longitude"
}
