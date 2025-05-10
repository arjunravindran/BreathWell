package com.example.breathwell.ui

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.breathwell.MainActivity
import com.example.breathwell.R
import com.example.breathwell.databinding.FragmentAnalyticsBinding
import com.example.breathwell.utils.AnalyticsHelper
import com.example.breathwell.viewmodel.BreathingViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Fragment for displaying breathing analytics and statistics
 */
class AnalyticsFragment : Fragment() {

    private var _binding: FragmentAnalyticsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BreathingViewModel

    // Date formatter for displaying dates
    private val dateFormatter = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault())

    // Current view mode
    private var currentMode = ViewMode.WEEKLY

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAnalyticsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[BreathingViewModel::class.java]

        // Set up back button
        binding.backButton.setOnClickListener {
            (requireActivity() as MainActivity).hideAllFragments()
        }

        // Set up tab selection
        setupTabs()

        // Load initial data
        loadWeeklyData()

        // Set up today's overview
        setupTodayOverview()
    }

    /**
     * Set up weekly/monthly tab selection
     */
    private fun setupTabs() {
        binding.weeklyTab.setOnClickListener {
            if (currentMode != ViewMode.WEEKLY) {
                selectWeeklyMode()
                loadWeeklyData()
            }
        }

        binding.monthlyTab.setOnClickListener {
            if (currentMode != ViewMode.MONTHLY) {
                selectMonthlyMode()
                loadMonthlyData()
            }
        }

        // Start with weekly selected
        selectWeeklyMode()
    }

    private fun selectWeeklyMode() {
        binding.weeklyTab.setBackgroundResource(R.drawable.tab_selected_background)
        binding.weeklyTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

        binding.monthlyTab.setBackgroundResource(R.drawable.tab_unselected_background)
        binding.monthlyTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_500))

        currentMode = ViewMode.WEEKLY
    }

    private fun selectMonthlyMode() {
        binding.monthlyTab.setBackgroundResource(R.drawable.tab_selected_background)
        binding.monthlyTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

        binding.weeklyTab.setBackgroundResource(R.drawable.tab_unselected_background)
        binding.weeklyTab.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_500))

        currentMode = ViewMode.MONTHLY
    }

    /**
     * Set up today's overview section
     */
    private fun setupTodayOverview() {
        // Set today's date
        val todayDate = LocalDate.now()
        binding.todayDate.text = dateFormatter.format(todayDate)

        // Observe today's sessions
        viewModel.todaysSessions.observe(viewLifecycleOwner) { sessions ->
            if (sessions.isEmpty()) {
                binding.todayNoSessions.visibility = View.VISIBLE
                binding.todayStats.visibility = View.GONE
            } else {
                binding.todayNoSessions.visibility = View.GONE
                binding.todayStats.visibility = View.VISIBLE

                // Get today's statistics
                val stats = AnalyticsHelper.calculateDayStatistics(sessions)

                // Update UI
                binding.sessionCount.text = stats.totalSessions.toString()

                // Format minutes
                val minutes = stats.totalDuration / 60
                binding.durationMinutes.text = resources.getQuantityString(
                    R.plurals.minutes, minutes, minutes
                )

                // Most used technique
                val mostUsed = stats.techniquesUsed.firstOrNull()
                if (mostUsed != null) {
                    binding.technique.text = mostUsed.techniqueName
                } else {
                    binding.technique.text = "-"
                }
            }
        }
    }

    /**
     * Load weekly statistics
     */
    private fun loadWeeklyData() {
        // Get date range for week
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(6) // Last 7 days

        // Update date range text
        binding.dateRangeText.text = getString(
            R.string.date_range_format,
            dateFormatter.format(startDate),
            dateFormatter.format(endDate)
        )

        // Observe weekly statistics
        viewModel.getWeeklyStatistics().observe(viewLifecycleOwner) { stats ->
            updateStatisticsUI(stats)
        }
    }

    /**
     * Load monthly statistics
     */
    private fun loadMonthlyData() {
        // Get date range for month
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(29) // Last 30 days

        // Update date range text
        binding.dateRangeText.text = getString(
            R.string.date_range_format,
            dateFormatter.format(startDate),
            dateFormatter.format(endDate)
        )

        // Observe monthly statistics
        viewModel.getMonthlyStatistics().observe(viewLifecycleOwner) { stats ->
            updateStatisticsUI(stats)
        }
    }

    /**
     * Update UI with statistics data
     */
    private fun updateStatisticsUI(stats: AnalyticsHelper.PeriodStatistics) {
        if (stats.totalSessions == 0) {
            showEmptyState()
            return
        }

        hideEmptyState()

        // Update summary values
        binding.totalSessionsValue.text = stats.totalSessions.toString()

        // Format minutes
        val minutes = stats.totalDuration / 60
        binding.totalMinutesValue.text = resources.getQuantityString(
            R.plurals.minutes, minutes, minutes
        )

        binding.averageDurationValue.text = resources.getQuantityString(
            R.plurals.session_seconds,
            stats.averageDurationPerSession,
            stats.averageDurationPerSession
        )

        // Consistency calculation: completed days / total days
        val consistencyPercentage = if (stats.totalDays > 0) {
            (stats.completedDays * 100) / stats.totalDays
        } else {
            0
        }
        binding.consistencyValue.text = getString(R.string.percentage_format, consistencyPercentage)

        // Favorite technique
        stats.mostUsedTechnique?.let { technique ->
            binding.favoriteTechniqueValue.text = technique.techniqueName
        } ?: run {
            binding.favoriteTechniqueValue.text = "-"
        }

        // Update technique breakdown section
        updateTechniqueBreakdown(stats.sessionsByTechnique)

        // Update time of day breakdown
        updateTimeOfDayBreakdown(stats.sessionsByTimeOfDay)
    }

    /**
     * Update technique breakdown visualization
     */
    private fun updateTechniqueBreakdown(techniques: List<AnalyticsHelper.TechniqueUsage>) {
        // Clear previous data
        binding.techniqueBreakdownContainer.removeAllViews()

        if (techniques.isEmpty()) {
            // Show empty message
            val emptyView = TextView(requireContext())
            emptyView.text = getString(R.string.no_data_available)
            emptyView.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_500))
            binding.techniqueBreakdownContainer.addView(emptyView)
            return
        }

        // Calculate total sessions for percentage
        val totalSessions = techniques.sumOf { it.count }

        // Add each technique item
        for (technique in techniques) {
            val itemView = layoutInflater.inflate(
                R.layout.item_technique_breakdown,
                binding.techniqueBreakdownContainer,
                false
            )

            // Set technique name
            itemView.findViewById<TextView>(R.id.techniqueName).text = technique.techniqueName

            // Set count and percentage
            val percentage = if (totalSessions > 0) {
                (technique.count * 100) / totalSessions
            } else {
                0
            }

            val countText = getString(
                R.string.technique_count_format,
                technique.count,
                percentage
            )
            itemView.findViewById<TextView>(R.id.techniqueCount).text = countText

            // Set progress bar
            val progressBar = itemView.findViewById<View>(R.id.techniqueProgress)
            val params = progressBar.layoutParams
            params.width = (percentage * 2).coerceAtLeast(5) // Minimum width of 5dp
            progressBar.layoutParams = params

            // Add to container
            binding.techniqueBreakdownContainer.addView(itemView)
        }
    }

    /**
     * Update time of day breakdown visualization
     */
    private fun updateTimeOfDayBreakdown(timeData: Map<AnalyticsHelper.TimeOfDay, Int>) {
        // Clear previous data
        binding.timeBreakdownContainer.removeAllViews()

        if (timeData.values.sum() == 0) {
            // Show empty message
            val emptyView = TextView(requireContext())
            emptyView.text = getString(R.string.no_data_available)
            emptyView.setTextColor(ContextCompat.getColor(requireContext(), R.color.gray_500))
            binding.timeBreakdownContainer.addView(emptyView)
            return
        }

        // Calculate total sessions for percentage
        val totalSessions = timeData.values.sum()

        // Time of day order
        val timeOrder = listOf(
            AnalyticsHelper.TimeOfDay.MORNING,
            AnalyticsHelper.TimeOfDay.AFTERNOON,
            AnalyticsHelper.TimeOfDay.EVENING,
            AnalyticsHelper.TimeOfDay.NIGHT
        )

        // Add each time of day item
        for (timeOfDay in timeOrder) {
            val count = timeData[timeOfDay] ?: 0

            val itemView = layoutInflater.inflate(
                R.layout.item_time_breakdown,
                binding.timeBreakdownContainer,
                false
            )

            // Set time name
            val timeName = when (timeOfDay) {
                AnalyticsHelper.TimeOfDay.MORNING -> getString(R.string.morning)
                AnalyticsHelper.TimeOfDay.AFTERNOON -> getString(R.string.afternoon)
                AnalyticsHelper.TimeOfDay.EVENING -> getString(R.string.evening)
                AnalyticsHelper.TimeOfDay.NIGHT -> getString(R.string.night)
            }
            itemView.findViewById<TextView>(R.id.timeName).text = timeName

            // Set count and percentage
            val percentage = if (totalSessions > 0) {
                (count * 100) / totalSessions
            } else {
                0
            }

            val countText = getString(
                R.string.technique_count_format,
                count,
                percentage
            )
            itemView.findViewById<TextView>(R.id.timeCount).text = countText

            // Set progress bar
            val progressBar = itemView.findViewById<View>(R.id.timeProgress)
            val params = progressBar.layoutParams
            params.width = (percentage * 2).coerceAtLeast(5) // Minimum width of 5dp
            progressBar.layoutParams = params

            // Set progress bar color based on time of day
            val colorRes = when (timeOfDay) {
                AnalyticsHelper.TimeOfDay.MORNING -> R.color.time_morning
                AnalyticsHelper.TimeOfDay.AFTERNOON -> R.color.time_afternoon
                AnalyticsHelper.TimeOfDay.EVENING -> R.color.time_evening
                AnalyticsHelper.TimeOfDay.NIGHT -> R.color.time_night
            }
            progressBar.setBackgroundColor(ContextCompat.getColor(requireContext(), colorRes))

            // Add to container
            binding.timeBreakdownContainer.addView(itemView)
        }
    }

    /**
     * Show empty state when no data is available
     */
    private fun showEmptyState() {
        binding.emptyState.visibility = View.VISIBLE
        binding.statisticsContent.visibility = View.GONE
    }

    /**
     * Hide empty state when data is available
     */
    private fun hideEmptyState() {
        binding.emptyState.visibility = View.GONE
        binding.statisticsContent.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    enum class ViewMode {
        WEEKLY, MONTHLY
    }

    companion object {
        fun newInstance(): AnalyticsFragment {
            return AnalyticsFragment()
        }
    }
}