package com.codewithbayramali.ftorigins.player.service;

import com.codewithbayramali.ftorigins.exception.NotFoundException;
import com.codewithbayramali.ftorigins.geo.model.Region;
import com.codewithbayramali.ftorigins.geo.service.RegionService;
import com.codewithbayramali.ftorigins.model.User;
import com.codewithbayramali.ftorigins.player.dto.AppearanceRequest;
import com.codewithbayramali.ftorigins.player.dto.PlayerResponse;
import com.codewithbayramali.ftorigins.player.dto.UpdatePlayerRequest;
import com.codewithbayramali.ftorigins.player.model.Foot;
import com.codewithbayramali.ftorigins.player.model.Player;
import com.codewithbayramali.ftorigins.player.model.PlayerAppearance;
import com.codewithbayramali.ftorigins.player.model.Position;
import com.codewithbayramali.ftorigins.player.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final RegionService regionService;

    public PlayerService(PlayerRepository playerRepository, RegionService regionService) {
        this.playerRepository = playerRepository;
        this.regionService = regionService;
    }

    /**
     * Creates the player profile during registration: resolves the pinned location to a region
     * (creating it if new), links the player, and bumps the region's player count.
     * Runs inside the caller's (registration) transaction.
     */
    @Transactional
    public Player createForRegistration(User user, double latitude, double longitude,
                                        Position position, Foot preferredFoot,
                                        String displayName, AppearanceRequest appearance,
                                        Integer kitHomeId, Integer kitAwayId) {
        Region region = regionService.getOrCreateForLocation(latitude, longitude, kitHomeId, kitAwayId);

        Player player = new Player();
        player.setUser(user);
        player.setRegion(region);
        player.setHomeLat(latitude);
        player.setHomeLng(longitude);
        player.setH3Index(region.getH3Index());
        player.setPosition(position != null ? position : Position.MID);
        player.setPreferredFoot(preferredFoot != null ? preferredFoot : Foot.RIGHT);
        player.setDisplayName(displayName != null && !displayName.isBlank() ? displayName.trim() : user.getUsername());
        if (appearance != null) {
            applyAppearance(player, appearance);
        }

        Player saved = playerRepository.save(player);
        region.setPlayerCount(region.getPlayerCount() + 1);
        return saved;
    }

    @Transactional(readOnly = true)
    public PlayerResponse getMyProfile(UUID userId) {
        return playerRepository.findDetailByUserId(userId)
                .map(PlayerResponse::from)
                .orElseThrow(() -> new NotFoundException("PLAYER_NOT_FOUND", "No player profile for this account"));
    }

    @Transactional
    public PlayerResponse updateProfile(UUID userId, UpdatePlayerRequest request) {
        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("PLAYER_NOT_FOUND", "No player profile for this account"));

        if (request.getDisplayName() != null && !request.getDisplayName().isBlank()) {
            player.setDisplayName(request.getDisplayName().trim());
        }
        if (request.getPosition() != null) {
            player.setPosition(request.getPosition());
        }
        if (request.getPreferredFoot() != null) {
            player.setPreferredFoot(request.getPreferredFoot());
        }
        if (request.getAppearance() != null) {
            applyAppearance(player, request.getAppearance());
        }

        playerRepository.save(player);
        return PlayerResponse.from(player);
    }

    private void applyAppearance(Player player, AppearanceRequest source) {
        PlayerAppearance target = player.getAppearance();
        if (source.getSkinToneId() != null) {
            target.setSkinToneId(source.getSkinToneId());
        }
        if (source.getHairStyleId() != null) {
            target.setHairStyleId(source.getHairStyleId());
        }
        if (source.getHairColorId() != null) {
            target.setHairColorId(source.getHairColorId());
        }
        if (source.getBeardId() != null) {
            target.setBeardId(source.getBeardId());
        }
        if (source.getKitNumber() != null) {
            target.setKitNumber(source.getKitNumber());
        }
        // height & weight are not cosmetic: they drive the body stat trade-off (see BodyModifiers).
        if (source.getHeightCm() != null) {
            player.setHeightCm(BodyModifiers.clampHeight(source.getHeightCm()));
        }
        if (source.getWeightKg() != null) {
            player.setWeightKg(BodyModifiers.clampWeight(source.getWeightKg()));
        }
    }
}
