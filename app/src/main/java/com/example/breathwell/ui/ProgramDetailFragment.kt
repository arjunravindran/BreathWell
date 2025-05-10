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
import com.example.breathwell.databinding.FragmentProgramDetailBinding
import com.example.breathwell.program.BreathingProgram
import com.example.breathwell.program.ProgramSession
import com.example.breathwell.viewmodel.BreathingViewModel
import com.example.breathwell.viewmodel.ProgramViewModel
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Fragment for displaying the details of a breathing program
 */
class ProgramDetailFragment : Fragment() {

    private var _binding: FragmentProgramDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var programViewModel: ProgramViewModel
    private lateinit var breathingViewModel: BreathingViewModel
    private lateinit var dayAdapter: ProgramDayAdapter
    private var programId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProgramDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get ViewModels
        programViewModel = ViewModelProvider(requireActivity())[ProgramViewModel::class.java]
        breathingViewModel = ViewModelProvider(requireActivity())[BreathingViewModel::class.java]

        // Get program ID from arguments
        programId = arguments?.getString(ARG_PROGRAM_ID)

        // Set up back button
        binding.backButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        // Set up days recycler view
        setupDaysRecyclerView()

        // Observe program changes
        observeProgram()

        // Set up action buttons
        setupActionButtons()
    }

    /**
     * Set up the days recycler view
     */
    private fun setupDaysRecyclerView() {
        dayAdapter = ProgramDayAdapter(
            onDayClick = { day ->
                // Toggle day expansion
                dayAdapter.toggleDayExpansion(day)
            },
            onSessionClick = { day, session ->
                // Start a session
                startProgramSession(day, session)
            }
        )

        binding.daysRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = dayAdapter
        }
    }

    /**
     * Observe program changes
     */
    private fun observeProgram() {
        programViewModel.currentProgram.observe(viewLifecycleOwner) { program ->
            if (program == null) {
                // Program not found
                if (programId != null) {
                    programViewModel.loadProgram(programId!!)
                } else {
                    requireActivity().supportFragmentManager.popBackStack()
                }
                return@observe
            }

            // Update program details
            updateProgramDetails(program)

            // Update days list
            val days = (0 until program.days).toList()
            dayAdapter.submitList(days, program)
        }
    }

    /**
     * Update program details in the UI
     */
    private fun updateProgramDetails(program: BreathingProgram) {
        binding.programTitle.text = program.name
        binding.programDescription.text = program.description

        // Format duration text
        val sessionsText = resources.getQuantityString(
            R.plurals.sessions, program.days * program.sessionsPerDay,
            program.days * program.sessionsPerDay
        )

        val daysText = resources.getQuantityString(
            R.plurals.days, program.days, program.days
        )

        binding.programDuration.text = "$daysText, $sessionsText"

        // Set difficulty indicators
        binding.difficultyContainer.removeAllViews()
        for (i in 1..3) {
            val dot = View(context)
            val size = resources.getDimensionPixelSize(R.dimen.difficulty_dot_size)
            val params = ViewGroup.MarginLayoutParams(size, size)
            params.marginEnd = resources.getDimensionPixelSize(R.dimen.difficulty_dot_margin)
            dot.layoutParams = params

            // Set dot color based on difficulty
            val colorRes = if (i <= program.difficulty) {
                R.color.cyan_400
            } else {
                R.color.gray_600
            }
            dot.setBackgroundResource(R.drawable.difficulty_dot)
            dot.backgroundTintList = ContextCompat.getColorStateList(requireContext(), colorRes)

            binding.difficultyContainer.addView(dot)
        }

        // Set program status
        val status = program.getStatus()
        val statusText = when (status) {
            BreathingProgram.ProgramStatus.NOT_STARTED -> getString(R.string.program_not_started)
            BreathingProgram.ProgramStatus.IN_PROGRESS -> getString(R.string.program_in_progress)
            BreathingProgram.ProgramStatus.COMPLETED -> getString(R.string.program_complete)
        }

        binding.programStatus.text = statusText

        // Set status color
        val statusColorRes = when (status) {
            BreathingProgram.ProgramStatus.NOT_STARTED -> R.color.gray_500
            BreathingProgram.ProgramStatus.IN_PROGRESS -> R.color.cyan_400
            BreathingProgram.ProgramStatus.COMPLETED -> R.color.green_500
        }

        binding.programStatus.setTextColor(ContextCompat.getColor(requireContext(), statusColorRes))

        // Set progress bar
        val progress = program.calculateProgress()
        binding.progressPercent.text = "$progress%"
        val progressWidth = (progress * 2).coerceAtMost(200) // Max width of 200dp
        binding.progressBar.layoutParams.width = progressWidth
        binding.progressBar.requestLayout()

        // Show progress bar only for in-progress or completed programs
        binding.progressLayout.visibility = if (program.isInProgress() || program.isCompleted)
            View.VISIBLE else View.GONE

        // Update action button
        updateActionButton(program)
    }

    /**
     * Set up action buttons
     */
    private fun setupActionButtons() {
        // Primary action button (Start/Continue/Restart)
        binding.actionButton.setOnClickListener {
            val program = programViewModel.currentProgram.value ?: return@setOnClickListener

            when (program.getStatus()) {
                BreathingProgram.ProgramStatus.NOT_STARTED -> {
                    // Start program
                    programViewModel.startProgram(program.id)
                    startCurrentSession()
                }
                BreathingProgram.ProgramStatus.IN_PROGRESS -> {
                    // Continue program
                    startCurrentSession()
                }
                BreathingProgram.ProgramStatus.COMPLETED -> {
                    // Show confirmation dialog
                    showRestartConfirmationDialog(program.id)
                }
            }
        }
    }

    /**
     * Start the current session in the program
     */
    private fun startCurrentSession() {
        val session = programViewModel.currentSession.value ?: return
        startBreathingSession(session)
    }

    /**
     * Start a specific session by day and session index
     */
    private fun startProgramSession(day: Int, sessionIndex: Int) {
        programViewModel.setCurrentSession(day, sessionIndex)
        val session = programViewModel.currentSession.value ?: return
        startBreathingSession(session)
    }

    /**
     * Start a breathing session with the given session
     */
    private fun startBreathingSession(session: ProgramSession) {
        // Set up breathing parameters
        breathingViewModel.setActivePattern(session.pattern)
        breathingViewModel.setTotalCycles(session.cycles)

        // Navigate back to main screen
        (requireActivity() as MainActivity).hideAllFragments()

        // Start breathing session
        breathingViewModel.startBreathing()

        // Mark session as completed
        programViewModel.completeCurrentSession()
    }

    /**
     * Update the primary action button based on program status
     */
    private fun updateActionButton(program: BreathingProgram) {
        val buttonText = when (program.getStatus()) {
            BreathingProgram.ProgramStatus.NOT_STARTED -> getString(R.string.start_program)
            BreathingProgram.ProgramStatus.IN_PROGRESS -> getString(R.string.continue_program)
            BreathingProgram.ProgramStatus.COMPLETED -> getString(R.string.restart_program)
        }

        binding.actionButton.text = buttonText
    }

    /**
     * Show a confirmation dialog for restarting a completed program
     */
    private fun showRestartConfirmationDialog(programId: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.restart_program_title)
            .setMessage(R.string.restart_program_message)
            .setPositiveButton(R.string.restart) { _, _ ->
                // Reset program
                programViewModel.resetProgram(programId)
                // Start program
                programViewModel.startProgram(programId)
                // Start first session
                startCurrentSession()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Adapter for program days
     */
    private inner class ProgramDayAdapter(
        private val onDayClick: (Int) -> Unit,
        private val onSessionClick: (Int, Int) -> Unit
    ) : RecyclerView.Adapter<ProgramDayAdapter.DayViewHolder>() {

        private var days: List<Int> = emptyList()
        private var program: BreathingProgram? = null
        private val expandedDays = mutableSetOf<Int>()

        fun submitList(newDays: List<Int>, newProgram: BreathingProgram) {
            days = newDays
            program = newProgram

            // Expand current day by default
            if (newProgram.currentDay < newProgram.days) {
                expandedDays.add(newProgram.currentDay)
            }

            notifyDataSetChanged()
        }

        fun toggleDayExpansion(day: Int) {
            if (expandedDays.contains(day)) {
                expandedDays.remove(day)
            } else {
                expandedDays.add(day)
            }
            notifyItemChanged(day)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_program_day, parent, false)
            return DayViewHolder(view)
        }

        override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
            val day = days[position]
            holder.bind(day)
        }

        override fun getItemCount(): Int = days.size

        inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val dayCard: MaterialCardView = itemView.findViewById(R.id.dayCard)
            private val dayTitle: TextView = itemView.findViewById(R.id.dayTitle)
            private val dayStatus: TextView = itemView.findViewById(R.id.dayStatus)
            private val sessionsContainer: ViewGroup = itemView.findViewById(R.id.sessionsContainer)
            private val expandIcon: View = itemView.findViewById(R.id.expandIcon)

            fun bind(day: Int) {
                val prog = program ?: return

                // Set day title
                dayTitle.text = getString(R.string.day_x, day + 1)

                // Check if day is locked
                val isLocked = prog.isDayLocked(day)

                // Set day status
                dayStatus.text = if (isLocked) {
                    getString(R.string.locked)
                } else if (day < prog.currentDay) {
                    getString(R.string.completed)
                } else if (day == prog.currentDay) {
                    getString(R.string.today)
                } else {
                    getString(R.string.unlocked)
                }

                // Set status color
                val statusColor = if (isLocked) {
                    R.color.gray_500
                } else if (day < prog.currentDay) {
                    R.color.green_500
                } else if (day == prog.currentDay) {
                    R.color.cyan_400
                } else {
                    R.color.white
                }

                dayStatus.setTextColor(ContextCompat.getColor(requireContext(), statusColor))

                // Set card background based on lock status
                dayCard.setCardBackgroundColor(ContextCompat.getColor(
                    requireContext(),
                    if (isLocked) R.color.gray_800 else R.color.gray_900
                ))

                // Set alpha for locked days
                dayCard.alpha = if (isLocked) 0.7f else 1.0f

                // Set day click listener
                dayCard.setOnClickListener {
                    if (!isLocked) {
                        onDayClick(day)
                    }
                }

                // Handle expansion
                val isExpanded = expandedDays.contains(day)
                expandIcon.rotation = if (isExpanded) 180f else 0f

                // Set up sessions
                sessionsContainer.visibility = if (isExpanded && !isLocked) View.VISIBLE else View.GONE

                if (isExpanded && !isLocked) {
                    setupSessions(day)
                }
            }

            private fun setupSessions(day: Int) {
                val prog = program ?: return

                // Clear previous sessions
                sessionsContainer.removeAllViews()

                // Add a view for each session
                for (sessionIndex in 0 until prog.sessionsPerDay) {
                    val sessionView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_program_session, sessionsContainer, false)

                    // Find the session in program
                    val session = prog.sessions.find { it.day == day && it.session == sessionIndex }
                    if (session != null) {
                        // Set session title
                        sessionView.findViewById<TextView>(R.id.sessionTitle).text = session.title

                        // Set session description
                        sessionView.findViewById<TextView>(R.id.sessionDescription).text = session.description

                        // Set session info (pattern and cycles)
                        val infoText = "${session.pattern.name} - " +
                                resources.getQuantityString(R.plurals.cycles, session.cycles, session.cycles)
                        sessionView.findViewById<TextView>(R.id.sessionInfo).text = infoText

                        // Check if session is available
                        val isAvailable = session.isAvailable(prog.currentDay, prog.currentSession)
                        val isCompleted = day < prog.currentDay ||
                                (day == prog.currentDay && sessionIndex < prog.currentSession)

                        // Set session status
                        val statusText = sessionView.findViewById<TextView>(R.id.sessionStatus)
                        statusText.text = when {
                            isCompleted -> getString(R.string.completed)
                            isAvailable -> getString(R.string.available)
                            else -> getString(R.string.locked)
                        }

                        // Set status color
                        val statusColor = when {
                            isCompleted -> R.color.green_500
                            isAvailable -> R.color.cyan_400
                            else -> R.color.gray_500
                        }

                        statusText.setTextColor(ContextCompat.getColor(requireContext(), statusColor))

                        // Set session click listener
                        val sessionCard = sessionView.findViewById<MaterialCardView>(R.id.sessionCard)
                        sessionCard.setOnClickListener {
                            if (isAvailable && !isCompleted) {
                                onSessionClick(day, sessionIndex)
                            }
                        }

                        // Set card color and opacity
                        sessionCard.setCardBackgroundColor(ContextCompat.getColor(
                            requireContext(),
                            if (isAvailable) R.color.gray_800 else R.color.gray_900
                        ))

                        sessionCard.alpha = if (isAvailable) 1.0f else 0.7f
                    }

                    // Add the session view
                    sessionsContainer.addView(sessionView)
                }
            }
        }
    }

    companion object {
        private const val ARG_PROGRAM_ID = "program_id"

        fun newInstance(programId: String): ProgramDetailFragment {
            val fragment = ProgramDetailFragment()
            val args = Bundle()
            args.putString(ARG_PROGRAM_ID, programId)
            fragment.arguments = args
            return fragment
        }
    }
}