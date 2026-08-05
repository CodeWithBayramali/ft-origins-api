package com.codewithbayramali.ftorigins.player.repository;

import com.codewithbayramali.ftorigins.player.model.PlayerBoot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PlayerBootRepository extends JpaRepository<PlayerBoot, UUID> {

    List<PlayerBoot> findByPlayerId(UUID playerId);

    boolean existsByPlayerIdAndDesignId(UUID playerId, int designId);
}
