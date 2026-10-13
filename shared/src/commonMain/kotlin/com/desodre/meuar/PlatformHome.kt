package com.desodre.meuar

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

data class UserLocationState(val point: GeoPoint? = null, val error: String? = null)

@Composable
expect fun rememberUserLocation(retryKey: Int): UserLocationState

@Composable
expect fun locationSettingsAction(): () -> Unit

expect val locationPermissionInstructions: String

@Composable
expect fun StationMap(
    user: GeoPoint,
    station: GeoPoint,
    stationName: String,
    markerKind: MapMarkerKind,
    dark: Boolean,
    modifier: Modifier = Modifier,
)
