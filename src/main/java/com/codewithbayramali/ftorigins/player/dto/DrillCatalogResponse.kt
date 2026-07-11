package com.codewithbayramali.ftorigins.player.dto

import com.codewithbayramali.ftorigins.player.model.TrainingDrill
import java.util.UUID

/** One attribute a drill trains, with its base (pre-diminishing) gain. */
data class DrillEffectView(
    val attribute: String,
    val baseGain: Int
)

/** Catalog view of a training drill for the client's training screen. */
data class DrillCatalogResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val category: String,
    val difficulty: String,
    val energyCost: Int,
    val durationSeconds: Int,
    val effects: List<DrillEffectView>
) {
    companion object {
        @JvmStatic
        fun from(drill: TrainingDrill): DrillCatalogResponse = DrillCatalogResponse(
            id = drill.id!!,
            code = drill.code,
            name = drill.name,
            category = drill.category.name,
            difficulty = drill.difficulty.name,
            energyCost = drill.energyCost,
            durationSeconds = drill.durationSeconds,
            effects = drill.effects.map { DrillEffectView(it.attribute.name, it.baseGain) }
        )
    }
}
