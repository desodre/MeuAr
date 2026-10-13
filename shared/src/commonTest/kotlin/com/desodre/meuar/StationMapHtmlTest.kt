package com.desodre.meuar

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

class StationMapHtmlTest {
    @Test
    fun placesBothRealCoordinatesAndVisibleAttribution() {
        val html = stationMapHtml(
            GeoPoint(-3.112545, -60.011875),
            GeoPoint(-3.101944, -60.025),
            "FIOTEC - Amazonas",
            MapMarkerKind.Station,
            dark = false,
        )

        assertContains(html, "var user = [-3.112545, -60.011875]")
        assertContains(html, "var station = [-3.101944, -60.025]")
        assertContains(html, "tile.openstreetmap.org/{z}/{x}/{y}.png")
        assertContains(html, "OpenStreetMap</a> contributors")
        assertContains(html, "map.fitBounds(bounds")
        assertContains(html, "grayscale(1)")
    }

    @Test
    fun escapesStationNamesBeforeAddingThemToHtml() {
        val name = "</script><img src=x onerror=alert(1)>{{userLatitude}}"
        val html = stationMapHtml(
            GeoPoint(-23.55, -46.63),
            GeoPoint(-23.54, -46.62),
            name,
            MapMarkerKind.NoMeasurement,
            dark = true,
        )

        assertContains(html, "&lt;/script&gt;&lt;img src=x onerror=alert(1)&gt;{{userLatitude}}")
        assertFalse(html.contains("</script><img"))
        assertContains(html, "invert(1)")
    }

    @Test
    fun rejectsInvalidCoordinates() {
        assertFailsWith<IllegalArgumentException> {
            stationMapHtml(
                GeoPoint(Double.NaN, 0.0),
                GeoPoint(0.0, 0.0),
                "Estação",
                MapMarkerKind.Station,
                dark = false,
            )
        }
    }
}
