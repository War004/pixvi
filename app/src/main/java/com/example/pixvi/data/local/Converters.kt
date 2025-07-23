package com.example.pixvi.data.local

import androidx.room.TypeConverter
import com.example.pixvi.data.local.NovelHistory.InteractionType

class Converters {
    @TypeConverter
    fun fromInteractionType(value: InteractionType): String {
        return value.name // Converts READ to "READ"
    }

    @TypeConverter
    fun toInteractionType(value: String): InteractionType {
        return InteractionType.valueOf(value) // Converts "READ" to READ
    }
}