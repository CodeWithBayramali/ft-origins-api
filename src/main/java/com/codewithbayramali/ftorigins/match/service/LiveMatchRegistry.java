package com.codewithbayramali.ftorigins.match.service;

import com.codewithbayramali.ftorigins.match.engine.EnginePlayer;
import com.codewithbayramali.ftorigins.match.engine.EngineSide;
import com.codewithbayramali.ftorigins.match.engine.EngineTeam;
import com.codewithbayramali.ftorigins.match.engine.MatchSetup;
import com.codewithbayramali.ftorigins.match.engine.MatchSimulation;
import com.codewithbayramali.ftorigins.match.model.Match;
import com.codewithbayramali.ftorigins.match.model.MatchCommand;
import com.codewithbayramali.ftorigins.match.model.MatchCommandType;
import com.codewithbayramali.ftorigins.match.model.MatchParticipant;
import com.codewithbayramali.ftorigins.match.repository.MatchCommandRepository;
import com.codewithbayramali.ftorigins.match.repository.MatchEventRepository;
import com.codewithbayramali.ftorigins.match.repository.MatchParticipantRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Holds each LIVE match's in-memory {@link MatchSimulation} behind a per-match lock (scheduler
 * ticks and founder commands never interleave). The memory is a pure cache: on a restart — or
 * whenever {@link #evict} drops a possibly-diverged sim — the state is rebuilt from the DB truth
 * by replaying seed + kickoff snapshots + the command log up to the persisted frontier, which the
 * engine guarantees walks the exact same path the live run did.
 */
@Component
public class LiveMatchRegistry {

    private static final Logger log = LoggerFactory.getLogger(LiveMatchRegistry.class);

    /** One live match: its simulation and the next event seq (gapless, so count = next). */
    public static final class LiveMatch {
        private final ReentrantLock lock = new ReentrantLock();
        private MatchSimulation sim;
        private int nextSeq;

        public MatchSimulation sim() {
            return sim;
        }

        public int takeSeq() {
            return nextSeq++;
        }
    }

    private final ConcurrentHashMap<UUID, LiveMatch> cache = new ConcurrentHashMap<>();

    private final MatchParticipantRepository participantRepository;
    private final MatchCommandRepository commandRepository;
    private final MatchEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public LiveMatchRegistry(MatchParticipantRepository participantRepository,
                             MatchCommandRepository commandRepository,
                             MatchEventRepository eventRepository,
                             ObjectMapper objectMapper) {
        this.participantRepository = participantRepository;
        this.commandRepository = commandRepository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    /** Runs {@code action} with the match's simulation under its lock, rebuilding it if absent. */
    public <T> T withMatch(Match match, Function<LiveMatch, T> action) {
        LiveMatch live = cache.computeIfAbsent(match.getId(), id -> new LiveMatch());
        live.lock.lock();
        try {
            if (live.sim == null) {
                rebuild(live, match);
            }
            return action.apply(live);
        } finally {
            live.lock.unlock();
        }
    }

    /** Drops the in-memory state; the next access rebuilds from the DB truth. */
    public void evict(UUID matchId) {
        if (cache.remove(matchId) != null) log.info("Evicted live simulation for match {}", matchId);
    }

    private void rebuild(LiveMatch live, Match match) {
        Long seed = match.getSeed();
        if (seed == null) {
            throw new IllegalStateException("Match " + match.getId() + " has no seed (not kicked off)");
        }
        List<MatchParticipant> participants =
                participantRepository.findByMatchIdOrderBySlotAscEnteredMinuteAsc(match.getId());
        UUID homeId = match.getHomeRegion().getId();
        UUID awayId = match.getAwayRegion().getId();
        MatchSimulation sim = new MatchSimulation(new MatchSetup(
                starters(participants, homeId), starters(participants, awayId), seed));

        // Replay the command log at the recorded minutes while walking back to the frontier.
        Map<Integer, List<MatchCommand>> commandsByMinute =
                commandRepository.findByMatchIdOrderByApplyMinuteAscCreatedAtAsc(match.getId()).stream()
                        .collect(Collectors.groupingBy(MatchCommand::getApplyMinute,
                                LinkedHashMap::new, Collectors.toList()));
        for (int m = 1; m <= match.getSimMinute(); m++) {
            queueCommands(sim, match, commandsByMinute.remove(m));
            sim.advanceMinute(); // events discarded — they are already persisted
        }
        // Commands accepted but not yet simulated (e.g. restart right after submit): they were
        // meant for the minute after the frontier, which is exactly the next one to be simulated.
        commandsByMinute.values().forEach(cmds -> queueCommands(sim, match, cmds));

        live.sim = sim;
        live.nextSeq = (int) eventRepository.countByMatchId(match.getId());
        log.info("Rebuilt simulation for match {} up to minute {} (replayed commands are silent)",
                match.getId(), match.getSimMinute());
    }

    private void queueCommands(MatchSimulation sim, Match match, List<MatchCommand> commands) {
        if (commands == null) {
            return;
        }
        for (MatchCommand command : commands) {
            if (command.getCommandType() != MatchCommandType.SUBSTITUTION) {
                continue;
            }
            int slot = readOutSlot(command);
            UUID regionId = command.getRegion().getId();
            EngineSide side = regionId.equals(match.getHomeRegion().getId())
                    ? EngineSide.HOME : EngineSide.AWAY;
            MatchParticipant sub = participantRepository
                    .findByMatchIdAndRegionIdAndSlotAndEnteredMinute(
                            match.getId(), regionId, slot, command.getApplyMinute())
                    .orElseThrow(() -> new IllegalStateException(
                            "No participant row for substitution command " + command.getId()));
            sim.queueSubstitution(side, slot, toEnginePlayer(sub));
        }
    }

    private int readOutSlot(MatchCommand command) {
        try {
            JsonNode payload = objectMapper.readTree(command.getPayload());
            return payload.get("outSlot").asInt();
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unreadable payload on command " + command.getId(), e);
        }
    }

    private EngineTeam starters(List<MatchParticipant> participants, UUID regionId) {
        List<EnginePlayer> players = participants.stream()
                .filter(p -> p.getRegion().getId().equals(regionId) && p.getEnteredMinute() == 0)
                .map(this::toEnginePlayer)
                .toList();
        return new EngineTeam(players);
    }

    private EnginePlayer toEnginePlayer(MatchParticipant p) {
        return new EnginePlayer(p.getSlot(), p.getRole(),
                stat(p, p.getPace()), stat(p, p.getShooting()), stat(p, p.getPassing()),
                stat(p, p.getDribbling()), stat(p, p.getDefending()), stat(p, p.getPhysical()),
                stat(p, p.getGoalkeeping()), stat(p, p.getCurve()), stat(p, p.getEnergy()));
    }

    private static int stat(MatchParticipant p, Integer value) {
        if (value == null) {
            throw new IllegalStateException("Participant " + p.getId() + " has no kickoff snapshot");
        }
        return value;
    }
}
