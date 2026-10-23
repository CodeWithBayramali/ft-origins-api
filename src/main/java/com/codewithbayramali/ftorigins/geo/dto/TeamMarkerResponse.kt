package com.codewithbayramali.ftorigins.geo.dto

import com.codewithbayramali.ftorigins.geo.model.Region
import java.util.UUID

/**
 * A single team on the registration map: the founder's anchor ([centerLat]/[centerLng]) with the join
 * radius (km) drawn as a circle around it, and what the team's card shows — crest colours, name, how
 * full the squad is and where it plays. A registering player sees which teams already exist nearby,
 * which one their pin would join and where they could found a new one. Deliberately lighter than
 * RegionResponse — a map viewport can return hundreds of these, so it carries only what the map needs.
 */
data class TeamMarkerResponse(
    val id: UUID,
    val name: String,
    val centerLat: Double,
    val centerLng: Double,
    val playerCount: Int,
    /** Squad cap (app.team.max-members). A team at the cap is skipped when someone registers nearby. */
    val maxMembers: Int,
    /** Join catchment radius in km (same for every team today — the app.team.join-radius-km config). */
    val radiusKm: Double,
    /** Home kit id into the client KitCatalog, for the crest colours. Null on teams founded before kits. */
    val kitHome: Int?,
    /** The team crest for the card; null when the team has none. */
    val crest: CrestDto?,
    /** Reverse-geocoded place labels, finest first; either may be null. */
    val neighborhood: String?,
    val district: String?
) {
    companion object {
        @JvmStatic
        fun from(region: Region, radiusKm: Double, maxMembers: Int): TeamMarkerResponse = TeamMarkerResponse(
            id = region.id!!,
            name = region.name,
            centerLat = region.centerLat,
            centerLng = region.centerLng,
            playerCount = region.playerCount,
            maxMembers = maxMembers,
            radiusKm = radiusKm,
            kitHome = region.kitHome,
            crest = CrestDto.from(region),
            neighborhood = region.neighborhood,
            district = region.district
        )
    }
}
