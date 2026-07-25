package com.voro.houseassessment.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
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
    onDismiss: () -> Unit
) {
    if (photos.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(initialIndex.coerceIn(0, photos.lastIndex)) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

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
                    .padding(vertical = 72.dp)
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
        }
    }
}
