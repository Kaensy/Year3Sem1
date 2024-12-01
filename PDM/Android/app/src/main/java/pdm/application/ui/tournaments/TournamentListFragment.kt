package pdm.application.ui.tournaments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import pdm.application.databinding.FragmentTournamentListBinding

class TournamentListFragment : Fragment() {
    private var _binding: FragmentTournamentListBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TournamentListViewModel by viewModels()
    private lateinit var adapter: TournamentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTournamentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()

        binding.fabAdd.setOnClickListener {
            findNavController().navigate(
                TournamentListFragmentDirections.actionListToEdit(null)
            )
        }

        binding.refreshLayout.setOnRefreshListener {
            viewModel.loadTournaments()
        }
    }
    private fun setupRecyclerView() {
        adapter = TournamentAdapter { tournament ->
            findNavController().navigate(
                TournamentListFragmentDirections.actionListToEdit(tournament.id, tournament)
            )
        }
        binding.recyclerView.adapter = adapter

        // Set up refresh listener
        binding.refreshLayout.setOnRefreshListener {
            viewModel.loadTournaments()
        }

        // Observe live data for tournaments
        viewModel.tournaments.observe(viewLifecycleOwner) { tournaments ->
            adapter.submitList(tournaments)
            binding.refreshLayout.isRefreshing = false
        }

        // Load tournaments initially
        viewModel.loadTournaments()
    }



    private fun observeViewModel() {
        viewModel.tournaments.observe(viewLifecycleOwner) { tournaments ->
            adapter.submitList(tournaments) {
                // This callback is called after the list update is complete
                binding.recyclerView.smoothScrollToPosition(0)
            }
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}