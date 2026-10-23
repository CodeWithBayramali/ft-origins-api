package com.codewithbayramali.ftorigins.match.repository;

import com.codewithbayramali.ftorigins.match.model.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, UUID> {

    List<MatchParticipant> findByMatchIdAndRegionIdOrderBySlot(UUID matchId, UUID regionId);

    /** Only the players currently on the pitch — after a substitution the replaced row stays behind
     *  for the timeline, so lineup views must ask for the open rows or they show a slot twice. */
    List<MatchParticipant> findByMatchIdAndRegionIdAndLeftMinuteIsNullOrderBySlot(UUID matchId, UUID regionId);

    List<MatchParticipant> findByMatchIdOrderBySlotAscEnteredMinuteAsc(UUID matchId);

    /** The slot's current occupant (their row is still open — no leftMinute yet). */
    Optional<MatchParticipant> findByMatchIdAndRegionIdAndSlotAndLeftMinuteIsNull(
            UUID matchId, UUID regionId, int slot);

    /** The substitute that entered a slot at a given minute — rebuilds map command rows to players. */
    Optional<MatchParticipant> findByMatchIdAndRegionIdAndSlotAndEnteredMinute(
            UUID matchId, UUID regionId, int slot, int enteredMinute);

    /** A player appears at most once per match: no re-entry after being subbed out. */
    boolean existsByMatchIdAndPlayerId(UUID matchId, UUID playerId);

    /** A person who played in a match (starter or substitute), and the side they played for. */
    interface HumanParticipant {
        UUID getRegionId();

        UUID getUserId();
    }

    /** Everyone who played in a match — the AI fillers left out — once each, by account. */
    @Query("select distinct mp.region.id as regionId, p.user.id as userId from MatchParticipant mp "
            + "join mp.player p where mp.match.id = :matchId and mp.aiFill = false")
    List<HumanParticipant> findHumanParticipants(@Param("matchId") UUID matchId);

    /**
     * Clears one side's slots before re-inserting a resubmitted lineup. Bulk JPQL on purpose: it
     * executes immediately, so the fresh rows can't race the old ones on the (match, region, slot)
     * unique constraint the way deferred entity removals would (Hibernate flushes inserts first).
     */
    @Modifying
    @Query("delete from MatchParticipant p where p.match.id = :matchId and p.region.id = :regionId")
    void deleteForSide(@Param("matchId") UUID matchId, @Param("regionId") UUID regionId);
}
