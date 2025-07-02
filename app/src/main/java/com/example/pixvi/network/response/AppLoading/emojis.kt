package com.example.pixvi.network.response.AppLoading

/**
 * Response that we expect while loading the emojis
 *  url ->
 */
data class EmojiDefinition(
    val id: Int,
    val image_url_medium: String,
    val slug: String
)

data class emojis(
    val emoji_definitions: List<EmojiDefinition>
)