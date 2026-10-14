package com.cryptic.piyek.history.novel.domain.model

data class NovelBrowserRequestBody(
    val novelId: Long,
    val dateTime: String
)

typealias NovelBrowsingRequestBody = NovelBrowserRequestBody
