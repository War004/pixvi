package com.example.pixvi.services

import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@UnstableApi
class NovelPlaybackService : MediaSessionService() {

    // The callback is now much simpler.
    private class NovelPlaybackCallback : MediaSession.Callback {
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: List<MediaItem>
        ): ListenableFuture<List<MediaItem>> {
            // We still need to approve the items.
            return Futures.immediateFuture(mediaItems)
        }

        /**
         * This is called when the system (e.g., from a media button) wants to resume playback.
         * We must tell it what to play and where to start.
         */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val player = mediaSession.player

            // Can't resume if there's nothing in the playlist.
            if (player.mediaItemCount == 0) {
                // Return an empty list, signalling that nothing should be played.
                return Futures.immediateFuture(
                    // C.TIME_UNSET is -9223372036854775807L
                    MediaSession.MediaItemsWithStartPosition(emptyList(), 0, -9223372036854775807L)
                )
            }

            // Manually build the list of current media items. This is the correct way.
            val currentPlaylist = mutableListOf<MediaItem>()
            for (i in 0 until player.mediaItemCount) {
                currentPlaylist.add(player.getMediaItemAt(i))
            }

            // Create the object that tells the session what to play.
            val mediaItemsWithStartPosition = MediaSession.MediaItemsWithStartPosition(
                currentPlaylist,                // The full playlist we just built
                player.currentMediaItemIndex,   // The specific item to start with
                0L                              // The position to start from. For TTS, restarting
                // the current paragraph (at 0ms) is the best behavior.
            )

            // Return this information inside an immediate future.
            return Futures.immediateFuture(mediaItemsWithStartPosition)
        }
    }

    private val player: TextToSpeechPlayer by lazy { TextToSpeechPlayer(this) }

    private val mediaSession: MediaSession by lazy {
        MediaSession.Builder(this, player)
            .setCallback(NovelPlaybackCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        player.release()
        mediaSession.release()
        super.onDestroy()
    }
}