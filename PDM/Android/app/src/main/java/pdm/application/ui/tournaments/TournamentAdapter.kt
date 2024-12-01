package pdm.application.ui.tournaments

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pdm.application.databinding.ItemTournamentBinding
import pdm.application.model.Tournament
import java.text.SimpleDateFormat
import java.util.Locale

class TournamentAdapter(
    private val onItemClick: (Tournament) -> Unit
) : ListAdapter<Tournament, TournamentAdapter.TournamentViewHolder>(TournamentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TournamentViewHolder {
        val binding = ItemTournamentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TournamentViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: TournamentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TournamentViewHolder(
        private val binding: ItemTournamentBinding,
        private val onItemClick: (Tournament) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

        fun bind(tournament: Tournament) {
            binding.apply {
                tournamentName.text = tournament.name
                tournamentDescription.text = tournament.description
                tournamentDates.text = "${dateFormat.format(tournament.startDate)} - ${dateFormat.format(tournament.endDate)}"
                tournamentStatus.text = tournament.status.name
                tournamentParticipants.text = "${tournament.participantsCount} participants"
                tournamentPrize.text = "Prize: $${tournament.prizePool}"

                // Show registration status
                registrationStatus.text = if (tournament.isRegistrationOpen) "Registration Open" else "Registration Closed"

                // Show winner if available
                tournament.winner?.let {
                    tournamentWinner.text = "Winner: $it"
                    tournamentWinner.visibility = android.view.View.VISIBLE
                } ?: run {
                    tournamentWinner.visibility = android.view.View.GONE
                }

                root.setOnClickListener { onItemClick(tournament) }
            }
        }
    }

    class TournamentDiffCallback : DiffUtil.ItemCallback<Tournament>() {
        override fun areItemsTheSame(oldItem: Tournament, newItem: Tournament): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Tournament, newItem: Tournament): Boolean {
            return oldItem == newItem
        }
    }
}