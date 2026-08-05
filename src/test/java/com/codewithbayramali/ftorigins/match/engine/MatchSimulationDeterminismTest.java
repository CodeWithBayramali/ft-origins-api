package com.codewithbayramali.ftorigins.match.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * The reproducibility guarantee: seed + setup fully determine the timeline. This is what makes
 * replays exact and lets any disputed match be re-run and inspected line by line.
 */
class MatchSimulationDeterminismTest {

    @Test
    void sameSeedProducesIdenticalTimeline() {
        MatchSetup setup = new MatchSetup(EngineTestTeams.uniform(62), EngineTestTeams.uniform(58), 424242L);
        List<EngineEvent> first = new MatchSimulation(setup).playToFullTime();
        List<EngineEvent> second = new MatchSimulation(setup).playToFullTime();
        assertEquals(first, second, "same seed must reproduce the exact event list");
    }

    @Test
    void differentSeedsDiverge() {
        EngineTeam a = EngineTestTeams.uniform(60);
        EngineTeam b = EngineTestTeams.uniform(60);
        List<EngineEvent> first = new MatchSimulation(new MatchSetup(a, b, 1L)).playToFullTime();
        List<EngineEvent> second = new MatchSimulation(new MatchSetup(a, b, 2L)).playToFullTime();
        assertNotEquals(first, second, "different seeds should produce different matches");
    }

    @Test
    void minuteByMinuteMatchesOneShotRun() {
        MatchSetup setup = new MatchSetup(EngineTestTeams.uniform(70), EngineTestTeams.uniform(55), 7L);
        MatchSimulation stepped = new MatchSimulation(setup);
        List<EngineEvent> collected = new java.util.ArrayList<>();
        while (!stepped.finished()) {
            collected.addAll(stepped.advanceMinute());
        }
        List<EngineEvent> oneShot = new MatchSimulation(setup).playToFullTime();
        assertEquals(oneShot, collected, "chunked advancement must equal a full run — the live "
                + "scheduler and the instant catch-up path must never disagree");
    }
}
