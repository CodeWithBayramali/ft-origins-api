package com.codewithbayramali.ftorigins.economy.repository;

import com.codewithbayramali.ftorigins.economy.model.CoinReason;
import com.codewithbayramali.ftorigins.economy.model.CoinTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.UUID;

public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, UUID> {

    /** Whether this grant or purchase has already happened. */
    boolean existsByUserIdAndReasonAndReference(UUID userId, CoinReason reason, String reference);

    /** How many changes of one kind a user has had since a moment — e.g. today's paid friendlies. */
    long countByUserIdAndReasonAndCreatedAtGreaterThanEqual(UUID userId, CoinReason reason, Instant since);
}
