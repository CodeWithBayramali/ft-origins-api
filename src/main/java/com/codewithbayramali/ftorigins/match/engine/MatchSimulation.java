package com.codewithbayramali.ftorigins.match.engine;

import com.codewithbayramali.ftorigins.match.model.MatchEventType;
import com.codewithbayramali.ftorigins.match.model.MatchRole;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.function.ToDoubleFunction;

/**
 * The deterministic 5v5 match simulation: a possession-sequence engine advanced one match minute at
 * a time (the live scheduler ticks it; catch-up and tests run it to full time in one go).
 *
 * <p>Model, in one breath: every minute the midfield battle decides who attacks (team ratings,
 * clamped so possession is never a shutout), the attack penetrates by dribble, through ball or long
 * shot (each a stat duel on the clamped logistic curve), and shots resolve against the keeper.
 * Failed penetrations can draw fouls -> free kicks, penalties, cards; saves and blocks can spawn
 * corners. Fatigue erodes everyone's stats linearly, worse for players who arrived low on energy.
 *
 * <p>Guarantees the product story leans on:
 * <ul>
 *   <li><b>Deterministic:</b> all randomness flows from the seeded {@link SplittableRandom}; the
 *       engine reads no clock, no I/O, no entities. Same {@link MatchSetup} = same timeline.</li>
 *   <li><b>Professional outcomes:</b> referee calls are explicit events (a FOUL that isn't in the
 *       timeline did not happen), and every probability is clamped — single duels can surprise,
 *       but superiority accumulates over 90 minutes, so training visibly pays off.</li>
 *   <li><b>Complete matches:</b> a sent-off player is excluded from every later pick; teams stay
 *       playable (reds stop below a floor of outfielders, the GK is never sent off).</li>
 * </ul>
 */
public final class MatchSimulation {

    public static final int MATCH_MINUTES = 90;
    private static final int HALF_TIME_MINUTE = 45;

    /** Weight of a role when picking who carries, passes, defends or attacks a header. */
    private static final double AFFINITY_HIGH = 3.0;
    private static final double AFFINITY_MID = 2.0;
    private static final double AFFINITY_LOW = 1.0;

    private final EngineTuning t;
    private final SplittableRandom rng;
    private final TeamState home;
    private final TeamState away;

    /** Last fully simulated match minute; 0 = kickoff not played yet. */
    private int minute;
    private int homeScore;
    private int awayScore;

    /** Founder substitutions waiting for the next minute boundary (see queueSubstitution). */
    private final List<PendingSubstitution> pendingSubstitutions = new ArrayList<>();

    private record PendingSubstitution(EngineSide side, int slot, EnginePlayer incoming) {
    }

    public MatchSimulation(MatchSetup setup) {
        this(setup, EngineTuning.DEFAULT);
    }

    public MatchSimulation(MatchSetup setup, EngineTuning tuning) {
        this.t = tuning;
        this.rng = new SplittableRandom(setup.seed());
        this.home = new TeamState(EngineSide.HOME, setup.home());
        this.away = new TeamState(EngineSide.AWAY, setup.away());
    }

    public int minute() {
        return minute;
    }

    public int homeScore() {
        return homeScore;
    }

    public int awayScore() {
        return awayScore;
    }

    public boolean finished() {
        return minute >= MATCH_MINUTES;
    }

    /** Simulates the next match minute and returns its events (may be empty — quiet minutes exist). */
    public List<EngineEvent> advanceMinute() {
        if (finished()) {
            throw new IllegalStateException("Match already finished");
        }
        minute++;
        List<EngineEvent> events = new ArrayList<>();
        Clock clock = new Clock();

        if (minute == 1) {
            events.add(event(clock.next(), MatchEventType.KICKOFF,
                    payload("side", (rng.nextBoolean() ? EngineSide.HOME : EngineSide.AWAY).name())));
        }
        applyPendingSubstitutions(events, clock);
        int sequences = sequencesThisMinute();
        for (int i = 0; i < sequences; i++) {
            simulateSequence(events, clock);
        }
        if (minute == HALF_TIME_MINUTE) {
            events.add(event(59, MatchEventType.HALF_TIME, scorePayload()));
        }
        if (minute == MATCH_MINUTES) {
            events.add(event(59, MatchEventType.FULL_TIME, scorePayload()));
        }
        return events;
    }

    /** Runs the whole remaining match — tests, and instant catch-up when nobody could watch live. */
    public List<EngineEvent> playToFullTime() {
        List<EngineEvent> all = new ArrayList<>();
        while (!finished()) {
            all.addAll(advanceMinute());
        }
        return all;
    }

    // -- substitutions ------------------------------------------------------------

    /** True while the slot's current occupant is on the pitch (not sent off). */
    public boolean isSlotActive(EngineSide side, int slot) {
        return teamOf(side).all.stream().anyMatch(ps -> ps.slot() == slot && !ps.sentOff);
    }

    /**
     * Queues a founder substitution: {@code incoming} takes over {@code slot} at the START of the
     * next simulated minute (deterministic replay applies the same command at the same minute).
     * The incoming player arrives fresh — their fatigue runs from the minute they enter.
     */
    public void queueSubstitution(EngineSide side, int slot, EnginePlayer incoming) {
        if (finished()) {
            throw new IllegalStateException("Match already finished");
        }
        if (incoming.slot() != slot) {
            throw new IllegalArgumentException("Incoming player must take over slot " + slot);
        }
        if (!isSlotActive(side, slot)) {
            throw new IllegalArgumentException("Slot " + slot + " has no active player to replace");
        }
        pendingSubstitutions.add(new PendingSubstitution(side, slot, incoming));
    }

    private void applyPendingSubstitutions(List<EngineEvent> events, Clock clock) {
        for (PendingSubstitution sub : pendingSubstitutions) {
            TeamState team = teamOf(sub.side());
            for (int i = 0; i < team.all.size(); i++) {
                PlayerState out = team.all.get(i);
                if (out.slot() == sub.slot() && !out.sentOff) {
                    team.all.set(i, new PlayerState(sub.incoming(), minute));
                    events.add(event(clock.next(), MatchEventType.SUBSTITUTION,
                            payload("side", sub.side().name(), "slot", sub.slot())));
                    break;
                }
            }
        }
        pendingSubstitutions.clear();
    }

    private TeamState teamOf(EngineSide side) {
        return side == EngineSide.HOME ? home : away;
    }

    // -- minute structure ---------------------------------------------------------

    private int sequencesThisMinute() {
        double r = rng.nextDouble();
        if (r < t.quietMinuteProb()) {
            return 0;
        }
        return r < t.quietMinuteProb() + t.doubleSequenceProb() ? 2 : 1;
    }

    private void simulateSequence(List<EngineEvent> events, Clock clock) {
        TeamState att = attackingTeam();
        TeamState def = att == home ? away : home;

        PlayerState carrier = pickCarrier(att, null);
        if (carrier == null) {
            return;
        }
        // Optional build-up pass — flavor for the choreography, and it moves the ball to the
        // player who actually attempts the penetration.
        if (rng.nextDouble() < 0.6) {
            PlayerState mate = pickCarrier(att, carrier);
            if (mate != null) {
                events.add(event(clock.next(), MatchEventType.PASS,
                        payload("side", att.side.name(), "slot", carrier.slot(), "toSlot", mate.slot())));
                carrier = mate;
            }
        }

        double r = rng.nextDouble();
        if (r < t.dribblePenetrationProb()) {
            dribblePenetration(events, clock, att, def, carrier);
        } else if (r < t.dribblePenetrationProb() + t.passPenetrationProb()) {
            passPenetration(events, clock, att, def, carrier);
        } else {
            takeShot(events, clock, att, def, carrier, ShotStyle.LONG, null, true);
        }
    }

    /** Midfield battle: who attacks this sequence. Clamped — nobody monopolizes the ball. */
    private TeamState attackingTeam() {
        double pHome = DuelMath.clamp(
                DuelMath.logistic(home.midfieldRating(), away.midfieldRating(), t.possessionScale()),
                t.possessionMin(), t.possessionMax());
        return rng.nextDouble() < pHome ? home : away;
    }

    // -- penetration --------------------------------------------------------------

    private void dribblePenetration(List<EngineEvent> events, Clock clock,
                                    TeamState att, TeamState def, PlayerState carrier) {
        PlayerState defender = pickDefender(def);
        if (defender == null) {
            takeShot(events, clock, att, def, carrier, ShotStyle.OPEN, null, true);
            return;
        }
        double pWin = duelProb(
                eff(carrier, 0.6 * carrier.p.dribbling() + 0.4 * carrier.p.pace()),
                eff(defender, 0.6 * defender.p.defending() + 0.4 * defender.p.physical()));
        if (rng.nextDouble() < pWin) {
            events.add(event(clock.next(), MatchEventType.DRIBBLE,
                    payload("side", att.side.name(), "slot", carrier.slot(), "defSlot", defender.slot())));
            takeShot(events, clock, att, def, carrier, ShotStyle.OPEN, null, true);
        } else if (rng.nextDouble() < t.foulOnFailedDribbleProb()) {
            foul(events, clock, att, def, defender, carrier);
        } else {
            events.add(event(clock.next(), MatchEventType.TACKLE,
                    payload("side", def.side.name(), "slot", defender.slot(), "onSlot", carrier.slot())));
        }
    }

    private void passPenetration(List<EngineEvent> events, Clock clock,
                                 TeamState att, TeamState def, PlayerState passer) {
        PlayerState interceptor = pickDefender(def);
        PlayerState receiver = pickCarrier(att, passer);
        if (receiver == null) {
            takeShot(events, clock, att, def, passer, ShotStyle.LONG, null, true);
            return;
        }
        double pWin = interceptor == null ? t.duelMax()
                : duelProb(eff(passer, passer.p.passing()),
                           eff(interceptor, interceptor.p.defending()));
        if (rng.nextDouble() < pWin) {
            events.add(event(clock.next(), MatchEventType.PASS,
                    payload("side", att.side.name(), "slot", passer.slot(),
                            "toSlot", receiver.slot(), "through", true)));
            takeShot(events, clock, att, def, receiver, ShotStyle.THROUGH, passer, true);
        } else {
            events.add(event(clock.next(), MatchEventType.INTERCEPTION,
                    payload("side", def.side.name(), "slot", interceptor.slot(), "fromSlot", passer.slot())));
        }
    }

    // -- shots --------------------------------------------------------------------

    private enum ShotStyle {OPEN, THROUGH, LONG, HEADER, FREE_KICK, PENALTY}

    private void takeShot(List<EngineEvent> events, Clock clock, TeamState att, TeamState def,
                          PlayerState shooter, ShotStyle style, PlayerState assist,
                          boolean cornersAllowed) {
        events.add(event(clock.next(), MatchEventType.SHOT,
                payload("side", att.side.name(), "slot", shooter.slot(), "style", style.name())));

        // Blocked by a defender before it reaches the goal (penalties are never blocked).
        double blockProb = switch (style) {
            case PENALTY -> 0.0;
            case LONG -> t.blockProbLong();
            default -> t.blockProbOpen();
        };
        if (rng.nextDouble() < blockProb) {
            PlayerState blocker = pickDefender(def);
            events.add(event(clock.next(), MatchEventType.BLOCK,
                    payload("side", def.side.name(),
                            "slot", blocker != null ? blocker.slot() : def.gk().slot())));
            if (cornersAllowed && rng.nextDouble() < t.cornerAfterBlockProb()) {
                corner(events, clock, att, def);
            }
            return;
        }

        double shootScore = shootScore(shooter, style);
        double pOnTarget = style == ShotStyle.PENALTY ? 0.92
                : DuelMath.clamp(t.onTargetFloor() + shootScore / 200.0, t.onTargetFloor(), t.onTargetCeil());
        if (rng.nextDouble() >= pOnTarget) {
            events.add(event(clock.next(), MatchEventType.MISS,
                    payload("side", att.side.name(), "slot", shooter.slot())));
            return;
        }

        PlayerState keeper = def.gk();
        double keeperFactor = 0.55 + 0.9 * DuelMath.logistic(
                shootScore, eff(keeper, keeper.p.goalkeeping()), t.keeperScale());
        double pGoal = style == ShotStyle.PENALTY
                ? DuelMath.clamp(t.goalBasePenalty() * keeperFactor, t.penaltyProbMin(), t.penaltyProbMax())
                : DuelMath.clamp(goalBase(style) * keeperFactor, t.goalProbMin(), t.goalProbMax());
        // Game management: a side already up by the mercy margin cruises instead of piling on
        // (penalties exempt — nobody takes a spot kick softly).
        int lead = att.side == EngineSide.HOME ? homeScore - awayScore : awayScore - homeScore;
        if (style != ShotStyle.PENALTY && lead >= t.mercyGoalDiff()) {
            pGoal *= t.mercyConversionFactor();
        }

        if (rng.nextDouble() < pGoal) {
            if (att.side == EngineSide.HOME) {
                homeScore++;
            } else {
                awayScore++;
            }
            Map<String, Object> goal = payload("side", att.side.name(), "slot", shooter.slot(),
                    "style", style.name());
            if (assist != null) {
                goal.put("assistSlot", assist.slot());
            }
            goal.put("homeScore", homeScore);
            goal.put("awayScore", awayScore);
            events.add(event(clock.next(), MatchEventType.GOAL, goal));
        } else {
            events.add(event(clock.next(), MatchEventType.SAVE,
                    payload("side", def.side.name(), "slot", keeper.slot(), "shooterSlot", shooter.slot())));
            if (cornersAllowed && rng.nextDouble() < t.cornerAfterSaveProb()) {
                corner(events, clock, att, def);
            }
        }
    }

    private double shootScore(PlayerState shooter, ShotStyle style) {
        double base = switch (style) {
            case FREE_KICK -> 0.6 * shooter.p.shooting() + 0.4 * shooter.p.curve();
            case HEADER -> 0.5 * shooter.p.shooting() + 0.5 * shooter.p.physical();
            default -> shooter.p.shooting();
        };
        return eff(shooter, base);
    }

    private double goalBase(ShotStyle style) {
        return switch (style) {
            case OPEN -> t.goalBaseOpen();
            case THROUGH -> t.goalBaseThrough();
            case LONG -> t.goalBaseLong();
            case HEADER -> t.goalBaseHeader();
            case FREE_KICK -> t.goalBaseFreeKick();
            case PENALTY -> t.goalBasePenalty();
        };
    }

    // -- dead balls ---------------------------------------------------------------

    private void corner(List<EngineEvent> events, Clock clock, TeamState att, TeamState def) {
        events.add(event(clock.next(), MatchEventType.CORNER, payload("side", att.side.name())));
        if (rng.nextDouble() >= t.cornerHeaderProb()) {
            return; // cleared, possession over
        }
        PlayerState header = weightedPick(att.activeOutfield(), ps ->
                (ps.p.role() == MatchRole.MID ? AFFINITY_LOW : AFFINITY_MID)
                        * (1.0 + eff(ps, 0.6 * ps.p.physical() + 0.4 * ps.p.shooting()) / 100.0));
        if (header != null) {
            // No corner chains: a headed shot's save/block ends the set piece.
            takeShot(events, clock, att, def, header, ShotStyle.HEADER, null, false);
        }
    }

    private void foul(List<EngineEvent> events, Clock clock,
                      TeamState att, TeamState def, PlayerState offender, PlayerState victim) {
        events.add(event(clock.next(), MatchEventType.FOUL,
                payload("side", def.side.name(), "slot", offender.slot(), "onSlot", victim.slot())));
        card(events, clock, def, offender);
        if (offender.sentOff && def.activeOutfield().isEmpty()) {
            return; // theoretical guard; the red-card floor keeps this from happening
        }
        if (rng.nextDouble() < t.boxFoulShare()) {
            events.add(event(clock.next(), MatchEventType.PENALTY, payload("side", att.side.name())));
            PlayerState taker = bestBy(att.activeOutfield(), ps -> eff(ps, ps.p.shooting()));
            if (taker != null) {
                takeShot(events, clock, att, def, taker, ShotStyle.PENALTY, null, true);
            }
        } else {
            events.add(event(clock.next(), MatchEventType.FREE_KICK, payload("side", att.side.name())));
            if (rng.nextDouble() < t.freeKickDirectProb()) {
                PlayerState taker = bestBy(att.activeOutfield(),
                        ps -> eff(ps, 0.6 * ps.p.shooting() + 0.4 * ps.p.curve()));
                if (taker != null) {
                    takeShot(events, clock, att, def, taker, ShotStyle.FREE_KICK, null, true);
                }
            }
        }
    }

    /**
     * Referee cards. The GK only ever collects a single yellow (never a red — a 5v5 team without
     * its keeper isn't a football match), and reds stop below the outfielder floor so a team is
     * never reduced to an unplayable squad.
     */
    private void card(List<EngineEvent> events, Clock clock, TeamState def, PlayerState offender) {
        boolean redPossible = offender.p.role() != MatchRole.GK
                && def.activeOutfield().size() > t.minOutfieldForRed();
        double r = rng.nextDouble();
        if (r < t.directRedPerFoulProb()) {
            if (redPossible) {
                offender.sentOff = true;
                events.add(event(clock.next(), MatchEventType.RED_CARD,
                        payload("side", def.side.name(), "slot", offender.slot())));
            }
            return;
        }
        if (r < t.directRedPerFoulProb() + t.yellowPerFoulProb()) {
            if (offender.p.role() == MatchRole.GK && offender.yellows >= 1) {
                return;
            }
            if (offender.yellows == 1 && !redPossible) {
                return; // a second yellow would be a red the floor forbids — swallow the card
            }
            offender.yellows++;
            events.add(event(clock.next(), MatchEventType.YELLOW_CARD,
                    payload("side", def.side.name(), "slot", offender.slot())));
            if (offender.yellows == 2) {
                offender.sentOff = true;
                events.add(event(clock.next(), MatchEventType.RED_CARD,
                        payload("side", def.side.name(), "slot", offender.slot(), "secondYellow", true)));
            }
        }
    }

    // -- picks & math -------------------------------------------------------------

    private PlayerState pickCarrier(TeamState team, PlayerState except) {
        return weightedPick(exclude(team.activeOutfield(), except), ps ->
                roleAffinity(ps, MatchRole.FWD, MatchRole.MID)
                        * (1.0 + eff(ps, ps.p.dribbling()) / 100.0));
    }

    private PlayerState pickDefender(TeamState team) {
        return weightedPick(team.activeOutfield(), ps ->
                roleAffinity(ps, MatchRole.DEF, MatchRole.MID)
                        * (1.0 + eff(ps, ps.p.defending()) / 100.0));
    }

    private double roleAffinity(PlayerState ps, MatchRole high, MatchRole mid) {
        if (ps.p.role() == high) {
            return AFFINITY_HIGH;
        }
        return ps.p.role() == mid ? AFFINITY_MID : AFFINITY_LOW;
    }

    private static List<PlayerState> exclude(List<PlayerState> pool, PlayerState except) {
        if (except == null) {
            return pool;
        }
        return pool.stream().filter(ps -> ps != except).toList();
    }

    private PlayerState weightedPick(List<PlayerState> pool, ToDoubleFunction<PlayerState> weight) {
        if (pool.isEmpty()) {
            return null;
        }
        double total = 0;
        for (PlayerState ps : pool) {
            total += Math.max(0.01, weight.applyAsDouble(ps));
        }
        double r = rng.nextDouble() * total;
        for (PlayerState ps : pool) {
            r -= Math.max(0.01, weight.applyAsDouble(ps));
            if (r <= 0) {
                return ps;
            }
        }
        return pool.get(pool.size() - 1);
    }

    private static PlayerState bestBy(List<PlayerState> pool, ToDoubleFunction<PlayerState> score) {
        PlayerState best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (PlayerState ps : pool) {
            double s = score.applyAsDouble(ps);
            if (s > bestScore) {
                best = ps;
                bestScore = s;
            }
        }
        return best;
    }

    private double duelProb(double attacker, double defender) {
        return DuelMath.clamp(DuelMath.logistic(attacker, defender, t.duelScale()),
                t.duelMin(), t.duelMax());
    }

    /** Attribute (or blend) after fatigue: linear decay over 90', worse when energy arrived low. */
    private double eff(PlayerState ps, double base) {
        return base * ps.condition(minute, t);
    }

    // -- events -------------------------------------------------------------------

    private EngineEvent event(int second, MatchEventType type, Map<String, Object> payload) {
        return new EngineEvent(minute, second, type, payload);
    }

    private Map<String, Object> scorePayload() {
        return payload("homeScore", homeScore, "awayScore", awayScore);
    }

    private static Map<String, Object> payload(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    /** Seconds within the current minute: starts near :00, steps 3-9s, saturates at :58. */
    private final class Clock {
        private int second = rng.nextInt(8);

        int next() {
            int s = Math.min(second, 58);
            second += 3 + rng.nextInt(7);
            return s;
        }
    }

    // -- state --------------------------------------------------------------------

    private static final class PlayerState {
        final EnginePlayer p;
        /** Match minute this player stepped on the pitch (0 = starter); fatigue runs from here. */
        final int enteredMinute;
        int yellows;
        boolean sentOff;

        PlayerState(EnginePlayer p, int enteredMinute) {
            this.p = p;
            this.enteredMinute = enteredMinute;
        }

        int slot() {
            return p.slot();
        }

        double condition(int minute, EngineTuning t) {
            double energyFactor = t.lowEnergyFloorFactor()
                    + (1.0 - t.lowEnergyFloorFactor()) * (p.energy() / 100.0);
            int minutesPlayed = Math.max(0, minute - enteredMinute);
            double fatigue = 1.0 - t.fatigueFullDrop() * (minutesPlayed / (double) MATCH_MINUTES);
            return energyFactor * fatigue;
        }
    }

    private final class TeamState {
        final EngineSide side;
        /** Mutable on purpose: substitutions replace a slot's occupant mid-match. */
        final List<PlayerState> all;

        TeamState(EngineSide side, EngineTeam team) {
            this.side = side;
            this.all = new ArrayList<>(team.players().stream()
                    .map(p -> new PlayerState(p, 0)).toList());
        }

        PlayerState gk() {
            return all.stream().filter(ps -> ps.p.role() == MatchRole.GK).findFirst().orElseThrow();
        }

        List<PlayerState> activeOutfield() {
            return all.stream().filter(ps -> ps.p.role() != MatchRole.GK && !ps.sentOff).toList();
        }

        /**
         * Ball-progression strength: passing/dribbling/pace over ACTIVE outfielders, MIDs weighted
         * up, divided by the FULL outfield count — so a red card is a real, lasting handicap.
         */
        double midfieldRating() {
            double sum = 0;
            int fullOutfield = 0;
            for (PlayerState ps : all) {
                if (ps.p.role() == MatchRole.GK) {
                    continue;
                }
                fullOutfield++;
                if (ps.sentOff) {
                    continue;
                }
                double contribution = 0.4 * ps.p.passing() + 0.3 * ps.p.dribbling() + 0.3 * ps.p.pace();
                if (ps.p.role() == MatchRole.MID) {
                    contribution *= 1.2;
                }
                sum += contribution * ps.condition(minute, t);
            }
            return fullOutfield == 0 ? 0 : sum / fullOutfield;
        }
    }
}
