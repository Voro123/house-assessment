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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.voro.houseassessment.data.CachedContact
import com.voro.houseassessment.data.RoomRecord
import com.voro.houseassessment.util.AssessmentEngine
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditRoomScreen(
    room: RoomRecord,
    cachedBudget: Double?,
    recentContacts: List<CachedContact>,
    onChange: (RoomRecord) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onPickPhotos: () -> Unit,
    onTakePhoto: () -> Unit,
    onRemovePhoto: (String) -> Unit,
    onApplyCachedBudget: () -> Unit,
    onApplyContact: (CachedContact) -> Unit,
    onUseCurrentLocation: () -> Unit,
    onOpenMap: () -> Unit,
    locationBusy: Boolean
) {
    val result = remember(room) { AssessmentEngine.evaluate(room) }
    var selectedPhotoIndex by remember { mutableStateOf<Int?>(null) }

    val photoIndex = selectedPhotoIndex
    if (photoIndex != null && room.photos.isNotEmpty()) {
        PhotoViewerDialog(
            photos = room.photos,
            initialIndex = photoIndex.coerceIn(0, room.photos.lastIndex),
            onDismiss = { selectedPhotoIndex = null },
            onDelete = { path ->
                val deletingLastPhoto = room.photos.size == 1
                onRemovePhoto(path)
                if (deletingLastPhoto) selectedPhotoIndex = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (room.id == 0L) "新增房源" else "编辑房源") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "返回") }
                },
                actions = {
                    IconButton(onClick = onSave) { Icon(Icons.Default.Save, contentDescription = "保存") }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("保存房源")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                AssessmentCard(result)
            }

            item {
                SectionCard(
                    title = "基本信息",
                    subtitle = "所有字段都可以留空。建议先记租金、面积和一个好认的名称。"
                ) {
                    OutlinedTextField(
                        value = room.title,
                        onValueChange = { onChange(room.copy(title = it)) },
                        label = { Text("房源名称 / 小区与房号") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NullableDoubleField(
                            label = "月租金",
                            value = room.rentMonthly,
                            onChange = { onChange(room.copy(rentMonthly = it)) },
                            suffix = "元",
                            modifier = Modifier.weight(1f)
                        )
                        NullableDoubleField(
                            label = "目标预算",
                            value = room.targetBudget,
                            onChange = { onChange(room.copy(targetBudget = it)) },
                            suffix = "元",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (cachedBudget != null) {
                        Spacer(Modifier.height(8.dp))
                        if (room.targetBudget == cachedBudget) {
                            Text(
                                "已使用本机缓存的常用预算：${formatMoney(cachedBudget)} 元",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            OutlinedButton(onClick = onApplyCachedBudget) {
                                Text("套用常用预算 ${formatMoney(cachedBudget)} 元")
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NullableDoubleField(
                            label = "面积",
                            value = room.areaSqm,
                            onChange = { onChange(room.copy(areaSqm = it)) },
                            suffix = "㎡",
                            modifier = Modifier.weight(1f)
                        )
                        NullableDoubleField(
                            label = "押金",
                            value = room.deposit,
                            onChange = { onChange(room.copy(deposit = it)) },
                            suffix = "元",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        NullableIntField(
                            label = "所在楼层",
                            value = room.floor,
                            onChange = { onChange(room.copy(floor = it)) },
                            suffix = "层",
                            modifier = Modifier.weight(1f)
                        )
                        NullableIntField(
                            label = "总楼层",
                            value = room.totalFloors,
                            onChange = { onChange(room.copy(totalFloors = it)) },
                            suffix = "层",
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    NullableDoubleField(
                        label = "物业、水电等额外月费用",
                        value = room.extraFees,
                        onChange = { onChange(room.copy(extraFees = it)) },
                        suffix = "元",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                SectionCard(title = "地址与定位", subtitle = "可自动定位、手输地址，或打开地图点击选择。") {
                    OutlinedTextField(
                        value = room.address,
                        onValueChange = { onChange(room.copy(address = it)) },
                        label = { Text("详细地址") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                    if (room.latitude != null && room.longitude != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "坐标：${"%.6f".format(room.latitude)}, ${"%.6f".format(room.longitude)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = onUseCurrentLocation, enabled = !locationBusy, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.LocationOn, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (locationBusy) "定位中…" else "当前位置")
                        }
                        OutlinedButton(onClick = onOpenMap, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.Map, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("地图选点")
                        }
                    }
                }
            }

            item {
                SectionCard(title = "现场照片", subtitle = "点击缩略图可全屏放大、缩放并切换查看。建议拍摄全景、窗外、厨卫、空调铭牌、墙角和水龙头。最多 8 张。") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = onTakePhoto, modifier = Modifier.weight(1f), enabled = room.photos.size < 8) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("拍照")
                        }
                        OutlinedButton(onClick = onPickPhotos, modifier = Modifier.weight(1f), enabled = room.photos.size < 8) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("相册")
                        }
                    }
                    if (room.photos.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            itemsIndexed(room.photos) { index, path ->
                                Box {
                                    AsyncImage(
                                        model = File(path),
                                        contentDescription = "房源照片 ${index + 1}，点击查看大图",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(132.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable { selectedPhotoIndex = index }
                                    )
                                    Card(
                                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f))
                                    ) {
                                        IconButton(onClick = { onRemovePhoto(path) }, modifier = Modifier.size(36.dp)) {
                                            Icon(Icons.Default.Delete, contentDescription = "删除照片")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(title = "联系人", subtitle = "保存房源时会在本机缓存最近 8 个联系人，下次点击即可整组带入。") {
                    if (recentContacts.isNotEmpty()) {
                        Text(
                            "最近联系人（点击带入）",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(6.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(recentContacts) { _, contact ->
                                SuggestionChip(
                                    onClick = { onApplyContact(contact) },
                                    label = {
                                        Text(
                                            buildString {
                                                append(contact.displayName)
                                                if (contact.phone.isNotBlank() && contact.phone != contact.displayName) {
                                                    append(" · ")
                                                    append(contact.phone)
                                                }
                                            },
                                            maxLines = 1
                                        )
                                    }
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    OutlinedTextField(
                        value = room.contactName,
                        onValueChange = { onChange(room.copy(contactName = it)) },
                        label = { Text("联系人姓名 / 身份") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = room.contactPhone,
                            onValueChange = { onChange(room.copy(contactPhone = it)) },
                            label = { Text("电话") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = room.contactChannel,
                            onValueChange = { onChange(room.copy(contactChannel = it)) },
                            label = { Text("微信 / LINE 等") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = room.contactNotes,
                        onValueChange = { onChange(room.copy(contactNotes = it)) },
                        label = { Text("沟通与预约备注") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2
                    )
                }
            }

            item {
                SectionCard(title = "核心居住体验", subtitle = "评分统一为 1 很差、5 很好；再次点击已选分数可以清空。") {
                    CompassPicker(selected = room.orientation, onSelected = { onChange(room.copy(orientation = it)) })
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    NullableBooleanRow("是否有独立阳台", room.hasBalcony, { onChange(room.copy(hasBalcony = it)) })
                    NullableRatingRow("水质", room.waterQuality, { onChange(room.copy(waterQuality = it)) }, "观察颜色、气味、水垢和水压")
                    ChoiceRow(
                        label = "空调与能耗",
                        value = room.acLevel,
                        choices = listOf(0 to "无空调", 1 to "老旧/高能耗", 2 to "一般", 3 to "新/低能耗"),
                        onChange = { onChange(room.copy(acLevel = it)) }
                    )
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    NullableBooleanRow("是否有洗衣机", room.hasWasher, { onChange(room.copy(hasWasher = it)) })
                    NullableRatingRow("空间大小与布局", room.spaceRating, { onChange(room.copy(spaceRating = it)) }, "考虑床、桌、柜摆放和动线")
                    NullableRatingRow("插座情况", room.outletRating, { onChange(room.copy(outletRating = it)) }, "数量、位置、接地与老化情况")
                    NullableRatingRow("噪音", room.noiseRating, { onChange(room.copy(noiseRating = it)) }, "1 很吵 · 5 很安静；建议关窗后再听")
                    NullableRatingRow("自然采光", room.lightingRating, { onChange(room.copy(lightingRating = it)) })
                    NullableRatingRow("通风", room.ventilationRating, { onChange(room.copy(ventilationRating = it)) })
                }
            }

            item {
                SectionCard(title = "房屋状态与设施", subtitle = "墙角、窗边、柜后和卫生间吊顶最容易藏问题。") {
                    NullableRatingRow("整体卫生与维护", room.cleanlinessRating, { onChange(room.copy(cleanlinessRating = it)) })
                    NullableRatingRow("潮湿 / 霉菌", room.dampMoldRating, { onChange(room.copy(dampMoldRating = it)) }, "1 严重潮湿发霉 · 5 完全正常")
                    NullableRatingRow("卫生间", room.bathroomRating, { onChange(room.copy(bathroomRating = it)) }, "排水、热水、水压、异味与干湿分离")
                    NullableRatingRow("厨房", room.kitchenRating, { onChange(room.copy(kitchenRating = it)) }, "油烟机、燃气/电磁炉、台面和收纳")
                    NullableRatingRow("家具家电", room.furnishingRating, { onChange(room.copy(furnishingRating = it)) })
                    NullableRatingRow("收纳空间", room.storageRating, { onChange(room.copy(storageRating = it)) })
                    NullableRatingRow("网络与手机信号", room.networkRating, { onChange(room.copy(networkRating = it)) })
                    if ((room.floor ?: 0) >= 4) {
                        NullableBooleanRow("高楼层是否有电梯", room.hasElevator, { onChange(room.copy(hasElevator = it)) })
                    } else {
                        NullableBooleanRow("是否有电梯", room.hasElevator, { onChange(room.copy(hasElevator = it)) })
                    }
                    NullableBooleanRow("是否允许养宠物", room.petAllowed, { onChange(room.copy(petAllowed = it)) }, "允许", "不允许")
                }
            }

            item {
                SectionCard(title = "安全、交通与周边") {
                    NullableRatingRow("门锁、消防与周边安全", room.securityRating, { onChange(room.copy(securityRating = it)) })
                    NullableRatingRow("交通便利度", room.transitRating, { onChange(room.copy(transitRating = it)) })
                    NullableIntField(
                        label = "到公司 / 学校通勤时间",
                        value = room.commuteMinutes,
                        onChange = { onChange(room.copy(commuteMinutes = it)) },
                        suffix = "分钟",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    NullableRatingRow("周边环境与生活配套", room.neighborhoodRating, { onChange(room.copy(neighborhoodRating = it)) }, "便利店、超市、医院、餐饮、垃圾点和夜间环境")
                }
            }

            item {
                SectionCard(title = "合同与房源风险", subtitle = "确认产权/转租授权、费用明细、押金退还、维修责任和违约条款。") {
                    NullableRatingRow("合同和房源可信度", room.leaseRiskRating, { onChange(room.copy(leaseRiskRating = it)) }, "1 高风险 / 信息矛盾 · 5 清晰可信")
                }
            }

            item {
                SectionCard(title = "自由备注") {
                    OutlinedTextField(
                        value = room.notes,
                        onValueChange = { onChange(room.copy(notes = it)) },
                        label = { Text("气味、邻居、房东承诺、待确认问题等") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 5
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("初次记录：${formatTimestamp(room.createdAt)}", style = MaterialTheme.typography.bodySmall)
                    Text("最后编辑：${formatTimestamp(room.updatedAt)}", style = MaterialTheme.typography.bodySmall)
                }
            }

            item { Spacer(Modifier.height(96.dp)) }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String = Instant.ofEpochMilli(timestamp)
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

private fun formatMoney(value: Double): String = if (value % 1.0 == 0.0) {
    value.toLong().toString()
} else {
    "%.2f".format(value)
}
