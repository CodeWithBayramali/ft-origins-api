package com.codewithbayramali.ftorigins.player.controller;

import com.codewithbayramali.ftorigins.auth.security.CustomUserDetails;
import com.codewithbayramali.ftorigins.player.dto.PlayerResponse;
import com.codewithbayramali.ftorigins.player.dto.TrainRequest;
import com.codewithbayramali.ftorigins.player.dto.TrainResult;
import com.codewithbayramali.ftorigins.player.dto.UpdatePlayerRequest;
import com.codewithbayramali.ftorigins.player.service.PlayerService;
import com.codewithbayramali.ftorigins.player.service.TrainingService;
import com.codewithbayramali.ftorigins.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/players")
public class PlayerController {

    private final PlayerService playerService;
    private final TrainingService trainingService;

    public PlayerController(PlayerService playerService, TrainingService trainingService) {
        this.playerService = playerService;
        this.trainingService = trainingService;
    }

    @GetMapping("/me")
    public ApiResponse<PlayerResponse> me(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok(playerService.getMyProfile(principal.getId()));
    }

    @PatchMapping("/me")
    public ApiResponse<PlayerResponse> updateMe(@AuthenticationPrincipal CustomUserDetails principal,
                                                @Valid @RequestBody UpdatePlayerRequest request) {
        return ApiResponse.ok(playerService.updateProfile(principal.getId(), request), "Profile updated");
    }

    @PostMapping("/me/train")
    public ApiResponse<TrainResult> train(@AuthenticationPrincipal CustomUserDetails principal,
                                          @Valid @RequestBody TrainRequest request) {
        return ApiResponse.ok(trainingService.train(principal.getId(), request.getAttribute()), "Training complete");
    }
}
