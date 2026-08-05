package com.codewithbayramali.ftorigins.match.service;

import com.codewithbayramali.ftorigins.exception.BusinessException;
import com.codewithbayramali.ftorigins.exception.ConflictException;
import com.codewithbayramali.ftorigins.exception.NotFoundException;
import com.codewithbayramali.ftorigins.geo.model.Region;
import com.codewithbayramali.ftorigins.geo.repository.RegionRepository;
import com.codewithbayramali.ftorigins.match.dto.CreateFriendlyRequest;
import com.codewithbayramali.ftorigins.match.dto.MatchResponse;
import com.codewithbayramali.ftorigins.match.dto.RegionMatchesResponse;
import com.codewithbayramali.ftorigins.match.model.Match;
import com.codewithbayramali.ftorigins.match.model.MatchStatus;
import com.codewithbayramali.ftorigins.match.model.MatchType;
import com.codewithbayramali.ftorigins.match.repository.MatchRepository;
import com.codewithbayramali.ftorigins.player.model.Player;
import com.codewithbayramali.ftorigins.player.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * Match scheduling and viewing. Friendlies are founder-arranged (league fixtures will come from the
 * fixture generator); viewing a match is open to every authenticated player — spectating is the
 * point of the simulation model.
 */
@Service
public class MatchService {

    /** Booking must leave the founder a real edit window on top of the lineup lock. */
    private static final Duration EXTRA_LEAD = Duration.ofMinutes(5);

    private static final int RECENT_LIMIT = 10;

    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final RegionRepository regionRepository;
    private final FounderGuard founderGuard;
    private final Duration lineupLock;
    private final Duration conflictWindow;
    private final Duration maxScheduleAhead;

    public MatchService(MatchRepository matchRepository,
                        PlayerRepository playerRepository,
                        RegionRepository regionRepository,
                        FounderGuard founderGuard,
                        @Value("${app.match.lineup-lock-minutes:15}") long lineupLockMinutes,
                        @Value("${app.match.conflict-window-minutes:30}") long conflictWindowMinutes,
                        @Value("${app.match.max-schedule-days-ahead:7}") long maxScheduleDaysAhead) {
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
        this.regionRepository = regionRepository;
        this.founderGuard = founderGuard;
        this.lineupLock = Duration.ofMinutes(lineupLockMinutes);
        this.conflictWindow = Duration.ofMinutes(conflictWindowMinutes);
        this.maxScheduleAhead = Duration.ofDays(maxScheduleDaysAhead);
    }

    /** Founder-only: book a friendly against another team; the booking team plays at home. */
    @Transactional
    public MatchResponse createFriendly(UUID userId, CreateFriendlyRequest request) {
        Player caller = playerRepository.findDetailByUserId(userId)
                .orElseThrow(() -> new NotFoundException("PLAYER_NOT_FOUND", "No player profile for this account"));
        Region home = caller.getRegion();
        founderGuard.requireFounder(caller, home);

        Region away = regionRepository.findById(request.getOpponentRegionId())
                .orElseThrow(() -> new NotFoundException("REGION_NOT_FOUND", "Opponent team not found"));
        if (away.getId().equals(home.getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "SELF_MATCH",
                    "A team can't play a friendly against itself");
        }

        Instant kickoff = request.getKickoffAt();
        requireSchedulable(kickoff);
        requireNoConflicts(home.getId(), away.getId(), kickoff);

        Match match = new Match();
        match.setHomeRegion(home);
        match.setAwayRegion(away);
        match.setMatchType(MatchType.FRIENDLY);
        match.setStatus(MatchStatus.SCHEDULED);
        match.setKickoffAt(kickoff);
        return MatchResponse.from(matchRepository.save(match));
    }

    /** Match header/card by id — open to any authenticated player (spectators included). */
    @Transactional(readOnly = true)
    public MatchResponse getMatch(UUID matchId) {
        Match match = matchRepository.findDetailById(matchId)
                .orElseThrow(() -> new NotFoundException("MATCH_NOT_FOUND", "Match not found"));
        return MatchResponse.from(match);
    }

    /** The caller's team's matches: upcoming/live plus the recent results. */
    @Transactional(readOnly = true)
    public RegionMatchesResponse myMatches(UUID userId) {
        Player caller = playerRepository.findDetailByUserId(userId)
                .orElseThrow(() -> new NotFoundException("PLAYER_NOT_FOUND", "No player profile for this account"));
        UUID regionId = caller.getRegion().getId();

        List<MatchResponse> upcoming = matchRepository
                .findByRegionAndStatuses(regionId, EnumSet.of(MatchStatus.SCHEDULED, MatchStatus.LIVE))
                .stream().map(MatchResponse::from).toList();
        List<MatchResponse> recent = matchRepository
                .findByRegionAndStatusNewestFirst(regionId, MatchStatus.FINISHED, PageRequest.of(0, RECENT_LIMIT))
                .stream().map(MatchResponse::from).toList();
        return new RegionMatchesResponse(upcoming, recent);
    }

    // -- scheduling rules ---------------------------------------------------------

    private void requireSchedulable(Instant kickoff) {
        Instant now = Instant.now();
        Duration minLead = lineupLock.plus(EXTRA_LEAD);
        if (kickoff.isBefore(now.plus(minLead))) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "KICKOFF_TOO_SOON",
                    "Kickoff must be at least " + minLead.toMinutes() + " minutes from now");
        }
        if (kickoff.isAfter(now.plus(maxScheduleAhead))) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "KICKOFF_TOO_FAR",
                    "Kickoff can be at most " + maxScheduleAhead.toDays() + " days ahead");
        }
    }

    /** One match at a time per team: nothing scheduled/live near the requested kickoff for either side. */
    private void requireNoConflicts(UUID homeId, UUID awayId, Instant kickoff) {
        long clashes = matchRepository.countInvolvingRegionsBetween(
                homeId, awayId,
                EnumSet.of(MatchStatus.SCHEDULED, MatchStatus.LIVE),
                kickoff.minus(conflictWindow), kickoff.plus(conflictWindow));
        if (clashes > 0) {
            throw new ConflictException("MATCH_CONFLICT",
                    "One of the teams already has a match around that time");
        }
    }
}
