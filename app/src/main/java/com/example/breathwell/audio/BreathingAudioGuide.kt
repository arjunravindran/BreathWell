package com.example.breathwell.audio

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import com.example.breathwell.model.BreathPhase
import com.example.breathwell.utils.SSMLHelper
import java.util.Locale
import java.util.UUID

/**
 * Provides audio guidance for breathing exercises using
 * Android's Text-to-Speech engine.
 */
class BreathingAudioGuide(private val context: Context) {
    
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false
    private var currentPhase: BreathPhase? = null
    private var currentUtteranceId: String? = null
    
    // Audio settings
    private var speechRate = 0.85f
    private var speechPitch = 0.95f
    private var voiceType = VoiceType.CALM
    
    // Callback for TTS events
    var audioCompletionListener: (() -> Unit)? = null
    
    /**
     * Initialize the Text-to-Speech engine
     */
    init {
        initializeTts()
    }
    
    /**
     * Set up the TTS engine with appropriate configuration
     */
    private fun initializeTts() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "Language not supported")
                    isTtsReady = false
                } else {
                    // Configure for more natural-sounding speech
                    textToSpeech?.setPitch(speechPitch)
                    textToSpeech?.setSpeechRate(speechRate)
                    
                    // Set the best available voice for selected type
                    setOptimalVoice()
                    
                    // Set utterance progress listener
                    textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            // Speech started
                        }
                        
                        override fun onDone(utteranceId: String?) {
                            currentUtteranceId = null
                            audioCompletionListener?.invoke()
                        }
                        
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            currentUtteranceId = null
                        }
                    })
                    
                    isTtsReady = true
                    Log.d(TAG, "TTS initialized successfully")
                }
            } else {
                Log.e(TAG, "TTS initialization failed")
                isTtsReady = false
            }
        }
    }
    
    /**
     * Choose the best voice based on selected voice type
     */
    private fun setOptimalVoice() {
        textToSpeech?.let { tts ->
            val voices = tts.voices
            if (voices != null) {
                // Filter to get English voices
                val englishVoices = voices.filter { it.locale.language == "en" }
                
                // Look for ideal voice for selected type
                val targetVoice = when (voiceType) {
                    VoiceType.STANDARD -> findBestVoice(englishVoices, listOf("en-us-x-sfg", "en-us"))
                    VoiceType.CALM -> findBestVoice(englishVoices, listOf("en-us-x-sfg", "en-us-x-sfg-local"))
                    VoiceType.ENERGETIC -> findBestVoice(englishVoices, listOf("en-us-x-tpf", "en-us-x-tpc"))
                }
                
                targetVoice?.let { voice ->
                    tts.voice = voice
                    Log.d(TAG, "Set voice to: ${voice.name}")
                }
            }
        }
    }
    
    /**
     * Find the best voice from available voices
     */
    private fun findBestVoice(voices: List<Voice>, preferences: List<String>): Voice? {
        // First try to find preferred voices
        for (preference in preferences) {
            val matchingVoice = voices.find { it.name.contains(preference) }
            if (matchingVoice != null) {
                return matchingVoice
            }
        }
        
        // Fall back to any available voice
        return voices.firstOrNull()
    }
    
    /**
     * Speak a text message with default options
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_ADD) {
        if (!isTtsReady) {
            Log.w(TAG, "TTS not ready, speech skipped")
            return
        }
        
        val utteranceId = UUID.randomUUID().toString()
        currentUtteranceId = utteranceId
        
        textToSpeech?.speak(text, queueMode, null, utteranceId)
    }
    
    /**
     * Speak text with SSML markup for better emphasis and pauses
     */
    fun speakWithSSML(ssml: String, queueMode: Int = TextToSpeech.QUEUE_ADD) {
        if (!isTtsReady) {
            Log.w(TAG, "TTS not ready, speech skipped")
            return
        }
        
        val utteranceId = UUID.randomUUID().toString()
        currentUtteranceId = utteranceId
        
        textToSpeech?.speak(ssml, queueMode, null, utteranceId)
    }
    
    /**
     * Announce the start of a breathing session
     */
    fun announceSessionStart(patternName: String, cycles: Int) {
        val message = "Starting ${patternName} breathing for ${cycles} cycles. " +
                "Find a comfortable position, and let's begin."
        speak(message, TextToSpeech.QUEUE_FLUSH)
    }
    
    /**
     * Announce the completion of a breathing session
     */
    fun announceSessionComplete() {
        val messages = listOf(
            "Great job. You've completed your breathing session.",
            "Well done. Your breathing session is now complete.",
            "Excellent. You've finished your breathing exercise."
        )
        speak(messages.random(), TextToSpeech.QUEUE_FLUSH)
    }
    
    /**
     * Announce current breathing phase with duration
     */
    fun announcePhase(phase: BreathPhase, duration: Int) {
        // Skip if this is a repeat of the current phase
        if (phase == currentPhase) return
        
        currentPhase = phase
        
        val ssml = when (phase) {
            BreathPhase.INHALE -> {
                SSMLHelper.breathInstructionSSML("Breathe in", duration)
            }
            BreathPhase.HOLD1 -> {
                SSMLHelper.holdInstructionSSML("Hold", duration)
            }
            BreathPhase.EXHALE -> {
                SSMLHelper.breathInstructionSSML("Breathe out", duration)
            }
            BreathPhase.HOLD2 -> {
                SSMLHelper.holdInstructionSSML("Hold", duration)
            }
            BreathPhase.READY -> {
                SSMLHelper.plainSSML("Get ready")
            }
            BreathPhase.COMPLETE -> {
                announceSessionComplete()
                return
            }
        }
        
        speakWithSSML(ssml, TextToSpeech.QUEUE_FLUSH)
    }
    
    /**
     * Stop any current speech
     */
    fun stopSpeaking() {
        textToSpeech?.stop()
        currentUtteranceId = null
    }
    
    /**
     * Set the speech rate
     * @param rate Value between 0.5 (slow) and 2.0 (fast)
     */
    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
        textToSpeech?.setSpeechRate(speechRate)
    }
    
    /**
     * Set the speech pitch
     * @param pitch Value between 0.5 (low) and 1.5 (high)
     */
    fun setSpeechPitch(pitch: Float) {
        speechPitch = pitch.coerceIn(0.5f, 1.5f)
        textToSpeech?.setPitch(speechPitch)
    }
    
    /**
     * Set the voice type
     */
    fun setVoiceType(type: VoiceType) {
        voiceType = type
        setOptimalVoice()
    }
    
    /**
     * Speak a preview sample to demonstrate current voice settings
     */
    fun speakPreview() {
        val previewText = "This is how your breathing guide will sound."
        speak(previewText, TextToSpeech.QUEUE_FLUSH)
    }
    
    /**
     * Clean up resources
     */
    fun release() {
        stopSpeaking()
        textToSpeech?.shutdown()
        textToSpeech = null
        isTtsReady = false
    }
    
    /**
     * Voice types for different guidance styles
     */
    enum class VoiceType {
        STANDARD, CALM, ENERGETIC
    }
    
    companion object {
        private const val TAG = "BreathingAudioGuide"
    }
}
