package com.example.pixvi.data.local.NovelHistory

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit


enum class InteractionType {
    VIEW,
    READ
}

@Entity(
    tableName = "novel_history",
    primaryKeys = ["novel_id", "interaction_type"]
)
data class NovelHistoryEntry(
    @ColumnInfo(name = "novel_id")
    val novelId: Int,

    @ColumnInfo(name = "interaction_type")
    var interactionType: InteractionType,

    @ColumnInfo(name = "timestamp")
    val timestamp: String = formattedNow()
) {
    companion object {
        private fun formattedNow(): String {
            return ZonedDateTime.now()
                .truncatedTo(ChronoUnit.MILLIS)//pixiv takes timestamps in milliseconds not microseconds
                .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        }
    }
}