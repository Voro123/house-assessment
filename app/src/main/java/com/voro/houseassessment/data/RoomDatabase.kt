package com.voro.houseassessment.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONArray

class RoomDatabase(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE rooms (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                rent_monthly REAL,
                target_budget REAL,
                deposit REAL,
                extra_fees REAL,
                area_sqm REAL,
                floor INTEGER,
                total_floors INTEGER,
                address TEXT NOT NULL,
                latitude REAL,
                longitude REAL,
                contact_name TEXT NOT NULL,
                contact_phone TEXT NOT NULL,
                contact_channel TEXT NOT NULL,
                contact_notes TEXT NOT NULL,
                orientation TEXT,
                has_balcony INTEGER,
                water_quality INTEGER,
                ac_level INTEGER,
                has_washer INTEGER,
                space_rating INTEGER,
                outlet_rating INTEGER,
                noise_rating INTEGER,
                lighting_rating INTEGER,
                ventilation_rating INTEGER,
                cleanliness_rating INTEGER,
                damp_mold_rating INTEGER,
                bathroom_rating INTEGER,
                kitchen_rating INTEGER,
                security_rating INTEGER,
                transit_rating INTEGER,
                neighborhood_rating INTEGER,
                network_rating INTEGER,
                storage_rating INTEGER,
                furnishing_rating INTEGER,
                lease_risk_rating INTEGER,
                has_elevator INTEGER,
                pet_allowed INTEGER,
                commute_minutes INTEGER,
                photos_json TEXT NOT NULL,
                notes TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion != newVersion) {
            db.execSQL("DROP TABLE IF EXISTS rooms")
            onCreate(db)
        }
    }

    fun getAll(): List<RoomRecord> {
        readableDatabase.query(
            "rooms",
            null,
            null,
            null,
            null,
            null,
            "updated_at DESC"
        ).use { cursor ->
            val rooms = mutableListOf<RoomRecord>()
            while (cursor.moveToNext()) rooms += cursor.toRoomRecord()
            return rooms
        }
    }

    fun getById(id: Long): RoomRecord? {
        readableDatabase.query(
            "rooms",
            null,
            "id = ?",
            arrayOf(id.toString()),
            null,
            null,
            null,
            "1"
        ).use { cursor ->
            return if (cursor.moveToFirst()) cursor.toRoomRecord() else null
        }
    }

    fun save(room: RoomRecord): Long {
        val now = System.currentTimeMillis()
        val values = room.copy(updatedAt = now).toValues()
        return if (room.id == 0L) {
            writableDatabase.insertOrThrow("rooms", null, values)
        } else {
            writableDatabase.update("rooms", values, "id = ?", arrayOf(room.id.toString()))
            room.id
        }
    }

    fun delete(id: Long) {
        writableDatabase.delete("rooms", "id = ?", arrayOf(id.toString()))
    }

    private fun RoomRecord.toValues() = ContentValues().apply {
        put("title", title)
        putNullable("rent_monthly", rentMonthly)
        putNullable("target_budget", targetBudget)
        putNullable("deposit", deposit)
        putNullable("extra_fees", extraFees)
        putNullable("area_sqm", areaSqm)
        putNullable("floor", floor)
        putNullable("total_floors", totalFloors)
        put("address", address)
        putNullable("latitude", latitude)
        putNullable("longitude", longitude)
        put("contact_name", contactName)
        put("contact_phone", contactPhone)
        put("contact_channel", contactChannel)
        put("contact_notes", contactNotes)
        putNullable("orientation", orientation)
        putNullableBoolean("has_balcony", hasBalcony)
        putNullable("water_quality", waterQuality)
        putNullable("ac_level", acLevel)
        putNullableBoolean("has_washer", hasWasher)
        putNullable("space_rating", spaceRating)
        putNullable("outlet_rating", outletRating)
        putNullable("noise_rating", noiseRating)
        putNullable("lighting_rating", lightingRating)
        putNullable("ventilation_rating", ventilationRating)
        putNullable("cleanliness_rating", cleanlinessRating)
        putNullable("damp_mold_rating", dampMoldRating)
        putNullable("bathroom_rating", bathroomRating)
        putNullable("kitchen_rating", kitchenRating)
        putNullable("security_rating", securityRating)
        putNullable("transit_rating", transitRating)
        putNullable("neighborhood_rating", neighborhoodRating)
        putNullable("network_rating", networkRating)
        putNullable("storage_rating", storageRating)
        putNullable("furnishing_rating", furnishingRating)
        putNullable("lease_risk_rating", leaseRiskRating)
        putNullableBoolean("has_elevator", hasElevator)
        putNullableBoolean("pet_allowed", petAllowed)
        putNullable("commute_minutes", commuteMinutes)
        put("photos_json", JSONArray(photos).toString())
        put("notes", notes)
        put("created_at", createdAt)
        put("updated_at", updatedAt)
    }

    private fun Cursor.toRoomRecord(): RoomRecord {
        val photoArray = runCatching { JSONArray(string("photos_json")) }.getOrElse { JSONArray() }
        val photoList = buildList {
            for (index in 0 until photoArray.length()) add(photoArray.optString(index))
        }.filter { it.isNotBlank() }

        return RoomRecord(
            id = long("id"),
            title = string("title"),
            rentMonthly = doubleOrNull("rent_monthly"),
            targetBudget = doubleOrNull("target_budget"),
            deposit = doubleOrNull("deposit"),
            extraFees = doubleOrNull("extra_fees"),
            areaSqm = doubleOrNull("area_sqm"),
            floor = intOrNull("floor"),
            totalFloors = intOrNull("total_floors"),
            address = string("address"),
            latitude = doubleOrNull("latitude"),
            longitude = doubleOrNull("longitude"),
            contactName = string("contact_name"),
            contactPhone = string("contact_phone"),
            contactChannel = string("contact_channel"),
            contactNotes = string("contact_notes"),
            orientation = stringOrNull("orientation"),
            hasBalcony = booleanOrNull("has_balcony"),
            waterQuality = intOrNull("water_quality"),
            acLevel = intOrNull("ac_level"),
            hasWasher = booleanOrNull("has_washer"),
            spaceRating = intOrNull("space_rating"),
            outletRating = intOrNull("outlet_rating"),
            noiseRating = intOrNull("noise_rating"),
            lightingRating = intOrNull("lighting_rating"),
            ventilationRating = intOrNull("ventilation_rating"),
            cleanlinessRating = intOrNull("cleanliness_rating"),
            dampMoldRating = intOrNull("damp_mold_rating"),
            bathroomRating = intOrNull("bathroom_rating"),
            kitchenRating = intOrNull("kitchen_rating"),
            securityRating = intOrNull("security_rating"),
            transitRating = intOrNull("transit_rating"),
            neighborhoodRating = intOrNull("neighborhood_rating"),
            networkRating = intOrNull("network_rating"),
            storageRating = intOrNull("storage_rating"),
            furnishingRating = intOrNull("furnishing_rating"),
            leaseRiskRating = intOrNull("lease_risk_rating"),
            hasElevator = booleanOrNull("has_elevator"),
            petAllowed = booleanOrNull("pet_allowed"),
            commuteMinutes = intOrNull("commute_minutes"),
            photos = photoList,
            notes = string("notes"),
            createdAt = long("created_at"),
            updatedAt = long("updated_at")
        )
    }

    private fun ContentValues.putNullable(key: String, value: String?) {
        if (value == null) putNull(key) else put(key, value)
    }

    private fun ContentValues.putNullable(key: String, value: Double?) {
        if (value == null) putNull(key) else put(key, value)
    }

    private fun ContentValues.putNullable(key: String, value: Int?) {
        if (value == null) putNull(key) else put(key, value)
    }

    private fun ContentValues.putNullableBoolean(key: String, value: Boolean?) {
        if (value == null) putNull(key) else put(key, if (value) 1 else 0)
    }

    private fun Cursor.index(name: String) = getColumnIndexOrThrow(name)
    private fun Cursor.string(name: String) = getString(index(name)) ?: ""
    private fun Cursor.stringOrNull(name: String) = index(name).let { if (isNull(it)) null else getString(it) }
    private fun Cursor.long(name: String) = getLong(index(name))
    private fun Cursor.intOrNull(name: String) = index(name).let { if (isNull(it)) null else getInt(it) }
    private fun Cursor.doubleOrNull(name: String) = index(name).let { if (isNull(it)) null else getDouble(it) }
    private fun Cursor.booleanOrNull(name: String) = intOrNull(name)?.let { it == 1 }

    companion object {
        private const val DATABASE_NAME = "house_assessment.db"
        private const val DATABASE_VERSION = 1
    }
}
