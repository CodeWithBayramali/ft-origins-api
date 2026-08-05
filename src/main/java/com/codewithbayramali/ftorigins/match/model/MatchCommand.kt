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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

/**
 * One founder in-match command, replayed at [applyMinute] when a simulation is rebuilt — part of
 * the determinism contract (seed + kickoff snapshots + command log = the exact same match).
 */
@Entity
@Table(
    name = "match_commands",
    indexes = [Index(name = "idx_match_commands_match", columnList = "match_id, apply_minute")]
)
class MatchCommand : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    var match: Match? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    var region: Region? = null

    @Enumerated(EnumType.STRING)
    @Column(name = "command_type", nullable = false, length = 16)
    var commandType: MatchCommandType = MatchCommandType.SUBSTITUTION

    /** The sim minute this command takes effect (frontier + 1 at submission time). */
    @Column(name = "apply_minute", nullable = false)
    var applyMinute: Int = 0

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var payload: String? = null

    /** Audit only. */
    @Column(name = "created_by_player_id")
    var createdByPlayerId: UUID? = null
}
