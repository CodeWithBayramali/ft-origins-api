package com.codewithbayramali.ftorigins.match.service;

import com.codewithbayramali.ftorigins.match.model.Match;
import com.codewithbayramali.ftorigins.match.model.MatchStatus;
import com.codewithbayramali.ftorigins.match.repository.MatchRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * The heartbeat of live football: every tick starts due kickoffs and advances running matches.
 * Ticks are serialized (Spring's default single scheduler thread), and every match is isolated in
 * its own try/catch so one broken match can't stall the rest of the world.
 *
 * <p>Single-node by design for now — with multiple app instances this needs a DB-level claim
 * (e.g. SELECT ... FOR UPDATE SKIP LOCKED) so two schedulers don't advance the same match.
 */
@Component
public class MatchTickScheduler {

    private static final Logger log = LoggerFactory.getLogger(MatchTickScheduler.class);

    private final MatchRepository matchRepository;
    private final MatchKickoffService kickoffService;
    private final MatchLiveService liveService;

    public MatchTickScheduler(MatchRepository matchRepository,
                              MatchKickoffService kickoffService,
                              MatchLiveService liveService) {
        this.matchRepository = matchRepository;
        this.kickoffService = kickoffService;
        this.liveService = liveService;
    }

    @Scheduled(fixedDelayString = "${app.match.tick-millis:2000}")
    public void tick() {
        Instant now = Instant.now();
        for (Match due : matchRepository.findByStatusAndKickoffAtLessThanEqual(MatchStatus.SCHEDULED, now)) {
            try {
                kickoffService.kickOff(due.getId());
            } catch (RuntimeException e) {
                log.error("Kickoff failed for match {}", due.getId(), e);
            }
        }
        for (Match live : matchRepository.findDetailByStatus(MatchStatus.LIVE)) {
            try {
                liveService.advance(live, now);
            } catch (RuntimeException e) {
                log.error("Advance failed for match {}", live.getId(), e);
            }
        }
    }
}
