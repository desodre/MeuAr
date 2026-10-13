package com.desodre.meuar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

@Composable
fun App() {
    var refreshKey by remember { mutableIntStateOf(0) }
    var showHelp by remember { mutableStateOf(false) }
    val openLocationSettings = locationSettingsAction()
    val location = rememberUserLocation(refreshKey)
    var stations by remember { mutableStateOf<List<Station>?>(null) }
    var loadError by remember { mutableStateOf(false) }
    var stationDetail by remember { mutableStateOf<Station?>(null) }

    LaunchedEffect(refreshKey) {
        loadError = false
        stations = null
        try { stations = loadAllStations() } catch (_: Exception) { loadError = true }
    }
    val nearby = remember(location.point, stations) {
        location.point?.let { user -> stations?.let { nearestStation(user, it) } }
    }
    LaunchedEffect(nearby?.station?.id, refreshKey) {
        stationDetail = null
        val id = nearby?.station?.id ?: return@LaunchedEffect
        stationDetail = try { loadStationById(id) } catch (_: Exception) { null }
    }
    val selected = stationDetail ?: nearby?.station
    val state = when {
        location.error != null -> HomeState.PermissionDenied
        location.point == null -> HomeState.LoadingLocation
        loadError -> HomeState.ConnectionFailure
        stations == null -> HomeState.LoadingStations
        nearby == null -> HomeState.NoStations
        selected?.reading?.index == null -> HomeState.NoMeasurement
        selected.olderThanThreeDays -> HomeState.StaleReading
        else -> HomeState.RecentReading
    }
    val palette = MeuArPalette(isSystemInDarkTheme())

    Column(
        Modifier.fillMaxSize().background(palette.background).safeDrawingPadding()
            .verticalScroll(rememberScrollState())
    ) {
        HomeHeader(palette) { showHelp = true }
        StationSchematicMap(
            palette, location.point,
            if (state == HomeState.PermissionDenied || state == HomeState.ConnectionFailure) null else nearby,
            when (state) {
                HomeState.NoMeasurement -> MapMarkerKind.NoMeasurement
                HomeState.StaleReading -> MapMarkerKind.Historical
                else -> MapMarkerKind.Station
            },
        )
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (state) {
                HomeState.LoadingLocation -> LoadingCard(palette, locationReady = false)
                HomeState.LoadingStations -> LoadingCard(palette, locationReady = true)
                HomeState.PermissionDenied -> StateCard(
                    palette, "⌖", "Permita sua localização",
                    "Sem acesso à localização, não podemos encontrar a estação mais próxima.",
                    "Abrir configurações", openLocationSettings,
                    noteTitle = "Como conceder acesso",
                    note = locationPermissionInstructions,
                    secondaryAction = "Verificar permissão", onSecondaryAction = { refreshKey++ },
                )
                HomeState.ConnectionFailure -> StateCard(
                    palette, "⌁", "Não foi possível conectar",
                    "Não conseguimos consultar as estações. Verifique sua conexão e tente novamente.",
                    "Tentar novamente", { refreshKey++ },
                    note = "Estações e qualidade do ar indisponíveis. Nenhuma leitura foi recebida.",
                )
                HomeState.NoStations -> StateCard(
                    palette, "♧", "Nenhuma estação encontrada",
                    "Não encontramos estações com coordenadas válidas para comparar.",
                    "Tentar novamente", { refreshKey++ },
                )
                else -> if (nearby != null && selected != null) {
                    StationCard(palette, nearby, selected, state) { refreshKey++ }
                }
            }
            InformationLimit(palette)
        }
    }
    if (showHelp) {
        AlertDialog(
            onDismissRequest = { showHelp = false },
            title = { Text("Sobre as leituras") },
            text = { Text("Meu Ar mostra a estação mais próxima pela distância em linha reta. A leitura pertence à estação e pode estar desatualizada; confira o horário e a fonte no cartão.") },
            confirmButton = {
                Text("Entendi", Modifier.clickable { showHelp = false }.padding(12.dp))
            },
        )
    }
}

private enum class HomeState {
    LoadingLocation, PermissionDenied, LoadingStations, RecentReading,
    StaleReading, NoMeasurement, ConnectionFailure, NoStations,
}

@Composable
private fun HomeHeader(p: MeuArPalette, onHelp: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(80.dp).background(p.surface).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Meu Ar", color = p.primary, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
            Text("Estações perto de você", color = p.secondary, fontSize = 14.sp)
        }
        Box(
            Modifier.size(48.dp).background(p.surfaceSecondary, RoundedCornerShape(50)).clickable(onClick = onHelp),
            contentAlignment = Alignment.Center,
        ) { Text("?", color = p.primary, fontSize = 22.sp, fontWeight = FontWeight.Medium) }
    }
}

@Composable
private fun LoadingCard(p: MeuArPalette, locationReady: Boolean) {
    Column(cardModifier(p), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        StateIcon(p, "◌")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                if (locationReady) "Buscando estações próximas" else "Obtendo sua localização",
                color = p.primary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp,
            )
            Text(
                if (locationReady) "Localização obtida. Consultando estações e comparando as distâncias em linha reta."
                else "Aguardando sua localização para encontrar a estação mais próxima.",
                color = p.secondary, fontSize = 16.sp, lineHeight = 24.sp,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("◎ Localização", color = p.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text("♧ Estações", color = if (locationReady) p.primary else p.secondary,
                fontSize = 14.sp, fontWeight = if (locationReady) FontWeight.SemiBold else FontWeight.Normal)
        }
        Box(Modifier.fillMaxWidth().height(12.dp).background(p.surfaceSecondary, RoundedCornerShape(8.dp)))
        ActionButton(p, if (locationReady) "Buscando estações…" else "Obtendo localização…", enabled = false) {}
        Text("Nenhuma leitura é exibida durante a consulta.", color = p.secondary, fontSize = 13.sp)
    }
}

@Composable
private fun StateCard(
    p: MeuArPalette, icon: String, title: String, description: String,
    action: String, onAction: () -> Unit,
    noteTitle: String? = null, note: String? = null,
    secondaryAction: String? = null, onSecondaryAction: (() -> Unit)? = null,
) {
    Column(cardModifier(p), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        StateIcon(p, icon)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, color = p.primary, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 28.sp)
            Text(description, color = p.secondary, fontSize = 16.sp, lineHeight = 24.sp)
        }
        if (note != null) {
            Column(
                Modifier.fillMaxWidth().background(p.surfaceSecondary, RoundedCornerShape(12.dp)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (noteTitle != null) Text(noteTitle, color = p.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(note, color = p.secondary, fontSize = 14.sp, lineHeight = 21.sp)
            }
        }
        ActionButton(p, action, onClick = onAction)
        if (secondaryAction != null && onSecondaryAction != null) {
            ActionButton(p, secondaryAction, secondary = true, onClick = onSecondaryAction)
        }
    }
}

@Composable
private fun StateIcon(p: MeuArPalette, glyph: String) {
    Box(Modifier.size(48.dp).background(p.surfaceSecondary, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center) {
        Text(glyph, color = p.primary, fontSize = 26.sp)
    }
}

@Composable
private fun StationCard(
    p: MeuArPalette, nearby: NearbyStation, selected: Station,
    state: HomeState, onRetry: () -> Unit,
) {
    val reading = selected.reading
    Column(cardModifier(p), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("ESTAÇÃO MAIS PRÓXIMA", color = p.secondary, fontSize = 13.sp)
            Text(selected.name, color = p.primary, fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold, lineHeight = 25.sp)
            Text("↗  ${formatDistance(nearby.distanceKm)} em linha reta", color = p.secondary, fontSize = 14.sp)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.border))
        if (state == HomeState.NoMeasurement) {
            InfoNotice(p, "⊖  Sem medição disponível",
                "A estação foi encontrada, mas não há leitura para exibir. Qualidade do ar indisponível.")
        } else {
            if (state == HomeState.StaleReading) {
                InfoNotice(p, "◴  Dado desatualizado",
                    "Mais de 3 dias. Esta leitura não indica a qualidade atual do ar.")
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(if (state == HomeState.StaleReading) "IQAr histórico" else "IQAr",
                        color = p.secondary, fontSize = 13.sp)
                    Text(reading?.index?.let(::formatIndex) ?: "—", color = p.primary,
                        fontSize = 48.sp, fontWeight = FontWeight.SemiBold, lineHeight = 53.sp)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    QualityBadge(p, reading?.classification, state == HomeState.StaleReading)
                    Text("Poluente · ${reading?.pollutant ?: "Não informado"}", color = p.secondary,
                        fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            if (reading?.measuredAt != null) {
                Text("◷  ${formatMeasurementDate(reading.measuredAt)}\n" +
                    if (state == HomeState.StaleReading) "Leitura desatualizada" else "Leitura recente",
                    color = p.secondary, fontSize = 13.sp, lineHeight = 19.sp)
            }
        }
        Text("Fonte: ${selected.source ?: "MonitorAr"}", color = p.secondary, fontSize = 13.sp, lineHeight = 18.sp)
        ActionButton(p, if (state == HomeState.NoMeasurement) "Tentar novamente" else "Atualizar estações",
            onClick = onRetry)
    }
}

@Composable
private fun InfoNotice(p: MeuArPalette, title: String, description: String) {
    Column(Modifier.fillMaxWidth().background(p.surfaceSecondary, RoundedCornerShape(12.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, color = p.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(description, color = p.secondary, fontSize = 13.sp, lineHeight = 19.sp)
    }
}

@Composable
private fun QualityBadge(p: MeuArPalette, classification: String?, historical: Boolean) {
    val (background, foreground) = p.qualityColors(classification)
    Text("⊙ ${classification ?: "Sem classificação"}${if (historical) " na medição" else ""}",
        Modifier.background(background, RoundedCornerShape(999.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
        color = foreground, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
}

@Composable
private fun ActionButton(
    p: MeuArPalette, label: String, secondary: Boolean = false, enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Box(
        Modifier.fillMaxWidth().height(52.dp)
            .background(if (secondary) p.surfaceSecondary else p.action, RoundedCornerShape(12.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (!enabled) CircularProgressIndicator(Modifier.size(18.dp), color = p.onAction, strokeWidth = 2.dp)
            Text(label, color = if (secondary) p.secondary else p.onAction,
                fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InformationLimit(p: MeuArPalette) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("ⓘ", color = p.secondary, fontSize = 16.sp)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Medição na estação, não no seu endereço", color = p.primary,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp)
            Text("Distância em linha reta: não é trajeto nem leitura no seu endereço.",
                color = p.secondary, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}

private fun cardModifier(p: MeuArPalette): Modifier =
    Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(16.dp)).padding(20.dp)

private fun formatDistance(km: Double): String =
    if (km < 1) "${(km * 1000).roundToInt()} m"
    else "${((km * 10).roundToInt() / 10.0).toString().replace('.', ',')} km"

private fun formatIndex(index: Double): String =
    if (index % 1.0 == 0.0) index.toInt().toString()
    else ((index * 10).roundToInt() / 10.0).toString().replace('.', ',')

private fun formatMeasurementDate(value: String): String {
    val parts = value.split(' ', 'T')
    val day = parts.firstOrNull()?.split('-')
    return if (day?.size == 3) "${day[2]}/${day[1]}/${day[0]} · ${parts.getOrNull(1) ?: "horário não informado"}"
    else value
}
