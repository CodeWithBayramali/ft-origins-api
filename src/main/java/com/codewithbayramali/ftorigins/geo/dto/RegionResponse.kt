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
    val centerLng: Double,
    // Team kit: colour ids into the client KitCatalog (0-9) + the shared pattern id (0-6),
    // so the client can render this team's players. Null on teams founded before kits/patterns.
    val kitHome: Int?,
    val kitAway: Int?,
    val kitPattern: Int?
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
            centerLng = region.centerLng,
            kitHome = region.kitHome,
            kitAway = region.kitAway,
            kitPattern = region.kitPattern
        )
    }
}
