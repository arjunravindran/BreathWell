package com.example.breathwell.model

/**
 * Categories for breathing techniques based on their primary benefit or purpose.
 * This allows users to easily find techniques for specific needs.
 */
enum class BreathingCategory(val displayName: String, val description: String) {
    BASIC("Basic Techniques", "Fundamental breathing exercises for beginners"),
    STRESS_REDUCTION("Stress Reduction", "Techniques to help calm your nervous system"),
    RELAXATION("Relaxation", "Exercises for deep relaxation and preparing for rest"),
    ENERGY("Energy & Focus", "Breathing to increase alertness and concentration"),
    MINDFULNESS("Mindfulness", "Breathing exercises for meditation and presence"),
    SLEEP("Sleep", "Techniques to help you fall asleep faster"),
    HEART_HEALTH("Heart Health", "Breathing for cardiovascular wellness"),
    PERFORMANCE("Performance", "Exercises for athletic or cognitive performance"),
    GENERAL("General Wellness", "Well-rounded techniques for overall health")
}
