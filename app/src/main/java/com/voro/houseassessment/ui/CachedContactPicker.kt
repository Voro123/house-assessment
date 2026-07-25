package com.voro.houseassessment.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.voro.houseassessment.data.ContactPreset
import com.voro.houseassessment.data.UserDefaultsRepository

@Composable
fun CachedContactPicker(onApply: (ContactPreset) -> Unit) {
    val context = LocalContext.current
    val contacts = remember {
        UserDefaultsRepository(context.applicationContext).getRecentContacts()
    }

    if (contacts.isEmpty()) {
        Text(
            "保存带联系人信息的房源后，联系人会自动缓存到这里。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        return
    }

    Text(
        "最近联系人 · 点击整组填入",
        style = MaterialTheme.typography.labelLarge
    )
    Spacer(Modifier.height(6.dp))
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(contacts, key = { "${it.name}|${it.phone}|${it.channel}" }) { preset ->
            AssistChip(
                onClick = { onApply(preset) },
                label = { Text(preset.label) }
            )
        }
    }
    Text(
        "联系人按最近使用排序，最多保留 8 组；再次保存会自动去重更新。",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(10.dp))
}
