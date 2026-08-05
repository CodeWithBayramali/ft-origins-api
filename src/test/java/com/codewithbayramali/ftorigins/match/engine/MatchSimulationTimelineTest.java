package com.codewithbayramali.ftorigins.match.engine;

import com.codewithbayramali.ftorigins.match.model.MatchEventType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Structural invariants of every timeline — what the Unity choreographer and the polling API rely
 * on. Checked across many seeds so rare paths (reds, penalties, corner chains) get covered too.
 */
class MatchSimulationTimelineTest {

    private static final int SEEDS = 200;

    private static long count(List<EngineEvent> events, MatchEventType type) {
        return events.stream().filter(e -> e.type() == type).count();
    }

    @Test
    void timelinesAreStructurallySound() {
        for (long seed = 0; seed < SEEDS; seed++) {
            MatchSimulation sim = new MatchSimulation(
                    new MatchSetup(EngineTestTeams.uniform(68), EngineTestTeams.uniform(52), seed));
            List<EngineEvent> events = sim.playToFullTime();

            assertTrue(sim.finished(), "seed " + seed + ": match must finish");
            assertEquals(MatchEventType.KICKOFF, events.get(0).type(), "seed " + seed + ": starts with kickoff");
            assertEquals(MatchEventType.FULL_TIME, events.get(events.size() - 1).type(),
                    "seed " + seed + ": ends with full time");
            assertEquals(1, count(events, MatchEventType.HALF_TIME), "seed " + seed + ": one half-time");
            assertEquals(1, count(events, MatchEventType.FULL_TIME), "seed " + seed + ": one full-time");

            int lastMinute = 0;
            for (EngineEvent e : events) {
                assertTrue(e.minute() >= lastMinute, "seed " + seed + ": minutes never go backwards");
                assertTrue(e.minute() >= 1 && e.minute() <= MatchSimulation.MATCH_MINUTES,
                        "seed " + seed + ": minute in range, got " + e.minute());
                assertTrue(e.second() >= 0 && e.second() <= 59,
                        "seed " + seed + ": second in range, got " + e.second());
                lastMinute = e.minute();
            }

            long homeGoals = events.stream()
                    .filter(e -> e.type() == MatchEventType.GOAL && "HOME".equals(e.payload().get("side"))).count();
            long awayGoals = events.stream()
                    .filter(e -> e.type() == MatchEventType.GOAL && "AWAY".equals(e.payload().get("side"))).count();
            assertEquals(sim.homeScore(), (int) homeGoals, "seed " + seed + ": home goals match score");
            assertEquals(sim.awayScore(), (int) awayGoals, "seed " + seed + ": away goals match score");

            EngineEvent fullTime = events.get(events.size() - 1);
            assertEquals(sim.homeScore(), fullTime.payload().get("homeScore"), "seed " + seed);
            assertEquals(sim.awayScore(), fullTime.payload().get("awayScore"), "seed " + seed);

            assertThrows(IllegalStateException.class, sim::advanceMinute,
                    "seed " + seed + ": a finished match can't advance");
        }
    }

    @Test
    void sentOffPlayersNeverActAgain() {
        int redsSeen = 0;
        for (long seed = 0; seed < SEEDS; seed++) {
            List<EngineEvent> events = new MatchSimulation(
                    new MatchSetup(EngineTestTeams.uniform(60), EngineTestTeams.uniform(60), seed))
                    .playToFullTime();

            Set<String> sentOff = new HashSet<>();
            for (EngineEvent e : events) {
                String actor = e.payload().get("side") + "#" + e.payload().get("slot");
                if (e.type() != MatchEventType.RED_CARD && e.payload().get("slot") != null) {
                    assertFalse(sentOff.contains(actor),
                            "seed " + seed + ": sent-off player acted again in " + e.type());
                }
                if (e.type() == MatchEventType.RED_CARD) {
                    sentOff.add(actor);
                    redsSeen++;
                }
            }
        }
        assertTrue(redsSeen > 0, "expected at least one red card across " + SEEDS + " matches");
    }
}
