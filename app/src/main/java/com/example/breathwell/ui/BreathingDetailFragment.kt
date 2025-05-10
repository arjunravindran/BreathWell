package com.example.breathwell.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.breathwell.MainActivity
import com.example.breathwell.R
import com.example.breathwell.databinding.FragmentBreathingDetailBinding
import com.example.breathwell.model.BreathingPattern
import com.example.breathwell.viewmodel.BreathingViewModel

/**
 * Fragment for displaying detailed information about a breathing technique
 * and allowing the user to start a session with this technique.
 */
class BreathingDetailFragment : Fragment() {

    private var _binding: FragmentBreathingDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BreathingViewModel
    private var breathingPattern: BreathingPattern? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBreathingDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[BreathingViewModel::class.java]
        
        // Get pattern name from arguments
        val patternName = arguments?.getString(ARG_PATTERN_NAME)
        if (patternName != null) {
            // Find the pattern by name
            breathingPattern = BreathingPattern.getAllPatterns().find { it.name == patternName }
            
            // Populate UI with pattern details
            breathingPattern?.let { pattern ->
                setupUI(pattern)
            } ?: run {
                // Pattern not found, go back
                requireActivity().supportFragmentManager.popBackStack()
            }
        } else {
            // No pattern name provided, go back
            requireActivity().supportFragmentManager.popBackStack()
        }
        
        // Setup back button
        binding.backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }
    
    private fun setupUI(pattern: BreathingPattern) {
        // Set basic details
        binding.techniqueName.text = pattern.name
        binding.techniqueDescription.text = pattern.description
        binding.techniqueBenefits.text = pattern.benefits
        binding.categoryName.text = pattern.category.displayName
        
        // Set visualization of the breathing pattern
        setupBreathingVisualization(pattern)
        
        // Set difficulty indicators
        binding.difficultyIndicator.text = when (pattern.difficulty) {
            1 -> getString(R.string.difficulty_beginner)
            2 -> getString(R.string.difficulty_intermediate)
            3 -> getString(R.string.difficulty_advanced)
            else -> getString(R.string.difficulty_beginner)
        }
        
        // Setup pattern timing info
        val inhaleText = resources.getQuantityString(
            R.plurals.seconds, pattern.inhale, pattern.inhale
        )
        val hold1Text = if (pattern.hold1 > 0) {
            resources.getQuantityString(
                R.plurals.seconds, pattern.hold1, pattern.hold1
            )
        } else {
            getString(R.string.no_hold)
        }
        val exhaleText = resources.getQuantityString(
            R.plurals.seconds, pattern.exhale, pattern.exhale
        )
        val hold2Text = if (pattern.hold2 > 0) {
            resources.getQuantityString(
                R.plurals.seconds, pattern.hold2, pattern.hold2
            )
        } else {
            getString(R.string.no_hold)
        }
        
        binding.inhaleValue.text = inhaleText
        binding.hold1Value.text = hold1Text
        binding.exhaleValue.text = exhaleText
        binding.hold2Value.text = hold2Text
        
        // Hide hold labels if not applicable
        binding.hold1Label.visibility = if (pattern.hold1 > 0) View.VISIBLE else View.GONE
        binding.hold1Value.visibility = if (pattern.hold1 > 0) View.VISIBLE else View.GONE
        binding.hold2Label.visibility = if (pattern.hold2 > 0) View.VISIBLE else View.GONE
        binding.hold2Value.visibility = if (pattern.hold2 > 0) View.VISIBLE else View.GONE
        
        // Set cycle duration
        val cycleDuration = pattern.getCycleDuration()
        binding.cycleDuration.text = resources.getQuantityString(
            R.plurals.seconds_per_cycle, cycleDuration, cycleDuration
        )
        
        // Setup start session button
        binding.startSessionButton.setOnClickListener {
            startBreathingSession(pattern)
        }
        
        // Setup the "add to favorites" button if applicable
        binding.favoriteButton.setOnClickListener {
            toggleFavorite(pattern)
        }
        
        // Update favorite button state
        updateFavoriteButtonState(pattern)
    }
    
    private fun setupBreathingVisualization(pattern: BreathingPattern) {
        // This would be a visual representation of the breathing pattern
        // For now, we'll use a simple text representation
        val totalDuration = pattern.inhale + pattern.hold1 + pattern.exhale + pattern.hold2
        
        // Calculate percentages for visualization
        val inhalePercent = (pattern.inhale.toFloat() / totalDuration) * 100
        val hold1Percent = (pattern.hold1.toFloat() / totalDuration) * 100
        val exhalePercent = (pattern.exhale.toFloat() / totalDuration) * 100
        val hold2Percent = (pattern.hold2.toFloat() / totalDuration) * 100
        
        // Set the widths of the visualization bars
        binding.inhaleBar.layoutParams.width = (inhalePercent * 3).toInt()
        binding.hold1Bar.layoutParams.width = (hold1Percent * 3).toInt()
        binding.exhaleBar.layoutParams.width = (exhalePercent * 3).toInt()
        binding.hold2Bar.layoutParams.width = (hold2Percent * 3).toInt()
        
        // Hide bars with zero duration
        binding.hold1Bar.visibility = if (pattern.hold1 > 0) View.VISIBLE else View.GONE
        binding.hold2Bar.visibility = if (pattern.hold2 > 0) View.VISIBLE else View.GONE
        
        // Request layout to apply changes
        binding.patternVisualizationLayout.requestLayout()
    }
    
    private fun startBreathingSession(pattern: BreathingPattern) {
        // Set this pattern as active in ViewModel
        viewModel.setActivePattern(pattern)
        
        // Navigate back to main breathing screen
        (requireActivity() as MainActivity).hideAllFragments()
        
        // Auto-start the breathing session
        viewModel.startBreathing()
    }
    
    private fun toggleFavorite(pattern: BreathingPattern) {
        // Toggle favorite status
        viewModel.toggleFavoritePattern(pattern.name)
        
        // Update button state
        updateFavoriteButtonState(pattern)
    }
    
    private fun updateFavoriteButtonState(pattern: BreathingPattern) {
        // Check if this pattern is a favorite
        val isFavorite = viewModel.isFavoritePattern(pattern.name)
        
        // Update button icon and text
        if (isFavorite) {
            binding.favoriteButton.setIconResource(R.drawable.ic_favorite_filled)
            binding.favoriteButton.setText(R.string.remove_from_favorites)
        } else {
            binding.favoriteButton.setIconResource(R.drawable.ic_favorite_outline)
            binding.favoriteButton.setText(R.string.add_to_favorites)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    companion object {
        private const val ARG_PATTERN_NAME = "pattern_name"
        
        fun newInstance(patternName: String): BreathingDetailFragment {
            val fragment = BreathingDetailFragment()
            val args = Bundle()
            args.putString(ARG_PATTERN_NAME, patternName)
            fragment.arguments = args
            return fragment
        }
    }
}
