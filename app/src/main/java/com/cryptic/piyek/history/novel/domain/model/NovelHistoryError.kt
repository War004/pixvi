package com.cryptic.piyek.history.novel.domain.model

import com.cryptic.piyek.core.domain.error.Error

enum class NovelHistoryError : Error {
    DISK_FULL,
    DATABASE_ERROR,
    UNKNOWN
}
