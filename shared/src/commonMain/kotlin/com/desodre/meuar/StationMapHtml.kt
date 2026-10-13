package com.desodre.meuar

enum class MapMarkerKind { Station, Historical, NoMeasurement }

/**
 * Geographic map shared by Android WebView and iOS WKWebView.
 * Only the tile layer is recolored; markers, overlays and attribution retain contrast.
 */
fun stationMapHtml(
    user: GeoPoint,
    station: GeoPoint,
    stationName: String,
    markerKind: MapMarkerKind,
    dark: Boolean,
): String {
    require(user.isValid() && station.isValid()) { "Invalid map coordinates" }
    val stationIcon = when (markerKind) {
        MapMarkerKind.Station ->
            """<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="8" r="2"/><path d="M12 10v10M7 20l5-8 5 8M7 5a5 5 0 0 0 0 6M17 5a5 5 0 0 1 0 6M4 2a9 9 0 0 0 0 12M20 2a9 9 0 0 1 0 12"/></svg>"""
        MapMarkerKind.Historical ->
            """<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M12 6v6l4 2M4 4v5h5"/></svg>"""
        MapMarkerKind.NoMeasurement ->
            """<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M8 12h8"/></svg>"""
    }
    return """
        <!doctype html>
        <html lang="pt-BR">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no">
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">
          <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.css">
          <style>
            :root {
              --surface: {{surface}};
              --surface-secondary: {{surfaceSecondary}};
              --ink: {{ink}};
              --muted: {{muted}};
              --tile-filter: {{tileFilter}};
            }
            * { box-sizing: border-box; }
            html, body, #map { width: 100%; height: 100%; margin: 0; }
            body { overflow: hidden; background: var(--surface-secondary); font: 13px Inter, system-ui, sans-serif; }
            #map { background: var(--surface-secondary); }
            .leaflet-tile-pane { filter: var(--tile-filter); }
            .leaflet-container { font: inherit; }
            .leaflet-control-attribution {
              margin: 0 !important; padding: 2px 5px !important;
              color: var(--muted) !important; background: var(--surface) !important;
              font-size: 9px !important;
            }
            .leaflet-control-attribution a { color: var(--ink) !important; }
            .map-kind, .legend, .recenter {
              position: absolute; z-index: 1000;
              color: var(--muted); background: var(--surface);
              box-shadow: 0 1px 6px rgba(0,0,0,.07);
            }
            .map-kind { top: 12px; left: 12px; padding: 7px 10px; border-radius: 8px; pointer-events: none; }
            .legend {
              left: 12px; bottom: 24px; border-radius: 8px;
              display: flex; align-items: center; gap: 12px;
              padding: 8px 10px; pointer-events: none; font-size: 12px;
            }
            .legend-item { display: flex; align-items: center; gap: 6px; white-space: nowrap; }
            .legend-dot { display: block; width: 10px; height: 10px; border-radius: 50%; background: var(--ink); }
            .legend-station { display: block; width: 12px; height: 12px; border-radius: 3px; background: var(--ink); }
            .recenter {
              right: 16px; bottom: 57px; width: 48px; height: 48px;
              border: 0; border-radius: 12px; display: grid; place-items: center;
            }
            .recenter svg { width: 24px; height: 24px; fill: none; stroke: var(--ink); stroke-width: 2; }
            .user-anchor, .station-anchor { border: 0; background: none; }
            .user-pin {
              width: 48px; height: 48px; border-radius: 50%;
              display: grid; place-items: center;
              background: var(--surface); box-shadow: 0 1px 10px rgba(0,0,0,.12);
            }
            .user-pin::after { content: ''; width: 18px; height: 18px; border-radius: 50%; background: var(--ink); }
            .station-pin {
              width: 48px; height: 48px; border-radius: 12px;
              display: grid; place-items: center; background: var(--ink);
              box-shadow: 0 2px 8px rgba(0,0,0,.16);
            }
            .station-pin svg { width: 24px; height: 24px; fill: none; stroke: var(--surface); stroke-width: 2; stroke-linecap: round; stroke-linejoin: round; }
            .station-label {
              position: absolute; left: 50%; bottom: 56px; transform: translateX(-50%);
              max-width: 220px; padding: 10px; border-radius: 8px;
              background: var(--ink); color: var(--surface);
              font-weight: 600; white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
              box-shadow: 0 2px 8px rgba(0,0,0,.16);
            }
            #fallback {
              position: absolute; z-index: 1100; inset: 0;
              display: flex; align-items: center; justify-content: center;
              flex-direction: column; gap: 12px; padding: 28px;
              text-align: center; color: var(--muted); background: var(--surface-secondary);
            }
            #retry-map {
              display: none; border: 0; border-radius: 8px; padding: 10px 16px;
              background: var(--ink); color: var(--surface); font: inherit; font-weight: 600;
            }
          </style>
        </head>
        <body>
          <div id="map" role="img" aria-label="Mapa com sua localização e a estação mais próxima"></div>
          <span id="station-name" hidden>{{stationName}}</span>
          <div id="fallback"><span id="fallback-message">Carregando mapa…</span><button id="retry-map" type="button">Tentar novamente</button></div>
          <div class="map-kind">Mapa geográfico · distância em linha reta</div>
          <button id="recenter" class="recenter" type="button" aria-label="Centralizar mapa">
            <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="7"/><circle cx="12" cy="12" r="2"/><path d="M12 1v4M12 19v4M1 12h4M19 12h4"/></svg>
          </button>
          <div class="legend" aria-hidden="true">
            <span class="legend-item"><span class="legend-dot"></span>Você</span>
            <span class="legend-item"><span class="legend-station"></span>Estação</span>
          </div>
          <script>
            (function () {
              var fallback = document.getElementById('fallback');
              var fallbackMessage = document.getElementById('fallback-message');
              var retryButton = document.getElementById('retry-map');
              var tiles;
              var hasLoadedTile = false;
              var initialized = false;
              var loadingLeaflet = false;
              var errorTimer;
              function showError() {
                if (hasLoadedTile) return;
                if (!tiles) loadingLeaflet = false;
                fallbackMessage.textContent = 'Mapa indisponível. Verifique sua conexão.';
                retryButton.style.display = 'block';
                fallback.style.display = 'flex';
              }
              function scheduleError() {
                clearTimeout(errorTimer);
                errorTimer = setTimeout(showError, 12000);
              }
              function loadLeaflet(sourceIndex) {
                var sources = [
                  'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js',
                  'https://cdn.jsdelivr.net/npm/leaflet@1.9.4/dist/leaflet.js'
                ];
                if (sourceIndex >= sources.length) {
                  loadingLeaflet = false;
                  showError();
                  return;
                }
                var script = document.createElement('script');
                script.src = sources[sourceIndex];
                script.onload = function () {
                  if (typeof L === 'undefined') loadLeaflet(sourceIndex + 1);
                  else { loadingLeaflet = false; initializeMap(); }
                };
                script.onerror = function () { loadLeaflet(sourceIndex + 1); };
                document.head.appendChild(script);
              }
              function initializeMap() {
                if (initialized) return;
                initialized = true;
              var user = [{{userLatitude}}, {{userLongitude}}];
              var station = [{{stationLatitude}}, {{stationLongitude}}];
              var map = L.map('map', { zoomControl: false, scrollWheelZoom: false });
              tiles = L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19,
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
              }).addTo(map);
              tiles.on('tileload', function () {
                hasLoadedTile = true;
                clearTimeout(errorTimer);
                fallback.style.display = 'none';
              });
              tiles.on('tileerror', showError);
              map.attributionControl.setPrefix(false);
              var label = document.createElement('div');
              label.className = 'station-label';
              label.textContent = document.getElementById('station-name').textContent;
              var userIcon = L.divIcon({
                className: 'user-anchor', html: '<div class="user-pin"></div>',
                iconSize: [48, 48], iconAnchor: [24, 24]
              });
              var stationIcon = L.divIcon({
                className: 'station-anchor',
                html: '<div class="station-pin">{{stationIcon}}</div>' + label.outerHTML,
                iconSize: [48, 48], iconAnchor: [24, 24]
              });
              L.polyline([user, station], {
                color: '{{lineColor}}', weight: 2, dashArray: '4,6', opacity: 0.85,
                interactive: false
              }).addTo(map);
              L.marker(user, { icon: userIcon, title: 'Você' }).addTo(map);
              L.marker(station, { icon: stationIcon, title: 'Estação mais próxima' }).addTo(map);
              var bounds = L.latLngBounds([user, station]);
              function recenter() {
                map.fitBounds(bounds, { padding: [64, 64], maxZoom: 15, animate: false });
              }
              recenter();
              document.getElementById('recenter').addEventListener('click', recenter);
              setTimeout(function () { map.invalidateSize(); recenter(); }, 100);
              }
              retryButton.addEventListener('click', function () {
                retryButton.style.display = 'none';
                fallbackMessage.textContent = 'Carregando mapa…';
                scheduleError();
                if (tiles) tiles.redraw();
                else if (!loadingLeaflet) { loadingLeaflet = true; loadLeaflet(0); }
              });
              loadingLeaflet = true;
              loadLeaflet(0);
              scheduleError();
            })();
          </script>
        </body>
        </html>
    """.trimIndent()
        .replace("{{surface}}", if (dark) "#1C1C1C" else "#FFFFFF")
        .replace("{{surfaceSecondary}}", if (dark) "#2A2A2A" else "#EEEEEE")
        .replace("{{ink}}", if (dark) "#FFFFFF" else "#000000")
        .replace("{{muted}}", if (dark) "#B8B8B8" else "#545454")
        .replace("{{tileFilter}}", if (dark) "grayscale(1) invert(1) brightness(.72) contrast(.9)" else "grayscale(1) contrast(.9) brightness(1.04)")
        .replace("{{stationIcon}}", stationIcon)
        .replace("{{userLatitude}}", user.latitude.toString())
        .replace("{{userLongitude}}", user.longitude.toString())
        .replace("{{stationLatitude}}", station.latitude.toString())
        .replace("{{stationLongitude}}", station.longitude.toString())
        .replace("{{lineColor}}", if (dark) "#FFFFFF" else "#545454")
        .replace("{{stationName}}", stationName.escapeHtml())
}

private fun String.escapeHtml(): String = buildString {
    for (character in this@escapeHtml) {
        append(when (character) {
            '&' -> "&amp;"
            '<' -> "&lt;"
            '>' -> "&gt;"
            '"' -> "&quot;"
            '\'' -> "&#39;"
            else -> character
        })
    }
}
