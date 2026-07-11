package com.codewithbayramali.ftorigins.geo.model

import com.codewithbayramali.ftorigins.model.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table

/**
 * A "bölge" — the in-game neighborhood/team. NOT a real-world named neighborhood:
 * it is a fixed spatial grid cell (H3) that a player's pinned location falls into.
 * Same cell = same team. Created lazily the first time a player lands in the cell,
 * with an auto-generated random name that members can change once the team fills.
 */
@Entity
@Table(
    name = "regions",
    indexes = [Index(name = "uq_regions_h3", columnList = "h3_index", unique = true)]
)
class Region : BaseEntity() {

    /** H3 cell address (hex string) at the configured resolution. Unique per region. */
    @Column(name = "h3_index", nullable = false, unique = true, length = 20)
    var h3Index: String = ""

    @Column(nullable = false, length = 60)
    var name: String = ""

    /** True once the team renamed itself; false while still on the auto-generated name. */
    @Column(name = "name_customized", nullable = false)
    var nameCustomized: Boolean = false

    @Column(name = "center_lat", nullable = false)
    var centerLat: Double = 0.0

    @Column(name = "center_lng", nullable = false)
    var centerLng: Double = 0.0

    /** Reverse-geocoded labels (best effort, may be null). Finest → coarsest below. */
    @Column(name = "country_code", length = 2)
    var countryCode: String? = null

    @Column(name = "city_name", length = 200)
    var cityName: String? = null

    /** Neighborhood (semt/mahalle) — finest label, from OSM/Nominatim; null when only GeoNames fallback was available. */
    @Column(name = "neighborhood", length = 200)
    var neighborhood: String? = null

    /** District (ilçe), from OSM/Nominatim. */
    @Column(name = "district", length = 200)
    var district: String? = null

    @Column(name = "player_count", nullable = false)
    var playerCount: Int = 0

    /** Team kit ids into the client-side KitCatalog (0-9); chosen by the founding player.
     *  Null on teams created before kits existed — the client falls back to its default kit. */
    @Column(name = "kit_home")
    var kitHome: Int? = null

    @Column(name = "kit_away")
    var kitAway: Int? = null

    /** Kit pattern id into the client-side pattern catalog (0-6); chosen by the founding
     *  player, applied to both home and away kits. Null on teams created before patterns
     *  existed — the client falls back to solid (pattern 0). */
    @Column(name = "kit_pattern")
    var kitPattern: Int? = null
}
