package com.codewithbayramali.ftorigins.match.dto

import java.util.UUID

/** Ack for a queued substitution: it takes effect at match minute [applyMinute]. */
data class SubstitutionResponse(
    val matchId: UUID,
    val regionId: UUID,
    val slot: Int,
    val applyMinute: Int,
    val inPlayerId: UUID
)
