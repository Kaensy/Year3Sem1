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
    val userId: String
) : Parcelable {
    companion object {
        val CREATOR = object : Parcelable.Creator<Tournament> {
            override fun createFromParcel(source: android.os.Parcel): Tournament {
                return Tournament(
                    id = source.readString() ?: "",
                    name = source.readString() ?: "",
                    description = source.readString() ?: "",
                    startDate = Date(source.readLong()),
                    endDate = Date(source.readLong()),
                    participantsCount = source.readInt(),
                    prizePool = source.readDouble(),
                    isRegistrationOpen = source.readInt() == 1,
                    winner = source.readString(),
                    status = TournamentStatus.valueOf(source.readString() ?: TournamentStatus.UPCOMING.name),
                    userId = source.readString() ?: ""
                )
            }

            override fun newArray(size: Int): Array<Tournament?> {
                return arrayOfNulls(size)
            }
        }
    }
}

@Parcelize
enum class TournamentStatus : Parcelable {
    @SerializedName("UPCOMING")
    UPCOMING,
    @SerializedName("IN_PROGRESS")
    IN_PROGRESS,
    @SerializedName("COMPLETED")
    COMPLETED
}