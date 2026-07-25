package com.voro.houseassessment.data

import android.content.Context

class RoomRepository(context: Context) {
    private val database = RoomDatabase(context)

    fun getAll(): List<RoomRecord> = database.getAll()
    fun getById(id: Long): RoomRecord? = database.getById(id)
    fun save(room: RoomRecord): Long = database.save(room)
    fun delete(id: Long) = database.delete(id)
}
