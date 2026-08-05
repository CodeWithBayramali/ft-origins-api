package com.codewithbayramali.ftorigins.match.model

/** Lifecycle of a match: SCHEDULED -> LIVE (kickoff) -> FINISHED; CANCELLED only before kickoff. */
enum class MatchStatus {
    SCHEDULED,
    LIVE,
    FINISHED,
    CANCELLED
}
