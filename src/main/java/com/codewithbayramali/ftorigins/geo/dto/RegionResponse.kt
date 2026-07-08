package com.codewithbayramali.ftorigins.geo.dto

import com.codewithbayramali.ftorigins.geo.model.Region
import java.util.UUID

data class RegionResponse(
    val id: UUID,
    val name: String,
    val nameCustomized: Boolean,
    val playerCount: Int,
    val countryCode: String?,
    val cityName: String?,
    val district: String?,
    val neighborhood: String?,
    val centerLat: Double,
    val centerLng: Double
) {
    companion object {
        @JvmStatic
        fun from(region: Region): RegionResponse = RegionResponse(
            id = region.id!!,
            name = region.name,
            nameCustomized = region.nameCustomized,
            playerCount = region.playerCount,
            countryCode = region.countryCode,
            cityName = region.cityName,
            district = region.district,
            neighborhood = region.neighborhood,
            centerLat = region.centerLat,
            centerLng = region.centerLng
        )
    }
}
