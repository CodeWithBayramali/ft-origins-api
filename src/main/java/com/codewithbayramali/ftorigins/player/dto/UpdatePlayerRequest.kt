package com.codewithbayramali.ftorigins.player.dto

import com.codewithbayramali.ftorigins.player.model.Foot
import jakarta.validation.Valid
import jakarta.validation.constraints.Size

/** Character-creation / profile edit. All fields optional — only non-null ones are applied. */
data class UpdatePlayerRequest(
    @field:Size(max = 60)
    val displayName: String? = null,

    val preferredFoot: Foot? = null,

    @field:Valid
    val appearance: AppearanceRequest? = null
)
