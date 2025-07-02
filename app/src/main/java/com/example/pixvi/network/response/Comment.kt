package com.example.pixvi.network.response

/**
 * Response that we expect while loading the comments for a particular post
 *  urls
 *  Illust, Manga  -> https://app-api.pixiv.net/v3/illust/comments?illust_id=129870084, https://app-api.pixiv.net/v3/illust/comments?illust_id=129870084&offset=30 #with offset
 * Novels -> https://app-api.pixiv.net/v3/novel/comments?novel_id=21948696
 *  get response
 */
data class Comment(
    val comment: String,
    val date: String,
    val has_replies: Boolean,
    val id: Int,
    val stamp: Stamp?,
    val user: User
)

data class Comments(
    val comment_access_control: Int,
    val comments: List<Comment>,
    val next_url: String
)

data class ProfileImageUrls(
    val medium: String
)

data class Stamp(
    val stamp_id: Int,
    val stamp_url: String
)

data class User(
    val account: String,
    val id: Int,
    val is_accept_request: Boolean,
    val name: String,
    val profile_image_urls: ProfileImageUrls
)