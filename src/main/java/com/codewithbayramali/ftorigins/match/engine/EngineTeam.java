package com.codewithbayramali.ftorigins.match.engine;

import com.codewithbayramali.ftorigins.match.model.MatchRole;

import java.util.List;

/** One side's five slots. Exactly one GK; slot indexes must be unique (they name players in events). */
public record EngineTeam(List<EnginePlayer> players) {

    public EngineTeam {
        players = List.copyOf(players);
        long gks = players.stream().filter(p -> p.role() == MatchRole.GK).count();
        if (gks != 1) {
            throw new IllegalArgumentException("A team needs exactly one GK, got " + gks);
        }
        long distinctSlots = players.stream().mapToInt(EnginePlayer::slot).distinct().count();
        if (distinctSlots != players.size()) {
            throw new IllegalArgumentException("Duplicate slot indexes in team");
        }
    }
}
