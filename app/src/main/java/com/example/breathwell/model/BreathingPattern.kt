package com.example.breathwell.model

data class BreathingPattern(
    val name: String,
    val inhale: Int,
    val hold1: Int,
    val exhale: Int,
    val hold2: Int
) {
    companion object {
        val BOX_BREATHING = BreathingPattern("Box Breathing", 4, 4, 4, 4)
        private val RELAXING_BREATH = BreathingPattern("4-7-8 Technique", 4, 7, 8, 0)
        private val CALMING_BREATH = BreathingPattern("Calming Breath", 6, 2, 7, 0)
        val CUSTOM = BreathingPattern("Custom", 4, 4, 4, 2)

        fun getAllPatterns(): List<BreathingPattern> {
            return listOf(BOX_BREATHING, RELAXING_BREATH, CALMING_BREATH, CUSTOM)
        }
    }
}