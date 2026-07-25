package com.voro.houseassessment.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class CachedContact(
    val name: String = "",
    val phone: String = "",
    val channel: String = "",
    val notes: String = ""
) {
    val displayName: String
        get() = name.ifBlank { phone.ifBlank { channel.ifBlank { "未命名联系人" } } }

    fun isMeaningful(): Boolean = name.isNotBlank() || phone.isNotBlank() || channel.isNotBlank()

    internal fun identityKey(): String {
        val normalizedPhone = phone.filter(Char::isDigit)
        return if (normalizedPhone.isNotBlank()) {
            "phone:$normalizedPhone"
        } else {
            "name:${name.trim().lowercase()}|channel:${channel.trim().lowercase()}"
        }
    }
}

data class UserDefaults(
    val targetBudget: Double? = null,
    val recentContacts: List<CachedContact> = emptyList()
)

class UserPreferencesRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun getDefaults(): UserDefaults = UserDefaults(
        targetBudget = preferences.getString(KEY_TARGET_BUDGET, null)?.toDoubleOrNull(),
        recentContacts = readContacts()
    )

    fun remember(room: RoomRecord) {
        val current = getDefaults()
        val candidate = CachedContact(
            name = room.contactName.trim(),
            phone = room.contactPhone.trim(),
            channel = room.contactChannel.trim(),
            notes = room.contactNotes.trim()
        )

        val updatedContacts = if (candidate.isMeaningful()) {
            listOf(candidate) + current.recentContacts
                .filterNot { it.identityKey() == candidate.identityKey() }
                .take(MAX_RECENT_CONTACTS - 1)
        } else {
            current.recentContacts
        }

        preferences.edit()
            .putString(KEY_TARGET_BUDGET, (room.targetBudget ?: current.targetBudget)?.toString())
            .putString(KEY_RECENT_CONTACTS, contactsToJson(updatedContacts).toString())
            .apply()
    }

    fun applyContact(room: RoomRecord, contact: CachedContact): RoomRecord = room.copy(
        contactName = contact.name,
        contactPhone = contact.phone,
        contactChannel = contact.channel,
        contactNotes = contact.notes
    )

    private fun readContacts(): List<CachedContact> {
        val raw = preferences.getString(KEY_RECENT_CONTACTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    val contact = CachedContact(
                        name = item.optString("name"),
                        phone = item.optString("phone"),
                        channel = item.optString("channel"),
                        notes = item.optString("notes")
                    )
                    if (contact.isMeaningful()) add(contact)
                }
            }.take(MAX_RECENT_CONTACTS)
        }.getOrDefault(emptyList())
    }

    private fun contactsToJson(contacts: List<CachedContact>): JSONArray = JSONArray().apply {
        contacts.take(MAX_RECENT_CONTACTS).forEach { contact ->
            put(
                JSONObject()
                    .put("name", contact.name)
                    .put("phone", contact.phone)
                    .put("channel", contact.channel)
                    .put("notes", contact.notes)
            )
        }
    }

    private companion object {
        const val PREFERENCES_NAME = "house_assessment_user_defaults"
        const val KEY_TARGET_BUDGET = "target_budget"
        const val KEY_RECENT_CONTACTS = "recent_contacts"
        const val MAX_RECENT_CONTACTS = 8
    }
}
