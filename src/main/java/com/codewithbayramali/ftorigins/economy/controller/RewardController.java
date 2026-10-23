package com.codewithbayramali.ftorigins.economy.controller;

import com.codewithbayramali.ftorigins.auth.security.CustomUserDetails;
import com.codewithbayramali.ftorigins.economy.dto.DailyRewardResponse;
import com.codewithbayramali.ftorigins.economy.service.DailyRewardService;
import com.codewithbayramali.ftorigins.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rewards")
public class RewardController {

    private final DailyRewardService dailyRewardService;

    public RewardController(DailyRewardService dailyRewardService) {
        this.dailyRewardService = dailyRewardService;
    }

    /** Today's daily reward: still to collect or not, the ladder day and what it pays. */
    @GetMapping("/daily")
    public ApiResponse<DailyRewardResponse> daily(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok(dailyRewardService.status(principal.getId()));
    }

    /** Collects today's daily reward (DAILY_REWARD_CLAIMED if it already was). */
    @PostMapping("/daily/claim")
    public ApiResponse<DailyRewardResponse> claimDaily(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok(dailyRewardService.claim(principal.getId()), "Daily reward collected");
    }
}
