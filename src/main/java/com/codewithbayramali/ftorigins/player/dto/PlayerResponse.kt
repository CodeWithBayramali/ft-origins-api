package com.codewithbayramali.ftorigins.player.dto

import com.codewithbayramali.ftorigins.player.model.Foot
import com.codewithbayramali.ftorigins.player.model.Player
import com.codewithbayramali.ftorigins.player.model.PlayerAppearance
import com.codewithbayramali.ftorigins.player.service.BodyModifiers
import java.util.UUID
import kotlin.math.roundToInt

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
                overall = overall(pace, shooting, passing, dribbling, defending, physical, goalkeeping, curve),
                energy = player.energy,
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

        /** General overall rating from EFFECTIVE attributes — a simple average across every skill.
         *  Position-neutral: every player can play anywhere, so all-round completeness is what counts. */
        private fun overall(
            pace: Int, shooting: Int, passing: Int, dribbling: Int,
            defending: Int, physical: Int, goalkeeping: Int, curve: Int
        ): Int = ((pace + shooting + passing + dribbling + defending + physical + goalkeeping + curve) / 8.0).roundToInt()
    }
}
