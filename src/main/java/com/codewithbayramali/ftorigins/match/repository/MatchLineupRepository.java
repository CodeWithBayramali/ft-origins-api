package com.codewithbayramali.ftorigins.match.repository;

import com.codewithbayramali.ftorigins.match.model.MatchLineup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MatchLineupRepository extends JpaRepository<MatchLineup, UUID> {

    Optional<MatchLineup> findByMatchIdAndRegionId(UUID matchId, UUID regionId);
}
