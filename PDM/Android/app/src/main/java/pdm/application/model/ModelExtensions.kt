package pdm.application.model

fun Tournament.toEntity() = TournamentEntity(
    id = id,
    name = name,
    description = description,
    startDate  = startDate ,
    endDate = endDate,
    participantsCount = participantsCount,
    prizePool = prizePool,
    isRegistrationOpen = isRegistrationOpen,
    winner = winner,
    status = status,
    userId = userId,
    version = 1,
    latitude = latitude,
    longitude = longitude
)

fun TournamentEntity.toDomain() = Tournament(
    id = id,
    name = name,
    description = description,
    startDate  = startDate ,
    endDate = endDate,
    participantsCount = participantsCount,
    prizePool = prizePool,
    isRegistrationOpen = isRegistrationOpen,
    winner = winner,
    status = status,
    userId = userId,
    latitude = latitude,
    longitude = longitude
)