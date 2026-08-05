package com.codewithbayramali.ftorigins.match.controller;

import com.codewithbayramali.ftorigins.auth.security.CustomUserDetails;
import com.codewithbayramali.ftorigins.match.dto.CreateFriendlyRequest;
import com.codewithbayramali.ftorigins.match.dto.LineupRequest;
import com.codewithbayramali.ftorigins.match.dto.LineupResponse;
import com.codewithbayramali.ftorigins.match.dto.MatchEventsResponse;
import com.codewithbayramali.ftorigins.match.dto.MatchLineupsResponse;
import com.codewithbayramali.ftorigins.match.dto.MatchResponse;
import com.codewithbayramali.ftorigins.match.dto.RegionMatchesResponse;
import com.codewithbayramali.ftorigins.match.dto.SubstitutionRequest;
import com.codewithbayramali.ftorigins.match.dto.SubstitutionResponse;
import com.codewithbayramali.ftorigins.match.service.LineupService;
import com.codewithbayramali.ftorigins.match.service.MatchFeedService;
import com.codewithbayramali.ftorigins.match.service.MatchService;
import com.codewithbayramali.ftorigins.match.service.SubstitutionService;
import com.codewithbayramali.ftorigins.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/matches")
public class MatchController {

    private final MatchService matchService;
    private final LineupService lineupService;
    private final MatchFeedService feedService;
    private final SubstitutionService substitutionService;

    public MatchController(MatchService matchService, LineupService lineupService,
                           MatchFeedService feedService, SubstitutionService substitutionService) {
        this.matchService = matchService;
        this.lineupService = lineupService;
        this.feedService = feedService;
        this.substitutionService = substitutionService;
    }

    /** Founder-only: book a friendly against another team (the caller's team plays at home). */
    @PostMapping("/friendly")
    public ApiResponse<MatchResponse> createFriendly(@AuthenticationPrincipal CustomUserDetails principal,
                                                     @Valid @RequestBody CreateFriendlyRequest request) {
        return ApiResponse.ok(matchService.createFriendly(principal.getId(), request), "Friendly match scheduled");
    }

    /** The caller's team's matches: upcoming/live + recent results. */
    @GetMapping("/mine")
    public ApiResponse<RegionMatchesResponse> myMatches(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok(matchService.myMatches(principal.getId()));
    }

    /** Match header/card — open to spectators. */
    @GetMapping("/{id}")
    public ApiResponse<MatchResponse> getMatch(@PathVariable UUID id) {
        return ApiResponse.ok(matchService.getMatch(id));
    }

    /** The caller's side of the lineup (submitted, or a live suggestion when none exists yet). */
    @GetMapping("/{id}/lineup")
    public ApiResponse<LineupResponse> getLineup(@AuthenticationPrincipal CustomUserDetails principal,
                                                 @PathVariable UUID id) {
        return ApiResponse.ok(lineupService.getMyLineup(principal.getId(), id));
    }

    /** Founder-only: submit the lineup (formation + slot-ordered starters; null slot = AI filler). */
    @PutMapping("/{id}/lineup")
    public ApiResponse<LineupResponse> submitLineup(@AuthenticationPrincipal CustomUserDetails principal,
                                                    @PathVariable UUID id,
                                                    @Valid @RequestBody LineupRequest request) {
        return ApiResponse.ok(lineupService.submit(principal.getId(), id, request), "Lineup saved");
    }

    /** Timeline poll: events after the caller's cursor (afterSeq=-1 = from kickoff / full replay). */
    @GetMapping("/{id}/events")
    public ApiResponse<MatchEventsResponse> getEvents(@PathVariable UUID id,
                                                      @RequestParam(defaultValue = "-1") int afterSeq) {
        return ApiResponse.ok(feedService.getEvents(id, afterSeq));
    }

    /** Both sides' lineups for the spectator/visualizer view. */
    @GetMapping("/{id}/lineups")
    public ApiResponse<MatchLineupsResponse> getLineups(@PathVariable UUID id) {
        return ApiResponse.ok(feedService.getLineups(id));
    }

    /** Founder-only, live matches: substitute a bench member into a lineup slot (next minute). */
    @PostMapping("/{id}/substitutions")
    public ApiResponse<SubstitutionResponse> substitute(@AuthenticationPrincipal CustomUserDetails principal,
                                                        @PathVariable UUID id,
                                                        @Valid @RequestBody SubstitutionRequest request) {
        return ApiResponse.ok(substitutionService.substitute(principal.getId(), id, request),
                "Substitution queued");
    }
}
