package com.desodre.meuar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun App() {
    var refreshKey by remember { mutableIntStateOf(0) }
    val location = rememberUserLocation(refreshKey)
    var stations by remember { mutableStateOf<List<Station>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var stationDetail by remember { mutableStateOf<Station?>(null) }

    LaunchedEffect(refreshKey) {
        loadError = null
        stations = null
        try {
            stations = loadAllStations()
        } catch (error: Exception) {
            loadError = error.message ?: "Não foi possível carregar as estações."
        }
    }

    val nearest = remember(location.point, stations) {
        location.point?.let { user -> stations?.let { nearestStation(user, it) } }
    }
    LaunchedEffect(nearest?.station?.id, refreshKey) {
        stationDetail = null
        val id = nearest?.station?.id ?: return@LaunchedEffect
        stationDetail = try { loadStationById(id) } catch (_: Exception) { null }
    }

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize().background(Color(0xFFF2F7F5)).safeContentPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            Text("meu ar", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("Qualidade do ar perto de você", color = Color(0xFF50625A))
            Spacer(Modifier.height(20.dp))

            when {
                location.error != null -> StatusCard(location.error ?: "Localização indisponível", onRetry = { refreshKey++ })
                location.point == null -> StatusCard("Obtendo sua localização…", loading = true)
                loadError != null -> StatusCard("Falha ao carregar as estações: $loadError", onRetry = { refreshKey++ })
                stations == null -> StatusCard("Carregando estações do MonitorAr…", loading = true)
                nearest == null -> StatusCard("Nenhuma estação com coordenadas válidas foi encontrada.", onRetry = { refreshKey++ })
                else -> {
                    val station = nearest.station
                    val reading = stationDetail?.reading ?: station.reading
                    Text("ESTAÇÃO MAIS PRÓXIMA", style = MaterialTheme.typography.labelMedium, color = Color(0xFF356B58))
                    Spacer(Modifier.height(8.dp))
                    Text(station.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text("Lat ${station.point.latitude} · Long ${station.point.longitude}", color = Color(0xFF50625A))
                    Spacer(Modifier.height(12.dp))
                    Card(shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("${formatDistance(nearest.distanceKm)} de você", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Distância em linha reta até a estação, calculada por Haversine.", style = MaterialTheme.typography.bodySmall)
                            val description = when {
                                reading == null -> "Sem medição de qualidade do ar disponível"
                                station.olderThanThreeDays -> "Última medição antiga · ${reading.classification ?: "Sem classificação"}"
                                else -> "${reading.classification ?: "Sem classificação"} · IQAr ${reading.index?.let { formatIndex(it) } ?: "—"}"
                            }
                            Text(description, fontWeight = FontWeight.SemiBold)
                            if (reading != null) {
                                Text("${reading.pollutant ?: "Poluente não informado"} · medido em ${reading.measuredAt ?: "data não informada"}", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFFDCEAE4), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                        StationMap(station.point, Modifier.fillMaxSize())
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Medição na estação, não no seu endereço. Fonte: ${station.source ?: "MonitorAr"}. Mapa: © OpenStreetMap contributors.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF50625A))
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { refreshKey++ }) { Text("Atualizar estações") }
                }
            }
        }
    }
}

@Composable
private fun StatusCard(message: String, loading: Boolean = false, onRetry: (() -> Unit)? = null) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (loading) CircularProgressIndicator(Modifier.height(24.dp))
            Column {
                Text(message)
                if (onRetry != null) Button(onClick = onRetry) { Text("Tentar novamente") }
            }
        }
    }
}

private fun formatDistance(km: Double): String = if (km < 1) "${(km * 1000).roundToInt()} m" else "${(km * 10).roundToInt() / 10.0} km"
private fun formatIndex(index: Double): String = if (index % 1.0 == 0.0) index.toInt().toString() else ((index * 10).roundToInt() / 10.0).toString()
