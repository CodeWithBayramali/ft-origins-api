package com.codewithbayramali.ftorigins.match.model

import com.codewithbayramali.ftorigins.model.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * One row of the match timeline. [seq] is the gapless play order within the match (the polling
 * cursor); [simMinute]/[simSecond] place the event on the match clock, which the client maps to
 * real time via the kickoff timestamp. [payload] is per-type JSON (actors, positions, outcome
 * details) — schemaless on purpose, the engine evolves faster than any column set would.
 */
@Entity
@Table(
    name = "match_events",
    uniqueConstraints = [UniqueConstraint(name = "uq_match_events_seq", columnNames = ["match_id", "seq"])]
)
class MatchEvent : BaseEntity() {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id", nullable = false)
    var match: Match? = null

    @Column(nullable = false)
    var seq: Int = 0

    @Column(name = "sim_minute", nullable = false)
    var simMinute: Int = 0

    @Column(name = "sim_second", nullable = false)
    var simSecond: Int = 0

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 24)
    var eventType: MatchEventType = MatchEventType.KICKOFF

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var payload: String? = null
}
