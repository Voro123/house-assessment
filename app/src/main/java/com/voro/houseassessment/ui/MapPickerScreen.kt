package com.voro.houseassessment.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.util.Locale
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onCancel: () -> Unit,
    onConfirm: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val fallback = remember { defaultMapCenter() }
    var centerLatitude by remember {
        mutableDoubleStateOf(initialLatitude ?: fallback.first)
    }
    var centerLongitude by remember {
        mutableDoubleStateOf(initialLongitude ?: fallback.second)
    }
    var zoom by remember {
        mutableIntStateOf(if (initialLatitude != null && initialLongitude != null) 16 else fallback.third)
    }
    var mapSize by remember { mutableStateOf(IntSize.Zero) }
    var loadedTiles by remember { mutableIntStateOf(0) }
    var failedTiles by remember { mutableIntStateOf(0) }

    BackHandler(onBack = onCancel)

    val accent = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val tileDp = with(density) { TILE_SIZE_PX.toDp() }
    val centerWorld = remember(centerLatitude, centerLongitude, zoom) {
        latLonToWorld(centerLatitude, centerLongitude, zoom)
    }
    val visibleTiles = remember(centerWorld, mapSize, zoom) {
        visibleTiles(centerWorld, mapSize, zoom)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("地图选择位置") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { onConfirm(centerLatitude, centerLongitude) }) {
                        Icon(Icons.Default.Check, contentDescription = "确认位置")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Text(
                        "准星：${formatCoordinate(centerLatitude)}, ${formatCoordinate(centerLongitude)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "拖动地图让准星对准房源。底图来自 OpenStreetMap；可用百度地图校对当前坐标。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(
                            onClick = { openInBaiduMap(context, centerLatitude, centerLongitude) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("百度地图校对")
                        }
                        Button(
                            onClick = { onConfirm(centerLatitude, centerLongitude) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("使用这个位置")
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFE7E4DE))
                .onSizeChanged { mapSize = it }
                .pointerInput(zoom, mapSize) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val currentWorld = latLonToWorld(centerLatitude, centerLongitude, zoom)
                        val worldSize = worldSize(zoom)
                        val movedX = wrapWorldX(currentWorld.x - dragAmount.x, worldSize)
                        val movedY = (currentWorld.y - dragAmount.y).coerceIn(0.0, worldSize)
                        val moved = worldToLatLon(movedX, movedY, zoom)
                        centerLatitude = moved.first
                        centerLongitude = moved.second
                    }
                }
        ) {
            visibleTiles.forEach { tile ->
                AsyncImage(
                    model = "https://tile.openstreetmap.org/${tile.zoom}/${tile.sourceX}/${tile.y}.png",
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    onSuccess = { loadedTiles += 1 },
                    onError = { failedTiles += 1 },
                    modifier = Modifier
                        .offset { IntOffset(tile.screenX.roundToInt(), tile.screenY.roundToInt()) }
                        .size(tileDp)
                )
            }

            if (loadedTiles == 0 && failedTiles >= 2) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.94f)
                ) {
                    Text(
                        "当前网络无法加载开放底图。坐标选择仍可使用，也可以点下方“百度地图校对”查看位置。",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Canvas(
                modifier = Modifier.align(Alignment.Center).size(42.dp)
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(color = Color.White, radius = 12.dp.toPx(), center = center)
                drawCircle(color = accent, radius = 10.dp.toPx(), center = center)
                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = center)
                drawLine(
                    color = accent,
                    start = Offset(center.x, center.y + 10.dp.toPx()),
                    end = Offset(center.x, size.height),
                    strokeWidth = 3.dp.toPx()
                )
            }

            Column(
                modifier = Modifier.align(Alignment.CenterEnd).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(shadowElevation = 5.dp, shape = MaterialTheme.shapes.medium) {
                    IconButton(
                        onClick = {
                            if (zoom < MAX_ZOOM) {
                                zoom += 1
                                loadedTiles = 0
                                failedTiles = 0
                            }
                        },
                        enabled = zoom < MAX_ZOOM
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "放大地图")
                    }
                }
                Surface(shadowElevation = 5.dp, shape = MaterialTheme.shapes.medium) {
                    IconButton(
                        onClick = {
                            if (zoom > MIN_ZOOM) {
                                zoom -= 1
                                loadedTiles = 0
                                failedTiles = 0
                            }
                        },
                        enabled = zoom > MIN_ZOOM
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "缩小地图")
                    }
                }
            }

            Surface(
                modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f)
            ) {
                Text(
                    "© OpenStreetMap contributors · Z$zoom",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private data class WorldPoint(val x: Double, val y: Double)

private data class VisibleTile(
    val zoom: Int,
    val sourceX: Int,
    val y: Int,
    val screenX: Double,
    val screenY: Double
)

private data class DefaultMapCenter(
    val first: Double,
    val second: Double,
    val third: Int
)

private fun defaultMapCenter(): DefaultMapCenter = when (Locale.getDefault().country.uppercase(Locale.ROOT)) {
    "SG" -> DefaultMapCenter(1.3521, 103.8198, 11)
    "CN" -> DefaultMapCenter(35.8617, 104.1954, 4)
    "HK" -> DefaultMapCenter(22.3193, 114.1694, 11)
    "MO" -> DefaultMapCenter(22.1987, 113.5439, 12)
    "TW" -> DefaultMapCenter(23.6978, 120.9605, 7)
    "JP" -> DefaultMapCenter(36.2048, 138.2529, 6)
    else -> DefaultMapCenter(20.0, 0.0, 2)
}

private fun visibleTiles(center: WorldPoint, size: IntSize, zoom: Int): List<VisibleTile> {
    if (size.width <= 0 || size.height <= 0) return emptyList()

    val tileCount = 1 shl zoom
    val left = center.x - size.width / 2.0
    val top = center.y - size.height / 2.0
    val right = center.x + size.width / 2.0
    val bottom = center.y + size.height / 2.0

    val startX = floor(left / TILE_SIZE_PX).toInt() - 1
    val endX = floor(right / TILE_SIZE_PX).toInt() + 1
    val startY = max(0, floor(top / TILE_SIZE_PX).toInt() - 1)
    val endY = min(tileCount - 1, floor(bottom / TILE_SIZE_PX).toInt() + 1)

    return buildList {
        for (tileY in startY..endY) {
            for (tileX in startX..endX) {
                val sourceX = ((tileX % tileCount) + tileCount) % tileCount
                add(
                    VisibleTile(
                        zoom = zoom,
                        sourceX = sourceX,
                        y = tileY,
                        screenX = tileX * TILE_SIZE_PX - left,
                        screenY = tileY * TILE_SIZE_PX - top
                    )
                )
            }
        }
    }
}

private fun latLonToWorld(latitude: Double, longitude: Double, zoom: Int): WorldPoint {
    val lat = latitude.coerceIn(-WEB_MERCATOR_LIMIT, WEB_MERCATOR_LIMIT)
    val world = worldSize(zoom)
    val x = (normalizeLongitude(longitude) + 180.0) / 360.0 * world
    val sinLatitude = sin(Math.toRadians(lat))
    val y = (0.5 - ln((1.0 + sinLatitude) / (1.0 - sinLatitude)) / (4.0 * PI)) * world
    return WorldPoint(x, y)
}

private fun worldToLatLon(x: Double, y: Double, zoom: Int): Pair<Double, Double> {
    val world = worldSize(zoom)
    val longitude = normalizeLongitude(x / world * 360.0 - 180.0)
    val latitude = Math.toDegrees(atan(0.5 * (exp(PI * (1.0 - 2.0 * y / world)) - exp(-PI * (1.0 - 2.0 * y / world)))))
        .coerceIn(-WEB_MERCATOR_LIMIT, WEB_MERCATOR_LIMIT)
    return latitude to longitude
}

private fun worldSize(zoom: Int): Double = TILE_SIZE_PX * 2.0.pow(zoom)

private fun wrapWorldX(x: Double, worldSize: Double): Double = ((x % worldSize) + worldSize) % worldSize

private fun normalizeLongitude(longitude: Double): Double = ((longitude + 180.0) % 360.0 + 360.0) % 360.0 - 180.0

private fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)

private fun openInBaiduMap(context: Context, latitude: Double, longitude: Double) {
    val appUri = Uri.Builder()
        .scheme("baidumap")
        .authority("map")
        .appendPath("marker")
        .appendQueryParameter("location", "$latitude,$longitude")
        .appendQueryParameter("title", "房源位置")
        .appendQueryParameter("content", "租房评估选点")
        .appendQueryParameter("coord_type", "wgs84")
        .appendQueryParameter("src", "andr.voro.houseassessment")
        .build()

    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, appUri))
    } catch (_: ActivityNotFoundException) {
        val webUri = Uri.parse("https://api.map.baidu.com/marker").buildUpon()
            .appendQueryParameter("location", "$latitude,$longitude")
            .appendQueryParameter("title", "房源位置")
            .appendQueryParameter("content", "租房评估选点")
            .appendQueryParameter("output", "html")
            .appendQueryParameter("coord_type", "wgs84")
            .appendQueryParameter("src", "andr.voro.houseassessment")
            .build()
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

private const val TILE_SIZE_PX = 256
private const val MIN_ZOOM = 2
private const val MAX_ZOOM = 19
private const val WEB_MERCATOR_LIMIT = 85.05112878
