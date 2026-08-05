package com.codewithbayramali.ftorigins.match.model

import com.codewithbayramali.ftorigins.geo.model.Region
import com.codewithbayramali.ftorigins.model.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

/**
 * A fixture between two teams (regions), simulated server-side as an event timeline.
 * [seed] is fixed at kickoff and — together with the participants' stat snapshots — makes the
 * whole match reproducible. [simMinute] is the live frontier: the last simulated match minute;
 * clients may only fetch events up to it, so a running match's future is unreadable.
 */
@Entity
@Table(
    name = "matches",
    indexes = [
        Index(name = "idx_matches_home_kickoff", columnList = "home_region_id, kickoff_at"),
        Index(name = "idx_matches_away_kickoff", columnList = "away_region_id, kickoff_at"),
        Index(name = "idx_matches_status_kickoff", columnList = "status, kickoff_at")
    ]
)
class Match : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "home_region_id", nullable = false)
    var homeRegion: Region? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "away_region_id", nullable = false)
    var awayRegion: Region? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "match_type", nullable = false, length = 12)
    var matchType: MatchType = MatchType.FRIENDLY

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    var status: MatchStatus = MatchStatus.SCHEDULED

    @Column(name = "kickoff_at", nullable = false)
    var kickoffAt: Instant = Instant.now()

    /** RNG seed, null until kickoff locks it. */
    @Column
    var seed: Long? = null

    /** Last simulated match minute (0 before kickoff, 90+ once finished). */
    @Column(name = "sim_minute", nullable = false)
    var simMinute: Int = 0

    @Column(name = "home_score", nullable = false)
    var homeScore: Int = 0

    @Column(name = "away_score", nullable = false)
    var awayScore: Int = 0

    @Column(name = "finished_at")
    var finishedAt: Instant? = null
}
