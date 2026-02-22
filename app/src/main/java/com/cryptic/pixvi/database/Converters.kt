package com.cryptic.pixvi.database

import androidx.room.TypeConverter
import com.cryptic.pixvi.database.notification.NotifType

class Converters {
    @TypeConverter
    fun toNotifType(value: Int): NotifType {
        return enumValues<NotifType>().firstOrNull { it.id == value } ?: NotifType.SYSTEM
    }

    @TypeConverter
    fun fromNotifType(type: NotifType): Int {
        return type.id
    }
}