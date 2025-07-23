package com.example.pixvi.data.local.NovelHistory

// A helper data class to make the return type clean
data class ApiHistoryPayload(
    val readNovelIds: List<String>,
    val readNovelDatetimes: List<String>,
    val viewNovelIds: List<String>,
    val viewNovelDatetimes: List<String>
)

class NovelHistoryRepository(private val novelHistoryDao: NovelHistoryDao) {

    // When a user views a novel
    suspend fun addViewEvent(novelId: Int) {
        val entry = NovelHistoryEntry(novelId = novelId, interactionType = InteractionType.VIEW)
        novelHistoryDao.upsert(entry)
    }

    // When a user finishes reading a novel
    suspend fun markNovelAsRead(novelId: Int) {
        novelHistoryDao.markAsReadAndDeleteView(novelId)
    }

    /**
     * Prepares the history data in the exact format required by the getRelatedNovels API call.
     */
    suspend fun prepareHistoryForApi(): ApiHistoryPayload {
        // Use the snapshot DAO method to get the current data without observing it
        val readList = novelHistoryDao.getHistoryByTypeSnapshot(InteractionType.READ)
        val viewList = novelHistoryDao.getHistoryByTypeSnapshot(InteractionType.VIEW)

        val readIds = readList.map { it.novelId.toString() }
        val readDates = readList.map { it.timestamp }

        val viewIds = viewList.map { it.novelId.toString() }
        val viewDates = viewList.map { it.timestamp }

        return ApiHistoryPayload(
            readNovelIds = readIds,
            readNovelDatetimes = readDates,
            viewNovelIds = viewIds,
            viewNovelDatetimes = viewDates
        )
    }
}