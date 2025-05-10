package com.example.breathwell.model

data class BreathingPattern(
    val name: String,
    val inhale: Int,
    val hold1: Int,
    val exhale: Int,
    val hold2: Int,
    val description: String
) {
    companion object {
        // Original patterns with added descriptions
        val BOX_BREATHING = BreathingPattern(
            "Box Breathing",
            4, 4, 4, 4,
            "Equal counts for inhale, hold, exhale, and hold. Used by Navy SEALs to reduce stress and improve focus."
        )

        private val RELAXING_BREATH = BreathingPattern(
            "4-7-8 Technique",
            4, 7, 8, 0,
            "Developed by Dr. Andrew Weil, this technique helps reduce anxiety and promotes better sleep."
        )

        private val CALMING_BREATH = BreathingPattern(
            "Calming Breath",
            6, 2, 7, 0,
            "Longer exhale than inhale promotes relaxation by activating the parasympathetic nervous system."
        )

        // New breathing patterns
        private val COHERENT_BREATHING = BreathingPattern(
            "Coherent Breathing",
            5, 0, 5, 0,
            "Breathe at a rate of 5 breaths per minute to optimize heart rate variability and promote calm."
        )

        private val DIAPHRAGMATIC_BREATHING = BreathingPattern(
            "Diaphragmatic",
            4, 0, 6, 0,
            "Deep belly breathing that engages the diaphragm fully, reducing blood pressure and cortisol levels."
        )

        private val ALTERNATE_NOSTRIL = BreathingPattern(
            "Alternate Nostril",
            4, 4, 4, 0,
            "Based on yogic pranayama practice, balances both hemispheres of the brain and reduces stress."
        )

        private val RESONANT_BREATHING = BreathingPattern(
            "Resonant Breathing",
            5, 0, 5, 0,
            "Breathe at your resonant frequency (typically around 6 breaths per minute) to optimize HRV."
        )

        private val ENERGIZING_BREATH = BreathingPattern(
            "Energizing Breath",
            2, 0, 2, 0,
            "Faster breathing pattern used to increase alertness and energy levels."
        )

        private val SAMA_VRITTI = BreathingPattern(
            "Sama Vritti",
            4, 0, 4, 0,
            "Equal ratio breathing from yoga, perfect for beginners to establish mindful breathing."
        )

        private val PROGRESSIVE_RELAXATION = BreathingPattern(
            "Progressive",
            4, 2, 6, 2,
            "Gradually increasing hold times. Excellent for deepening relaxation over a session."
        )

        private val CUSTOM = BreathingPattern(
            "Custom",
            4, 4, 4, 2,
            "Create your own custom breathing pattern with personalized timings."
        )

        fun getAllPatterns(): List<BreathingPattern> {
            return listOf(
                BOX_BREATHING,
                RELAXING_BREATH,
                CALMING_BREATH,
                COHERENT_BREATHING,
                DIAPHRAGMATIC_BREATHING,
                ALTERNATE_NOSTRIL,
                RESONANT_BREATHING,
                ENERGIZING_BREATH,
                SAMA_VRITTI,
                PROGRESSIVE_RELAXATION,
                CUSTOM
            )
        }
    }
}