package com.cryptic.piyek.history.novel.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "novel_browsing_table")
data class NovelBrowsingEntity(
    @PrimaryKey
    @ColumnInfo("novel_id") val novelId: Long,
    @ColumnInfo("datetime") val dateTime: Long
)
