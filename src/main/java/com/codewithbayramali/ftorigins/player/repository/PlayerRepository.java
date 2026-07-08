package com.codewithbayramali.ftorigins.player.repository;

import com.codewithbayramali.ftorigins.player.model.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface PlayerRepository extends JpaRepository<Player, UUID> {

    Optional<Player> findByUserId(UUID userId);

    boolean existsByUserId(UUID userId);

    /** Fetches the player with user + region eagerly so DTOs can be built outside a transaction. */
    @Query("select p from Player p join fetch p.user join fetch p.region where p.user.id = :userId")
    Optional<Player> findDetailByUserId(@Param("userId") UUID userId);
}
