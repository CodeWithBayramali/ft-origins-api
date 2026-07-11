package com.codewithbayramali.ftorigins.player.controller;

import com.codewithbayramali.ftorigins.auth.security.CustomUserDetails;
import com.codewithbayramali.ftorigins.player.dto.DrillCatalogResponse;
import com.codewithbayramali.ftorigins.player.dto.PlayerResponse;
import com.codewithbayramali.ftorigins.player.dto.TrainRequest;
import com.codewithbayramali.ftorigins.player.dto.TrainResult;
import com.codewithbayramali.ftorigins.player.dto.TrainingStatus;
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

import java.util.List;

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

    /** The training catalog (same for every player; roles/positions are chosen per match). */
    @GetMapping("/train/drills")
    public ApiResponse<List<DrillCatalogResponse>> drills() {
        return ApiResponse.ok(trainingService.listDrills());
    }

    /** Current training session (or idle) — drives the client's live countdown. */
    @GetMapping("/me/train/status")
    public ApiResponse<TrainingStatus> trainingStatus(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok(trainingService.status(principal.getId()));
    }

    /** Begin a timed training (spends energy; the stat gain lands on claim). */
    @PostMapping("/me/train/start")
    public ApiResponse<TrainingStatus> startTraining(@AuthenticationPrincipal CustomUserDetails principal,
                                                     @Valid @RequestBody TrainRequest request) {
        return ApiResponse.ok(trainingService.start(principal.getId(), request.getDrillId()), "Training started");
    }

    /** Collect a finished training's stat gains. */
    @PostMapping("/me/train/claim")
    public ApiResponse<TrainResult> claimTraining(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok(trainingService.claim(principal.getId()), "Training complete");
    }

    /** Abandon the active training (no gain). */
    @PostMapping("/me/train/cancel")
    public ApiResponse<TrainingStatus> cancelTraining(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.ok(trainingService.cancel(principal.getId()), "Training cancelled");
    }
}
