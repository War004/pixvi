package com.example.pixvi.viewModels

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.example.pixvi.network.api.PixivApiService
import com.example.pixvi.network.response.Detail.AuthorDetails
import com.example.pixvi.network.response.Detail.NovelData
import com.example.pixvi.services.NovelPlaybackService
import com.example.pixvi.utils.ContentBlock
import com.example.pixvi.utils.NovelParser
import com.example.pixvi.utils.ParsedNovelResult
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody

@UnstableApi
class NovelDetailViewModel(
    application: Application, // Now an AndroidViewModel to get the context
    private val pixivApiService: PixivApiService,
    private val novelId: Int
) : AndroidViewModel(application) {

    // Add new state properties for the player
    data class NovelDetailUiState(
        val isLoading: Boolean = true,
        val errorMessage: String? = null,
        val novelData: NovelData? = null,
        val authorDetails: AuthorDetails? = null,
        val contentBlocks: List<ContentBlock> = emptyList(),
        val settings: NovelReaderSettings = NovelReaderSettings(),
        // --- Player State ---
        val isPlayerReady: Boolean = false,
        val isPlaying: Boolean = false,
        val currentPlayingNovelId: Int? = null,
        // The unique ID of the specific paragraph being played
        val currentPlayingMediaId: Int? = null
    )

    private val _uiState = MutableStateFlow(NovelDetailUiState())
    val uiState: StateFlow<NovelDetailUiState> = _uiState.asStateFlow()

    // The MediaController is our gateway to the service
    private var mediaControllerFuture: ListenableFuture<MediaController>

    private var mediaController: MediaController? = null

    private val playerListener = object : Player.Listener {
        // The player is the source of truth for the playing state.
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _uiState.update { it.copy(isPlaying = isPlaying) }
        }

        // The player tells us when it moves to a new paragraph.
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            super.onMediaItemTransition(mediaItem, reason)
            mediaItem ?: return

            val extras = mediaItem.mediaMetadata.extras

            val currentPlayingIndex: Int? = mediaItem.mediaId.toIntOrNull()
            val currentNovelId: Int = extras?.getInt("KEY_NOVEL_ID", -1) ?: -1

            _uiState.update {
                it.copy(
                    currentPlayingNovelId = currentNovelId,
                    currentPlayingMediaId = currentPlayingIndex
                )
            }
        }
    }

    init {
        loadNovel(novelId)

        val sessionToken = SessionToken(
            getApplication(),
            ComponentName(getApplication(), NovelPlaybackService::class.java)
        )
        mediaControllerFuture = MediaController.Builder(getApplication(), sessionToken).buildAsync()
        mediaControllerFuture.addListener({
            try {
                mediaController = mediaControllerFuture.get()
                mediaController?.addListener(playerListener)
                _uiState.update { it.copy(isPlayerReady = true) }
            } catch (e: Exception) {
                Log.e("NovelDetailViewModel", "Error connecting to MediaSession", e)
            }
        }, ContextCompat.getMainExecutor(getApplication()))
    }

    fun startPlayback() {
        val controller = this.mediaController ?: return

        // If it's already playing or paused, just un-pause it.
        if (controller.playbackState != Player.STATE_IDLE) {
            if (!controller.playWhenReady) {
                controller.play()
            }
            return
        }

        val speakableBlocksWithOriginalIndices = uiState.value.contentBlocks
            .mapIndexed { index, block -> Pair(index, block) }
            .filter { (_, block) -> block is ContentBlock.Text || block is ContentBlock.Chapter }

        if (speakableBlocksWithOriginalIndices.isEmpty()) return

        // Map them to a full playlist of MediaItems.
        val mediaItems = speakableBlocksWithOriginalIndices.map { (index, block) ->
            // Use the original index directly as media ID for consistency
            val mediaId = "$index"
            val extras = Bundle().apply {
                putInt("KEY_NOVEL_ID", novelId)
            }
            val textToSpeak: String = when (block) {
                is ContentBlock.Chapter -> block.title
                is ContentBlock.Text -> block.annotatedString.text
                else -> ""
            }

            MediaItem.Builder()
                .setMediaId(mediaId)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Part ${index + 1}")
                        .setSubtitle(textToSpeak)
                        .setExtras(extras)
                        .build()
                )
                .build()
        }

        controller.setMediaItems(mediaItems)
        controller.prepare()
        try {
            controller.play()
        } catch (e: Exception) {
            Log.d("STARTPLAYBACK", "${e.message}\nPlease no bug happen in future :;;;")
        }
    }

    fun pausePlayback() {
        mediaController?.pause()
    }

    fun stopPlayback() {
        mediaController?.stop()
        mediaController?.clearMediaItems() // Clear the playlist in the service
    }

    fun togglePlayPause() {
        if (uiState.value.isPlaying) {
            pausePlayback()
        } else {
            // startPlayback is smart enough to resume if already prepared.
            startPlayback()
        }
    }

    override fun onCleared() {
        super.onCleared()
        MediaController.releaseFuture(mediaControllerFuture)
    }

    fun scrollToCurrentlyPlayingItem(listState: LazyListState) {
        viewModelScope.launch {
            val currentId = uiState.value.currentPlayingMediaId ?: return@launch

            // Check if the target item is currently visible
            val isTargetVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == currentId + 1 } // +1 because of header item

            if (!isTargetVisible && !listState.isScrollInProgress) {
                try {
                    // Scroll to the target item with animation
                    listState.animateScrollToItem(currentId + 1) // +1 because of header item
                    Log.d("AutoScroll", "Scrolled to index: ${currentId + 1}")
                } catch (e: Exception) {
                    Log.e("AutoScroll", "Error scrolling to item: ${e.message}")
                }
            }
        }
    }

    /*
    /**
     * This is the main function the UI will call to start TTS playback.
     */
    fun startPlayback() {
        val controller = this.mediaController ?: return

        if (controller.playbackState != Player.STATE_IDLE) {
            // If it's paused, just resume. Otherwise, let it be.
            if (!controller.playWhenReady) {
                controller.play()
            }
            Log.w("NovelDetailViewModel", "Returning from startPlayBack as the the Player was not Idle.")
            return
        }

        // 1. Filter the blocks to get only the parts we can speak.
        val speakableBlocks = uiState.value.contentBlocks.filter {
            it is ContentBlock.Text || it is ContentBlock.Chapter
        }

        if (speakableBlocks.isEmpty()) {
            Log.w("NovelDetailViewModel", "No speakable content found to play.")
            return
        }

        // 2. Map the speakable blocks to MediaItems.
        val mediaItems = speakableBlocks.mapIndexed { index, block ->
            val mediaId = "novel_${novelId}_part_$index"
            val textToSpeak: String
            val metadataTitle: String // A title for debugging/notification purposes.

            when (block) {
                is ContentBlock.Chapter -> {
                    metadataTitle = "Chapter"
                    textToSpeak = block.title
                }
                is ContentBlock.Text -> {
                    metadataTitle = "Paragraph ${index + 1}"
                    textToSpeak = block.annotatedString.text
                }
                // This case is unreachable due to the filter, but good for safety.
                else -> return@mapIndexed null
            }

            // 3. Create the MediaItem, putting the paragraph/title in the SUBTITLE field.
            MediaItem.Builder()
                .setMediaId(mediaId)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(metadataTitle) // Good for notifications, not spoken.
                        .setSubtitle(textToSpeak)    // <-- THIS IS WHAT THE PLAYER WILL SPEAK.
                        .build()
                )
                .build()
        }.filterNotNull()

        if (mediaItems.isEmpty()) {
            Log.w("NovelDetailViewModel", "Created mediaItems list is empty.")
            return
        }

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                // 3. As soon as the player becomes ready...
                if (playbackState == Player.STATE_READY) {
                    // 4. ...it is now 100% safe to command it to play.
                    controller.play()
                    // 5. Clean up this temporary listener so it doesn't fire again.
                    controller.removeListener(this)
                }
            }
        }


        // 4. Send the correct playlist to the service and start playback.
        controller.addListener(listener)
        controller.setMediaItems(mediaItems)
        controller.prepare()
    }*/
    fun loadNovel(novelId: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val result: ParsedNovelResult = withContext(Dispatchers.IO) {
                    val responseBody: ResponseBody = pixivApiService.getNovel(novelId)
                    val htmlString = responseBody.string()
                    NovelParser.parseFullResponse(htmlString)
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        novelData = result.novelResponse.novel,
                        authorDetails = result.novelResponse.authorDetails,
                        contentBlocks = result.contentBlocks
                    )
                }

            } catch (e: Exception) {
                Log.d("NovelDetail", "Failed to load or parse novel: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load or parse novel: ${e.message}"
                    )
                }
            }
        }
    }

    fun updatePageColor(newColor: Color) {
        _uiState.update { currentState ->
            currentState.copy(settings = currentState.settings.copy(pageColor = newColor))
        }
    }

    fun updateTextColor(newColor: Color) {
        _uiState.update { currentState ->
            currentState.copy(settings = currentState.settings.copy(textColor = newColor))
        }
    }

    fun updateLineSpacing(newLineSpacing: TextUnit) {
        _uiState.update { currentState ->
            currentState.copy(settings = currentState.settings.copy(lineSpacing = newLineSpacing))
        }
    }
}

// --- ViewModelFactory remains the same, but now needs Application ---
@UnstableApi
class NovelDetailViewModelFactory(
    private val application: Application, // Add application parameter
    private val pixivApiService: PixivApiService,
    private val novelId: Int
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NovelDetailViewModel::class.java)) {
            return NovelDetailViewModel(application, pixivApiService, novelId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

//for the flaoting bar settings:
data class NovelReaderSettings(
    val pageColor: Color = Color.Black,
    val textColor: Color = Color.White.copy(alpha = 0.95f),
    val lineSpacing: TextUnit = 28.sp,
    // For future Text-to-Speech feature
    val ttsSpeechRate: Float = 1.0f,
    val ttsPitch: Float = 1.0f
)