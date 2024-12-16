package pdm.application.ui.tournaments

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import pdm.application.R
import pdm.application.databinding.FragmentTournamentListBinding
import pdm.application.util.NotificationHelper
import pdm.application.util.SessionManager
import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.icu.util.Calendar
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import pdm.application.model.Tournament
import pdm.application.model.TournamentStatus
import pdm.application.util.NetworkManager
import pdm.application.workers.CountdownWorker
import pdm.application.workers.NotificationWorker
import pdm.application.workers.TournamentSyncWorker
import java.util.Date

class TournamentListFragment : Fragment() {
    private var _binding: FragmentTournamentListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TournamentListViewModel by viewModels()
    private lateinit var adapter: TournamentAdapter
    private lateinit var sessionManager: SessionManager
    private lateinit var networkManager: NetworkManager

    private fun setupUiRefresh() {
        lifecycleScope.launch {
            while (isActive) {
                viewModel.loadTournaments()
                delay(60_000) // Refresh every 60 seconds
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = SessionManager(requireContext())
        networkManager = NetworkManager(requireContext())

    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            sendTestNotification()
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTournamentListBinding.inflate(inflater, container, false)
        sessionManager = SessionManager(requireContext())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ensureNotificationPermission()
        setupRecyclerView()
        observeViewModel()
        NotificationHelper(requireContext()).requestNotificationPermission(requireActivity())
        setupUiRefresh()

        viewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            Snackbar.make(
                binding.root,
                if (isConnected) "Online" else "Offline",
                Snackbar.LENGTH_SHORT
            ).setBackgroundTint(
                ContextCompat.getColor(
                    requireContext(),
                    if (isConnected) android.R.color.holo_green_dark
                    else android.R.color.holo_red_dark
                )
            ).show()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.networkAvailable.collect { isAvailable ->
                updateNetworkStatus(isAvailable)
            }
        }

        adapter = TournamentAdapter(
            onItemClick = { tournament ->
                findNavController().navigate(
                    TournamentListFragmentDirections.actionListToEdit(
                        tournament.id,
                        tournament
                    )
                )
            },
            onLocationClick = { lat, lon ->
                // Open Google Maps with the location
                val uri = Uri.parse("geo:$lat,$lon?z=15")
                val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    Snackbar.make(binding.root, "Google Maps not installed", Snackbar.LENGTH_SHORT).show()
                }
            }


        )

        // Set up toolbar
        val toolbar = binding.toolbar
        toolbar.setTitleTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)

        // Set up logout button click listener
        binding.toolbar.findViewById<ImageButton>(R.id.logout_button).setOnClickListener {
            // Cancel any pending callbacks
            _binding?.recyclerView?.adapter = null
            // Clear data and session
            viewLifecycleOwner.lifecycleScope.launch {
                sessionManager.clearSession()
                viewModel.clearLocalData()
                // Navigate after clearing data
                findNavController().navigate(
                    TournamentListFragmentDirections.actionTournamentListToLogin()
                )
            }
        }

        // Set up FAB click listener
        binding.fabAdd.setOnClickListener {
            findNavController().navigate(
                TournamentListFragmentDirections.actionListToEdit(
                    tournamentId = null,
                    tournament = null
                )
            )
        }

        // Set up swipe refresh
        binding.refreshLayout.setOnRefreshListener {
            viewModel.loadTournaments()
        }

        binding.testNotification.setOnClickListener {
            NotificationHelper(requireContext()).showTournamentNotification(
                "Test Notification",
                "This is a test tournament notification",
                1234
            )
        }
        binding.testNotification.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestNotificationPermission()
            } else {
                sendTestNotification()
            }
        }

        binding.testBackgroundTask.setOnClickListener {
            testBackgroundTasks()
        }
    }

    private fun updateNetworkStatus(isAvailable: Boolean) {
        // Update UI based on network status
        val message = if (isAvailable) {
            "Online"
        } else {
            "Offline"
        }

        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_SHORT
        ).setBackgroundTint(
            ContextCompat.getColor(
                requireContext(),
                if (isAvailable) android.R.color.holo_green_dark else android.R.color.holo_red_dark
            )
        ).show()
    }

    private fun testBackgroundTasks() {
        val twoMinutesFromNow = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 2)
        }.time

        val fourMinutesFromNow = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 4)
        }.time

        viewModel.createTournament(
            name = "Test Tournament",
            description = "This tournament will start in 2 minutes",
            participantsCount = 10,
            prizePool = 1000.0,
            isRegistrationOpen = true,
            startDate = twoMinutesFromNow,
            endDate = fourMinutesFromNow,
            latitude = null,
            longitude = null,
            status = TournamentStatus.UPCOMING,
            winner = null
        )

        // Schedule immediate checks
        val workManager = WorkManager.getInstance(requireContext())

        val syncWork = OneTimeWorkRequestBuilder<TournamentSyncWorker>()
            .build()

        val countdownWork = OneTimeWorkRequestBuilder<CountdownWorker>()
            .build()

        // Chain the work and repeat every 30 seconds for 5 minutes
        workManager.beginWith(syncWork)
            .then(countdownWork)
            .enqueue()

        // Schedule more checks after 1 minute
        Handler(Looper.getMainLooper()).postDelayed({
            workManager.beginWith(syncWork)
                .then(countdownWork)
                .enqueue()
        }, 60 * 1000)

        Snackbar.make(
            binding.root,
            "Created test tournament and scheduled background tasks.\n" +
                    "Check notifications and logs for updates.",
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun createTestTournament() {
        val twoMinutesFromNow = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 2)
        }.time

        val fourMinutesFromNow = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 4)
        }.time

        viewModel.createTournament(
            name = "Test Tournament",
            description = "This tournament will start in 2 minutes",
            participantsCount = 10,
            prizePool = 1000.0,
            isRegistrationOpen = true,
            startDate = twoMinutesFromNow,
            endDate = fourMinutesFromNow,
            latitude = null,
            longitude = null,
            status = TournamentStatus.UPCOMING,
            winner = null
        )

        Snackbar.make(
            binding.root,
            "Created test tournament. You should receive notifications:\n" +
                    "1. Every minute (background check)\n" +
                    "2. When tournament starts in 2 minutes",
            Snackbar.LENGTH_LONG
        ).show()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun sendTestNotification() {
        NotificationHelper(requireContext()).showNotification(
            "Test Tournament",
            "This is a test notification for tournaments!",
            1234
        )
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            setHasFixedSize(true)
            itemAnimator = null
            layoutManager = LinearLayoutManager(context)
            adapter = TournamentAdapter(
                onItemClick = { tournament ->
                    findNavController().navigate(
                        TournamentListFragmentDirections.actionListToEdit(
                            tournamentId = tournament.id,
                            tournament = tournament
                        )
                    )
                },
                onLocationClick = { lat, lon ->
                    val uri = Uri.parse("geo:$lat,$lon?z=15")
                    val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    if (mapIntent.resolveActivity(requireActivity().packageManager) != null) {
                        startActivity(mapIntent)
                    } else {
                        Snackbar.make(
                            binding.root,
                            "Google Maps not installed",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
    }


    private fun observeViewModel() {
        viewModel.tournaments.observe(viewLifecycleOwner) { tournaments ->
            (binding.recyclerView.adapter as? TournamentAdapter)?.submitList(tournaments)
            binding.refreshLayout.isRefreshing = false
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                binding.refreshLayout.isRefreshing = false
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.refreshLayout.isRefreshing = isLoading
        }

        viewModel.isConnected.observe(viewLifecycleOwner) { isConnected ->
            binding.networkStatus.setColorFilter(
                ContextCompat.getColor(
                    requireContext(),
                    if (isConnected) android.R.color.holo_green_dark
                    else android.R.color.holo_red_dark
                ),
                PorterDuff.Mode.SRC_IN
            )
        }

        // Initial load
        viewModel.loadTournaments()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupNotificationTest() {
        binding.testNotification.setOnClickListener {
            showNotificationTestDialog()
        }
    }


    private fun showNotificationTestDialog() {
        val options = arrayOf(
            "Registration closing in 1 hour",
            "Tournament starting in 30 minutes",
            "Tournament starting now",
            "Tournament results"
        )

        AlertDialog.Builder(requireContext())
            .setTitle("Test Notifications")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        // Registration reminder
                        NotificationHelper(requireContext()).showRegistrationReminder(
                            "Test Tournament",
                            1,
                            "test-id",
                            "Final call! Registration for Test Tournament closes in less than an hour!"
                        )
                    }
                    1 -> {
                        // Starting soon
                        NotificationHelper(requireContext()).showTournamentStartingReminder(
                            "Test Tournament",
                            System.currentTimeMillis() + 30 * 60 * 1000,
                            "test-id",
                            "Almost time! Test Tournament starts in 30 minutes"
                        )
                    }
                    2 -> {
                        // Starting now
                        NotificationHelper(requireContext()).showTournamentStartingReminder(
                            "Test Tournament",
                            System.currentTimeMillis(),
                            "test-id",
                            "Test Tournament is starting now!"
                        )
                    }
                    3 -> {
                        // Results
                        NotificationHelper(requireContext()).showTournamentResultNotification(
                            "Test Tournament",
                            "Test Winner",
                            "test-id",
                            "Congratulations to Test Winner for winning Test Tournament!"
                        )
                    }
                }
            }
            .show()
    }
    private fun testNotifications() {
        val now = Date()
        val calendar = Calendar.getInstance()

        // Create test tournaments with different timings
        val testTournaments = listOf(
            // Starting in 24 hours
            Tournament(
                id = "test1",
                name = "24h Tournament",
                description = "Starts in 24 hours",
                startDate = Date(now.time + 24 * 60 * 60 * 1000), // 24 hours from now
                endDate = Date(now.time + 48 * 60 * 60 * 1000),
                participantsCount = 10,
                prizePool = 1000.0,
                isRegistrationOpen = true,
                winner = null,
                status = TournamentStatus.UPCOMING,
                userId = "1"
            ),
            // Starting in 2 hours
            Tournament(
                id = "test2",
                name = "2h Tournament",
                description = "Starts in 2 hours",
                startDate = Date(now.time + 2 * 60 * 60 * 1000), // 2 hours from now
                endDate = Date(now.time + 26 * 60 * 60 * 1000),
                participantsCount = 10,
                prizePool = 1000.0,
                isRegistrationOpen = true,
                winner = null,
                status = TournamentStatus.UPCOMING,
                userId = "1"
            ),
            // Starting in 30 minutes
            Tournament(
                id = "test3",
                name = "30min Tournament",
                description = "Starts in 30 minutes",
                startDate = Date(now.time + 30 * 60 * 1000), // 30 minutes from now
                endDate = Date(now.time + 24 * 60 * 60 * 1000),
                participantsCount = 10,
                prizePool = 1000.0,
                isRegistrationOpen = true,
                winner = null,
                status = TournamentStatus.UPCOMING,
                userId = "1"
            ),
            // Completed tournament with winner
            Tournament(
                id = "test4",
                name = "Completed Tournament",
                description = "Already finished",
                startDate = Date(now.time - 24 * 60 * 60 * 1000), // 24 hours ago
                endDate = Date(now.time - 12 * 60 * 60 * 1000),
                participantsCount = 10,
                prizePool = 1000.0,
                isRegistrationOpen = false,
                winner = "Test Winner",
                status = TournamentStatus.COMPLETED,
                userId = "1"
            )
        )

    }
    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Permissions granted, proceed with notifications
            Log.d("TournamentListFragment", "Notification permission granted")
        }
    }

}