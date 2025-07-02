package com.example.pixvi.network.response.Home.Novels

data class RelatedNovel(
    val next_url: String,
    val novels: List<NovelRelated>
)

data class NovelRelated(
    val algorithm: String,
    val caption: String,
    val create_date: String,
    val id: Int,
    val image_urls: ImageUrls,
    val is_bookmarked: Boolean,
    val is_muted: Boolean,
    val is_mypixiv_only: Boolean,
    val is_original: Boolean,
    val is_x_restricted: Boolean,
    val novel_ai_type: Int,
    val page_count: Int,
    val request: Any?,
    val restrict: Int,
    val series: Series?,
    val tags: List<Tag>,
    val text_length: Int,
    val title: String,
    val total_bookmarks: Int,
    val total_comments: Int,
    val total_view: Int,
    val user: User,
    val visible: Boolean,
    val x_restrict: Int
)