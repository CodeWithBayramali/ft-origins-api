package com.codewithbayramali.ftorigins.match.model

import com.codewithbayramali.ftorigins.geo.model.Region
import com.codewithbayramali.ftorigins.model.BaseEntity
import com.codewithbayramali.ftorigins.player.model.Foot
import com.codewithbayramali.ftorigins.player.model.Player
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

/**
 * One lineup slot in a match (slot 0 = GK). [player] is null for a standard-stat AI filler —
 * teams never play short-handed. The attribute fields are the KICKOFF SNAPSHOT of the player's
 * EFFECTIVE stats (base + body modifiers): frozen so mid-match training can't touch a running
 * match, stored so replays use exactly what the engine saw. Null until kickoff.
 */
@Entity
@Table(
    name = "match_participants",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uq_match_participants_slot",
            columnNames = ["match_id", "region_id", "slot", "entered_minute"]
        )
    ]
)
class MatchParticipant : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    var match: Match? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    var region: Region? = null

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id")
    var player: Player? = null

    /** Lineup slot index within the formation (0 = GK). */
    @Column(nullable = false)
    var slot: Int = 0

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    var role: MatchRole = MatchRole.GK

    @Column(name = "ai_fill", nullable = false)
    var aiFill: Boolean = false

    /** Occupancy window: this row owns its slot during [enteredMinute, leftMinute). Starters enter
     *  at 0; a substitute enters at the minute the sub applies, and the outgoing row is closed by
     *  setting leftMinute (null = still on the pitch / played to the end). */
    @Column(name = "entered_minute", nullable = false)
    var enteredMinute: Int = 0

    @Column(name = "left_minute")
    var leftMinute: Int? = null

    // Display snapshot (kickoff): survives account deletion and names AI fillers.
    @Column(name = "display_name", length = 60)
    var displayName: String? = null

    @Column(name = "kit_number")
    var kitNumber: Int? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_foot", length = 8)
    var preferredFoot: Foot? = null

    // Effective attribute snapshot (kickoff). Null until the match goes LIVE.
    @Column var pace: Int? = null
    @Column var shooting: Int? = null
    @Column var passing: Int? = null
    @Column var dribbling: Int? = null
    @Column var defending: Int? = null
    @Column var physical: Int? = null
    @Column var goalkeeping: Int? = null
    @Column var curve: Int? = null
    @Column var energy: Int? = null
}
