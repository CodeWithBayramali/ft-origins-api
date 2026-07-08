package com.codewithbayramali.ftorigins.geo.dto

data class LocationResolveResponse(
    val h3Index: String,
    val centerLat: Double,
    val centerLng: Double,
    val countryCode: String?,
    val countryName: String?,
    val cityName: String?,
    val region: RegionResponse?
)
