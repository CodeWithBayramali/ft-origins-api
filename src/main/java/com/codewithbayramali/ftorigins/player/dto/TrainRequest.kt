package com.codewithbayramali.ftorigins.player.dto

import com.codewithbayramali.ftorigins.player.model.TrainableAttribute
import jakarta.validation.constraints.NotNull

data class TrainRequest(
    @field:NotNull
    val attribute: TrainableAttribute?
)
