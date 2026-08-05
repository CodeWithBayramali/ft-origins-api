package com.codewithbayramali.ftorigins.auth.dto

/** Answer to [AvailabilityRequest]: a field that was not asked about is reported as not taken. */
data class AvailabilityResponse(
    val emailTaken: Boolean,
    val usernameTaken: Boolean
)
