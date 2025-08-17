package com.example.pixvi.viewModels

//F means Flase

import android.util.Log
import androidx.compose.runtime.Stable
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import com.example.pixvi.network.response.Home.Illust.Illust as IllustModel
import com.example.pixvi.network.response.Home.Manga.Illust as MangaModel
import com.example.pixvi.network.response.Home.Manga.RankingIllust as MangaRankingModel

/**
 * Defines the user-selectable image quality options.
 * This enum is the single source of truth for quality settings.
 */
enum class ImageQuality {
    AUTO,
    LARGE,
    MEDIUM
}


/**
 * A dedicated state holder class is cleaner and safer than ad-hoc tuples.
 * It holds all the dynamic data and actions the UI needs, derived from the ViewModels.
 * The @Stable annotation is a hint to the Compose compiler that this class is stable,
 * which can help with performance optimizations.
 */
@Stable
data class DetailScreenState(
    val items: List<DisplayableItem> = emptyList(),
    val initialIndex: Int = 0,
    val initialSubPageIndex: Int = 0,
    val isLoadingMore: Boolean = false,
    val nextUrl: String? = null,
)

/**
 * Enum to define the content type in a type-safe way for navigation.
 */
enum class ContentType {
    ILLUST,
    MANGA
}

/**
 * A unified data class that the UI will use. This is the "Adapter" pattern.
 * It contains all the fields needed for display, abstracting the source models.
 */
@Stable
data class DisplayableItem(
    val id: Long,
    val title: String,
    val userName: String,
    val createDate: String,
    val isBookmarked: Boolean,
    val totalBookmarks: Int,
    val pageCount: Int,
    val width: Int,
    val height: Int,
    val mediumImageUrls: List<String>,
    val largeImageUrls: List<String>,
    val originalImageUrls: List<String>
)

// --- Mapper Functions ---

fun IllustModel.toDisplayableItem(): DisplayableItem {
    val mediumUrls = if (this.page_count > 1) this.meta_pages.map { it.image_urls.medium } else listOf(this.image_urls.medium)
    val largeUrls = if (this.page_count > 1) this.meta_pages.map { it.image_urls.large } else listOf(this.image_urls.large)
    val originalUrls = if (this.page_count > 1) this.meta_pages.map { it.image_urls.original } else listOfNotNull(this.meta_single_page.original_image_url)

    return DisplayableItem(
        id = this.id.toLong(),
        title = this.title,
        userName = this.user.name,
        createDate = this.create_date,
        isBookmarked = this.is_bookmarked,
        totalBookmarks = this.total_bookmarks,
        pageCount = this.page_count,
        width = this.width,
        height = this.height,
        mediumImageUrls = mediumUrls,
        largeImageUrls = largeUrls,
        originalImageUrls = originalUrls
    )
}

fun MangaModel.toDisplayableItem(): DisplayableItem {
    val mediumUrls = if (this.page_count > 1) this.meta_pages.map { it.image_urls.medium } else listOf(this.image_urls.medium)
    val largeUrls = if (this.page_count > 1) this.meta_pages.map { it.image_urls.large } else listOf(this.image_urls.large)
    val originalUrls = if (this.page_count > 1) this.meta_pages.map { it.image_urls.original } else listOf(this.meta_single_page.original_image_url)

    return DisplayableItem(
        id = this.id.toLong(),
        title = this.title,
        userName = this.user.name,
        createDate = this.create_date,
        isBookmarked = this.is_bookmarked,
        totalBookmarks = this.total_bookmarks,
        pageCount = this.page_count,
        width = this.width,
        height = this.height,
        mediumImageUrls = mediumUrls,
        largeImageUrls = largeUrls,
        originalImageUrls = originalUrls
    )
}

fun MangaRankingModel.toDisplayableItem(): DisplayableItem {
    val mediumUrls = if (this.page_count > 1) this.meta_pages.map { it.image_urls.medium } else listOf(this.image_urls.medium)
    val largeUrls = if (this.page_count > 1) this.meta_pages.map { it.image_urls.large } else listOf(this.image_urls.large)
    val originalUrls = if (this.page_count > 1) this.meta_pages.map { it.image_urls.original } else listOf(this.meta_single_page.original_image_url)

    return DisplayableItem(
        id = this.id.toLong(),
        title = this.title,
        userName = this.user.name,
        createDate = this.create_date,
        isBookmarked = this.is_bookmarked,
        totalBookmarks = this.total_bookmarks,
        pageCount = this.page_count,
        width = this.width,
        height = this.height,
        mediumImageUrls = mediumUrls,
        largeImageUrls = largeUrls,
        originalImageUrls = originalUrls
    )
}

/**
 * Point 4: Defensive date formatting.
 * This helper function safely parses the date string and returns a formatted
 * string or a placeholder "--" on failure, preventing crashes.
 */
fun formatDateStringSafe(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "--"
    return try {
        ZonedDateTime.parse(dateString)
            .format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
    } catch (e: DateTimeParseException) {
        Log.e("DateParse", "Failed to parse date: $dateString", e)
        "--"
    }
}