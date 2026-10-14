package com.cryptic.piyek.history.novel.data.local.mapper

import com.cryptic.piyek.history.novel.data.local.db.NovelBrowsingEntity
import com.cryptic.piyek.history.novel.data.local.db.NovelFinishedEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.ZoneOffset

class NovelHistoryMapperTest {

    @Test
    fun `toIso8601String formats epoch millis with UTC offset correctly`() {
        val epochMillis = 1700000000000L // 2023-11-14T22:13:20Z
        val formatted = epochMillis.toIso8601String(zoneId = ZoneOffset.UTC)

        assertEquals("2023-11-14T22:13:20Z", formatted)
    }

    @Test
    fun `toIso8601String formats epoch millis with custom timezone offset correctly`() {
        val epochMillis = 1700000000000L
        val tokyoZone = ZoneId.of("Asia/Tokyo")
        val formatted = epochMillis.toIso8601String(zoneId = tokyoZone)

        // UTC 22:13:20 + 9 hours = next day 07:13:20
        assertTrue(formatted.startsWith("2023-11-15T07:13:20+09:00"))
    }

    @Test
    fun `NovelBrowsingEntity toDomain maps correctly`() {
        val entity = NovelBrowsingEntity(novelId = 12345L, dateTime = 1700000000000L)
        val domain = entity.toDomain()

        assertEquals(12345L, domain.novelId)
        assertEquals(1700000000000L, domain.dateTime)
    }

    @Test
    fun `NovelFinishedEntity toDomain maps correctly`() {
        val entity = NovelFinishedEntity(novelId = 67890L, dateTime = 1700000000000L)
        val domain = entity.toDomain()

        assertEquals(67890L, domain.novelId)
        assertEquals(1700000000000L, domain.dateTime)
    }

    @Test
    fun `NovelBrowsingEntity toRequestBody maps correctly with ISO-8601 string`() {
        val entity = NovelBrowsingEntity(novelId = 12345L, dateTime = 1700000000000L)
        val requestBody = entity.toRequestBody()

        assertEquals(12345L, requestBody.novelId)
        assertTrue(requestBody.dateTime.contains("T"))
    }

    @Test
    fun `NovelFinishedEntity toRequestBody maps correctly with ISO-8601 string`() {
        val entity = NovelFinishedEntity(novelId = 67890L, dateTime = 1700000000000L)
        val requestBody = entity.toRequestBody()

        assertEquals(67890L, requestBody.novelId)
        assertTrue(requestBody.dateTime.contains("T"))
    }
}
