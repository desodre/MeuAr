package com.desodre.meuar

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

data class GeoPoint(val latitude: Double, val longitude: Double) {
    fun isValid(): Boolean = latitude in -90.0..90.0 && longitude in -180.0..180.0
}

data class AirReading(val index: Double?, val classification: String?, val pollutant: String?, val measuredAt: String?)

data class Station(
    val id: Long,
    val name: String,
    val point: GeoPoint,
    val city: String?,
    val state: String?,
    val source: String?,
    val reading: AirReading?,
    val olderThanThreeDays: Boolean,
)

data class NearbyStation(val station: Station, val distanceKm: Double)

/** Great-circle distance on a spherical Earth with mean radius 6,371.0088 km. */
fun haversineKm(from: GeoPoint, to: GeoPoint): Double {
    require(from.isValid() && to.isValid()) { "Invalid coordinates" }
    val latitudeDelta = (to.latitude - from.latitude).toRadians()
    val longitudeDelta = (to.longitude - from.longitude).toRadians()
    val a = sin(latitudeDelta / 2).pow(2) +
        cos(from.latitude.toRadians()) * cos(to.latitude.toRadians()) *
        sin(longitudeDelta / 2).pow(2)
    return 2 * 6371.0088 * asin(min(1.0, kotlin.math.sqrt(a)))
}

fun nearestStation(user: GeoPoint, stations: List<Station>): NearbyStation? {
    if (!user.isValid()) return null
    return stations.asSequence()
        .filter { it.point.isValid() }
        .map { NearbyStation(it, haversineKm(user, it.point)) }
        .minByOrNull { it.distanceKm }
}

private fun Double.toRadians() = this * kotlin.math.PI / 180.0

expect suspend fun loadAllStations(): List<Station>
expect suspend fun loadStationById(id: Long): Station?
