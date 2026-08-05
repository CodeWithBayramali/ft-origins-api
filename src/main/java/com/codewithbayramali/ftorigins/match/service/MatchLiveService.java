package com.codewithbayramali.ftorigins.match.service;

import com.codewithbayramali.ftorigins.match.engine.EngineEvent;
import com.codewithbayramali.ftorigins.match.engine.MatchSimulation;
import com.codewithbayramali.ftorigins.match.model.Match;
import com.codewithbayramali.ftorigins.match.model.MatchEvent;
import com.codewithbayramali.ftorigins.match.model.MatchStatus;
import com.codewithbayramali.ftorigins.match.repository.MatchEventRepository;
import com.codewithbayramali.ftorigins.match.repository.MatchRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Advances LIVE matches with real time: the match clock runs at one simulated minute per
 * {@code app.match.sim-minute-real-seconds} real seconds, and each tick simulates every minute
 * that real time has already covered — which is also the catch-up path (server downtime, or a
 * kickoff that was missed entirely, is simulated in one burst; the timeline is identical either
 * way, spectators just receive it later).
 *
 * <p>Events are persisted before the in-memory sim is trusted any further: if the transaction
 * fails, the sim (now ahead of the DB) is evicted and the next tick rebuilds it from the DB truth.
 */
@Service
public class MatchLiveService {

    private static final Logger log = LoggerFactory.getLogger(MatchLiveService.class);

    private final LiveMatchRegistry registry;
    private final MatchRepository matchRepository;
    private final MatchEventRepository eventRepository;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final long simMinuteRealSeconds;

    public MatchLiveService(LiveMatchRegistry registry,
                            MatchRepository matchRepository,
                            MatchEventRepository eventRepository,
                            ObjectMapper objectMapper,
                            TransactionTemplate transactionTemplate,
                            @Value("${app.match.sim-minute-real-seconds:8}") long simMinuteRealSeconds) {
        this.registry = registry;
        this.matchRepository = matchRepository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.simMinuteRealSeconds = simMinuteRealSeconds;
    }

    /** Simulates every minute real time has covered since kickoff; no-op when already caught up. */
    public void advance(Match match, Instant now) {
        if (match.getStatus() != MatchStatus.LIVE || match.getSeed() == null) {
            return;
        }
        int target = targetMinute(match.getKickoffAt(), now);
        if (target <= match.getSimMinute()) {
            return;
        }
        try {
            registry.withMatch(match, live -> {
                MatchSimulation sim = live.sim();
                // Another writer may have moved this match on while we held a stale simulation.
                if (sim.minute() != match.getSimMinute()) {
                    throw new StaleSimulationException(match.getId(), sim.minute(), match.getSimMinute());
                }
                int fromMinute = sim.minute();
                List<MatchEvent> rows = new ArrayList<>();
                while (sim.minute() < target && !sim.finished()) {
                    for (EngineEvent event : sim.advanceMinute()) {
                        rows.add(toRow(match, live, event));
                    }
                }
                for (MatchEvent row : rows)
                    if (row.getEventType() == com.codewithbayramali.ftorigins.match.model.MatchEventType.SUBSTITUTION)
                        log.info("Match {}: substitution event persisted at minute {}", match.getId(), row.getSimMinute());
                boolean finished = sim.finished();
                transactionTemplate.executeWithoutResult(tx -> {
                    eventRepository.saveAll(rows);
                    Instant now2 = Instant.now();
                    int claimed = matchRepository.claimAdvance(match.getId(), fromMinute, sim.minute(),
                            sim.homeScore(), sim.awayScore(),
                            finished ? MatchStatus.FINISHED : MatchStatus.LIVE,
                            finished ? now2 : null, now2);
                    if (claimed == 0) {
                        // Someone else advanced this match: roll the events back with the transaction.
                        throw new StaleSimulationException(match.getId(), fromMinute, -1);
                    }
                    match.setSimMinute(sim.minute());
                    match.setHomeScore(sim.homeScore());
                    match.setAwayScore(sim.awayScore());
                    if (finished) {
                        match.setStatus(MatchStatus.FINISHED);
                        match.setFinishedAt(now2);
                    }
                });
                if (finished) {
                    registry.evict(match.getId());
                    log.info("Match {} finished {}-{}", match.getId(), sim.homeScore(), sim.awayScore());
                }
                return null;
            });
        } catch (StaleSimulationException stale) {
            // Expected when another instance owns this match: drop our copy and pick up the real
            // state on the next tick instead of writing a second, conflicting timeline.
            registry.evict(match.getId());
            log.warn("{}", stale.getMessage());
        } catch (RuntimeException e) {
            // The in-memory sim may be ahead of what was persisted — drop it, rebuild next tick.
            registry.evict(match.getId());
            throw e;
        }
    }

    /** Raised when this instance's simulation no longer matches the stored match clock. */
    private static final class StaleSimulationException extends RuntimeException {
        StaleSimulationException(java.util.UUID matchId, int ourMinute, int storedMinute) {
            super(storedMinute >= 0
                    ? "Match " + matchId + " moved on elsewhere (we were at " + ourMinute
                      + ", stored is " + storedMinute + ") — dropping our simulation"
                    : "Match " + matchId + " was advanced by another writer at minute " + ourMinute
                      + " — dropping our simulation");
        }
    }

    /** The match minute real time has reached: floor(elapsed / pace), capped at full time. */
    private int targetMinute(Instant kickoffAt, Instant now) {
        long elapsedSeconds = Duration.between(kickoffAt, now).getSeconds();
        if (elapsedSeconds <= 0) {
            return 0;
        }
        return (int) Math.min(MatchSimulation.MATCH_MINUTES, elapsedSeconds / simMinuteRealSeconds);
    }

    private MatchEvent toRow(Match match, LiveMatchRegistry.LiveMatch live, EngineEvent event) {
        MatchEvent row = new MatchEvent();
        row.setMatch(match);
        row.setSeq(live.takeSeq());
        row.setSimMinute(event.minute());
        row.setSimSecond(event.second());
        row.setEventType(event.type());
        try {
            row.setPayload(objectMapper.writeValueAsString(event.payload()));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unserializable engine event payload", e);
        }
        return row;
    }
}
