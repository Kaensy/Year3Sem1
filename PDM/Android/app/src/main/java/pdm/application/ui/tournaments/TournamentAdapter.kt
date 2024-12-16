package pdm.application.ui.tournaments

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import pdm.application.databinding.ItemTournamentBinding
import pdm.application.model.Tournament
import pdm.application.model.TournamentStatus
import java.text.SimpleDateFormat
import java.util.Locale

class TournamentAdapter(
    private val onItemClick: (Tournament) -> Unit,
    private val onLocationClick: (Double, Double) -> Unit
) : ListAdapter<Tournament, TournamentAdapter.TournamentViewHolder>(TournamentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TournamentViewHolder {
        val binding = ItemTournamentBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TournamentViewHolder(binding, onItemClick, onLocationClick)  // Pass both click handlers
    }

    override fun onBindViewHolder(holder: TournamentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class TournamentViewHolder(
        private val binding: ItemTournamentBinding,
        private val onItemClick: (Tournament) -> Unit,
        private val onLocationClick: (Double, Double) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        private val dateFormatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        private val dateTimeFormatter = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())

        fun bind(tournament: Tournament) {
            binding.apply {
                tournamentName.text = tournament.name
                tournamentDescription.text = tournament.description

                registrationStatus.apply {
                    val isOpen = tournament.isRegistrationOpen
                    text = if (isOpen) "Registration Open" else "Registration Closed"
                    setTextColor(context.getColor(
                        if (isOpen) android.R.color.holo_green_dark
                        else android.R.color.holo_red_dark
                    ))
                }

                // Format dates with start time
                val formattedDates = "${dateTimeFormatter.format(tournament.startDate)} - " +
                        "${dateFormatter.format(tournament.endDate)}"
                tournamentDates.text = formattedDates

                tournamentStatus.text = tournament.status.name
                tournamentParticipants.text = "${tournament.participantsCount} participants"
                tournamentPrize.text = "Prize: $${tournament.prizePool}"

                registrationStatus.text = if (tournament.isRegistrationOpen) "Registration Open" else "Registration Closed"

                // Handle location click
                // Show location if available
                if (tournament.latitude != null && tournament.longitude != null) {
                    tournamentLocation.apply {
                        visibility = View.VISIBLE
                        text = "Location: %.4f, %.4f".format(
                            tournament.latitude,
                            tournament.longitude
                        )
                        setOnClickListener {
                            onLocationClick(tournament.latitude, tournament.longitude)
                        }
                    }
                } else {
                    tournamentLocation.visibility = View.GONE
                }

                // Handle item click
                root.setOnClickListener { onItemClick(tournament) }
                tournamentStatus.apply {
                    text = tournament.status.name
                    setBackgroundColor(when(tournament.status) {
                        TournamentStatus.UPCOMING -> Color.BLUE
                        TournamentStatus.IN_PROGRESS -> Color.GREEN
                        TournamentStatus.COMPLETED -> Color.RED
                    })
                }
                // Handle winner text visibility
                if (tournament.winner != null) {
                    tournamentWinner.visibility = View.VISIBLE
                    tournamentWinner.text = "Winner: ${tournament.winner}"
                } else {
                    tournamentWinner.visibility = View.GONE
                }
            }
        }
    }

    class TournamentDiffCallback : DiffUtil.ItemCallback<Tournament>() {
        override fun areItemsTheSame(oldItem: Tournament, newItem: Tournament): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Tournament, newItem: Tournament): Boolean {
            return oldItem.id == newItem.id &&
                    oldItem.name == newItem.name &&
                    oldItem.status == newItem.status &&
                    oldItem.startDate == newItem.startDate &&
                    oldItem.endDate == newItem.endDate &&
                    oldItem.participantsCount == newItem.participantsCount &&
                    oldItem.prizePool == newItem.prizePool &&
                    oldItem.isRegistrationOpen == newItem.isRegistrationOpen &&
                    oldItem.winner == newItem.winner
        }
    }
}
