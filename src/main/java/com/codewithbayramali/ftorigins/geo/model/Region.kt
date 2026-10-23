package com.codewithbayramali.ftorigins.geo.model

import com.codewithbayramali.ftorigins.model.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

/**
 * A "bölge" — the in-game neighborhood/team. NOT a real-world named neighborhood: it is the circle
 * of the join radius (app.team.join-radius-km) around the founder's pinned location (the anchor).
 * A player whose pin falls inside a team's circle (and the team has room) joins it; anyone else
 * founds a new team at their pin. The H3 cell is only the uniqueness key.
 */
@Entity
@Table(
    name = "regions",
    indexes = [Index(name = "uq_regions_h3", columnList = "h3_index", unique = true)]
)
class Region : BaseEntity() {

    /** H3 cell address (hex string) at the configured resolution. Unique per region: normally the cell of
     *  the founder's pin; a team founded where a full team already holds that cell takes a free cell next
     *  to it (membership goes by distance from the anchor, the cell is only the key). */
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

    /** The player who founded the team (picked the location and created it) — the team's manager:
     *  sets match lineups and makes live match changes. Plain id, not a relation, to avoid a
     *  geo -> player model cycle. Null only while vacant (founder's account deleted); the next
     *  player to join claims the seat (see PlayerService), and auto-lineups cover the gap. */
    @Column(name = "founder_player_id")
    var founderPlayerId: UUID? = null

    /** Team kit ids into the client-side KitCatalog (0-9); chosen by the founding player.
     *  Null on teams created before kits existed — the client falls back to its default kit. */
    @Column(name = "kit_home")
    var kitHome: Int? = null

    @Column(name = "kit_away")
    var kitAway: Int? = null

    /** Kit pattern id into the client-side pattern catalog (0-13); chosen by the founding
     *  player, applied to both home and away kits. Null on teams created before patterns
     *  existed — the client falls back to solid (pattern 0). */
    @Column(name = "kit_pattern")
    var kitPattern: Int? = null

    /** Team crest ("arma"): ids into the client-side crest catalogs (sizes in CrestDto) — the shape,
     *  the symbol drawn on it, the base colour and the detail colour the symbol is drawn in (never the
     *  base colour). Chosen by the founding player, later changeable by the founder only. All four null
     *  = no crest (teams founded before crests, or a founder who skipped it). The columns are SMALLINT,
     *  so the JDBC type is pinned: a plain Int maps to INTEGER and ddl-auto=validate would reject it
     *  (the trap V7 and V13 had to undo). */
    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "crest_shape")
    var crestShape: Int? = null

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "crest_symbol")
    var crestSymbol: Int? = null

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "crest_base")
    var crestBase: Int? = null

    @JdbcTypeCode(SqlTypes.SMALLINT)
    @Column(name = "crest_detail")
    var crestDetail: Int? = null
}
