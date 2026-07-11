package com.codewithbayramali.ftorigins.player.dto

/**
 * One attribute moved by a drill. [newValue] is the BASE attribute (what training raises); the
 * body-modified effective value is what the profile shows.
 */
data class AttributeChange(
    val attribute: String,
    val gain: Int,
    val newValue: Int
)

/** Outcome of claiming a finished training: what improved and the energy left. */
data class TrainResult(
    val drill: String,
    val changes: List<AttributeChange>,
    val energy: Int
)
