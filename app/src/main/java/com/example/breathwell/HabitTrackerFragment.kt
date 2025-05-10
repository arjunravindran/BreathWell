package com.example.breathwell

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.breathwell.databinding.FragmentHabitTrackerBinding
import com.example.breathwell.viewmodel.BreathingViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class HabitTrackerFragment : Fragment() {

    private var _binding: FragmentHabitTrackerBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: BreathingViewModel
    private var currentYearMonth = YearMonth.now()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHabitTrackerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[BreathingViewModel::class.java]

        setupCalendarNavigation()
        observeViewModel()
        renderCalendar(currentYearMonth)
    }

    private fun setupCalendarNavigation() {
        binding.previousMonthButton.setOnClickListener {
            currentYearMonth = currentYearMonth.minusMonths(1)
            updateMonthTitle()
            renderCalendar(currentYearMonth)
        }

        binding.nextMonthButton.setOnClickListener {
            currentYearMonth = currentYearMonth.plusMonths(1)
            updateMonthTitle()
            renderCalendar(currentYearMonth)
        }

        updateMonthTitle()
    }

    private fun updateMonthTitle() {
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy")
        binding.monthTitle.text = currentYearMonth.format(formatter)
    }

    private fun observeViewModel() {
        this.viewModel.completedDates.observe(viewLifecycleOwner) { _ ->
            // Update calendar with completed dates
            renderCalendar(currentYearMonth)
        }

        viewModel.getCurrentStreak().observe(viewLifecycleOwner) { streak ->
            binding.currentStreakValue.text = streak.toString()
        }
    }

    private fun renderCalendar(yearMonth: YearMonth) {
        // Clear existing calendar days
        binding.calendarGrid.removeAllViews()

        val monthLength = yearMonth.lengthOfMonth()
        val firstDayOfMonth = yearMonth.atDay(1).dayOfWeek.value % 7 // 0 for Sunday

        // Calculate the number of rows needed
        val totalDays = firstDayOfMonth + monthLength
        val rowsNeeded = (totalDays + 6) / 7 // Round up

        // Add empty spaces for days before the 1st of the month
        for (i in 0 until firstDayOfMonth) {
            val emptyView = layoutInflater.inflate(R.layout.item_calendar_empty, binding.calendarGrid, false)
            binding.calendarGrid.addView(emptyView)
        }

        // Add day cells for each day of the month
        val today = LocalDate.now()

        for (day in 1..monthLength) {
            val date = yearMonth.atDay(day)
            val dayView = layoutInflater.inflate(R.layout.item_calendar_day, binding.calendarGrid, false)

            // Set day number
            dayView.findViewById<TextView>(R.id.dayNumber).text = day.toString()

            // Highlight today
            if (date.equals(today)) {
                dayView.findViewById<View>(R.id.dayBackground).setBackgroundResource(R.drawable.calendar_today_bg)
            }

            // Observe if this date is completed
            viewModel.isDateCompleted(date).observe(viewLifecycleOwner) { isCompleted ->
                if (isCompleted) {
                    dayView.findViewById<View>(R.id.completionIndicator).visibility = View.VISIBLE
                } else {
                    dayView.findViewById<View>(R.id.completionIndicator).visibility = View.INVISIBLE

                    // If date is in the past and not completed, mark as missed
                    if (date.isBefore(today)) {
                        dayView.findViewById<View>(R.id.missedIndicator).visibility = View.VISIBLE
                    }
                }
            }

            binding.calendarGrid.addView(dayView)
        }

        // Add filler views for remaining cells in the grid to complete the matrix
        val totalCells = rowsNeeded * 7
        val fillerCellsNeeded = totalCells - (firstDayOfMonth + monthLength)
        for (i in 0 until fillerCellsNeeded) {
            val emptyView = layoutInflater.inflate(R.layout.item_calendar_empty, binding.calendarGrid, false)
            binding.calendarGrid.addView(emptyView)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}