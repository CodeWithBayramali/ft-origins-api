package com.codewithbayramali.ftorigins.player.dto

import jakarta.validation.constraints.NotNull
import java.util.UUID

/** Request to run a single training drill, identified by its catalog id. */
data class TrainRequest(
    @field:NotNull
    val drillId: UUID?
)
