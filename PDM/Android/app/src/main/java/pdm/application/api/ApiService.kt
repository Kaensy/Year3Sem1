package pdm.application.api

import pdm.application.model.Tournament
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @POST("auth/login")
    suspend fun login(@Body credentials: LoginRequest): Response<LoginResponse>

    @GET("tournaments")
    suspend fun getTournaments(
        @Header("Authorization") token: String,
        @Query("page") page: Int,
        @Query("limit") limit: Int
    ): Response<TournamentResponse>

    @GET("tournaments/{id}")
    suspend fun getTournamentById(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Tournament>

    @POST("tournaments")
    suspend fun createTournament(
        @Header("Authorization") token: String,
        @Body tournament: Tournament
    ): Response<Tournament>

    @PUT("tournaments/{id}")
    suspend fun updateTournament(
        @Header("Authorization") token: String,
        @Path("id") id: String,
        @Body tournament: Tournament
    ): Response<Tournament>

    @DELETE("tournaments/{id}")
    suspend fun deleteTournament(
        @Header("Authorization") token: String,
        @Path("id") id: String
    ): Response<Unit>
}

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val token: String,
    val user: User
)

data class User(
    val id: String,
    val email: String
)

data class TournamentResponse(
    val tournaments: List<Tournament>,
    val pagination: Pagination
)

data class Pagination(
    val currentPage: Int,
    val totalPages: Int,
    val totalItems: Int,
    val hasMore: Boolean
)