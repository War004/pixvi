package com.example.pixvi.network.response.Home.HomePage

/**
 * Response that we expect while loading the home page under the illustrations filter
 *  url -> https://app-api.pixiv.net/v1/home/all
 *  POST response
 */
data class HomePage(
    val contents: List<Content>,
    val next_params: NextParams,
    val states: List<Any?>
)

data class Content(
    val access: Access?,
    val kind: String,
    val pickup: Pickup?,
    val thumbnails: List<Thumbnail>?,
    val trend_tags: List<TrendTag>?
)

data class NextParams(
    val content_index_prev: Int,
    val page: Int
)

data class Access(
    val aui: Int,
    val c: Int?,
    val cid: Int,
    val i: Int,
    val key: String,
    val rms: Rms?,
    val t: Int,
    val tgs: List<String>,
    val tt: String?,
    val typ: String
)

data class Pickup(
    val comment: String,
    val comment_count: Int,
    val comment_id: Int,
    val profile_image_url: String,
    val type: String,
    val user_id: Int,
    val user_name: String
)

data class Thumbnail(
    val ai_type: Int,
    val app_model: AppModel,
    val book_style: Int?,
    val bookmark_count: Int?,
    val bookmarkable: Boolean,
    val comment_off_setting: Int,
    val create_date: String,
    val custom_thumbnail: CustomThumbnail?,
    val description: String?,
    val episode_count: Int?,
    val id: Int,
    val is_original: Boolean?,
    val page_count: Int?,
    val pages: List<Page>?,
    val profile_image_url: String,
    val reading_time: Int?,
    val series_id: Int?,
    val series_title: String?,
    val show_tags: Boolean,
    val tags: List<TagX>,
    val text: String?,
    val text_count: Int?,
    val title: String,
    val type: String,
    val update_date: String,
    val url: String?,
    val use_word_count: Boolean?,
    val user_id: Int,
    val user_name: String,
    val word_count: Int?
)

data class CustomThumbnail(
    val crop_x: Double,
    val crop_y: Double
)

data class ImageUrls(
    val large: String,
    val medium: String,
    val square_medium: String
)

data class ImageUrlsX(
    val large: String,
    val medium: String,
    val original: String,
    val square_medium: String
)

data class MetaPage(
    val image_urls: ImageUrlsX
)

data class MetaSinglePage(
    val original_image_url: String?
)

data class Page(
    val height: Int,
    val urls: Urls,
    val width: Int
)

data class ProfileImageUrls(
    val medium: String
)

data class Rms(
    val mtd: List<String>,
    val pos: Int,
    val rli: String?,
    val scr: Double,
    val sii: List<String>,
    val sni: List<Int>
)

data class Series(
    val id: Int?,
    val title: String?
)

data class Tag(
    val added_by_uploaded_user: Boolean?,
    val name: String,
    val translated_name: String?
)

data class TagX(
    val is_emphasized: Boolean,
    val name: String,
    val translated_name: String?
)

data class TrendTag(
    val name: String,
    val tagged_count: Int,
    val translated_name: String?,
    val url: String
)

data class Urls(
    val `1200x1200_standard`: String,
    val `360x360`: String,
    val `540x540`: String
)

data class User(
    val account: String,
    val id: Int,
    val is_accept_request: Boolean,
    val is_followed: Boolean,
    val name: String,
    val profile_image_urls: ProfileImageUrls
)