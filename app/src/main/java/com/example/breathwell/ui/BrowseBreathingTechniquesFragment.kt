package com.example.breathwell.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.breathwell.MainActivity
import com.example.breathwell.R
import com.example.breathwell.databinding.FragmentBrowseBreathingTechniquesBinding
import com.example.breathwell.model.BreathingCategory
import com.example.breathwell.model.BreathingPattern
import com.example.breathwell.viewmodel.BreathingViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout

/**
 * Fragment for browsing and selecting breathing techniques by category
 */
class BrowseBreathingTechniquesFragment : Fragment() {

    private var _binding: FragmentBrowseBreathingTechniquesBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BreathingViewModel
    private lateinit var techniqueAdapter: BreathingTechniqueAdapter
    
    // Track current category selection
    private var currentCategory: BreathingCategory = BreathingCategory.GENERAL

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBrowseBreathingTechniquesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[BreathingViewModel::class.java]
        
        // Setup back button
        binding.backButton.setOnClickListener {
            (requireActivity() as MainActivity).hideAllFragments()
        }
        
        // Setup category tabs
        setupCategoryTabs()
        
        // Setup techniques RecyclerView
        setupTechniquesRecyclerView()
        
        // Load initial techniques
        loadTechniques(BreathingCategory.GENERAL)
    }
    
    private fun setupCategoryTabs() {
        // Remove any existing tabs first
        binding.categoryTabs.removeAllTabs()
        
        // Add a tab for each breathing category
        BreathingCategory.values().forEach { category ->
            val tab = binding.categoryTabs.newTab()
            tab.text = category.displayName
            tab.tag = category
            binding.categoryTabs.addTab(tab)
        }
        
        // Set tab selection listener
        binding.categoryTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val category = tab.tag as BreathingCategory
                currentCategory = category
                binding.categoryDescription.text = category.description
                loadTechniques(category)
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
    
    private fun setupTechniquesRecyclerView() {
        techniqueAdapter = BreathingTechniqueAdapter { pattern ->
            // Handle pattern selection
            viewModel.setActivePattern(pattern)
            showTechniqueDetails(pattern)
        }
        
        binding.techniquesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = techniqueAdapter
        }
    }
    
    private fun loadTechniques(category: BreathingCategory) {
        val techniques = if (category == BreathingCategory.GENERAL) {
            BreathingPattern.getAllPatterns().filter { !it.isCustom }
        } else {
            BreathingPattern.getPatternsByCategory(category)
        }
        
        techniqueAdapter.submitList(techniques)
        
        // Update empty state visibility
        binding.emptyState.visibility = if (techniques.isEmpty()) View.VISIBLE else View.GONE
    }
    
    private fun showTechniqueDetails(pattern: BreathingPattern) {
        // Navigate to technique details fragment
        val detailFragment = BreathingDetailFragment.newInstance(pattern.name)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, detailFragment)
            .addToBackStack("technique_detail")
            .commit()
        
        // Update the active fragment in MainActivity
        (requireActivity() as MainActivity).activeFragment = MainActivity.BREATHING_DETAIL_FRAGMENT
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    /**
     * Adapter for the breathing techniques RecyclerView
     */
    private inner class BreathingTechniqueAdapter(
        private val onItemClick: (BreathingPattern) -> Unit
    ) : androidx.recyclerview.widget.ListAdapter<BreathingPattern, BreathingTechniqueAdapter.ViewHolder>(
        object : androidx.recyclerview.widget.DiffUtil.ItemCallback<BreathingPattern>() {
            override fun areItemsTheSame(oldItem: BreathingPattern, newItem: BreathingPattern): Boolean {
                return oldItem.name == newItem.name
            }
            
            override fun areContentsTheSame(oldItem: BreathingPattern, newItem: BreathingPattern): Boolean {
                return oldItem == newItem
            }
        }
    ) {
        
        inner class ViewHolder(val cardView: MaterialCardView) : androidx.recyclerview.widget.RecyclerView.ViewHolder(cardView) {
            fun bind(pattern: BreathingPattern) {
                // Find views within the card
                val nameTextView = cardView.findViewById<android.widget.TextView>(R.id.techniqueName)
                val descriptionTextView = cardView.findViewById<android.widget.TextView>(R.id.techniqueDescription)
                val infoTextView = cardView.findViewById<android.widget.TextView>(R.id.techniqueInfo)
                val difficultyIcon = cardView.findViewById<android.widget.ImageView>(R.id.difficultyIcon)
                
                // Set content
                nameTextView.text = pattern.name
                descriptionTextView.text = pattern.description
                
                // Set pattern timing info
                val timingText = "${pattern.inhale}-${pattern.hold1}-${pattern.exhale}-${pattern.hold2}"
                infoTextView.text = timingText
                
                // Set difficulty indicators (1-3 dots)
                when (pattern.difficulty) {
                    1 -> difficultyIcon.setImageResource(R.drawable.ic_difficulty_1)
                    2 -> difficultyIcon.setImageResource(R.drawable.ic_difficulty_2)
                    3 -> difficultyIcon.setImageResource(R.drawable.ic_difficulty_3)
                    else -> difficultyIcon.setImageResource(R.drawable.ic_difficulty_1)
                }
                
                // Set click listener
                cardView.setOnClickListener {
                    onItemClick(pattern)
                }
            }
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val cardView = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_breathing_technique, parent, false) as MaterialCardView
            return ViewHolder(cardView)
        }
        
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(getItem(position))
        }
    }
    
    companion object {
        fun newInstance(): BrowseBreathingTechniquesFragment {
            return BrowseBreathingTechniquesFragment()
        }
    }
}
