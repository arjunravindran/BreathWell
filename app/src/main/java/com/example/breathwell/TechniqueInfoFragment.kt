package com.example.breathwell

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.breathwell.databinding.FragmentTechniqueInfoBinding
import com.example.breathwell.model.BreathingPattern
import com.example.breathwell.viewmodel.BreathingViewModel

class TechniqueInfoFragment : Fragment() {

    private var _binding: FragmentTechniqueInfoBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BreathingViewModel
    private var patternName: String? = null

    companion object {
        private const val ARG_PATTERN_NAME = "pattern_name"

        fun newInstance(patternName: String): TechniqueInfoFragment {
            val fragment = TechniqueInfoFragment()
            val args = Bundle()
            args.putString(ARG_PATTERN_NAME, patternName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            patternName = it.getString(ARG_PATTERN_NAME)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTechniqueInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[BreathingViewModel::class.java]

        // Find the selected breathing pattern
        val pattern = findBreathingPattern(patternName)

        if (pattern != null) {
            setupUI(pattern)

            // Set up the start button
            binding.startButton.setOnClickListener {
                viewModel.setActivePattern(pattern)

                // Go back to main screen and start breathing
                (requireActivity() as MainActivity).hideAllFragments()
                viewModel.toggleBreathing()
            }
        }
    }

    private fun findBreathingPattern(name: String?): BreathingPattern? {
        return name?.let { targetName ->
            BreathingPattern.getAllPatterns().find { it.name == targetName }
        }
    }

    private fun setupUI(pattern: BreathingPattern) {
        // Set technique title
        binding.techniqueTitle.text = pattern.name

        // Set timing values
        binding.inhaleDuration.text = "${pattern.inhale}s"
        binding.hold1Duration.text = "${pattern.hold1}s"
        binding.exhaleDuration.text = "${pattern.exhale}s"
        binding.hold2Duration.text = "${pattern.hold2}s"

        // Set basic description (from pattern)
        binding.descriptionText.text = pattern.description

        // Set detailed benefits based on pattern name
        val benefitsText = getTechniqueBenefits(pattern.name)
        binding.benefitsText.text = benefitsText
    }

    private fun getTechniqueBenefits(patternName: String): String {
        return when (patternName) {
            "Box Breathing" -> "• Reduces stress and anxiety\n" +
                    "• Improves concentration and performance\n" +
                    "• Used by Navy SEALs in high-stress situations\n" +
                    "• Helps manage acute stress responses\n" +
                    "• Can lower blood pressure"

            "4-7-8 Technique" -> "• Acts as a natural tranquilizer for the nervous system\n" +
                    "• Helps you fall asleep faster\n" +
                    "• Reduces anxiety and stress levels\n" +
                    "• May help manage food cravings\n" +
                    "• Can help control emotional responses"

            "Calming Breath" -> "• Activates the parasympathetic nervous system\n" +
                    "• Quickly reduces feelings of tension\n" +
                    "• Helps lower heart rate and blood pressure\n" +
                    "• Useful during panic or anxiety attacks\n" +
                    "• Can be practiced discreetly anywhere"

            "Coherent Breathing" -> "• Optimizes heart rate variability (HRV)\n" +
                    "• Balances the autonomic nervous system\n" +
                    "• Improves cognitive function\n" +
                    "• Reduces symptoms of depression and anxiety\n" +
                    "• Enhances immune system function"

            "Diaphragmatic" -> "• Strengthens the diaphragm muscle\n" +
                    "• Decreases oxygen demand and reduces workload on the body\n" +
                    "• Slows breathing rate and reduces stress\n" +
                    "• Lowers cortisol levels in the body\n" +
                    "• Improves core muscle stability"

            "Alternate Nostril" -> "• Balances left and right hemispheres of the brain\n" +
                    "• Purifies subtle energy channels (nadis)\n" +
                    "• Improves focus and concentration\n" +
                    "• Reduces stress and anxiety\n" +
                    "• Promotes mental clarity"

            "Resonant Breathing" -> "• Maximizes heart rate variability (HRV)\n" +
                    "• Synchronizes breath with heart rhythms\n" +
                    "• Reduces blood pressure\n" +
                    "• Improves gas exchange in the lungs\n" +
                    "• Promotes emotional regulation"

            "Energizing Breath" -> "• Increases alertness and energy levels\n" +
                    "• Improves concentration and focus\n" +
                    "• Provides a natural alternative to caffeine\n" +
                    "• Helps clear mental fog\n" +
                    "• Can elevate mood and motivation"

            "Sama Vritti" -> "• Creates balance and harmony in the nervous system\n" +
                    "• Establishes mindful awareness of the breath\n" +
                    "• Perfect technique for beginners\n" +
                    "• Reduces stress and anxiety\n" +
                    "• Promotes mental clarity and focus"

            "Progressive" -> "• Gradually deepens relaxation response\n" +
                    "• Creates cumulative calming effect\n" +
                    "• Helps manage persistent anxiety\n" +
                    "• Can be useful for preparing for sleep\n" +
                    "• Builds respiratory strength and control"

            "Custom" -> "• Personalized to your specific needs\n" +
                    "• Adaptable for different situations and conditions\n" +
                    "• Can be modified as your practice advances\n" +
                    "• Allows experimentation to find what works best for you\n" +
                    "• Can target specific goals (relaxation, energy, focus, etc.)"

            else -> ""
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}