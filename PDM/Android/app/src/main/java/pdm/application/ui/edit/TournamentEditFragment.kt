package pdm.application.ui.edit

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.snackbar.Snackbar
import pdm.application.databinding.FragmentTournamentEditBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TournamentEditFragment : Fragment() {
    private var _binding: FragmentTournamentEditBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TournamentEditViewModel by viewModels()
    private val args: TournamentEditFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTournamentEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDatePickers()

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }


        // Get tournament from navigation arguments
        args.tournament?.let { tournament ->
            binding.nameInput.setText(tournament.name)
            binding.descriptionInput.setText(tournament.description)
            binding.participantsInput.setText(tournament.participantsCount.toString())
            binding.prizeInput.setText(tournament.prizePool.toString())
            binding.registrationSwitch.isChecked = tournament.isRegistrationOpen
            viewModel.setStartDate(tournament.startDate)
            viewModel.setEndDate(tournament.endDate)
            // Update date inputs
            binding.startDateInput.setText(formatDate(tournament.startDate))
            binding.endDateInput.setText(formatDate(tournament.endDate))
        }

        binding.saveButton.setOnClickListener {
            saveTournament()
        }

        observeViewModel()
    }

    private fun setupDatePickers() {
        val calendar = Calendar.getInstance()

        binding.startDateInput.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    viewModel.setStartDate(calendar.time)
                    binding.startDateInput.setText(formatDate(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        binding.endDateInput.setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    calendar.set(year, month, day)
                    viewModel.setEndDate(calendar.time)
                    binding.endDateInput.setText(formatDate(calendar.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun formatDate(date: Date): String {
        return SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
    }


    private fun observeViewModel() {
        viewModel.tournament.observe(viewLifecycleOwner) { tournament ->
            tournament?.let {
                binding.nameInput.setText(it.name)
                binding.descriptionInput.setText(it.description)
                binding.participantsInput.setText(it.participantsCount.toString())
                binding.prizeInput.setText(it.prizePool.toString())
                binding.registrationSwitch.isChecked = it.isRegistrationOpen
            }
        }

        viewModel.saveResult.observe(viewLifecycleOwner) { success ->
            if (success) {
                findNavController().navigateUp()
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun saveTournament() {
        val name = binding.nameInput.text.toString()
        val description = binding.descriptionInput.text.toString()
        val participants = binding.participantsInput.text.toString().toIntOrNull() ?: 0
        val prizePool = binding.prizeInput.text.toString().toDoubleOrNull() ?: 0.0
        val isRegistrationOpen = binding.registrationSwitch.isChecked

        if (name.isBlank() || description.isBlank()) {
            Snackbar.make(binding.root, "Please fill in all required fields", Snackbar.LENGTH_LONG).show()
            return
        }

        // Pass the existing tournament ID if editing
        args.tournament?.let { existingTournament ->
            viewModel.saveTournament(
                id = existingTournament.id,
                name = name,
                description = description,
                participantsCount = participants,
                prizePool = prizePool,
                isRegistrationOpen = isRegistrationOpen,
                startDate = existingTournament.startDate,
                endDate = existingTournament.endDate
            )
        } ?: viewModel.saveTournament(
            name = name,
            description = description,
            participantsCount = participants,
            prizePool = prizePool,
            isRegistrationOpen = isRegistrationOpen
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}