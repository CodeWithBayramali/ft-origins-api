package com.codewithbayramali.ftorigins.player.model

import com.codewithbayramali.ftorigins.geo.model.Region
import com.codewithbayramali.ftorigins.model.BaseEntity
import com.codewithbayramali.ftorigins.model.User
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * The in-game player profile. 1-1 with [User] (account/currency stays on User).
 * Belongs to a [Region] (= team) derived from the home location pinned at registration.
 */
@Entity
@Table(
    name = "players",
    indexes = [
        Index(name = "uq_players_user", columnList = "user_id", unique = true),
        Index(name = "idx_players_region", columnList = "region_id"),
        Index(name = "idx_players_h3", columnList = "h3_index")
    ]
)
class Player : BaseEntity() {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    var user: User? = null

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "region_id", nullable = false)
    var region: Region? = null

    @Column(name = "home_lat", nullable = false)
    var homeLat: Double = 0.0

    @Column(name = "home_lng", nullable = false)
    var homeLng: Double = 0.0

    @Column(name = "h3_index", nullable = false, length = 20)
    var h3Index: String = ""

    @Column(name = "display_name", length = 60)
    var displayName: String? = null

    @Embedded
    var appearance: PlayerAppearance = PlayerAppearance()

    /**
     * Body height (cm) and weight (kg). These two axes apply a balanced trade-off to effective
     * attributes (taller/heavier = stronger & more defensive but slower & less agile). See BodyModifiers.
     */
    @Column(name = "height_cm", nullable = false)
    var heightCm: Int = 178

    @Column(name = "weight_kg", nullable = false)
    var weightKg: Int = 75

    /** Preferred (dominant) foot — the foot the player dribbles and shoots with in matches. */
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_foot", nullable = false, length = 8)
    var preferredFoot: Foot = Foot.RIGHT

    /** Equipped boot design (client BootCatalog id 0-19). 0 = the free default every player owns. */
    @Column(name = "boot_design_id", nullable = false)
    var bootDesignId: Int = 0

    // Base attributes (1-99). Everyone starts equal at a low, fair floor (35) and earns growth through
    // training + matches — no positional head start. Effective values may add bounded item bonuses later.
    // Exception: pace floors at 45 — the run speed the client derives from it must feel playable on the
    // pitch from day one (a 35-pace starter crawled), and this is the server-owned knob that tunes it.
    @Column(nullable = false) var pace: Int = 45
    @Column(nullable = false) var shooting: Int = 35
    @Column(nullable = false) var passing: Int = 35
    @Column(nullable = false) var dribbling: Int = 35
    @Column(nullable = false) var defending: Int = 35
    @Column(nullable = false) var physical: Int = 35
    @Column(nullable = false) var goalkeeping: Int = 35

    // Curve / "falso": how much bend the player can put on a struck ball. Body-neutral; trainable.
    @Column(nullable = false) var curve: Int = 35

    // Training energy regenerates over time; gates daily training volume for fairness.
    @Column(nullable = false) var energy: Int = 100

    @Column(name = "energy_updated_at", nullable = false)
    var energyUpdatedAt: Instant = Instant.now()

    // Inactivity decay (see PlayerUpkeepService): after a grace period without training, attributes
    // erode so recent activity — not seniority — drives the overall rating. lastTrainedAt resets on
    // every training; decayUpdatedAt marks how far decay has already been applied.
    @Column(name = "last_trained_at", nullable = false)
    var lastTrainedAt: Instant = Instant.now()

    @Column(name = "decay_updated_at", nullable = false)
    var decayUpdatedAt: Instant = Instant.now()

    // Active timed-training session. Null = idle. Starting spends energy + sets trainingFinishesAt; the
    // gain lands only on claim (after the duration), and cancelling forfeits it. Blocks a second session
    // and (see PlayerUpkeepService) pauses energy regen while active. Matches will check this busy state.
    @Column(name = "active_drill_id")
    var activeDrillId: UUID? = null

    @Column(name = "training_started_at")
    var trainingStartedAt: Instant? = null

    @Column(name = "training_finishes_at")
    var trainingFinishesAt: Instant? = null
}
