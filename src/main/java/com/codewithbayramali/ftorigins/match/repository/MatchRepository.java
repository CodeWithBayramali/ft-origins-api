package com.codewithbayramali.ftorigins.match.repository;

import com.codewithbayramali.ftorigins.match.model.Match;
import com.codewithbayramali.ftorigins.match.model.MatchStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchRepository extends JpaRepository<Match, UUID> {

    /** Match with both teams fetched, ready for DTO building outside the session. */
    @Query("select m from Match m join fetch m.homeRegion join fetch m.awayRegion where m.id = :id")
    Optional<Match> findDetailById(@Param("id") UUID id);

    /** A team's matches in the given statuses, soonest kickoff first (upcoming/live list). */
    @Query("select m from Match m join fetch m.homeRegion join fetch m.awayRegion "
            + "where (m.homeRegion.id = :regionId or m.awayRegion.id = :regionId) "
            + "and m.status in :statuses order by m.kickoffAt")
    List<Match> findByRegionAndStatuses(@Param("regionId") UUID regionId,
                                        @Param("statuses") Collection<MatchStatus> statuses);

    /** A team's most recently played matches in the given status, newest kickoff first (capped by the pageable). */
    @Query("select m from Match m join fetch m.homeRegion join fetch m.awayRegion "
            + "where (m.homeRegion.id = :regionId or m.awayRegion.id = :regionId) "
            + "and m.status = :status order by m.kickoffAt desc")
    List<Match> findByRegionAndStatusNewestFirst(@Param("regionId") UUID regionId,
                                                 @Param("status") MatchStatus status,
                                                 Pageable pageable);

    /**
     * Advances the match clock ONLY if it is still where this instance thinks it is. Two app
     * instances (or a stray dev server) pointed at one database would otherwise each run their own
     * in-memory simulation of the same match and interleave two different timelines — which silently
     * loses events, including queued substitutions. Losing the race returns 0 rows; the caller then
     * throws away its stale simulation and rebuilds from the database.
     */
    @Modifying
    @Query("update Match m set m.simMinute = :toMinute, m.homeScore = :homeScore, m.awayScore = :awayScore, "
            + "m.status = :status, m.finishedAt = :finishedAt, m.updatedAt = :now "
            + "where m.id = :id and m.simMinute = :fromMinute and m.status = com.codewithbayramali.ftorigins.match.model.MatchStatus.LIVE")
    int claimAdvance(@Param("id") UUID id,
                     @Param("fromMinute") int fromMinute,
                     @Param("toMinute") int toMinute,
                     @Param("homeScore") int homeScore,
                     @Param("awayScore") int awayScore,
                     @Param("status") MatchStatus status,
                     @Param("finishedAt") Instant finishedAt,
                     @Param("now") Instant now);

    /** Due kickoffs — the scheduler tick starts these. */
    List<Match> findByStatusAndKickoffAtLessThanEqual(MatchStatus status, Instant cutoff);

    /** Every match in a status with both teams fetched — the tick advances the LIVE ones. */
    @Query("select m from Match m join fetch m.homeRegion join fetch m.awayRegion where m.status = :status")
    List<Match> findDetailByStatus(@Param("status") MatchStatus status);

    /**
     * Matches in the given statuses that either team is involved in with a kickoff inside
     * [from, to] — the "one match at a time per team" guard when scheduling a new fixture.
     */
    @Query("select count(m) from Match m where m.status in :statuses "
            + "and (m.homeRegion.id = :regionA or m.awayRegion.id = :regionA "
            + "or m.homeRegion.id = :regionB or m.awayRegion.id = :regionB) "
            + "and m.kickoffAt between :from and :to")
    long countInvolvingRegionsBetween(@Param("regionA") UUID regionA,
                                      @Param("regionB") UUID regionB,
                                      @Param("statuses") Collection<MatchStatus> statuses,
                                      @Param("from") Instant from,
                                      @Param("to") Instant to);
}
