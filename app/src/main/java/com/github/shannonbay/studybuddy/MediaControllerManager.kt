package com.github.shannonbay.studybuddy

import android.content.ComponentName
import android.content.Context
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Bundle

class MediaControllerManager(private val context: Context) {

    private val mediaSessionManager: MediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    fun pauseActiveMediaSessions() {
        val activeSessions = mediaSessionManager.getActiveSessions(ComponentName(context, "Your Component Name"))

        for (session in activeSessions) {
            val controller = MediaController(context, session.sessionToken)

            // Check if the session is currently playing
            val playbackState = controller.playbackState
            if (playbackState != null &&
                playbackState.state == PlaybackState.STATE_PLAYING
            ) {
                // Pause the playback
                val extras = Bundle()
                controller.transportControls.pause()
            }
        }
    }
}
