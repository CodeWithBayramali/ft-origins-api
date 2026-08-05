package com.codewithbayramali.ftorigins.match.service;

import com.codewithbayramali.ftorigins.exception.NotFoundException;
import com.codewithbayramali.ftorigins.geo.model.Region;
import com.codewithbayramali.ftorigins.match.dto.MatchEventResponse;
import com.codewithbayramali.ftorigins.match.dto.MatchEventsResponse;
import com.codewithbayramali.ftorigins.match.dto.MatchLineupsResponse;
import com.codewithbayramali.ftorigins.match.dto.MatchParticipantResponse;
import com.codewithbayramali.ftorigins.match.dto.MatchSideLineupResponse;
import com.codewithbayramali.ftorigins.match.model.Match;
import com.codewithbayramali.ftorigins.match.model.MatchEvent;
import com.codewithbayramali.ftorigins.match.model.MatchLineup;
import com.codewithbayramali.ftorigins.match.model.MatchParticipant;
import com.codewithbayramali.ftorigins.match.repository.MatchEventRepository;
import com.codewithbayramali.ftorigins.match.repository.MatchLineupRepository;
import com.codewithbayramali.ftorigins.match.repository.MatchParticipantRepository;
import com.codewithbayramali.ftorigins.match.repository.MatchRepository;
import com.codewithbayramali.ftorigins.player.model.Player;
import com.codewithbayramali.ftorigins.player.service.EffectiveAttributes;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * The spectator surface: the polled event timeline (live viewing AND replays are the same read —
 * a replay is just a cursor over a finished match) and both lineups for the visualizer. Storage
 * only ever contains simulated minutes, so a live match's future is structurally unreadable.
 */
@Service
public class MatchFeedService {

    private final MatchRepository matchRepository;
    private final MatchEventRepository eventRepository;
    private final MatchLineupRepository lineupRepository;
    private final MatchParticipantRepository participantRepository;
    private final ObjectMapper objectMapper;
    private final int simMinuteRealSeconds;

    public MatchFeedService(MatchRepository matchRepository,
                            MatchEventRepository eventRepository,
                            MatchLineupRepository lineupRepository,
                            MatchParticipantRepository participantRepository,
                            ObjectMapper objectMapper,
                            @Value("${app.match.sim-minute-real-seconds:8}") int simMinuteRealSeconds) {
        this.matchRepository = matchRepository;
        this.eventRepository = eventRepository;
        this.lineupRepository = lineupRepository;
        this.participantRepository = participantRepository;
        this.objectMapper = objectMapper;
        this.simMinuteRealSeconds = simMinuteRealSeconds;
    }

    /** Timeline tail after {@code afterSeq} (-1 = from the start). Empty while SCHEDULED. */
    @Transactional(readOnly = true)
    public MatchEventsResponse getEvents(UUID matchId, int afterSeq) {
        Match match = loadMatch(matchId);
        List<MatchEventResponse> events = eventRepository
                .findByMatchIdAndSeqGreaterThanOrderBySeq(matchId, afterSeq)
                .stream().map(this::toEventResponse).toList();
        return new MatchEventsResponse(match.getId(), match.getStatus(), match.getSimMinute(),
                match.getHomeScore(), match.getAwayScore(), match.getKickoffAt(),
                simMinuteRealSeconds, events);
    }

    /** Both sides for the visualizer; sides are empty/formation-null until they're finalized. */
    @Transactional(readOnly = true)
    public MatchLineupsResponse getLineups(UUID matchId) {
        Match match = loadMatch(matchId);
        List<MatchParticipant> participants =
                participantRepository.findByMatchIdOrderBySlotAscEnteredMinuteAsc(matchId);
        return new MatchLineupsResponse(match.getId(),
                toSideResponse(match, match.getHomeRegion(), participants),
                toSideResponse(match, match.getAwayRegion(), participants));
    }

    private Match loadMatch(UUID matchId) {
        return matchRepository.findDetailById(matchId)
                .orElseThrow(() -> new NotFoundException("MATCH_NOT_FOUND", "Match not found"));
    }

    private MatchEventResponse toEventResponse(MatchEvent event) {
        JsonNode payload = null;
        if (event.getPayload() != null) {
            try {
                payload = objectMapper.readTree(event.getPayload());
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Unreadable payload on event " + event.getId(), e);
            }
        }
        return new MatchEventResponse(event.getSeq(), event.getSimMinute(), event.getSimSecond(),
                event.getEventType(), payload);
    }

    private MatchSideLineupResponse toSideResponse(Match match, Region region,
                                                   List<MatchParticipant> all) {
        MatchLineup lineup = lineupRepository
                .findByMatchIdAndRegionId(match.getId(), region.getId()).orElse(null);
        List<MatchParticipantResponse> participants = all.stream()
                .filter(p -> p.getRegion().getId().equals(region.getId()))
                .map(this::toParticipantResponse)
                .toList();
        return new MatchSideLineupResponse(region.getId(),
                lineup != null ? lineup.getFormation() : null,
                lineup != null ? lineup.getAutoGenerated() : null,
                participants);
    }

    private MatchParticipantResponse toParticipantResponse(MatchParticipant p) {
        Player player = p.getPlayer();
        return new MatchParticipantResponse(
                p.getSlot(), p.getRole(),
                player != null ? player.getId() : null,
                p.getDisplayName() != null ? p.getDisplayName()
                        : player != null ? player.getDisplayName() : null,
                p.getKitNumber() != null ? p.getKitNumber()
                        : player != null ? player.getAppearance().getKitNumber() : null,
                p.getAiFill(), p.getEnteredMinute(), p.getLeftMinute(),
                overall(p, player),
                pace(p, player),
                player != null ? player.getAppearance() : null,
                player != null ? player.getBootDesignId() : null);
    }

    /** Effective pace: the frozen kickoff snapshot once it exists, the live value before kickoff. */
    private Integer pace(MatchParticipant p, Player player) {
        if (p.getPace() != null) {
            return p.getPace();
        }
        return player != null ? EffectiveAttributes.of(player).pace() : null;
    }

    /** The frozen kickoff snapshot once it exists; the live effective value before kickoff. */
    private Integer overall(MatchParticipant p, Player player) {
        if (p.getPace() != null) {
            int sum = p.getPace() + p.getShooting() + p.getPassing() + p.getDribbling()
                    + p.getDefending() + p.getPhysical() + p.getGoalkeeping() + p.getCurve();
            return (int) Math.round(sum / 8.0);
        }
        return player != null ? EffectiveAttributes.of(player).overall() : null;
    }
}
