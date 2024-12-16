package pdm.application.ui.edit

import android.R
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import pdm.application.databinding.FragmentTournamentEditBinding
import pdm.application.model.TournamentStatus
import pdm.application.model.determineTournamentStatus
import java.text.SimpleDateFormat
import java.util.*

class TournamentEditFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentTournamentEditBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TournamentEditViewModel by viewModels()
    private val args: TournamentEditFragmentArgs by navArgs()  // Add this line

    private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val startCalendar = Calendar.getInstance()
    private val endCalendar = Calendar.getInstance()

    private var googleMap: GoogleMap? = null
    private var selectedLatitude: Double? = null
    private var selectedLongitude: Double? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTournamentEditBinding.inflate(inflater, container, false)
        // Initialize map
        binding.mapView.onCreate(savedInstanceState)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Release map first to avoid memory leaks
        binding.mapView.onDestroy()
        // Then clear the binding
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDateTimePickers()
        setupLocationSelection()

        // Initialize map
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        // Initialize with tournament data if editing
        args.tournament?.let { tournament ->
            // Fill in the fields with tournament data
            binding.apply {
                nameInput.setText(tournament.name)
                descriptionInput.setText(tournament.description)
                participantsInput.setText(tournament.participantsCount.toString())
                prizeInput.setText(tournament.prizePool.toString())
                registrationSwitch.isChecked = tournament.isRegistrationOpen

                // Set location if available
                selectedLatitude = tournament.latitude
                selectedLongitude = tournament.longitude
                if (selectedLatitude != null && selectedLongitude != null) {
                    updateLocationDisplay()
                }

                // Set start date
                startCalendar.time = tournament.startDate
                startDateInput.setText(dateFormatter.format(startCalendar.time))
                startTimeInput.setText(timeFormatter.format(startCalendar.time))

                // Set end date
                endCalendar.time = tournament.endDate
                endDateInput.setText(dateFormatter.format(endCalendar.time))

                // Show winner field only if tournament is completed
                val currentStatus = determineTournamentStatus(tournament.startDate, tournament.endDate)
                winnerInput.setText(tournament.winner)
                winnerLayout.visibility = if (currentStatus == TournamentStatus.COMPLETED) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
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

        binding.saveButton.setOnClickListener {
            saveTournament()
        }

        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }


    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        // Load existing location if available
        if (selectedLatitude != null && selectedLongitude != null) {
            val position = LatLng(selectedLatitude!!, selectedLongitude!!)
            googleMap.clear()
            googleMap.addMarker(MarkerOptions().position(position))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
        }
    }

    private fun updateMapLocation() {
        if (selectedLatitude != null && selectedLongitude != null && googleMap != null) {
            val location = LatLng(selectedLatitude!!, selectedLongitude!!)
            googleMap?.clear()
            googleMap?.addMarker(MarkerOptions().position(location))
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
            binding.mapView.visibility = View.VISIBLE
        } else {
            binding.mapView.visibility = View.VISIBLE
            // Show default location or world view if no location selected
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(0.0, 0.0), 0f))
        }
    }

    private fun setupLocationSelection() {
        binding.selectLocationButton.setOnClickListener {
            findNavController().navigate(
                TournamentEditFragmentDirections.actionEditToSelectLocation()
            )
        }

        // Update to properly store location values
        setFragmentResultListener("location_request") { _, bundle ->
            selectedLatitude = bundle.getDouble("latitude")
            selectedLongitude = bundle.getDouble("longitude")
            updateLocationDisplay()

            // Update map marker
            googleMap?.clear()
            val position = LatLng(selectedLatitude!!, selectedLongitude!!)
            googleMap?.addMarker(MarkerOptions().position(position))
            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 15f))
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        binding.mapView.onSaveInstanceState(outState)
    }

    private fun updateLocationDisplay() {
        if (selectedLatitude != null && selectedLongitude != null) {
            binding.locationText.apply {
                visibility = View.VISIBLE
                text = "Location selected: %.4f, %.4f".format(
                    selectedLatitude,
                    selectedLongitude
                )
            }
        } else {
            binding.locationText.visibility = View.GONE
        }
    }

    private fun setupDateTimePickers() {
        binding.startDateInput.setOnClickListener { showStartDatePicker() }
        binding.startTimeInput.setOnClickListener { showStartTimePicker() }
        binding.endDateInput.setOnClickListener { showEndDatePicker() }
        updateDateTimeDisplays()
    }

    private fun showStartDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                startCalendar.set(Calendar.YEAR, year)
                startCalendar.set(Calendar.MONTH, month)
                startCalendar.set(Calendar.DAY_OF_MONTH, day)
                updateDateTimeDisplays()
            },
            startCalendar.get(Calendar.YEAR),
            startCalendar.get(Calendar.MONTH),
            startCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showStartTimePicker() {
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                startCalendar.set(Calendar.HOUR_OF_DAY, hour)
                startCalendar.set(Calendar.MINUTE, minute)
                updateDateTimeDisplays()
            },
            startCalendar.get(Calendar.HOUR_OF_DAY),
            startCalendar.get(Calendar.MINUTE),
            true // 24-hour format
        ).show()
    }

    private fun showEndDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                endCalendar.set(Calendar.YEAR, year)
                endCalendar.set(Calendar.MONTH, month)
                endCalendar.set(Calendar.DAY_OF_MONTH, day)
                updateDateTimeDisplays()
            },
            endCalendar.get(Calendar.YEAR),
            endCalendar.get(Calendar.MONTH),
            endCalendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateTimeDisplays() {
        binding.startDateInput.setText(dateFormatter.format(startCalendar.time))
        binding.startTimeInput.setText(timeFormatter.format(startCalendar.time))
        binding.endDateInput.setText(dateFormatter.format(endCalendar.time))
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

        endCalendar.set(Calendar.HOUR_OF_DAY, 23)
        endCalendar.set(Calendar.MINUTE, 59)
        endCalendar.set(Calendar.SECOND, 59)

        // Determine status based on dates
        val status = determineTournamentStatus(startCalendar.time, endCalendar.time)

        // Only allow winner if the tournament is completed
        val winner = if (status == TournamentStatus.COMPLETED) {
            binding.winnerInput.text.toString().takeIf { it.isNotBlank() }
        } else {
            null
        }

        args.tournament?.let { existingTournament ->
            viewModel.saveTournament(
                existingTournament.copy(
                    name = name,
                    description = description,
                    participantsCount = participants,
                    prizePool = prizePool,
                    isRegistrationOpen = isRegistrationOpen,
                    startDate = startCalendar.time,
                    endDate = endCalendar.time,
                    latitude = selectedLatitude,
                    longitude = selectedLongitude,
                    status = status,
                    winner = winner
                )
            )
        } ?: run {
            viewModel.createTournament(
                name = name,
                description = description,
                participantsCount = participants,
                prizePool = prizePool,
                isRegistrationOpen = isRegistrationOpen,
                startDate = startCalendar.time,
                endDate = endCalendar.time,
                latitude = selectedLatitude,
                longitude = selectedLongitude,
                status = status,
                winner = winner
            )
        }
    }
}