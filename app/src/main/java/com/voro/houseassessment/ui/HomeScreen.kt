package com.voro.houseassessment.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voro.houseassessment.data.RoomRecord
import com.voro.houseassessment.util.AssessmentEngine
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    rooms: List<RoomRecord>,
    onAdd: () -> Unit,
    onEdit: (RoomRecord) -> Unit,
    onDelete: (RoomRecord) -> Unit
) {
    var pendingDelete by remember { mutableStateOf<RoomRecord?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("租房评估")
                        Text("边看房边记录，最后统一比较", style = MaterialTheme.typography.bodySmall)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "新增房源")
            }
        }
    ) { padding ->
        if (rooms.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(12.dp))
                    Text("还没有房源记录", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("点击右下角 +，看房时任何项目都可以先留空，之后继续编辑。")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rooms, key = { it.id }) { room ->
                    RoomCard(room = room, onClick = { onEdit(room) }, onDelete = { pendingDelete = room })
                }
                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    pendingDelete?.let { room ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除房源？") },
            text = { Text("“${room.title.ifBlank { room.address.ifBlank { "未命名房源" } }}”的数据和评分记录将被删除。") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    onDelete(room)
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun RoomCard(room: RoomRecord, onClick: () -> Unit, onDelete: () -> Unit) {
    val result = remember(room) { AssessmentEngine.evaluate(room) }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        room.title.ifBlank { room.address.ifBlank { "未命名房源" } },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (room.address.isNotBlank() && room.title.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Text(room.address, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        result.score?.let { String.format(Locale.US, "%.1f", it) } ?: "待评",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "删除") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(room.rentMonthly?.let { "¥${formatMoney(it)}/月" } ?: "租金未填")
                Text(room.areaSqm?.let { "${formatMoney(it)}㎡" } ?: "面积未填")
            }
            Spacer(Modifier.height(6.dp))
            Text(result.summary, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Text("最后编辑：${formatTime(room.updatedAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatMoney(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.1f", value)

private fun formatTime(timestamp: Long): String = Instant.ofEpochMilli(timestamp)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
