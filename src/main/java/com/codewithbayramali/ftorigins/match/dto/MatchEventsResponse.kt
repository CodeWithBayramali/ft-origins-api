package com.codewithbayramali.ftorigins.match.dto

import com.codewithbayramali.ftorigins.match.model.MatchEventType
import com.codewithbayramali.ftorigins.match.model.MatchStatus
import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant
import java.util.UUID

/** One timeline entry. Actors are referenced as side + slot in [payload]; the client resolves them
 *  against the lineups endpoint (a slot's occupant can change via substitutions). */
data class MatchEventResponse(
    val seq: Int,
    val minute: Int,
    val second: Int,
    val type: MatchEventType,
    val payload: JsonNode?
)

/**
 * Timeline tail after the caller's polling cursor. Only simulated minutes exist in storage, so a
 * LIVE match can never leak its future. The client maps match-clock to wall-clock via [kickoffAt] +
 * [secondsPerSimMinute] (event at m:s plays at kickoff + (m-1)*pace + s*pace/60), which keeps every
 * spectator in sync without any push channel; [simMinute] is the live frontier so far.
 */
data class MatchEventsResponse(
    val matchId: UUID,
    val status: MatchStatus,
    val simMinute: Int,
    val homeScore: Int,
    val awayScore: Int,
    val kickoffAt: Instant,
    val secondsPerSimMinute: Int,
    val events: List<MatchEventResponse>
)
