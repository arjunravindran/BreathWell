package com.example.breathwell

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.breathwell.databinding.FragmentSettingsBinding
import com.example.breathwell.viewmodel.BreathingViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BreathingViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[BreathingViewModel::class.java]

        populateInitialValues()
        setupApplyButton()
    }

    private fun populateInitialValues() {
        val customPattern = viewModel.customPattern.value ?: return

        binding.inhaleInput.setText(customPattern.inhale.toString())
        binding.hold1Input.setText(customPattern.hold1.toString())
        binding.exhaleInput.setText(customPattern.exhale.toString())
        binding.hold2Input.setText(customPattern.hold2.toString())
    }

    private fun setupApplyButton() {
        binding.applyButton.setOnClickListener {
            val inhaleText = binding.inhaleInput.text.toString()
            val hold1Text = binding.hold1Input.text.toString()
            val exhaleText = binding.exhaleInput.text.toString()
            val hold2Text = binding.hold2Input.text.toString()

            val inhale = inhaleText.toIntOrNull() ?: 4
            val hold1 = hold1Text.toIntOrNull() ?: 4
            val exhale = exhaleText.toIntOrNull() ?: 4
            val hold2 = hold2Text.toIntOrNull() ?: 2

            viewModel.updateCustomPattern(inhale, hold1, exhale, hold2)
            viewModel.setActivePattern(viewModel.customPattern.value!!)

            // Close settings and return to main screen
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}