package com.codewithbayramali.ftorigins.match.repository;

import com.codewithbayramali.ftorigins.match.model.MatchCommand;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MatchCommandRepository extends JpaRepository<MatchCommand, UUID> {

    /** Replay order for simulation rebuilds. */
    List<MatchCommand> findByMatchIdOrderByApplyMinuteAscCreatedAtAsc(UUID matchId);

    /** Enforces the per-team substitution budget. */
    long countByMatchIdAndRegionId(UUID matchId, UUID regionId);
}
