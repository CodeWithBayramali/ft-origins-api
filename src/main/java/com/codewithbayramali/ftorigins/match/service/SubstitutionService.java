package com.codewithbayramali.ftorigins.match.service;

import com.codewithbayramali.ftorigins.exception.BusinessException;
import com.codewithbayramali.ftorigins.exception.NotFoundException;
import com.codewithbayramali.ftorigins.geo.model.Region;
import com.codewithbayramali.ftorigins.match.dto.SubstitutionRequest;
import com.codewithbayramali.ftorigins.match.dto.SubstitutionResponse;
import com.codewithbayramali.ftorigins.match.engine.EnginePlayer;
import com.codewithbayramali.ftorigins.match.engine.EngineSide;
import com.codewithbayramali.ftorigins.match.model.Match;
import com.codewithbayramali.ftorigins.match.model.MatchCommand;
import com.codewithbayramali.ftorigins.match.model.MatchCommandType;
import com.codewithbayramali.ftorigins.match.model.MatchParticipant;
import com.codewithbayramali.ftorigins.match.model.MatchStatus;
import com.codewithbayramali.ftorigins.match.repository.MatchCommandRepository;
import com.codewithbayramali.ftorigins.match.repository.MatchParticipantRepository;
import com.codewithbayramali.ftorigins.match.repository.MatchRepository;
import com.codewithbayramali.ftorigins.player.model.Player;
import com.codewithbayramali.ftorigins.player.repository.PlayerRepository;
import com.codewithbayramali.ftorigins.player.service.EffectiveAttributes;
import com.codewithbayramali.ftorigins.player.service.PlayerUpkeepService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * The founder's live influence: substitutions. A sub is validated against the running simulation,
 * persisted as a participant handover + a command-log row (the determinism trail), and queued on
 * the engine to take effect at the next minute boundary. Everything happens under the match's
 * registry lock, so a scheduler tick can never interleave with a half-applied sub.
 */
@Service
public class SubstitutionService {

    private static final Logger log = LoggerFactory.getLogger(SubstitutionService.class);

    private final MatchRepository matchRepository;
    private final PlayerRepository playerRepository;
    private final MatchParticipantRepository participantRepository;
    private final MatchCommandRepository commandRepository;
    private final LiveMatchRegistry registry;
    private final FounderGuard founderGuard;
    private final PlayerUpkeepService upkeep;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final int maxSubstitutions;

    public SubstitutionService(MatchRepository matchRepository,
                               PlayerRepository playerRepository,
                               MatchParticipantRepository participantRepository,
                               MatchCommandRepository commandRepository,
                               LiveMatchRegistry registry,
                               FounderGuard founderGuard,
                               PlayerUpkeepService upkeep,
                               ObjectMapper objectMapper,
                               TransactionTemplate transactionTemplate,
                               @Value("${app.match.max-substitutions:3}") int maxSubstitutions) {
        this.matchRepository = matchRepository;
        this.playerRepository = playerRepository;
        this.participantRepository = participantRepository;
        this.commandRepository = commandRepository;
        this.registry = registry;
        this.founderGuard = founderGuard;
        this.upkeep = upkeep;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.maxSubstitutions = maxSubstitutions;
    }

    public SubstitutionResponse substitute(UUID userId, UUID matchId, SubstitutionRequest request) {
        Match match = matchRepository.findDetailById(matchId)
                .orElseThrow(() -> new NotFoundException("MATCH_NOT_FOUND", "Match not found"));
        if (match.getStatus() != MatchStatus.LIVE) {
            throw new BusinessException(HttpStatus.CONFLICT, "MATCH_NOT_LIVE",
                    "Substitutions are only possible while the match is live");
        }
        Player caller = playerRepository.findDetailByUserId(userId)
                .orElseThrow(() -> new NotFoundException("PLAYER_NOT_FOUND", "No player profile for this account"));
        Region side = resolveSide(match, caller);
        founderGuard.requireFounder(caller, side);

        int outSlot = request.getOutSlot();
        UUID inPlayerId = request.getInPlayerId();
        EngineSide engineSide = side.getId().equals(match.getHomeRegion().getId())
                ? EngineSide.HOME : EngineSide.AWAY;

        return registry.withMatch(match, live -> {
            if (live.sim().finished()) {
                throw new BusinessException(HttpStatus.CONFLICT, "MATCH_OVER",
                        "The match just ended");
            }
            if (!live.sim().isSlotActive(engineSide, outSlot)) {
                throw new BusinessException(HttpStatus.CONFLICT, "SLOT_INACTIVE",
                        "That slot's player was sent off — a red card can't be substituted");
            }
            if (commandRepository.countByMatchIdAndRegionId(matchId, side.getId()) >= maxSubstitutions) {
                throw new BusinessException(HttpStatus.CONFLICT, "SUBSTITUTION_LIMIT",
                        "No substitutions left (" + maxSubstitutions + " per match)");
            }
            int applyMinute = live.sim().minute() + 1;
            try {
                transactionTemplate.executeWithoutResult(tx ->
                        persistAndQueue(live, match, side, engineSide, caller, outSlot, inPlayerId, applyMinute));
            } catch (RuntimeException e) {
                // The sim may hold a queued sub the DB rejected — drop it; rebuild from DB truth.
                registry.evict(matchId);
                throw e;
            }
            log.info("Queued substitution on match {}: slot {} at minute {} (sim is at {})",
                    matchId, outSlot, applyMinute, live.sim().minute());
            return new SubstitutionResponse(matchId, side.getId(), outSlot, applyMinute, inPlayerId);
        });
    }

    /** Runs inside both the registry lock and a transaction: DB writes + engine queue, atomically. */
    private void persistAndQueue(LiveMatchRegistry.LiveMatch live, Match match, Region side,
                                 EngineSide engineSide, Player caller,
                                 int outSlot, UUID inPlayerId, int applyMinute) {
        Player incoming = playerRepository.findById(inPlayerId)
                .orElseThrow(() -> new NotFoundException("PLAYER_NOT_FOUND", "Incoming player not found"));
        if (incoming.getRegion() == null || !side.getId().equals(incoming.getRegion().getId())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "NOT_A_MEMBER",
                    "The incoming player is not a member of this team");
        }
        if (participantRepository.existsByMatchIdAndPlayerId(match.getId(), inPlayerId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "ALREADY_IN_MATCH",
                    "That player already took part in this match — no re-entry");
        }
        MatchParticipant out = participantRepository
                .findByMatchIdAndRegionIdAndSlotAndLeftMinuteIsNull(match.getId(), side.getId(), outSlot)
                .orElseThrow(() -> new NotFoundException("SLOT_NOT_FOUND", "No active player in that slot"));

        // Snapshot the incoming player NOW (energy regen credited first) — this row is what a
        // rebuilt simulation replays, so live and rebuilt matches see identical stats.
        upkeep.refresh(incoming);
        EffectiveAttributes eff = EffectiveAttributes.of(incoming);

        out.setLeftMinute(applyMinute);
        participantRepository.save(out);

        MatchParticipant sub = new MatchParticipant();
        sub.setMatch(out.getMatch());
        sub.setRegion(out.getRegion());
        sub.setSlot(outSlot);
        sub.setRole(out.getRole());
        sub.setEnteredMinute(applyMinute);
        sub.setPlayer(incoming);
        sub.setAiFill(false);
        sub.setDisplayName(incoming.getDisplayName());
        sub.setKitNumber(incoming.getAppearance().getKitNumber());
        sub.setPreferredFoot(incoming.getPreferredFoot());
        sub.setPace(eff.pace());
        sub.setShooting(eff.shooting());
        sub.setPassing(eff.passing());
        sub.setDribbling(eff.dribbling());
        sub.setDefending(eff.defending());
        sub.setPhysical(eff.physical());
        sub.setGoalkeeping(eff.goalkeeping());
        sub.setCurve(eff.curve());
        sub.setEnergy(incoming.getEnergy());
        participantRepository.save(sub);

        MatchCommand command = new MatchCommand();
        command.setMatch(out.getMatch());
        command.setRegion(out.getRegion());
        command.setCommandType(MatchCommandType.SUBSTITUTION);
        command.setApplyMinute(applyMinute);
        command.setCreatedByPlayerId(caller.getId());
        command.setPayload(commandPayload(outSlot, inPlayerId));
        commandRepository.save(command);

        live.sim().queueSubstitution(engineSide, outSlot, new EnginePlayer(outSlot, out.getRole(),
                eff.pace(), eff.shooting(), eff.passing(), eff.dribbling(),
                eff.defending(), eff.physical(), eff.goalkeeping(), eff.curve(),
                incoming.getEnergy()));
    }

    private String commandPayload(int outSlot, UUID inPlayerId) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("outSlot", outSlot);
        payload.put("inPlayerId", inPlayerId.toString());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unserializable substitution payload", e);
        }
    }

    private Region resolveSide(Match match, Player caller) {
        UUID regionId = caller.getRegion().getId();
        if (regionId.equals(match.getHomeRegion().getId())) {
            return match.getHomeRegion();
        }
        if (regionId.equals(match.getAwayRegion().getId())) {
            return match.getAwayRegion();
        }
        throw new BusinessException(HttpStatus.FORBIDDEN, "NOT_IN_MATCH",
                "Your team is not part of this match");
    }
}
