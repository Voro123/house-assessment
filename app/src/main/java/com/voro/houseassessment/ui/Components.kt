package com.voro.houseassessment.ui

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Help
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.voro.houseassessment.util.AssessmentResult
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun NullableRatingRow(
    label: String,
    value: Int?,
    onChange: (Int?) -> Unit,
    hint: String = "1 很差 · 5 很好"
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(value?.let { "$it / 5" } ?: "未评", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items((1..5).toList()) { score ->
                FilterChip(
                    selected = value == score,
                    onClick = { onChange(if (value == score) null else score) },
                    label = { Text(score.toString()) }
                )
            }
            item {
                AssistChip(onClick = { onChange(null) }, label = { Text("留空") })
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun NullableBooleanRow(
    label: String,
    value: Boolean?,
    onChange: (Boolean?) -> Unit,
    yesText: String = "有",
    noText: String = "无"
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(selected = value == true, onClick = { onChange(true) }, label = { Text(yesText) })
            }
            item {
                FilterChip(selected = value == false, onClick = { onChange(false) }, label = { Text(noText) })
            }
            item {
                FilterChip(selected = value == null, onClick = { onChange(null) }, label = { Text("未确认") })
            }
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
fun <T> ChoiceRow(
    label: String,
    value: T?,
    choices: List<Pair<T, String>>,
    onChange: (T?) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(choices) { (key, text) ->
                FilterChip(
                    selected = value == key,
                    onClick = { onChange(if (value == key) null else key) },
                    label = { Text(text) }
                )
            }
            item { AssistChip(onClick = { onChange(null) }, label = { Text("留空") }) }
        }
    }
}

@Composable
fun NullableDoubleField(
    label: String,
    value: Double?,
    onChange: (Double?) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null
) {
    var text by remember(value) { mutableStateOf(value?.let(::formatNumber) ?: "") }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.filter { char -> char.isDigit() || char == '.' }
            onChange(text.toDoubleOrNull())
        },
        label = { Text(label) },
        suffix = suffix?.let { suffixText -> { Text(suffixText) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        modifier = modifier
    )
}

@Composable
fun NullableIntField(
    label: String,
    value: Int?,
    onChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null
) {
    var text by remember(value) { mutableStateOf(value?.toString() ?: "") }
    OutlinedTextField(
        value = text,
        onValueChange = {
            text = it.filter(Char::isDigit)
            onChange(text.toIntOrNull())
        },
        label = { Text(label) },
        suffix = suffix?.let { suffixText -> { Text(suffixText) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        modifier = modifier
    )
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')

@Composable
fun CompassPicker(
    selected: String?,
    onSelected: (String?) -> Unit
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(SensorManager::class.java) }
    val sensor = remember { sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) }
    var heading by remember { mutableFloatStateOf(0f) }
    var hasReading by remember { mutableStateOf(false) }

    DisposableEffect(sensorManager, sensor) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val rotation = FloatArray(9)
                val orientation = FloatArray(3)
                SensorManager.getRotationMatrixFromVector(rotation, event.values)
                SensorManager.getOrientation(rotation, orientation)
                heading = ((Math.toDegrees(orientation[0].toDouble()) + 360.0) % 360.0).toFloat()
                hasReading = true
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (sensor != null) sensorManager?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager?.unregisterListener(listener) }
    }

    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("房屋朝向", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text("将手机顶部朝向主要窗户或阳台", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (hasReading) "指南针 ${heading.roundToInt()}° · ${directionFromDegrees(heading)}" else "设备指南针不可用或正在初始化",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AssistChip(
                enabled = hasReading,
                onClick = { onSelected(directionFromDegrees(heading)) },
                label = { Text("采用当前方向") }
            )
        }
        Spacer(Modifier.height(6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(listOf("北", "东北", "东", "东南", "南", "西南", "西", "西北")) { direction ->
                FilterChip(
                    selected = selected == direction,
                    onClick = { onSelected(if (selected == direction) null else direction) },
                    label = { Text(direction) }
                )
            }
        }
    }
}

private fun directionFromDegrees(degrees: Float): String {
    val directions = listOf("北", "东北", "东", "东南", "南", "西南", "西", "西北")
    return directions[((degrees + 22.5f) / 45f).toInt() % 8]
}

@Composable
fun AssessmentCard(result: AssessmentResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("综合评价", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("可信度：${result.confidence}（${result.filledCount}/${result.importantCount} 项）", style = MaterialTheme.typography.bodySmall)
                }
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primary) {
                    Text(
                        result.score?.let { String.format(Locale.US, "%.1f 分", it) } ?: "待评估",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(result.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(result.summary)

            if (result.warnings.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                result.warnings.forEach {
                    Row {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (result.pros.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("主要优点", fontWeight = FontWeight.SemiBold)
                result.pros.forEach {
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(it)
                    }
                }
            }
            if (result.cons.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("主要缺点", fontWeight = FontWeight.SemiBold)
                result.cons.forEach {
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.Error, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(it)
                    }
                }
            }
            if (result.missing.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("待确认", fontWeight = FontWeight.SemiBold)
                result.missing.take(4).forEach {
                    Row(modifier = Modifier.padding(top = 4.dp)) {
                        Icon(Icons.Default.Help, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(it)
                    }
                }
            }
        }
    }
}
