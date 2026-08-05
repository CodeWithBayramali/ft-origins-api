package com.codewithbayramali.ftorigins.geo.controller;

import com.codewithbayramali.ftorigins.auth.security.CustomUserDetails;
import com.codewithbayramali.ftorigins.geo.dto.TeamResponse;
import com.codewithbayramali.ftorigins.geo.service.TeamRosterService;
import com.codewithbayramali.ftorigins.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamRosterService teamRosterService;

    public TeamController(TeamRosterService teamRosterService) {
        this.teamRosterService = teamRosterService;
    }

    /** The caller's team + full squad with effective ratings — the "My Team" page's data. */
    @GetMapping("/mine")
    public ApiResponse<TeamResponse> myTeam(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok(teamRosterService.getMyTeam(principal.getId()));
    }
}
