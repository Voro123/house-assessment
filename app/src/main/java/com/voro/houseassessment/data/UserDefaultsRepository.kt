package com.voro.houseassessment.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class ContactPreset(
    val name: String = "",
    val phone: String = "",
    val channel: String = "",
    val notes: String = ""
) {
    val label: String
        get() = when {
            name.isNotBlank() && phone.isNotBlank() -> "$name · $phone"
            name.isNotBlank() -> name
            phone.isNotBlank() -> phone
            else -> channel
        }
}

class UserDefaultsRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun createRoomWithDefaults(now: Long = System.currentTimeMillis()): RoomRecord = RoomRecord(
        targetBudget = preferences.getString(KEY_TARGET_BUDGET, null)?.toDoubleOrNull(),
        createdAt = now,
        updatedAt = now
    )

    fun capture(room: RoomRecord) {
        val editor = preferences.edit()
        room.targetBudget?.let { editor.putString(KEY_TARGET_BUDGET, it.toString()) }

        val preset = ContactPreset(
            name = room.contactName.trim(),
            phone = room.contactPhone.trim(),
            channel = room.contactChannel.trim(),
            notes = room.contactNotes.trim()
        )
        if (preset.name.isNotBlank() || preset.phone.isNotBlank() || preset.channel.isNotBlank()) {
            val updated = listOf(preset) + getRecentContacts().filterNot { it.identityKey() == preset.identityKey() }
            editor.putString(KEY_RECENT_CONTACTS, encodeContacts(updated.take(MAX_RECENT_CONTACTS)))
        }
        editor.apply()
    }

    fun getRecentContacts(): List<ContactPreset> {
        val raw = preferences.getString(KEY_RECENT_CONTACTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val preset = ContactPreset(
                        name = item.optString("name"),
                        phone = item.optString("phone"),
                        channel = item.optString("channel"),
                        notes = item.optString("notes")
                    )
                    if (preset.name.isNotBlank() || preset.phone.isNotBlank() || preset.channel.isNotBlank()) {
                        add(preset)
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun encodeContacts(contacts: List<ContactPreset>): String {
        val array = JSONArray()
        contacts.forEach { preset ->
            array.put(
                JSONObject()
                    .put("name", preset.name)
                    .put("phone", preset.phone)
                    .put("channel", preset.channel)
                    .put("notes", preset.notes)
            )
        }
        return array.toString()
    }

    private fun ContactPreset.identityKey(): String = listOf(name, phone, channel)
        .joinToString("|") { it.trim().lowercase() }

    private companion object {
        const val PREFERENCES_NAME = "house_assessment_user_defaults"
        const val KEY_TARGET_BUDGET = "target_budget"
        const val KEY_RECENT_CONTACTS = "recent_contacts"
        const val MAX_RECENT_CONTACTS = 8
    }
}
