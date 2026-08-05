package com.codewithbayramali.ftorigins.match.model

/**
 * Everything that can happen on the timeline. The engine (phase 2) emits these; the Unity client
 * choreographs them 1:1 — it never invents or decides outcomes. Referee calls (FOUL, cards) are
 * events like any other, so "there was a foul but it wasn't given" cannot exist: what the event
 * says IS what happened.
 */
enum class MatchEventType {
    KICKOFF,
    PASS,
    DRIBBLE,
    TACKLE,
    INTERCEPTION,
    SHOT,
    SAVE,
    GOAL,
    MISS,
    BLOCK,
    FOUL,
    YELLOW_CARD,
    RED_CARD,
    FREE_KICK,
    PENALTY,
    CORNER,
    GOAL_KICK,
    THROW_IN,
    HALF_TIME,
    FULL_TIME,
    SUBSTITUTION,
    TACTIC_CHANGE
}
