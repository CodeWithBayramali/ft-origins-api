package com.codewithbayramali.ftorigins.geo.dto

import com.codewithbayramali.ftorigins.match.model.Match
import com.codewithbayramali.ftorigins.match.model.MatchStatus
import java.time.Instant
import java.util.UUID

/**
 * One match from the caller's team's point of view — scores already flipped to "us vs them" so the
 * client renders a row without knowing which side we were.
 */
data class TeamMatchResponse(
    val matchId: UUID,
    val opponentRegionId: UUID,
    val opponentName: String,
    val atHome: Boolean,
    val kickoffAt: Instant,
    val status: MatchStatus,
    val myScore: Int,
    val opponentScore: Int,
    /** Live match clock; 0 before kickoff. */
    val simMinute: Int,
    /** "W" / "D" / "L" for finished matches, null while scheduled or live. */
    val result: String?,
    /** Opponent kit ids so the row can show their colours. */
    val opponentKitHome: Int?,
    val opponentKitAway: Int?,
    val opponentKitPattern: Int?
) {
    companion object {
        /** Requires homeRegion/awayRegion to be fetched. */
        @JvmStatic
        fun from(match: Match, myRegionId: UUID): TeamMatchResponse {
            val home = match.homeRegion!!
            val away = match.awayRegion!!
            val atHome = home.id == myRegionId
            val opponent = if (atHome) away else home
            val myScore = if (atHome) match.homeScore else match.awayScore
            val theirScore = if (atHome) match.awayScore else match.homeScore
            val result = when {
                match.status != MatchStatus.FINISHED -> null
                myScore > theirScore -> "W"
                myScore < theirScore -> "L"
                else -> "D"
            }
            return TeamMatchResponse(
                matchId = match.id!!,
                opponentRegionId = opponent.id!!,
                opponentName = opponent.name,
                atHome = atHome,
                kickoffAt = match.kickoffAt,
                status = match.status,
                myScore = myScore,
                opponentScore = theirScore,
                simMinute = match.simMinute,
                result = result,
                opponentKitHome = opponent.kitHome,
                opponentKitAway = opponent.kitAway,
                opponentKitPattern = opponent.kitPattern
            )
        }
    }
}
