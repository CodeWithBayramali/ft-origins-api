package com.codewithbayramali.ftorigins.player.model

import com.codewithbayramali.ftorigins.model.BaseEntity
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table

/**
 * A training drill from the data-driven catalog (seeded by migration, editable later via superadmin).
 * Every player can perform every drill regardless of where they play. Cost [energyCost] is paid from
 * the player's energy bar; [effects] lists the one or two attributes it raises.
 */
@Entity
@Table(
    name = "training_drills",
    indexes = [Index(name = "uq_training_drills_code", columnList = "code", unique = true)]
)
class TrainingDrill : BaseEntity() {

    /** Stable machine key (e.g. "FINISHING"). Clients/superadmin reference drills by this. */
    @Column(nullable = false, length = 40)
    var code: String = ""

    /** Human-facing name (English; i18n added later). */
    @Column(nullable = false, length = 60)
    var name: String = ""

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    var category: DrillCategory = DrillCategory.PHYSICAL

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    var difficulty: DrillDifficulty = DrillDifficulty.MEDIUM

    @Column(name = "energy_cost", nullable = false)
    var energyCost: Int = 0

    /** Real time (seconds) a session of this drill takes before it can be claimed. */
    @Column(name = "duration_seconds", nullable = false)
    var durationSeconds: Int = 3600

    /** Soft on/off switch so superadmin can retire a drill without deleting history. */
    @Column(nullable = false)
    var active: Boolean = true

    /** Display order within the catalog. */
    @Column(name = "sort_order", nullable = false)
    var sortOrder: Int = 0

    // Small, read-mostly collection loaded with the drill. The catalog query uses a fetch-join to
    // avoid N+1 when listing every drill.
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "training_drill_effects",
        joinColumns = [JoinColumn(name = "drill_id")]
    )
    var effects: MutableList<DrillEffect> = mutableListOf()
}
