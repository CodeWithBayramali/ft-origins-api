package com.codewithbayramali.ftorigins.auth.dto

import jakarta.validation.constraints.Size

/**
 * Pre-registration availability probe: the wizard asks whether an email (step 1) or username
 * (character step) is already registered BEFORE the player walks the rest of the flow, so a taken
 * value is reported on the form where it was typed instead of at the final CREATE. Both fields are
 * optional — a blank/omitted field is simply not checked. Sent as a POST body (never query params)
 * so emails don't leak into URLs/access logs.
 */
data class AvailabilityRequest(
    @field:Size(max = 100)
    val email: String? = null,

    @field:Size(max = 50)
    val username: String? = null
)
