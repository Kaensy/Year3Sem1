package pdm.application.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import pdm.application.model.TournamentEntity

@Dao
interface TournamentDao {
    @Query("SELECT * FROM tournaments ORDER BY startDate DESC, name ASC")
    fun getAllTournaments(): Flow<List<TournamentEntity>>

    @Query("SELECT * FROM tournaments WHERE id = :id")
    suspend fun getTournamentById(id: String): TournamentEntity?

    @Query("SELECT * FROM tournaments WHERE hasPendingChanges = 1")
    suspend fun getTournamentsWithPendingChanges(): List<TournamentEntity>

    @Query("UPDATE tournaments SET hasPendingChanges = :hasPending WHERE id = :id")
    suspend fun updatePendingStatus(id: String, hasPending: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournament(tournament: TournamentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTournaments(tournaments: List<TournamentEntity>)

    @Update
    suspend fun updateTournament(tournament: TournamentEntity)

    @Delete
    suspend fun deleteTournament(tournament: TournamentEntity)

    @Query("DELETE FROM tournaments")
    suspend fun deleteAllTournaments()

}