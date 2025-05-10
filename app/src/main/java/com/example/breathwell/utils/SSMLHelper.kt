package com.example.breathwell.utils

/**
 * Helper class for creating Speech Synthesis Markup Language (SSML)
 * to improve the quality and timing of TTS output.
 */
object SSMLHelper {

    /**
     * Creates SSML for a simple text phrase
     */
    fun plainSSML(text: String): String {
        return "<speak>$text</speak>"
    }

    /**
     * Creates SSML for breathing instructions with appropriate pauses and emphasis
     */
    fun breathInstructionSSML(instruction: String, duration: Int): String {
        return "<speak>" +
                "<prosody rate=\"slow\" pitch=\"-2st\">" +
                "<emphasis level=\"moderate\">$instruction</emphasis>" +
                "<break time=\"500ms\"/>" +
                generateCountdown(duration) +
                "</prosody>" +
                "</speak>"
    }

    /**
     * Creates SSML for hold instructions with appropriate pauses
     */
    fun holdInstructionSSML(instruction: String, duration: Int): String {
        return "<speak>" +
                "<prosody rate=\"slow\" pitch=\"-4st\">" +
                "<emphasis level=\"moderate\">$instruction</emphasis>" +
                "<break time=\"500ms\"/>" +
                generateCountdown(duration) +
                "</prosody>" +
                "</speak>"
    }

    /**
     * Generates a countdown sequence with appropriate pauses
     */
    private fun generateCountdown(duration: Int): String {
        if (duration <= 1) return ""

        val builder = StringBuilder()
        // Only count down for durations over 3 seconds
        if (duration > 3) {
            // Add countdown for the last 3 numbers
            val startCount = minOf(3, duration)
            for (i in startCount downTo 1) {
                builder.append("$i")
                // Add longer pause between numbers
                builder.append("<break time=\"${(1000 / startCount)}ms\"/>")
            }
        }
        return builder.toString()
    }

    /**
     * Creates SSML for session introduction
     */
    fun sessionIntroSSML(patternName: String, cycles: Int): String {
        return "<speak>" +
                "Starting $patternName breathing for $cycles cycles. " +
                "<break time=\"300ms\"/>" +
                "Find a comfortable position, " +
                "<break time=\"300ms\"/>" +
                "and let's begin." +
                "<break time=\"1s\"/>" +
                "</speak>"
    }

    /**
     * Creates SSML for session completion
     */
    fun sessionCompleteSSML(message: String): String {
        return "<speak>" +
                "<prosody rate=\"0.9\" pitch=\"-2st\">" +
                message +
                "</prosody>" +
                "</speak>"
    }

    /**
     * Creates SSML for guided relaxation introduction
     */
    fun relaxationIntroSSML(): String {
        return "<speak>" +
                "<prosody rate=\"0.85\" pitch=\"-3st\">" +
                "Let's take a moment to relax." +
                "<break time=\"500ms\"/>" +
                "Close your eyes if you're comfortable doing so." +
                "<break time=\"1s\"/>" +
                "Focus on your breath, and let go of any tension in your body." +
                "<break time=\"1s\"/>" +
                "</prosody>" +
                "</speak>"
    }
}