package com.codewithbayramali.ftorigins.match.dto

import com.codewithbayramali.ftorigins.geo.model.Region
import com.codewithbayramali.ftorigins.match.model.Match
import com.codewithbayramali.ftorigins.match.model.MatchStatus
import com.codewithbayramali.ftorigins.match.model.MatchType
import java.time.Instant
import java.util.UUID

/** One side of a match, with everything the client needs to render the team. */
data class MatchTeamResponse(
    val regionId: UUID,
    val name: String,
    val kitHome: Int?,
    val kitAway: Int?,
    val kitPattern: Int?,
    val founderPlayerId: UUID?
) {
    companion object {
        @JvmStatic
        fun from(region: Region): MatchTeamResponse = MatchTeamResponse(
            regionId = region.id!!,
            name = region.name,
            kitHome = region.kitHome,
            kitAway = region.kitAway,
            kitPattern = region.kitPattern,
            founderPlayerId = region.founderPlayerId
        )
    }
}

/** Match card / header view. [simMinute] is the live frontier (0 before kickoff, 90+ when done). */
data class MatchResponse(
    val id: UUID,
    val matchType: MatchType,
    val status: MatchStatus,
    val kickoffAt: Instant,
    val simMinute: Int,
    val homeScore: Int,
    val awayScore: Int,
    val home: MatchTeamResponse,
    val away: MatchTeamResponse,
    val finishedAt: Instant?
) {
    companion object {
        /** Requires homeRegion/awayRegion to be fetched (see MatchRepository's fetch-join queries). */
        @JvmStatic
        fun from(match: Match): MatchResponse = MatchResponse(
            id = match.id!!,
            matchType = match.matchType,
            status = match.status,
            kickoffAt = match.kickoffAt,
            simMinute = match.simMinute,
            homeScore = match.homeScore,
            awayScore = match.awayScore,
            home = MatchTeamResponse.from(match.homeRegion!!),
            away = MatchTeamResponse.from(match.awayRegion!!),
            finishedAt = match.finishedAt
        )
    }
}
