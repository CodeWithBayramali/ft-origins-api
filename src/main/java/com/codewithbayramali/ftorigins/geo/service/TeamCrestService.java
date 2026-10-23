package com.codewithbayramali.ftorigins.geo.service;

import com.codewithbayramali.ftorigins.exception.NotFoundException;
import com.codewithbayramali.ftorigins.geo.dto.CrestDto;
import com.codewithbayramali.ftorigins.geo.dto.TeamResponse;
import com.codewithbayramali.ftorigins.geo.model.Region;
import com.codewithbayramali.ftorigins.match.service.FounderGuard;
import com.codewithbayramali.ftorigins.player.model.Player;
import com.codewithbayramali.ftorigins.player.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Changes a team's crest after founding. Founder-only: the crest is the team's identity, so it is held
 * by the same seat that runs the team (see {@link FounderGuard}).
 */
@Service
public class TeamCrestService {

    private final PlayerRepository playerRepository;
    private final FounderGuard founderGuard;
    private final TeamCrestPolicy crestPolicy;
    private final TeamRosterService teamRosterService;

    public TeamCrestService(PlayerRepository playerRepository,
                            FounderGuard founderGuard,
                            TeamCrestPolicy crestPolicy,
                            TeamRosterService teamRosterService) {
        this.playerRepository = playerRepository;
        this.founderGuard = founderGuard;
        this.crestPolicy = crestPolicy;
        this.teamRosterService = teamRosterService;
    }

    /**
     * Replaces the caller's team crest; 403 NOT_FOUNDER unless the caller holds the founder seat. Answers
     * with the "My Team" view — the same body GET /teams/mine returns — so the client refreshes in one step.
     */
    @Transactional
    public TeamResponse changeMyCrest(UUID userId, CrestDto crest) {
        Player caller = playerRepository.findDetailByUserId(userId)
                .orElseThrow(() -> new NotFoundException("PLAYER_NOT_FOUND", "No player profile for this account"));
        Region team = caller.getRegion();
        founderGuard.requireFounder(caller, team);
        crestPolicy.apply(team, crest);   // managed entity: dirty checking writes it on commit
        return teamRosterService.getMyTeam(userId);
    }
}
