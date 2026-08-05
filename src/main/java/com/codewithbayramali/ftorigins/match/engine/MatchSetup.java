package com.codewithbayramali.ftorigins.match.engine;

/**
 * Everything a simulation needs — and the ONLY things it may read. Same setup + same seed =
 * bit-identical timeline (the reproducibility guarantee behind replays and dispute review).
 */
public record MatchSetup(EngineTeam home, EngineTeam away, long seed) {

    public EngineTeam team(EngineSide side) {
        return side == EngineSide.HOME ? home : away;
    }
}
