package com.codewithbayramali.ftorigins.match.engine;

import com.codewithbayramali.ftorigins.match.model.MatchEventType;
import com.codewithbayramali.ftorigins.match.model.MatchRole;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MatchSimulationSubstitutionTest {

    private static final EnginePlayer FRESH_STRIKER =
            new EnginePlayer(5, MatchRole.FWD, 90, 90, 90, 90, 90, 90, 90, 90, 100);

    @Test
    void substitutionAppliesAtTheNextMinute() {
        MatchSimulation sim = new MatchSimulation(
                new MatchSetup(EngineTestTeams.uniform(60), EngineTestTeams.uniform(60), 5L));
        for (int i = 0; i < 30; i++) {
            sim.advanceMinute();
        }
        sim.queueSubstitution(EngineSide.HOME, 5, FRESH_STRIKER);

        List<EngineEvent> minute31 = sim.advanceMinute();
        EngineEvent sub = minute31.stream()
                .filter(e -> e.type() == MatchEventType.SUBSTITUTION)
                .findFirst().orElseThrow();
        assertEquals(31, sub.minute());
        assertEquals("HOME", sub.payload().get("side"));
        assertEquals(5, sub.payload().get("slot"));
        assertTrue(sim.isSlotActive(EngineSide.HOME, 5), "slot stays active after the swap");
    }

    @Test
    void rebuildReplayProducesTheSameMatchAsLive() {
        MatchSetup setup = new MatchSetup(EngineTestTeams.uniform(58), EngineTestTeams.uniform(63), 99L);

        // "Live" run: sub queued while the match is at minute 30.
        MatchSimulation live = new MatchSimulation(setup);
        List<EngineEvent> liveTail = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            live.advanceMinute();
        }
        live.queueSubstitution(EngineSide.HOME, 5, FRESH_STRIKER);
        while (!live.finished()) {
            liveTail.addAll(live.advanceMinute());
        }

        // "Rebuild" run (what LiveMatchRegistry does after a restart): replay the same command at
        // the same minute — the remaining timeline must be identical.
        MatchSimulation rebuilt = new MatchSimulation(setup);
        for (int i = 0; i < 30; i++) {
            rebuilt.advanceMinute();
        }
        rebuilt.queueSubstitution(EngineSide.HOME, 5, FRESH_STRIKER);
        List<EngineEvent> rebuiltTail = new ArrayList<>();
        while (!rebuilt.finished()) {
            rebuiltTail.addAll(rebuilt.advanceMinute());
        }

        assertEquals(liveTail, rebuiltTail, "rebuild must walk the exact same path as the live run");
        assertEquals(live.homeScore(), rebuilt.homeScore());
        assertEquals(live.awayScore(), rebuilt.awayScore());
    }

    @Test
    void queueRejectsBadInput() {
        MatchSimulation sim = new MatchSimulation(
                new MatchSetup(EngineTestTeams.uniform(60), EngineTestTeams.uniform(60), 1L));
        sim.advanceMinute();

        assertThrows(IllegalArgumentException.class,
                () -> sim.queueSubstitution(EngineSide.HOME, 3, FRESH_STRIKER),
                "incoming slot must match the slot being replaced");
        assertThrows(IllegalArgumentException.class,
                () -> sim.queueSubstitution(EngineSide.HOME, 9,
                        new EnginePlayer(9, MatchRole.FWD, 50, 50, 50, 50, 50, 50, 50, 50, 100)),
                "unknown slot has no active player");
    }
}
