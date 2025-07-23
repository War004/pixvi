package com.example.pixvi.services

import android.content.Context
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import java.util.*
import kotlin.collections.ArrayList

@UnstableApi
class TextToSpeechPlayer(context: Context) :
    SimpleBasePlayer(Looper.getMainLooper()), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var maxSpeechInputLength = 4000 // A safe default

    // Player state variables
    private var currentPlaybackState: Int = Player.STATE_IDLE
    private var playWhenReady: Boolean = false

    // --- PLAYLIST LOGIC IS BACK ---
    private var playlist: List<MediaItem> = emptyList()
    private var currentTrackIndex = 0


    // The queue for smaller text chunks of the *current* MediaItem
    private var chunkedPlaylist: Queue<String> = LinkedList()

    private val utteranceListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {}

        // When a chunk finishes, play the next one.
        override fun onDone(utteranceId: String?) {
            if (playWhenReady) {
                // This is the core engine loop!
                playCurrentChunk()
            }
        }

        override fun onError(utteranceId: String?) {
            updateState(Player.STATE_IDLE)
        }
    }

    init {
        tts = TextToSpeech(context, this)
        tts?.setOnUtteranceProgressListener(utteranceListener)
    }

    private fun mediaItemToMediaItemData(mediaItem: MediaItem): SimpleBasePlayer.MediaItemData {
        return SimpleBasePlayer.MediaItemData.Builder(mediaItem.mediaId)
            .setMediaItem(mediaItem)
            .build()
    }

    override fun getState(): State {
        val mediaItemDataList = playlist.map { mediaItemToMediaItemData(it) }

        return State.Builder()
            .setAvailableCommands(
                Player.Commands.Builder()
                    .add(Player.COMMAND_PLAY_PAUSE)
                    .add(Player.COMMAND_SET_MEDIA_ITEM)
                    .add(Player.COMMAND_CHANGE_MEDIA_ITEMS)
                    .add(Player.COMMAND_PREPARE)
                    .add(Player.COMMAND_STOP)
                    .build()
            )
            .setPlaybackState(currentPlaybackState)
            .setPlayWhenReady(playWhenReady, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaylist(mediaItemDataList)
            .setCurrentMediaItemIndex(if (playlist.isEmpty()) C.INDEX_UNSET else currentTrackIndex)
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        this.playWhenReady = playWhenReady
        invalidateState()

        if (playWhenReady && currentPlaybackState == Player.STATE_READY) {
            // This starts the whole chain
            playCurrentTrack()
        } else if (!playWhenReady) {
            tts?.stop()
            updateState(Player.STATE_READY) // When paused, we are still "ready"
        }
        return Futures.immediateVoidFuture()
    }

    override fun handleSetMediaItems(
        mediaItems: List<MediaItem>,
        startIndex: Int,
        startPositionMs: Long
    ): ListenableFuture<*> {
        this.playlist = ArrayList(mediaItems)
        this.currentTrackIndex = if (startIndex in mediaItems.indices) startIndex else 0
        this.chunkedPlaylist.clear()

        updateState(if (mediaItems.isEmpty()) Player.STATE_IDLE else Player.STATE_READY)
        return Futures.immediateVoidFuture()
    }

    override fun handlePrepare(): ListenableFuture<*> {
        updateState(if (playlist.isEmpty()) Player.STATE_IDLE else Player.STATE_READY)
        return Futures.immediateVoidFuture()
    }

    override fun handleStop(): ListenableFuture<*> {
        tts?.stop()
        chunkedPlaylist.clear()
        playlist = emptyList()
        currentTrackIndex = 0
        updateState(Player.STATE_IDLE)
        return Futures.immediateVoidFuture()
    }

    override fun handleRelease(): ListenableFuture<*> {
        tts?.stop()
        tts?.shutdown()
        tts = null
        updateState(Player.STATE_IDLE)
        return Futures.immediateVoidFuture()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsReady = true
            tts?.language = Locale.JAPANESE
            maxSpeechInputLength = TextToSpeech.getMaxSpeechInputLength()
            invalidateState()
        }
    }

    private fun updateState(newState: Int) {
        if (currentPlaybackState != newState) {
            currentPlaybackState = newState
            invalidateState()
        }
    }

    // --- The Main Playback Engine ---

    private fun playCurrentTrack() {
        if (playlist.isEmpty() || currentTrackIndex >= playlist.size) {
            updateState(Player.STATE_ENDED)
            return
        }
        chunkedPlaylist.clear()

        val currentItem = playlist[currentTrackIndex]
        val textToSpeak = currentItem.mediaMetadata.subtitle?.toString() ?: ""

        if (textToSpeak.isNotBlank()) {
            val chunks = textToSpeak.chunked(maxSpeechInputLength - 1)
            chunkedPlaylist.addAll(chunks)
        } else {
            // If a block is empty, just skip it immediately.
            chunkedPlaylist.add("")
        }
        playCurrentChunk()
    }

    private fun playCurrentChunk() {
        val chunk = chunkedPlaylist.poll()
        if (chunk != null) {
            // Speak the next chunk. onDone will be called, continuing the loop.
            updateState(Player.STATE_READY)
            tts?.speak(chunk, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
        } else {
            // No more chunks in this paragraph, move to the next MediaItem.
            playNextTrack()
        }
    }

    private fun playNextTrack() {
        currentTrackIndex++
        if (currentTrackIndex < playlist.size) {
            playCurrentTrack()
        } else {
            // Reached the end of the entire novel.
            updateState(Player.STATE_ENDED)
        }
    }
}