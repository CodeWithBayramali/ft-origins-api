package com.codewithbayramali.ftorigins.match.dto

import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

/** Founder-only: schedule a friendly against another team. The founder's own team plays at home. */
data class CreateFriendlyRequest(
    @field:NotNull
    val opponentRegionId: UUID?,

    // Must also leave enough lead time for the lineup window; that rule (and the far-future cap)
    // lives in MatchService where the configured minutes are known.
    @field:NotNull
    @field:Future
    val kickoffAt: Instant?
)
