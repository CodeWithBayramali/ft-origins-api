package com.codewithbayramali.ftorigins.geo.dto

import com.codewithbayramali.ftorigins.geo.model.Region
import java.util.UUID

/**
 * The caller's team with its full squad — what the "My Team" management page is built from.
 * [amFounder] decides whether the page is editable (lineups and tactics are founder-only).
 */
data class TeamResponse(
    val regionId: UUID,
    val name: String,
    val nameCustomized: Boolean,
    val neighborhood: String?,
    val district: String?,
    val cityName: String?,
    val countryCode: String?,
    val kitHome: Int?,
    val kitAway: Int?,
    val kitPattern: Int?,
    /** The team crest; null when it has none. Only the founder may change it (PUT /teams/mine/crest). */
    val crest: CrestDto?,
    val founderPlayerId: UUID?,
    /** True when the CALLER holds the founder (manager) seat. */
    val amFounder: Boolean,
    val playerCount: Int,
    val maxMembers: Int,
    /** Players on the pitch per side (1 GK + the rest outfield). */
    val squadSize: Int,
    /** Squad sorted by overall, best first — the order the page lists them in. */
    val members: List<TeamMemberResponse>,
    /** Record from finished matches (points, goals, form). */
    val stats: TeamStatsResponse,
    /** Finished matches, newest first (capped). */
    val recentMatches: List<TeamMatchResponse>,
    /** Scheduled/live matches, soonest first. */
    val upcomingMatches: List<TeamMatchResponse>
) {
    companion object {
        @JvmStatic
        fun of(region: Region, members: List<TeamMemberResponse>, amFounder: Boolean,
               maxMembers: Int, squadSize: Int,
               stats: TeamStatsResponse,
               recentMatches: List<TeamMatchResponse>,
               upcomingMatches: List<TeamMatchResponse>): TeamResponse = TeamResponse(
            regionId = region.id!!,
            name = region.name,
            nameCustomized = region.nameCustomized,
            neighborhood = region.neighborhood,
            district = region.district,
            cityName = region.cityName,
            countryCode = region.countryCode,
            kitHome = region.kitHome,
            kitAway = region.kitAway,
            kitPattern = region.kitPattern,
            crest = CrestDto.from(region),
            founderPlayerId = region.founderPlayerId,
            amFounder = amFounder,
            playerCount = region.playerCount,
            maxMembers = maxMembers,
            squadSize = squadSize,
            members = members,
            stats = stats,
            recentMatches = recentMatches,
            upcomingMatches = upcomingMatches
        )
    }
}
