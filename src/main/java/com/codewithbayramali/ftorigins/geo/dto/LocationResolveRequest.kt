package com.codewithbayramali.ftorigins.geo.dto

import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull

data class LocationResolveRequest(
    @field:NotNull
    @field:DecimalMin("-90.0")
    @field:DecimalMax("90.0")
    val latitude: Double?,

    @field:NotNull
    @field:DecimalMin("-180.0")
    @field:DecimalMax("180.0")
    val longitude: Double?
)
