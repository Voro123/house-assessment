package com.voro.houseassessment.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import java.io.File

@Composable
fun PhotoViewerDialog(
    photos: List<String>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    if (photos.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, photos.lastIndex)) }

    LaunchedEffect(photos.size) {
        if (photos.isEmpty()) {
            onDismiss()
        } else {
            currentIndex = currentIndex.coerceIn(0, photos.lastIndex)
        }
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
        ) {
            ZoomablePhoto(
                path = photos[currentIndex],
                modifier = Modifier.fillMaxSize()
            )

            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 12.dp, vertical = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "关闭大图", tint = Color.White)
                }
                Text(
                    text = "${currentIndex + 1} / ${photos.size}  ·  双击或双指缩放",
                    color = Color.White
                )
                IconButton(onClick = { onDelete(photos[currentIndex]) }) {
                    Icon(Icons.Default.Delete, contentDescription = "删除当前照片", tint = Color.White)
                }
            }

            if (currentIndex > 0) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 12.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    IconButton(onClick = { currentIndex -= 1 }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "上一张", tint = Color.White)
                    }
                }
            }

            if (currentIndex < photos.lastIndex) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 12.dp),
                    shape = CircleShape,
                    color = Color.Black.copy(alpha = 0.55f)
                ) {
                    IconButton(onClick = { currentIndex += 1 }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "下一张", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomablePhoto(path: String, modifier: Modifier = Modifier) {
    var scale by remember(path) { mutableFloatStateOf(1f) }
    var offset by remember(path) { mutableStateOf(Offset.Zero) }

    AsyncImage(
        model = File(path),
        contentDescription = "房源现场大图",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .pointerInput(path) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                        }
                    }
                )
            }
            .pointerInput(path) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val nextScale = (scale * zoom).coerceIn(1f, 5f)
                    scale = nextScale
                    offset = if (nextScale == 1f) Offset.Zero else offset + pan
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
    )
}
