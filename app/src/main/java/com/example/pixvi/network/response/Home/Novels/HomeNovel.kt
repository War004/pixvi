package com.example.pixvi.network.response.Home.Novels

import androidx.compose.ui.text.AnnotatedString

/**
 * Response that we expect while loading the home page under the novels filter
 *  url -> https://app-api.pixiv.net/v1/novel/recommended
 *  post response -> include_ranking_novels=true&include_privacy_policy=true
 */

data class HomeNovel(
    val next_url: String,
    val novels: List<Novel>,
    val privacy_policy: PrivacyPolicy,
    val ranking_novels: List<RankingNovel>
)

data class ImageUrls(
    val large: String,
    val medium: String,
    val square_medium: String
)

data class Novel(
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
    val request: Any,
    val restrict: Int,
    val series: Series,
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

data class PrivacyPolicy(
    val message: String,
    val version: String
)

data class ProfileImageUrls(
    val medium: String
)

data class RankingNovel(
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
    val request: Any,
    val restrict: Int,
    val series: Series,
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

data class Series(
    val id: Int,
    val title: String
)

data class Tag(
    val added_by_uploaded_user: Boolean,
    val name: String,
    val translated_name: String
)

data class User(
    val account: String,
    val id: Int,
    val is_accept_request: Boolean,
    val is_followed: Boolean,
    val name: String,
    val profile_image_urls: ProfileImageUrls
)


//Custom  for ui state
data class NovelForDisplay(
    val id: Int,
    val title: String,
    val user: User,
    val image_urls: ImageUrls,
    val caption: String, // Keep the original for features like "copy raw text"
    val page_count: Int,
    val text_length: Int,
    val total_view: Int,
    val total_bookmarks: Int,
    val total_comments: Int,
    val is_bookmarked: Boolean,
    val tags: List<Tag>,
    // The new, pre-processed field that makes the UI fast.
    val parsedCaption: AnnotatedString
)