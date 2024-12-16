package pdm.application.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Tournament(
    val id: String,
    val name: String,
    val description: String,
    val startDate: Date,
    val endDate: Date,
    val participantsCount: Int,
    val prizePool: Double,
    val isRegistrationOpen: Boolean,
    val winner: String?,
    val status: TournamentStatus,
    val userId: String,
    val latitude: Double? = null,
    val longitude: Double? = null
) : Parcelable {}

@Parcelize
enum class TournamentStatus : Parcelable {
    @SerializedName("UPCOMING")
    UPCOMING,
    @SerializedName("IN_PROGRESS")
    IN_PROGRESS,
    @SerializedName("COMPLETED")
    COMPLETED
}

fun determineTournamentStatus(startDate: Date, endDate: Date): TournamentStatus {
    val currentDate = Date()
    return when {
        currentDate.before(startDate) -> TournamentStatus.UPCOMING
        currentDate.after(endDate) -> TournamentStatus.COMPLETED
        else -> TournamentStatus.IN_PROGRESS
    }
}