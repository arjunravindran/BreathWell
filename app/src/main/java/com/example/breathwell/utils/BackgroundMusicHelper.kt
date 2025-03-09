package com.example.breathwell.utils

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

/**
 * Helper class to manage background music playback
 */
class BackgroundMusicHelper(private val context: Context) : DefaultLifecycleObserver {
    
    private var mediaPlayer: MediaPlayer? = null
    private var isMusicEnabled = true
    private var volume = 0.3f // Default volume (30%)
    
    /**
     * Initialize and start playing background music
     * @param musicResId Resource ID of the music file
     */
    fun initialize(musicResId: Int) {
        try {
            // Clean up any existing MediaPlayer
            release()
            
            // Create and set up the MediaPlayer
            mediaPlayer = MediaPlayer.create(context, musicResId).apply {
                isLooping = true
                setVolume(volume, volume)
                
                // Start playing if music is enabled
                if (isMusicEnabled) {
                    start()
                }
            }
            
            Log.d(TAG, "Background music initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing background music: ${e.message}")
        }
    }
    
    /**
     * Start or resume music playback
     */
    fun start() {
        if (isMusicEnabled && mediaPlayer != null) {
            try {
                if (!mediaPlayer!!.isPlaying) {
                    mediaPlayer!!.start()
                    Log.d(TAG, "Background music started")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting background music: ${e.message}")
            }
        }
    }
    
    /**
     * Pause music playback
     */
    fun pause() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    it.pause()
                    Log.d(TAG, "Background music paused")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing background music: ${e.message}")
        }
    }
    
    /**
     * Enable or disable background music
     * @param enabled True to enable, false to disable
     */
    fun setMusicEnabled(enabled: Boolean) {
        isMusicEnabled = enabled
        
        if (enabled) {
            start()
        } else {
            pause()
        }
        
        Log.d(TAG, "Background music enabled: $enabled")
    }
    
    /**
     * Set the volume of the background music
     * @param volumeLevel Volume level from 0.0 (silent) to 1.0 (max)
     */
    fun setVolume(volumeLevel: Float) {
        volume = volumeLevel.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(volume, volume)
        Log.d(TAG, "Background music volume set to: $volume")
    }
    
    /**
     * Release resources
     */
    fun release() {
        try {
            mediaPlayer?.apply {
                if (isPlaying) {
                    stop()
                }
                release()
            }
            mediaPlayer = null
            Log.d(TAG, "Background music released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing background music: ${e.message}")
        }
    }
    
    // Lifecycle methods
    override fun onResume(owner: LifecycleOwner) {
        start()
    }
    
    override fun onPause(owner: LifecycleOwner) {
        pause()
    }
    
    override fun onDestroy(owner: LifecycleOwner) {
        release()
    }
    
    companion object {
        private const val TAG = "BackgroundMusicHelper"
    }
}