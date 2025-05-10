package com.example.breathwell.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.breathwell.MainActivity
import com.example.breathwell.R
import com.example.breathwell.databinding.FragmentProgramsBinding
import com.example.breathwell.program.BreathingProgram
import com.example.breathwell.viewmodel.ProgramViewModel
import com.google.android.material.card.MaterialCardView

/**
 * Fragment for browsing guided breathing programs
 */
class ProgramsFragment : Fragment() {

    private var _binding: FragmentProgramsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: ProgramViewModel
    private lateinit var programAdapter: ProgramAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgramsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(requireActivity())[ProgramViewModel::class.java]

        // Set up back button
        binding.backButton.setOnClickListener {
            (requireActivity() as MainActivity).hideAllFragments()
        }

        // Set up filter tabs
        setupFilterTabs()

        // Set up program list
        setupProgramList()

        // Observe program changes
        observePrograms()
    }

    /**
     * Set up the filter tabs for program categories
     */
    private fun setupFilterTabs() {
        // All programs tab
        binding.allTab.setOnClickListener {
            selectFilter(ProgramViewModel.ProgramFilter.ALL)
        }

        // Featured tab
        binding.featuredTab.setOnClickListener {
            selectFilter(ProgramViewModel.ProgramFilter.FEATURED)
        }

        // In Progress tab
        binding.inProgressTab.setOnClickListener {
            selectFilter(ProgramViewModel.ProgramFilter.IN_PROGRESS)
        }

        // Initially select All
        selectFilter(ProgramViewModel.ProgramFilter.ALL)
    }

    /**
     * Select a program filter and update UI
     */
    private fun selectFilter(filter: ProgramViewModel.ProgramFilter) {
        // Update ViewModel
        viewModel.setFilter(filter)

        // Update UI
        binding.allTab.setBackgroundResource(
            if (filter == ProgramViewModel.ProgramFilter.ALL)
                R.drawable.tab_selected_background
            else
                R.drawable.tab_unselected_background
        )

        binding.featuredTab.setBackgroundResource(
            if (filter == ProgramViewModel.ProgramFilter.FEATURED)
                R.drawable.tab_selected_background
            else
                R.drawable.tab_unselected_background
        )

        binding.inProgressTab.setBackgroundResource(
            if (filter == ProgramViewModel.ProgramFilter.IN_PROGRESS)
                R.drawable.tab_selected_background
            else
                R.drawable.tab_unselected_background
        )

        // Update text colors
        binding.allTab.setTextColor(ContextCompat.getColor(requireContext(),
            if (filter == ProgramViewModel.ProgramFilter.ALL) R.color.white else R.color.gray_500
        ))

        binding.featuredTab.setTextColor(ContextCompat.getColor(requireContext(),
            if (filter == ProgramViewModel.ProgramFilter.FEATURED) R.color.white else R.color.gray_500
        ))

        binding.inProgressTab.setTextColor(ContextCompat.getColor(requireContext(),
            if (filter == ProgramViewModel.ProgramFilter.IN_PROGRESS) R.color.white else R.color.gray_500
        ))
    }

    /**
     * Set up the program list RecyclerView
     */
    private fun setupProgramList() {
        programAdapter = ProgramAdapter { program ->
            // Navigate to program details
            showProgramDetails(program.id)
        }

        binding.programsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = programAdapter
        }
    }

    /**
     * Observe program changes from ViewModel
     */
    private fun observePrograms() {
        viewModel.filteredPrograms.observe(viewLifecycleOwner) { programs ->
            programAdapter.submitList(programs)

            // Show empty state if no programs
            binding.emptyState.visibility = if (programs.isEmpty()) View.VISIBLE else View.GONE
            binding.programsRecyclerView.visibility = if (programs.isEmpty()) View.GONE else View.VISIBLE
        }
    }

    /**
     * Navigate to program details screen
     */
    private fun showProgramDetails(programId: String) {
        // Load the program in ViewModel
        viewModel.loadProgram(programId)

        // Navigate to program details
        val detailFragment = ProgramDetailFragment.newInstance(programId)
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.contentContainer, detailFragment)
            .addToBackStack("program_detail")
            .commit()

        // Update active fragment in MainActivity
        (requireActivity() as MainActivity).activeFragment = MainActivity.PROGRAM_DETAIL_FRAGMENT
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Adapter for breathing programs
     */
    private inner class ProgramAdapter(
        private val onItemClick: (BreathingProgram) -> Unit
    ) : RecyclerView.Adapter<ProgramAdapter.ViewHolder>() {

        private var programs: List<BreathingProgram> = emptyList()

        fun submitList(newPrograms: List<BreathingProgram>) {
            programs = newPrograms
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_program, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.bind(programs[position])
        }

        override fun getItemCount(): Int = programs.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val card: MaterialCardView = itemView.findViewById(R.id.programCard)
            private val titleText: TextView = itemView.findViewById(R.id.programTitle)
            private val descriptionText: TextView = itemView.findViewById(R.id.programDescription)
            private val durationText: TextView = itemView.findViewById(R.id.programDuration)
            private val statusText: TextView = itemView.findViewById(R.id.programStatus)
            private val progressBar: View = itemView.findViewById(R.id.programProgress)

            fun bind(program: BreathingProgram) {
                titleText.text = program.name
                descriptionText.text = program.description

                // Format duration text
                val sessionsText = resources.getQuantityString(
                    R.plurals.sessions, program.days * program.sessionsPerDay,
                    program.days * program.sessionsPerDay
                )

                val daysText = resources.getQuantityString(
                    R.plurals.days, program.days, program.days
                )

                durationText.text = "$daysText, $sessionsText"

                // Set status text and color
                when (program.getStatus()) {
                    BreathingProgram.ProgramStatus.NOT_STARTED -> {
                        statusText.text = getString(R.string.program_not_started)
                        statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_500))
                    }
                    BreathingProgram.ProgramStatus.IN_PROGRESS -> {
                        statusText.text = getString(R.string.program_in_progress)
                        statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.cyan_400))
                    }
                    BreathingProgram.ProgramStatus.COMPLETED -> {
                        statusText.text = getString(R.string.program_complete)
                        statusText.setTextColor(ContextCompat.getColor(requireContext(), R.color.green_500))
                    }
                }

                // Set progress bar
                val progress = program.calculateProgress()
                val params = progressBar.layoutParams
                params.width = (progress * 2).coerceAtMost(200) // Max width of 200dp
                progressBar.layoutParams = params

                // Make progress bar visible only for in-progress or completed programs
                progressBar.visibility = if (program.isInProgress() || program.isCompleted)
                    View.VISIBLE else View.INVISIBLE

                // Set click listener
                card.setOnClickListener {
                    onItemClick(program)
                }
            }
        }
    }

    companion object {
        fun newInstance(): ProgramsFragment {
            return ProgramsFragment()
        }
    }
}