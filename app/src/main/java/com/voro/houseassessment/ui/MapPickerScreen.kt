package com.voro.houseassessment.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.baidu.mapapi.map.BaiduMap
import com.baidu.mapapi.map.MapPoi
import com.baidu.mapapi.map.MapStatus
import com.baidu.mapapi.map.MapStatusUpdateFactory
import com.baidu.mapapi.map.MapView
import com.baidu.mapapi.model.LatLng
import com.voro.houseassessment.data.BaiduMapConfigRepository
import com.voro.houseassessment.util.BaiduMapRuntime
import com.voro.houseassessment.util.getAppSigningSha1
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun MapPickerScreen(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onCancel: () -> Unit,
    onConfirm: (Double, Double) -> Unit
) {
    val context = LocalContext.current
    val repository = remember { BaiduMapConfigRepository(context.applicationContext) }
    var showSetup by remember {
        mutableStateOf(repository.getApiKey().isBlank() || !repository.isPrivacyAccepted())
    }
    var configRevision by remember { mutableStateOf(0) }

    BackHandler(onBack = onCancel)

    if (showSetup) {
        BaiduMapSetupScreen(
            repository = repository,
            onCancel = onCancel,
            onConfigured = {
                configRevision += 1
                showSetup = false
            }
        )
        return
    }

    val apiKey = remember(configRevision) { repository.getApiKey() }
    val initializeResult = remember(apiKey) { BaiduMapRuntime.initialize(context, apiKey) }
    if (initializeResult.isFailure) {
        BaiduMapInitializationError(
            message = initializeResult.exceptionOrNull()?.message ?: "百度地图初始化失败",
            onBack = onCancel,
            onConfigure = { showSetup = true }
        )
        return
    }

    NativeBaiduMapPicker(
        initialLatitude = initialLatitude,
        initialLongitude = initialLongitude,
        onCancel = onCancel,
        onConfirm = onConfirm,
        onConfigure = { showSetup = true }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BaiduMapSetupScreen(
    repository: BaiduMapConfigRepository,
    onCancel: () -> Unit,
    onConfigured: () -> Unit
) {
    val context = LocalContext.current
    var apiKey by remember { mutableStateOf(repository.getApiKey()) }
    var privacyAccepted by remember { mutableStateOf(repository.isPrivacyAccepted()) }
    val packageName = remember { context.packageName }
    val signingSha1 = remember { getAppSigningSha1(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("配置百度地图") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "地图选点现在使用百度原生 Android MapView。百度要求 AK 与当前 APK 的包名和签名 SHA1 匹配。",
                style = MaterialTheme.typography.bodyMedium
            )

            CopyableConfigRow(label = "程序包名", value = packageName)
            CopyableConfigRow(label = "签名 SHA1", value = signingSha1)

            Button(
                onClick = { openUrl(context, BAIDU_AK_CONSOLE_URL) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("打开百度开放平台申请 AK")
            }

            Text(
                "在百度控制台创建“Android SDK”类型应用，把上面的包名和 SHA1 原样填入，然后把生成的 AK 粘贴到下面。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it.trim() },
                label = { Text("百度地图 AK") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = privacyAccepted,
                    onCheckedChange = { privacyAccepted = it }
                )
                Text(
                    "我同意在打开地图选点时初始化百度地图 SDK，并已了解其隐私处理说明。",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            TextButton(
                onClick = { openUrl(context, BAIDU_PRIVACY_URL) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("查看百度地图开放平台隐私政策")
            }

            Button(
                enabled = apiKey.isNotBlank() && privacyAccepted,
                onClick = {
                    repository.save(apiKey, privacyAccepted)
                    onConfigured()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存并打开百度地图")
            }

            Text(
                "AK 只保存在这台手机本地，不会提交到 GitHub。若填错 AK，地图可能仍无法加载；可返回这里重新配置。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CopyableConfigRow(label: String, value: String) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelMedium)
                Text(value, style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = { copyToClipboard(context, label, value) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "复制$label")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BaiduMapInitializationError(
    message: String,
    onBack: () -> Unit,
    onConfigure: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("百度地图初始化失败") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(message, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onConfigure) { Text("重新配置 AK") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NativeBaiduMapPicker(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onCancel: () -> Unit,
    onConfirm: (Double, Double) -> Unit,
    onConfigure: () -> Unit
) {
    val initialPoint = remember(initialLatitude, initialLongitude) {
        LatLng(initialLatitude ?: DEFAULT_LATITUDE, initialLongitude ?: DEFAULT_LONGITUDE)
    }
    var selectedPoint by remember { mutableStateOf(initialPoint) }
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var mapLoaded by remember { mutableStateOf(false) }
    var showLoadHint by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(8000)
        if (!mapLoaded) showLoadHint = true
    }

    DisposableEffect(mapView) {
        val view = mapView
        onDispose {
            if (view != null) {
                runCatching { view.onPause() }
                runCatching { view.onDestroy() }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("百度地图选点") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = onConfigure) { Text("AK 设置") }
                    IconButton(onClick = { onConfirm(selectedPoint.latitude, selectedPoint.longitude) }) {
                        Icon(Icons.Default.Check, contentDescription = "确认位置")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        "已选：${formatCoordinate(selectedPoint.latitude)}, ${formatCoordinate(selectedPoint.longitude)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "点击地图直接选点，或拖动地图让中心准星对准房源。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { onConfirm(selectedPoint.latitude, selectedPoint.longitude) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("使用这个位置")
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    MapView(context).apply {
                        mapView = this
                        onResume()
                        val baiduMap = map
                        baiduMap.setMapStatus(MapStatusUpdateFactory.newLatLngZoom(initialPoint, 18f))
                        baiduMap.setOnMapLoadedCallback {
                            mapLoaded = true
                            showLoadHint = false
                        }
                        baiduMap.setOnMapClickListener(object : BaiduMap.OnMapClickListener {
                            override fun onMapClick(point: LatLng) {
                                selectedPoint = point
                                baiduMap.animateMapStatus(MapStatusUpdateFactory.newLatLng(point))
                            }

                            override fun onMapPoiClick(mapPoi: MapPoi) {
                                // Keep POI taps from unexpectedly replacing the selected position.
                            }
                        })
                        baiduMap.setOnMapStatusChangeListener(object : BaiduMap.OnMapStatusChangeListener {
                            override fun onMapStatusChangeStart(status: MapStatus) = Unit

                            override fun onMapStatusChangeStart(status: MapStatus, reason: Int) = Unit

                            override fun onMapStatusChange(status: MapStatus) = Unit

                            override fun onMapStatusChangeFinish(status: MapStatus) {
                                selectedPoint = status.target
                            }
                        })
                    }
                }
            )

            Canvas(modifier = Modifier.align(Alignment.Center).size(38.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(Color.White, radius = 11.dp.toPx(), center = center)
                drawCircle(Color(0xFF246BFD), radius = 9.dp.toPx(), center = center)
                drawCircle(Color.White, radius = 3.dp.toPx(), center = center)
                drawLine(
                    color = Color(0xFF246BFD),
                    start = Offset(center.x, center.y + 9.dp.toPx()),
                    end = Offset(center.x, size.height),
                    strokeWidth = 3.dp.toPx()
                )
            }

            if (showLoadHint) {
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(12.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.96f),
                    shadowElevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "百度底图暂未加载。请检查网络，以及 AK 是否与本页显示的包名 / SHA1 完全匹配。",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(onClick = onConfigure) { Text("检查 AK 设置") }
                    }
                }
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, value: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, value))
    Toast.makeText(context, "$label 已复制", Toast.LENGTH_SHORT).show()
}

private fun openUrl(context: Context, url: String) {
    runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }.onFailure {
        Toast.makeText(context, "无法打开浏览器：${it.localizedMessage ?: "未知错误"}", Toast.LENGTH_LONG).show()
    }
}

private fun formatCoordinate(value: Double): String = String.format(Locale.US, "%.6f", value)

private const val DEFAULT_LATITUDE = 1.3521
private const val DEFAULT_LONGITUDE = 103.8198
private const val BAIDU_AK_CONSOLE_URL = "https://lbsyun.baidu.com/apiconsole/key#/home"
private const val BAIDU_PRIVACY_URL = "https://lbsyun.baidu.com/docs/pcsa?title=compliance/openprivacy"
