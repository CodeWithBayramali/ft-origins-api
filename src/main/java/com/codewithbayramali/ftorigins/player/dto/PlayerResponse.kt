package com.codewithbayramali.ftorigins.player.dto

import com.codewithbayramali.ftorigins.player.model.Foot
import com.codewithbayramali.ftorigins.player.model.Player
import com.codewithbayramali.ftorigins.player.model.PlayerAppearance
import com.codewithbayramali.ftorigins.player.model.Position
import com.codewithbayramali.ftorigins.player.service.BodyModifiers
import java.util.UUID
import kotlin.math.roundToInt

/**
 * Player view. Attributes are EFFECTIVE values (base + body trade-off, clamped 1-99); [overall] is
 * weighted from those. Base attributes live on the entity and are what training raises.
 */
data class PlayerResponse(
    val id: UUID,
    val username: String?,
    val displayName: String?,
    val position: Position,
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
    val level: Int,
    val xp: Int,
    val energy: Int,
    val appearance: PlayerAppearance,
    val regionId: UUID?,
    val regionName: String?,
    val countryCode: String?,
    val cityName: String?,
    val district: String?,
    val neighborhood: String?,
    /** Team kit ids into the client KitCatalog (0-9); null on pre-kit teams (client uses its default). */
    val teamKitHome: Int?,
    val teamKitAway: Int?
) {
    companion object {
        @JvmStatic
        fun from(player: Player): PlayerResponse {
            val weight = player.weightKg
            val height = player.heightCm

            // Body modifiers apply on read; base attributes (trained) stay untouched.
            val pace = BodyModifiers.clamp(player.pace + BodyModifiers.paceDelta(weight, height))
            val shooting = BodyModifiers.clamp(player.shooting) // body-neutral
            val passing = BodyModifiers.clamp(player.passing)   // body-neutral
            val dribbling = BodyModifiers.clamp(player.dribbling + BodyModifiers.dribblingDelta(weight, height))
            val defending = BodyModifiers.clamp(player.defending + BodyModifiers.defendingDelta(weight, height))
            val physical = BodyModifiers.clamp(player.physical + BodyModifiers.physicalDelta(weight, height))
            val goalkeeping = BodyModifiers.clamp(player.goalkeeping + BodyModifiers.goalkeepingDelta(weight, height))
            val curve = BodyModifiers.clamp(player.curve) // body-neutral

            return PlayerResponse(
                id = player.id!!,
                username = player.user?.username,
                displayName = player.displayName,
                position = player.position,
                preferredFoot = player.preferredFoot,
                heightCm = height,
                weightKg = weight,
                pace = pace,
                shooting = shooting,
                passing = passing,
                dribbling = dribbling,
                defending = defending,
                physical = physical,
                goalkeeping = goalkeeping,
                curve = curve,
                overall = overall(player.position, pace, shooting, passing, dribbling, defending, physical, goalkeeping),
                level = player.level,
                xp = player.xp,
                energy = player.energy,
                appearance = player.appearance,
                regionId = player.region?.id,
                regionName = player.region?.name,
                countryCode = player.region?.countryCode,
                cityName = player.region?.cityName,
                district = player.region?.district,
                neighborhood = player.region?.neighborhood,
                teamKitHome = player.region?.kitHome,
                teamKitAway = player.region?.kitAway
            )
        }

        /** Position-weighted overall rating from EFFECTIVE attributes (each weight set sums to 1.0). */
        private fun overall(
            position: Position,
            pace: Int, shooting: Int, passing: Int, dribbling: Int,
            defending: Int, physical: Int, goalkeeping: Int
        ): Int = when (position) {
            Position.GK -> 0.55 * goalkeeping + 0.20 * defending + 0.15 * physical + 0.10 * passing
            Position.DEF -> 0.35 * defending + 0.25 * physical + 0.20 * pace + 0.20 * passing
            Position.MID -> 0.30 * passing + 0.25 * dribbling + 0.20 * defending + 0.15 * pace + 0.10 * shooting
            Position.FWD -> 0.35 * shooting + 0.25 * dribbling + 0.20 * pace + 0.20 * passing
        }.roundToInt()
    }
}
