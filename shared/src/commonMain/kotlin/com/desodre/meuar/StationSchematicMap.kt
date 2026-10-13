package com.desodre.meuar

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.sqrt

/**
 * The Figma map is explicitly schematic. Markers use the real relative direction
 * of the nearest station, while the surrounding blocks remain illustrative.
 */
@Composable
fun StationSchematicMap(
    palette: MeuArPalette,
    user: GeoPoint?,
    nearby: NearbyStation?,
    markerKind: MapMarkerKind,
) {
    Column {
        Column(
            Modifier.fillMaxWidth().background(palette.surfaceSecondary)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text("Dados informados pelo MonitorAr", color = palette.primary,
                fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("A medição e seu horário aparecem no cartão da estação.",
                color = palette.secondary, fontSize = 13.sp)
        }
        if (user != null && nearby != null) {
            StationMap(
                user = user,
                station = nearby.station.point,
                stationName = nearby.station.name,
                markerKind = markerKind,
                dark = palette.dark,
                modifier = Modifier.fillMaxWidth().height(310.dp),
            )
        } else BoxWithConstraints(
            Modifier.fillMaxWidth().height(310.dp).background(palette.surfaceSecondary)
        ) {
            val width = maxWidth
            val userX = width * 0.24f
            val userY = 220.dp
            val direction = if (nearby != null && user != null) {
                val dx = (nearby.station.point.longitude - user.longitude) *
                    kotlin.math.cos(user.latitude * kotlin.math.PI / 180.0)
                val dy = user.latitude - nearby.station.point.latitude
                val length = sqrt(dx * dx + dy * dy).coerceAtLeast(0.000001)
                Pair((dx / length).toFloat(), (dy / length).toFloat())
            } else Pair(1f, -0.6f)
            val stationX = (userX + 150.dp * direction.first).coerceIn(70.dp, width - 62.dp)
            val stationY = (userY + 140.dp * direction.second).coerceIn(80.dp, 235.dp)
            val density = androidx.compose.ui.platform.LocalDensity.current

            Canvas(Modifier.matchParentSize()) {
                val scale = size.width / 412f
                fun px(v: Float) = v * scale
                val blocks = listOf(
                    floatArrayOf(8f, 52f, 60f, 60f), floatArrayOf(85f, 47f, 65f, 82f),
                    floatArrayOf(164f, 43f, 62f, 73f), floatArrayOf(247f, 45f, 59f, 45f),
                    floatArrayOf(327f, 41f, 78f, 77f), floatArrayOf(16f, 150f, 51f, 80f),
                    floatArrayOf(84f, 155f, 63f, 48f), floatArrayOf(158f, 145f, 77f, 62f),
                    floatArrayOf(251f, 174f, 58f, 69f), floatArrayOf(329f, 160f, 72f, 79f),
                )
                blocks.forEach { b ->
                    drawRoundRect(
                        color = palette.background,
                        topLeft = Offset(px(b[0]), px(b[1])),
                        size = Size(px(b[2]), px(b[3])),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(px(8f)),
                    )
                }
                drawRoundRect(palette.border, Offset(px(160f), px(224f)),
                    Size(px(140f), px(70f)),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(px(24f)))
                listOf(75f, 151f, 239f, 319f).forEach { x ->
                    drawRect(palette.surface, Offset(px(x), 0f), Size(px(12f), size.height))
                }
                listOf(132f, 249f).forEach { y ->
                    drawRect(palette.surface, Offset(0f, px(y)), Size(size.width, px(12f)))
                }
                if (nearby != null && user != null) {
                    val ux = with(density) { (userX + 24.dp).toPx() }
                    val uy = with(density) { (userY + 24.dp).toPx() }
                    val sx = with(density) { (stationX + 24.dp).toPx() }
                    val sy = with(density) { (stationY + 24.dp).toPx() }
                    drawLine(palette.secondary, Offset(ux, uy), Offset(sx, sy),
                        strokeWidth = px(2f), pathEffect = PathEffect.dashPathEffect(floatArrayOf(px(4f), px(5f))))
                }
            }
            Text("ÁREA ILUSTRATIVA", Modifier.offset(x = 18.dp, y = 116.dp),
                color = palette.secondary, fontSize = 11.sp)
            Text("Parque ilustrativo", Modifier.offset(x = width * 0.4f, y = 232.dp),
                color = palette.secondary, fontSize = 11.sp)
            Box(
                Modifier.offset(x = 12.dp, y = 12.dp)
                    .background(palette.surface, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 7.dp)
            ) {
                Text("Mapa esquemático · sem navegação", color = palette.secondary, fontSize = 12.sp)
            }
            if (user != null) UserMarker(palette, Modifier.offset(x = userX, y = userY))
            if (nearby != null && user != null) {
                StationMarker(palette, markerKind, Modifier.offset(x = stationX, y = stationY))
                Box(
                    Modifier.offset(x = (stationX - 110.dp).coerceIn(8.dp, (width - 230.dp).coerceAtLeast(8.dp)),
                        y = stationY - 44.dp)
                        .widthIn(max = 220.dp)
                        .background(palette.action, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 9.dp)
                ) {
                    Text("${markerKind.symbol} ${nearby.station.name}", color = palette.onAction,
                        fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(
                Modifier.offset(x = 12.dp, y = 270.dp)
                    .background(palette.surface, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (user == null) "●  Localização pendente" else "●  Você",
                    color = palette.secondary, fontSize = 12.sp)
                if (nearby != null && user != null) {
                    Text("♧  Estação", color = palette.secondary, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun UserMarker(p: MeuArPalette, modifier: Modifier) {
    Box(modifier.size(48.dp).background(p.surface, RoundedCornerShape(50)),
        contentAlignment = Alignment.Center) {
        Box(Modifier.size(18.dp).background(p.primary, RoundedCornerShape(50)))
    }
}

@Composable
private fun StationMarker(p: MeuArPalette, kind: MapMarkerKind, modifier: Modifier) {
    Box(modifier.size(48.dp).background(p.action, RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center) {
        if (kind == MapMarkerKind.Station) RadioTowerIcon(p)
        else Text(kind.symbol, color = p.onAction, fontSize = 27.sp)
    }
}

@Composable
private fun RadioTowerIcon(p: MeuArPalette) {
    Canvas(Modifier.size(24.dp)) {
        val middle = size.width / 2f
        val stroke = size.width / 12f
        drawCircle(p.onAction, radius = size.width / 10f, center = Offset(middle, size.height * 0.36f))
        drawLine(p.onAction, Offset(middle, size.height * 0.47f),
            Offset(middle, size.height * 0.88f), strokeWidth = stroke)
        drawLine(p.onAction, Offset(middle, size.height * 0.53f),
            Offset(size.width * 0.34f, size.height * 0.88f), strokeWidth = stroke)
        drawLine(p.onAction, Offset(middle, size.height * 0.53f),
            Offset(size.width * 0.66f, size.height * 0.88f), strokeWidth = stroke)
        drawArc(p.onAction, 130f, 100f, false,
            topLeft = Offset(size.width * 0.2f, size.height * 0.08f),
            size = Size(size.width * 0.6f, size.height * 0.6f),
            style = Stroke(stroke))
        drawArc(p.onAction, 310f, 100f, false,
            topLeft = Offset(size.width * 0.2f, size.height * 0.08f),
            size = Size(size.width * 0.6f, size.height * 0.6f),
            style = Stroke(stroke))
    }
}

private val MapMarkerKind.symbol: String
    get() = when (this) {
        MapMarkerKind.Station -> "♧"
        MapMarkerKind.Historical -> "◴"
        MapMarkerKind.NoMeasurement -> "⊖"
    }
