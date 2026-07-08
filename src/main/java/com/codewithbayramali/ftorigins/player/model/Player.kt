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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    var position: Position = Position.MID

    /** Preferred (dominant) foot — the foot the player dribbles and shoots with in matches. */
    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_foot", nullable = false, length = 8)
    var preferredFoot: Foot = Foot.RIGHT

    // Base attributes (1-99). Effective values may later add bounded item bonuses.
    @Column(nullable = false) var pace: Int = 50
    @Column(nullable = false) var shooting: Int = 50
    @Column(nullable = false) var passing: Int = 50
    @Column(nullable = false) var dribbling: Int = 50
    @Column(nullable = false) var defending: Int = 50
    @Column(nullable = false) var physical: Int = 50
    @Column(nullable = false) var goalkeeping: Int = 45

    // Curve / "falso": how much bend the player can put on a struck ball. Body-neutral; trainable.
    @Column(nullable = false) var curve: Int = 50

    @Column(nullable = false) var level: Int = 1
    @Column(nullable = false) var xp: Int = 0

    // Training energy regenerates over time; gates daily training for fairness.
    @Column(nullable = false) var energy: Int = 100

    @Column(name = "energy_updated_at", nullable = false)
    var energyUpdatedAt: Instant = Instant.now()
}
