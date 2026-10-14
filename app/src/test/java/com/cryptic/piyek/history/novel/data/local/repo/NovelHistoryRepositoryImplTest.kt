package com.cryptic.piyek.history.novel.data.local.repo

import android.database.sqlite.SQLiteFullException
import com.cryptic.piyek.core.domain.error.Result
import com.cryptic.piyek.history.novel.data.local.db.NovelBrowsingEntity
import com.cryptic.piyek.history.novel.data.local.db.NovelFinishedEntity
import com.cryptic.piyek.history.novel.data.local.db.NovelHistoryDao
import com.cryptic.piyek.history.novel.domain.model.NovelHistoryError
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NovelHistoryRepositoryImplTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private val dao = mockk<NovelHistoryDao>(relaxed = true)

    private lateinit var repository: NovelHistoryRepositoryImpl

    @BeforeEach
    fun setUp() {
        repository = NovelHistoryRepositoryImpl(
            dao = dao,
            ioDispatcher = testDispatcher
        )
    }

    // ========================================================================
    // Request Body (Limit 50 + ISO-8601) Tests
    // ========================================================================

    @Test
    fun `getLatestBrowsingRequestBody queries dao with limit 50 and maps to request body`() = runTest(testDispatcher) {
        val entities = listOf(
            NovelBrowsingEntity(novelId = 1L, dateTime = 1700000000000L),
            NovelBrowsingEntity(novelId = 2L, dateTime = 1700000010000L)
        )
        coEvery { dao.getLatestBrowsing(50) } returns entities

        val result = repository.getLatestBrowsingRequestBody()

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(2, data.size)
        assertEquals(1L, data[0].novelId)
        assertTrue(data[0].dateTime.contains("T"))
        assertEquals(2L, data[1].novelId)
        assertTrue(data[1].dateTime.contains("T"))
        coVerify(exactly = 1) { dao.getLatestBrowsing(50) }
    }

    @Test
    fun `getLatestFinishedRequestBody queries dao with limit 50 and maps to request body`() = runTest(testDispatcher) {
        val entities = listOf(
            NovelFinishedEntity(novelId = 10L, dateTime = 1700000000000L)
        )
        coEvery { dao.getLatestFinished(50) } returns entities

        val result = repository.getLatestFinishedRequestBody()

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.size)
        assertEquals(10L, data[0].novelId)
        assertTrue(data[0].dateTime.contains("T"))
        coVerify(exactly = 1) { dao.getLatestFinished(50) }
    }

    // ========================================================================
    // Domain Models Tests
    // ========================================================================

    @Test
    fun `getBrowsingHistory queries dao with custom limit and maps to domain`() = runTest(testDispatcher) {
        val entities = listOf(
            NovelBrowsingEntity(novelId = 100L, dateTime = 1700000000000L)
        )
        coEvery { dao.getLatestBrowsing(20) } returns entities

        val result = repository.getBrowsingHistory(limit = 20)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.size)
        assertEquals(100L, data[0].novelId)
        assertEquals(1700000000000L, data[0].dateTime)
        coVerify(exactly = 1) { dao.getLatestBrowsing(20) }
    }

    @Test
    fun `getFinishedHistory queries dao with custom limit and maps to domain`() = runTest(testDispatcher) {
        val entities = listOf(
            NovelFinishedEntity(novelId = 200L, dateTime = 1700000000000L)
        )
        coEvery { dao.getLatestFinished(30) } returns entities

        val result = repository.getFinishedHistory(limit = 30)

        assertTrue(result is Result.Success)
        val data = (result as Result.Success).data
        assertEquals(1, data.size)
        assertEquals(200L, data[0].novelId)
        assertEquals(1700000000000L, data[0].dateTime)
        coVerify(exactly = 1) { dao.getLatestFinished(30) }
    }

    // ========================================================================
    // Record & Clear Tests
    // ========================================================================

    @Test
    fun `recordBrowsing calls dao recordBrowsing with parameters`() = runTest(testDispatcher) {
        val novelId = 999L
        val timestamp = 1700000050000L
        val maxLimit = 50

        val result = repository.recordBrowsing(novelId, timestamp, maxLimit)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { dao.recordBrowsing(novelId, timestamp, maxLimit) }
    }

    @Test
    fun `recordFinished calls dao recordFinished with parameters`() = runTest(testDispatcher) {
        val novelId = 888L
        val timestamp = 1700000060000L
        val maxLimit = 50

        val result = repository.recordFinished(novelId, timestamp, maxLimit)

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { dao.recordFinished(novelId, timestamp, maxLimit) }
    }

    @Test
    fun `clearBrowsingHistory calls dao clearBrowsing`() = runTest(testDispatcher) {
        val result = repository.clearBrowsingHistory()

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { dao.clearBrowsing() }
    }

    @Test
    fun `clearFinishedHistory calls dao clearFinished`() = runTest(testDispatcher) {
        val result = repository.clearFinishedHistory()

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { dao.clearFinished() }
    }

    @Test
    fun `clearAllHistory calls dao clearAll`() = runTest(testDispatcher) {
        val result = repository.clearAllHistory()

        assertTrue(result is Result.Success)
        coVerify(exactly = 1) { dao.clearAll() }
    }

    // ========================================================================
    // Error Handling Tests
    // ========================================================================

    @Test
    fun `when dao throws SQLiteFullException, returns DISK_FULL error`() = runTest(testDispatcher) {
        coEvery { dao.getLatestBrowsing(any()) } throws SQLiteFullException("Disk is full")

        val result = repository.getBrowsingHistory(50)

        assertTrue(result is Result.Error)
        assertEquals(NovelHistoryError.DISK_FULL, (result as Result.Error).error)
    }

    @Test
    fun `when dao throws generic Exception, returns DATABASE_ERROR error`() = runTest(testDispatcher) {
        coEvery { dao.getLatestFinished(any()) } throws RuntimeException("Database error")

        val result = repository.getFinishedHistory(50)

        assertTrue(result is Result.Error)
        assertEquals(NovelHistoryError.DATABASE_ERROR, (result as Result.Error).error)
    }

    @Test
    fun `when recordBrowsing fails, returns DATABASE_ERROR`() = runTest(testDispatcher) {
        coEvery { dao.recordBrowsing(any(), any(), any()) } throws RuntimeException("Constraint error")

        val result = repository.recordBrowsing(1L, 100L, 50)

        assertTrue(result is Result.Error)
        assertEquals(NovelHistoryError.DATABASE_ERROR, (result as Result.Error).error)
    }
}
