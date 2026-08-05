package com.codewithbayramali.ftorigins.geo.service;

import com.codewithbayramali.ftorigins.exception.NotFoundException;
import com.codewithbayramali.ftorigins.geo.dto.TeamMatchResponse;
import com.codewithbayramali.ftorigins.geo.dto.TeamMemberResponse;
import com.codewithbayramali.ftorigins.geo.dto.TeamResponse;
import com.codewithbayramali.ftorigins.geo.dto.TeamStatsResponse;
import com.codewithbayramali.ftorigins.geo.model.Region;
import com.codewithbayramali.ftorigins.match.model.Formation;
import com.codewithbayramali.ftorigins.match.model.Match;
import com.codewithbayramali.ftorigins.match.model.MatchStatus;
import com.codewithbayramali.ftorigins.match.repository.MatchRepository;
import com.codewithbayramali.ftorigins.player.model.Player;
import com.codewithbayramali.ftorigins.player.repository.PlayerRepository;
import com.codewithbayramali.ftorigins.player.service.EffectiveAttributes;
import com.codewithbayramali.ftorigins.player.service.PlayerUpkeepService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

/**
 * The caller's team plus its squad — the read side of the "My Team" management page. Every member
 * is refreshed through {@link PlayerUpkeepService} first, so the overalls the founder picks from are
 * the same numbers the engine will freeze at kickoff (energy regen and inactivity decay applied).
 */
@Service
public class TeamRosterService {

    /** How many finished matches the page lists. */
    private static final int RECENT_LIMIT = 12;

    /** How many results the form string carries. */
    private static final int FORM_LENGTH = 5;

    private final PlayerRepository playerRepository;
    private final MatchRepository matchRepository;
    private final PlayerUpkeepService upkeep;
    private final int maxMembers;

    public TeamRosterService(PlayerRepository playerRepository,
                             MatchRepository matchRepository,
                             PlayerUpkeepService upkeep,
                             @Value("${app.team.max-members:10}") int maxMembers) {
        this.playerRepository = playerRepository;
        this.matchRepository = matchRepository;
        this.upkeep = upkeep;
        this.maxMembers = maxMembers;
    }

    @Transactional
    public TeamResponse getMyTeam(UUID userId) {
        Player caller = playerRepository.findDetailByUserId(userId)
                .orElseThrow(() -> new NotFoundException("PLAYER_NOT_FOUND", "No player profile for this account"));
        Region region = caller.getRegion();
        UUID callerId = caller.getId();
        UUID founderId = region.getFounderPlayerId();

        List<Player> squad = playerRepository.findByRegionId(region.getId());
        squad.forEach(upkeep::refresh);

        List<TeamMemberResponse> members = squad.stream()
                .map(p -> TeamMemberResponse.from(p, founderId, callerId))
                // Best first: the founder's default reading order, and what auto-lineups pick from.
                .sorted(Comparator.comparingInt(TeamMemberResponse::getOverall).reversed()
                        .thenComparing(m -> m.getDisplayName() == null ? "" : m.getDisplayName()))
                .toList();

        boolean amFounder = founderId != null && founderId.equals(callerId);
        UUID regionId = region.getId();

        // Oldest -> newest from the repository; the page wants newest first.
        List<Match> finished = matchRepository.findByRegionAndStatuses(
                regionId, EnumSet.of(MatchStatus.FINISHED));
        List<TeamMatchResponse> recent = new ArrayList<>();
        for (int i = finished.size() - 1; i >= 0 && recent.size() < RECENT_LIMIT; i--) {
            recent.add(TeamMatchResponse.from(finished.get(i), regionId));
        }
        List<TeamMatchResponse> upcoming = matchRepository
                .findByRegionAndStatuses(regionId, EnumSet.of(MatchStatus.SCHEDULED, MatchStatus.LIVE))
                .stream().map(m -> TeamMatchResponse.from(m, regionId)).toList();

        return TeamResponse.of(region, members, amFounder, maxMembers, Formation.SQUAD_SIZE,
                statsOf(finished, regionId), recent, upcoming);
    }

    /** Points and goals over every finished match, plus the last {@link #FORM_LENGTH} results. */
    private TeamStatsResponse statsOf(List<Match> finished, UUID regionId) {
        if (finished.isEmpty()) {
            return TeamStatsResponse.empty();
        }
        int wins = 0, draws = 0, losses = 0, goalsFor = 0, goalsAgainst = 0;
        List<String> form = new ArrayList<>();
        for (int i = finished.size() - 1; i >= 0; i--) {
            TeamMatchResponse view = TeamMatchResponse.from(finished.get(i), regionId);
            goalsFor += view.getMyScore();
            goalsAgainst += view.getOpponentScore();
            String result = view.getResult();
            if ("W".equals(result)) wins++;
            else if ("D".equals(result)) draws++;
            else losses++;
            if (form.size() < FORM_LENGTH && result != null) form.add(result);
        }
        return new TeamStatsResponse(finished.size(), wins, draws, losses,
                goalsFor, goalsAgainst, goalsFor - goalsAgainst, wins * 3 + draws, form);
    }

    /** Kept next to the roster read so callers can rank members for a slot the same way. */
    public static int overallOf(Player player) {
        return EffectiveAttributes.of(player).overall();
    }
}
