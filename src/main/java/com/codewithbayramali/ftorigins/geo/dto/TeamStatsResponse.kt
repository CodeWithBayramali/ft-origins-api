package com.codewithbayramali.ftorigins.geo.dto

/**
 * A team's record, derived from its finished matches (3 points a win, 1 a draw). Standings tables
 * arrive with the league phase; until then this is the single source of "how are we doing".
 */
data class TeamStatsResponse(
    val played: Int,
    val wins: Int,
    val draws: Int,
    val losses: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val goalDifference: Int,
    val points: Int,
    /** Most recent first, at most five entries: "W" / "D" / "L". */
    val form: List<String>
) {
    companion object {
        @JvmStatic
        fun empty(): TeamStatsResponse = TeamStatsResponse(0, 0, 0, 0, 0, 0, 0, 0, emptyList())
    }
}
