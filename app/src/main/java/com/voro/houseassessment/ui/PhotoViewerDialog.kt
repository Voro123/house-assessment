package com.voro.houseassessment.ui

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@Composable
fun PhotoViewerDialog(
    photos: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit
) {
    if (photos.isEmpty()) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, photos.lastIndex)) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var pendingLegacyExport by remember { mutableStateOf<String?>(null) }

    fun savePhoto(path: String) {
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching { saveOriginalToGallery(context, path) }
            }
            Toast.makeText(
                context,
                if (result.isSuccess) "原图已保存到系统相册 / Pictures/HouseAssessment"
                else "导出失败：${result.exceptionOrNull()?.localizedMessage ?: "未知错误"}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    val legacyStoragePermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val path = pendingLegacyExport
        pendingLegacyExport = null
        if (granted && path != null) {
            savePhoto(path)
        } else if (!granted) {
            Toast.makeText(context, "未获得存储权限，无法保存到系统相册", Toast.LENGTH_LONG).show()
        }
    }

    fun requestSave(path: String) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            pendingLegacyExport = path
            legacyStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            savePhoto(path)
        }
    }

    LaunchedEffect(currentIndex) {
        scale = 1f
        offset = Offset.Zero
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .systemBarsPadding()
        ) {
            AsyncImage(
                model = File(photos[currentIndex]),
                contentDescription = "现场照片 ${currentIndex + 1}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 72.dp, bottom = 104.dp)
                    .pointerInput(currentIndex) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            val newScale = (scale * zoom).coerceIn(1f, 5f)
                            scale = newScale
                            offset = if (newScale > 1f) offset + pan else Offset.Zero
                        }
                    }
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    }
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(color = Color.Black.copy(alpha = 0.62f)) {
                    Text(
                        "${currentIndex + 1} / ${photos.size} · 双指缩放",
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = "关闭大图", tint = Color.White)
            }

            IconButton(
                onClick = { currentIndex -= 1 },
                enabled = currentIndex > 0,
                modifier = Modifier.align(Alignment.CenterStart).padding(8.dp)
            ) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "上一张",
                    tint = if (currentIndex > 0) Color.White else Color.Gray
                )
            }

            IconButton(
                onClick = { currentIndex += 1 },
                enabled = currentIndex < photos.lastIndex,
                modifier = Modifier.align(Alignment.CenterEnd).padding(8.dp)
            ) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "下一张",
                    tint = if (currentIndex < photos.lastIndex) Color.White else Color.Gray
                )
            }

            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
                color = Color.Black.copy(alpha = 0.68f)
            ) {
                Row(
                    modifier = Modifier.padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(onClick = { requestSave(photos[currentIndex]) }) {
                        Icon(Icons.Default.SaveAlt, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("保存原图")
                    }
                    FilledTonalButton(onClick = { shareOriginalPhoto(context, photos[currentIndex]) }) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("分享原图")
                    }
                }
            }
        }
    }
}

private fun saveOriginalToGallery(context: Context, path: String) {
    val source = File(path)
    require(source.exists()) { "原图文件不存在" }

    val extension = source.extension.lowercase(Locale.ROOT).ifBlank { "jpg" }
    val mimeType = mimeTypeForExtension(extension)
    val displayName = "house_assessment_${System.currentTimeMillis()}.$extension"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/HouseAssessment")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("无法创建系统相册文件")
        try {
            resolver.openOutputStream(uri)?.use { output ->
                source.inputStream().use { input -> input.copyTo(output) }
            } ?: error("无法写入系统相册")
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    } else {
        @Suppress("DEPRECATION")
        val directory = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "HouseAssessment"
        ).apply { mkdirs() }
        val destination = File(directory, displayName)
        source.copyTo(destination, overwrite = false)
        MediaScannerConnection.scanFile(
            context,
            arrayOf(destination.absolutePath),
            arrayOf(mimeType),
            null
        )
    }
}

private fun shareOriginalPhoto(context: Context, path: String) {
    val file = File(path)
    if (!file.exists()) {
        Toast.makeText(context, "原图文件不存在", Toast.LENGTH_LONG).show()
        return
    }
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeTypeForExtension(file.extension.lowercase(Locale.ROOT))
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享现场原图"))
}

private fun mimeTypeForExtension(extension: String): String = when (extension.lowercase(Locale.ROOT)) {
    "png" -> "image/png"
    "webp" -> "image/webp"
    "heic", "heif" -> "image/heif"
    else -> "image/jpeg"
}
