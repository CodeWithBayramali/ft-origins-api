package com.codewithbayramali.ftorigins.match.repository;

import com.codewithbayramali.ftorigins.match.model.MatchEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MatchEventRepository extends JpaRepository<MatchEvent, UUID> {

    /** Timeline tail after a polling cursor — live viewing ("events since seq N") and replays (N = -1). */
    List<MatchEvent> findByMatchIdAndSeqGreaterThanOrderBySeq(UUID matchId, int afterSeq);

    /** seq values are gapless from 0, so the count IS the next seq — used when a sim is rebuilt. */
    long countByMatchId(UUID matchId);
}
