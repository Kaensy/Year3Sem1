package pdm.application.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.Date

@Entity(tableName = "tournaments")
data class TournamentEntity(
    @PrimaryKey
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
    val version: Int,
    val lastUpdated: Long = System.currentTimeMillis(),
    val hasPendingChanges: Boolean = false,
    val latitude: Double? = null,
    val longitude: Double? = null
)

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStatus(status: TournamentStatus): String {
        return status.name
    }

    @TypeConverter
    fun toStatus(status: String): TournamentStatus {
        return TournamentStatus.valueOf(status)
    }
}