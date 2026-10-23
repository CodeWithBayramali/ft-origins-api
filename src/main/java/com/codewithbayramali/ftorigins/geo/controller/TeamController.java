package com.codewithbayramali.ftorigins.geo.controller;

import com.codewithbayramali.ftorigins.auth.security.CustomUserDetails;
import com.codewithbayramali.ftorigins.geo.dto.CrestDto;
import com.codewithbayramali.ftorigins.geo.dto.TeamResponse;
import com.codewithbayramali.ftorigins.geo.service.TeamCrestService;
import com.codewithbayramali.ftorigins.geo.service.TeamRosterService;
import com.codewithbayramali.ftorigins.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamRosterService teamRosterService;
    private final TeamCrestService teamCrestService;

    public TeamController(TeamRosterService teamRosterService, TeamCrestService teamCrestService) {
        this.teamRosterService = teamRosterService;
        this.teamCrestService = teamCrestService;
    }

    /** The caller's team + full squad with effective ratings — the "My Team" page's data. */
    @GetMapping("/mine")
    public ApiResponse<TeamResponse> myTeam(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok(teamRosterService.getMyTeam(principal.getId()));
    }

    /** Founder-only: replaces the team crest; answers with the same team view as GET /mine. */
    @PutMapping("/mine/crest")
    public ApiResponse<TeamResponse> changeCrest(@AuthenticationPrincipal CustomUserDetails principal,
                                                 @Valid @RequestBody CrestDto crest) {
        return ApiResponse.ok(teamCrestService.changeMyCrest(principal.getId(), crest), "Crest updated");
    }
}
