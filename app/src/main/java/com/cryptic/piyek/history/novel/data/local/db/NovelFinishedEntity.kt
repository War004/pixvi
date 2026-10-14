package com.cryptic.piyek.history.novel.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity("novel_finished_table")
/*
    datetime is of the format ISO-8601
    Ex: "2026-10-15T00:03:00+05:30"
 */
data class NovelFinishedEntity(
    @PrimaryKey
    @ColumnInfo("novel_id") val novelId: Long,
    @ColumnInfo("datetime") val dateTime: Long
)
