package com.codewithbayramali.ftorigins.match.engine;

import com.codewithbayramali.ftorigins.match.model.MatchEventType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The statistical contract behind "professional, complaint-proof" simulation: results feel like
 * football (sane scorelines, no side bias) AND training visibly pays — a stronger squad reliably
 * wins over many matches while single upsets stay possible. Thresholds are deliberately below the
 * measured values so healthy tuning drift doesn't flake the build.
 */
class MatchSimulationBalanceTest {

    private static final int SIMS = 1000;

    private record Aggregate(double winRateA, double winRateB, double drawRate,
                             double avgGoalsA, double avgGoalsB, double avgShotsB) {

        static Aggregate of(int ovrA, int energyA, int ovrB, int energyB) {
            int winsA = 0;
            int winsB = 0;
            long goalsA = 0;
            long goalsB = 0;
            long shotsB = 0;
            for (long seed = 0; seed < SIMS; seed++) {
                MatchSimulation sim = new MatchSimulation(new MatchSetup(
                        EngineTestTeams.uniform(ovrA, energyA),
                        EngineTestTeams.uniform(ovrB, energyB), seed));
                List<EngineEvent> events = sim.playToFullTime();
                if (sim.homeScore() > sim.awayScore()) {
                    winsA++;
                } else if (sim.awayScore() > sim.homeScore()) {
                    winsB++;
                }
                goalsA += sim.homeScore();
                goalsB += sim.awayScore();
                shotsB += events.stream()
                        .filter(e -> e.type() == MatchEventType.SHOT && "AWAY".equals(e.payload().get("side")))
                        .count();
            }
            return new Aggregate(
                    winsA / (double) SIMS, winsB / (double) SIMS,
                    (SIMS - winsA - winsB) / (double) SIMS,
                    goalsA / (double) SIMS, goalsB / (double) SIMS,
                    shotsB / (double) SIMS);
        }
    }

    @Test
    void equalTeamsAreFair() {
        Aggregate agg = Aggregate.of(55, 100, 55, 100);
        double bias = Math.abs(agg.winRateA() - agg.winRateB());
        assertTrue(bias <= 0.06, "equal teams should win equally often, bias=" + bias + " " + agg);
        double totalGoals = agg.avgGoalsA() + agg.avgGoalsB();
        assertTrue(totalGoals >= 3.0 && totalGoals <= 7.5,
                "average total goals should look like 5v5 football, got " + totalGoals + " " + agg);
    }

    @Test
    void strongSquadDominatesOverManyMatches() {
        Aggregate agg = Aggregate.of(75, 100, 50, 100);
        assertTrue(agg.winRateA() >= 0.80, "OVR 75 must dominate OVR 50, " + agg);
        double goalDiff = agg.avgGoalsA() - agg.avgGoalsB();
        assertTrue(goalDiff >= 1.5, "average goal difference should be decisive, " + agg);
        assertTrue(agg.avgGoalsB() >= 0.1, "the weaker side still scores sometimes, " + agg);
    }

    @Test
    void moderateTrainingEdgeIsVisible() {
        Aggregate agg = Aggregate.of(60, 100, 50, 100);
        assertTrue(agg.winRateA() - agg.winRateB() >= 0.08,
                "10 OVR points must produce a clear edge, " + agg);
    }

    @Test
    void blowoutsStayBoundedAndTheWeakStillPlay() {
        Aggregate agg = Aggregate.of(95, 100, 20, 100);
        assertTrue(agg.avgGoalsA() <= 12.0, "even extreme mismatches stay football-shaped, " + agg);
        assertTrue(agg.avgShotsB() >= 1.0,
                "possession clamp must keep the weak side in the game, " + agg);
    }

    @Test
    void arrivingWithLowEnergyCosts() {
        Aggregate agg = Aggregate.of(60, 100, 60, 20);
        assertTrue(agg.winRateA() - agg.winRateB() >= 0.05,
                "training energy management must matter on match day, " + agg);
    }
}
