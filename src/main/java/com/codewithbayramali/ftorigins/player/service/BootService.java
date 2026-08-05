package com.codewithbayramali.ftorigins.player.service;

import com.codewithbayramali.ftorigins.exception.BusinessException;
import com.codewithbayramali.ftorigins.exception.ConflictException;
import com.codewithbayramali.ftorigins.exception.NotFoundException;
import com.codewithbayramali.ftorigins.model.User;
import com.codewithbayramali.ftorigins.player.dto.BootsResponse;
import com.codewithbayramali.ftorigins.player.model.Player;
import com.codewithbayramali.ftorigins.player.model.PlayerBoot;
import com.codewithbayramali.ftorigins.player.repository.PlayerBootRepository;
import com.codewithbayramali.ftorigins.player.repository.PlayerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

/**
 * The boot market: ownership, purchase (coins) and equipping. Design ids index the shared
 * client/server BootCatalog; design 0 is free and implicitly owned by everyone, so ownership rows
 * exist only for bought designs.
 */
@Service
public class BootService {

    private final PlayerRepository playerRepository;
    private final PlayerBootRepository playerBootRepository;

    public BootService(PlayerRepository playerRepository, PlayerBootRepository playerBootRepository) {
        this.playerRepository = playerRepository;
        this.playerBootRepository = playerBootRepository;
    }

    @Transactional(readOnly = true)
    public BootsResponse getBoots(UUID userId) {
        Player player = requirePlayer(userId);
        return toResponse(player);
    }

    /** Buys a design with coins. Fails on unknown id, already owned, or not enough coins. */
    @Transactional
    public BootsResponse purchase(UUID userId, int designId) {
        requireKnownDesign(designId);
        Player player = requirePlayer(userId);

        if (designId == BootCatalog.DEFAULT_DESIGN
                || playerBootRepository.existsByPlayerIdAndDesignId(player.getId(), designId)) {
            throw new ConflictException("BOOT_ALREADY_OWNED", "You already own this boot design");
        }

        int price = BootCatalog.priceOf(designId);
        User user = player.getUser();
        if (user.getCoins() < price) {
            throw new BusinessException(HttpStatus.CONFLICT, "INSUFFICIENT_COINS",
                    "Not enough coins for this boot design");
        }
        user.setCoins(user.getCoins() - price);

        PlayerBoot boot = new PlayerBoot();
        boot.setPlayer(player);
        boot.setDesignId(designId);
        playerBootRepository.save(boot);

        return toResponse(player);
    }

    /** Puts an owned design on the player's feet. */
    @Transactional
    public BootsResponse equip(UUID userId, int designId) {
        requireKnownDesign(designId);
        Player player = requirePlayer(userId);

        boolean owned = designId == BootCatalog.DEFAULT_DESIGN
                || playerBootRepository.existsByPlayerIdAndDesignId(player.getId(), designId);
        if (!owned) {
            throw new ConflictException("BOOT_NOT_OWNED", "Buy this boot design before equipping it");
        }

        player.setBootDesignId(designId);
        return toResponse(player);
    }

    private Player requirePlayer(UUID userId) {
        return playerRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("PLAYER_NOT_FOUND", "No player profile for this account"));
    }

    private void requireKnownDesign(int designId) {
        if (!BootCatalog.exists(designId)) {
            throw new NotFoundException("BOOT_NOT_FOUND", "Unknown boot design id: " + designId);
        }
    }

    private BootsResponse toResponse(Player player) {
        List<Integer> owned = new ArrayList<>();
        owned.add(BootCatalog.DEFAULT_DESIGN);
        playerBootRepository.findByPlayerId(player.getId())
                .forEach(b -> owned.add(b.getDesignId()));
        owned.sort(Integer::compareTo);

        List<Integer> prices = IntStream.range(0, BootCatalog.COUNT)
                .mapToObj(BootCatalog::priceOf)
                .toList();

        return new BootsResponse(player.getBootDesignId(), owned, player.getUser().getCoins(), prices);
    }
}
