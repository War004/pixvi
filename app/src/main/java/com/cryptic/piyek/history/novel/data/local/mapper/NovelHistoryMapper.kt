package com.cryptic.piyek.history.novel.data.local.mapper

import com.cryptic.piyek.history.novel.data.local.db.NovelBrowsingEntity
import com.cryptic.piyek.history.novel.data.local.db.NovelFinishedEntity
import com.cryptic.piyek.history.novel.domain.model.NovelBrowserRequestBody
import com.cryptic.piyek.history.novel.domain.model.NovelBrowsing
import com.cryptic.piyek.history.novel.domain.model.NovelFinished
import com.cryptic.piyek.history.novel.domain.model.NovelFinishedRequestBody
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Formats epoch milliseconds to an ISO-8601 offset datetime string
 * (e.g., "2026-10-15T00:03:00+05:30") expected by the Pixiv API.
 */
fun Long.toIso8601String(zoneId: ZoneId = ZoneId.systemDefault()): String {
    return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
        Instant.ofEpochMilli(this).atZone(zoneId)
    )
}

fun NovelBrowsingEntity.toDomain(): NovelBrowsing =
    NovelBrowsing(
        novelId = novelId,
        dateTime = dateTime
    )

fun NovelFinishedEntity.toDomain(): NovelFinished =
    NovelFinished(
        novelId = novelId,
        dateTime = dateTime
    )

fun NovelBrowsingEntity.toRequestBody(): NovelBrowserRequestBody =
    NovelBrowserRequestBody(
        novelId = novelId,
        dateTime = dateTime.toIso8601String()
    )

fun NovelFinishedEntity.toRequestBody(): NovelFinishedRequestBody =
    NovelFinishedRequestBody(
        novelId = novelId,
        dateTime = dateTime.toIso8601String()
    )
