package com.codewithbayramali.ftorigins.geo.dto

import com.codewithbayramali.ftorigins.geo.model.Region

/**
 * A single team's footprint on the registration map: the founder's anchor ([centerLat]/[centerLng])
 * and the join radius (km) drawn as a circle around it, so a registering player can see which teams
 * already exist nearby and where they could found a new one. Deliberately lighter than RegionResponse
 * — a map viewport can return hundreds of these, so it carries only what the marker needs.
 */
data class TeamMarkerResponse(
    val name: String,
    val centerLat: Double,
    val centerLng: Double,
    val playerCount: Int,
    /** Join catchment radius in km (same for every team today — the app.team.join-radius-km config). */
    val radiusKm: Double
) {
    companion object {
        @JvmStatic
        fun from(region: Region, radiusKm: Double): TeamMarkerResponse = TeamMarkerResponse(
            name = region.name,
            centerLat = region.centerLat,
            centerLng = region.centerLng,
            playerCount = region.playerCount,
            radiusKm = radiusKm
        )
    }
}
