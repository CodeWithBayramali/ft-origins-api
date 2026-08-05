package com.codewithbayramali.ftorigins.match.dto

import com.codewithbayramali.ftorigins.match.model.Formation
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.util.UUID

/** Founder-only lineup submission. */
data class LineupRequest(
    @field:NotNull
    val formation: Formation?,

    // Slot-ordered player ids (index = slot; slot 0 = GK, outfield roles follow the formation).
    // A null entry leaves that slot to a standard-stat AI filler at kickoff.
    @field:NotNull
    @field:Size(min = Formation.SQUAD_SIZE, max = Formation.SQUAD_SIZE)
    val starterPlayerIds: List<UUID?>?
)
