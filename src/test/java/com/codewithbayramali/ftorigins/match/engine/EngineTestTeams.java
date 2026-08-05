package com.codewithbayramali.ftorigins.match.engine;

import com.codewithbayramali.ftorigins.match.model.MatchRole;

import java.util.List;

/** Team builders for engine tests: 1-2-1 lineups with uniform attributes. */
final class EngineTestTeams {

    private EngineTestTeams() {
    }

    static EngineTeam uniform(int ovr) {
        return uniform(ovr, 100);
    }

    static EngineTeam uniform(int ovr, int energy) {
        return new EngineTeam(List.of(
                player(0, MatchRole.GK, ovr, energy),
                player(1, MatchRole.DEF, ovr, energy),
                player(2, MatchRole.DEF, ovr, energy),
                player(3, MatchRole.MID, ovr, energy),
                player(4, MatchRole.MID, ovr, energy),
                player(5, MatchRole.FWD, ovr, energy)));
    }

    private static EnginePlayer player(int slot, MatchRole role, int ovr, int energy) {
        return new EnginePlayer(slot, role, ovr, ovr, ovr, ovr, ovr, ovr, ovr, ovr, energy);
    }
}
