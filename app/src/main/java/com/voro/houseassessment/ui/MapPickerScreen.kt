package com.voro.houseassessment.ui

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun MapPickerScreen(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onCancel: () -> Unit,
    onConfirm: (Double, Double) -> Unit
) {
    var selected by remember {
        mutableStateOf(
            if (initialLatitude != null && initialLongitude != null) initialLatitude to initialLongitude else null
        )
    }
    var webView by remember { mutableStateOf<WebView?>(null) }
    BackHandler(onBack = onCancel)

    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val bridge = remember {
        object {
            @JavascriptInterface
            fun onMapSelected(latitude: Double, longitude: Double) {
                mainHandler.post { selected = latitude to longitude }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("地图选择位置") },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    IconButton(
                        enabled = selected != null,
                        onClick = { selected?.let { onConfirm(it.first, it.second) } }
                    ) { Icon(Icons.Default.Check, contentDescription = "确认位置") }
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    selected?.let { "已选择：${formatCoordinate(it.first)}, ${formatCoordinate(it.second)}" }
                        ?: "点击地图选择房源位置",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(
                    enabled = selected != null,
                    onClick = { selected?.let { onConfirm(it.first, it.second) } },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("使用这个位置")
                }
            }
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(padding),
            factory = { context ->
                WebView(context).apply {
                    webView = this
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.setSupportZoom(true)
                    addJavascriptInterface(bridge, "Android")
                    webViewClient = WebViewClient()
                    loadDataWithBaseURL(
                        "https://www.openstreetmap.org/",
                        mapHtml(initialLatitude, initialLongitude),
                        "text/html",
                        "UTF-8",
                        null
                    )
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.removeJavascriptInterface("Android")
            webView?.destroy()
        }
    }
}

private fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)

private fun mapHtml(latitude: Double?, longitude: Double?): String {
    val lat = latitude ?: 35.681236
    val lng = longitude ?: 139.767125
    val zoom = if (latitude != null && longitude != null) 16 else 11
    val hasInitial = latitude != null && longitude != null
    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
          <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
          <style>
            html, body, #map { height: 100%; width: 100%; margin: 0; padding: 0; }
            .tip { position: absolute; z-index: 1000; top: 12px; left: 12px; right: 12px; padding: 10px 12px; border-radius: 10px; background: rgba(255,255,255,.94); font-family: sans-serif; box-shadow: 0 2px 10px rgba(0,0,0,.18); }
          </style>
        </head>
        <body>
          <div id="map"></div>
          <div class="tip">点击地图放置或移动房源位置</div>
          <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
          <script>
            const map = L.map('map').setView([$lat, $lng], $zoom);
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
              maxZoom: 19,
              attribution: '&copy; OpenStreetMap contributors'
            }).addTo(map);
            let marker = null;
            function selectPoint(lat, lng) {
              if (marker) marker.setLatLng([lat, lng]);
              else marker = L.circleMarker([lat, lng], { radius: 10, color: '#6750A4', fillColor: '#6750A4', fillOpacity: .8 }).addTo(map);
              if (window.Android && Android.onMapSelected) Android.onMapSelected(lat, lng);
            }
            ${if (hasInitial) "selectPoint($lat, $lng);" else ""}
            map.on('click', function(e) { selectPoint(e.latlng.lat, e.latlng.lng); });
          </script>
        </body>
        </html>
    """.trimIndent()
}
