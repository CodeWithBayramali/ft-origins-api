package com.codewithbayramali.ftorigins.geo.dto

import com.codewithbayramali.ftorigins.player.model.Player
import com.codewithbayramali.ftorigins.player.service.EffectiveAttributes
import java.util.UUID

/**
 * One squad member as the "My Team" page shows them. Attributes are EFFECTIVE values (base + body
 * trade-off), same numbers the profile shows, so the founder picks a lineup from what the engine
 * will actually use. [isFounder] marks the manager; [isMe] lets the client highlight the caller.
 */
data class TeamMemberResponse(
    val playerId: UUID,
    val username: String?,
    val displayName: String?,
    val kitNumber: Int,
    val overall: Int,
    val pace: Int,
    val shooting: Int,
    val passing: Int,
    val dribbling: Int,
    val defending: Int,
    val physical: Int,
    val goalkeeping: Int,
    val curve: Int,
    val energy: Int,
    val isFounder: Boolean,
    val isMe: Boolean
) {
    companion object {
        @JvmStatic
        fun from(player: Player, founderId: UUID?, callerId: UUID?): TeamMemberResponse {
            val eff = EffectiveAttributes.of(player)
            val id = player.id!!
            return TeamMemberResponse(
                playerId = id,
                username = player.user?.username,
                displayName = player.displayName,
                kitNumber = player.appearance.kitNumber,
                overall = eff.overall(),
                pace = eff.pace(),
                shooting = eff.shooting(),
                passing = eff.passing(),
                dribbling = eff.dribbling(),
                defending = eff.defending(),
                physical = eff.physical(),
                goalkeeping = eff.goalkeeping(),
                curve = eff.curve(),
                energy = player.energy,
                isFounder = id == founderId,
                isMe = id == callerId
            )
        }
    }
}
