package com.codewithbayramali.ftorigins.match.dto

/** A team's match overview: what's coming (or running), and what was recently played. */
data class RegionMatchesResponse(
    /** SCHEDULED + LIVE, soonest kickoff first. */
    val upcoming: List<MatchResponse>,
    /** Last FINISHED matches, newest first (capped). */
    val recent: List<MatchResponse>
)
