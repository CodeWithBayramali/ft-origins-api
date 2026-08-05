package com.codewithbayramali.ftorigins.player.dto

import com.codewithbayramali.ftorigins.player.model.Foot
import com.codewithbayramali.ftorigins.player.model.Player
import com.codewithbayramali.ftorigins.player.model.PlayerAppearance
import com.codewithbayramali.ftorigins.player.service.EffectiveAttributes
import java.util.UUID

/**
 * Player view. Attributes are EFFECTIVE values (base + body trade-off, clamped 1-99); [overall] is
 * the average of those. Base attributes live on the entity and are what training raises.
 */
data class PlayerResponse(
    val id: UUID,
    val username: String?,
    val displayName: String?,
    val preferredFoot: Foot,
    val heightCm: Int,
    val weightKg: Int,
    val pace: Int,
    val shooting: Int,
    val passing: Int,
    val dribbling: Int,
    val defending: Int,
    val physical: Int,
    val goalkeeping: Int,
    val curve: Int,
    val overall: Int,
    val energy: Int,
    /** Equipped boot design id into the client BootCatalog (0-19); the avatar dresses with this. */
    val bootDesignId: Int,
    val appearance: PlayerAppearance,
    val regionId: UUID?,
    val regionName: String?,
    val countryCode: String?,
    val cityName: String?,
    val district: String?,
    val neighborhood: String?,
    /** Team kit ids into the client KitCatalog (0-9); null on pre-kit teams (client uses its default). */
    val teamKitHome: Int?,
    val teamKitAway: Int?,
    /** Team kit pattern id (0-6); null on teams founded before patterns (client uses solid). */
    val teamKitPattern: Int?
) {
    companion object {
        @JvmStatic
        fun from(player: Player): PlayerResponse {
            // Body modifiers apply on read; base attributes (trained) stay untouched.
            val effective = EffectiveAttributes.of(player)

            return PlayerResponse(
                id = player.id!!,
                username = player.user?.username,
                displayName = player.displayName,
                preferredFoot = player.preferredFoot,
                heightCm = player.heightCm,
                weightKg = player.weightKg,
                pace = effective.pace(),
                shooting = effective.shooting(),
                passing = effective.passing(),
                dribbling = effective.dribbling(),
                defending = effective.defending(),
                physical = effective.physical(),
                goalkeeping = effective.goalkeeping(),
                curve = effective.curve(),
                overall = effective.overall(),
                energy = player.energy,
                bootDesignId = player.bootDesignId,
                appearance = player.appearance,
                regionId = player.region?.id,
                regionName = player.region?.name,
                countryCode = player.region?.countryCode,
                cityName = player.region?.cityName,
                district = player.region?.district,
                neighborhood = player.region?.neighborhood,
                teamKitHome = player.region?.kitHome,
                teamKitAway = player.region?.kitAway,
                teamKitPattern = player.region?.kitPattern
            )
        }
    }
}
