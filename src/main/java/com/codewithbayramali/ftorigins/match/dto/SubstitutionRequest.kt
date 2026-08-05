package com.codewithbayramali.ftorigins.match.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.util.UUID

/** Founder-only live substitution: [inPlayerId] takes over lineup slot [outSlot] next minute. */
data class SubstitutionRequest(
    @field:NotNull
    @field:Min(0)
    @field:Max(5) // Formation.SQUAD_SIZE - 1
    val outSlot: Int?,

    @field:NotNull
    val inPlayerId: UUID?
)
