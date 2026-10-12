package com.desodre.meuar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NearestStationTest {
    private fun station(id: Long, latitude: Double, longitude: Double) = Station(
        id, "Station $id", GeoPoint(latitude, longitude), null, null, null, null, false,
    )

    @Test
    fun findsNearestByGreatCircleDistance() {
        val user = GeoPoint(-3.112545, -60.011875)
        val result = nearestStation(user, listOf(
            station(1, -1.408397, -48.462754),
            station(178586, -3.112545, -60.011875),
            station(3, -8.798777, -63.830615),
        ))
        assertEquals(178586L, result?.station?.id)
        assertEquals(0.0, result?.distanceKm)
    }

    @Test
    fun ignoresInvalidStationCoordinates() {
        val result = nearestStation(GeoPoint(0.0, 0.0), listOf(station(1, 100.0, 0.0), station(2, 0.0, 1.0)))
        assertEquals(2L, result?.station?.id)
        assertTrue(result!!.distanceKm in 111.0..112.0)
    }

    @Test
    fun returnsNullWhenNoStationCanBeUsed() {
        assertNull(nearestStation(GeoPoint(0.0, 0.0), emptyList()))
        assertNull(nearestStation(GeoPoint(100.0, 0.0), listOf(station(1, 0.0, 0.0))))
    }
}
