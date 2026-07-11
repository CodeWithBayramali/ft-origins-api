package com.codewithbayramali.ftorigins.geo.dto

data class LocationResolveResponse(
    val h3Index: String,
    val centerLat: Double,
    val centerLng: Double,
    val countryCode: String?,
    val countryName: String?,
    val cityName: String?,
    /** The team a pin here would JOIN (null when the pin would found a new team). */
    val region: RegionResponse?,
    /** True when no joinable team is in range → the registering player would FOUND the team and
     *  therefore gets to pick its kit + pattern. False → joining; the kit is locked to [region]. */
    val willFoundTeam: Boolean
)
