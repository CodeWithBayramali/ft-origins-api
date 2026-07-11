package com.codewithbayramali.ftorigins.player.model

/**
 * Relative intensity of a drill. Drives energy cost and base gain: harder drills cost more energy
 * and usually train two attributes at once (fewer taps, but a bigger energy bite).
 */
enum class DrillDifficulty {
    LIGHT,
    MEDIUM,
    HARD,
    ELITE
}
