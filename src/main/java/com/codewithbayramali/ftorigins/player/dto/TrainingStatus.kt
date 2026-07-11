package com.codewithbayramali.ftorigins.player.dto

/**
 * The player's current training session (or idle). Drives the client's live countdown: the client
 * ticks [remainingSeconds] down locally and can re-sync against [finishesAt].
 */
data class TrainingStatus(
    val training: Boolean,
    val drillId: String?,
    val drillName: String?,
    val finishesAt: String?,       // ISO-8601 instant, e.g. 2026-07-10T14:05:00Z
    val remainingSeconds: Long,    // seconds left; 0 when ready or idle
    val ready: Boolean             // finished and waiting to be claimed
) {
    companion object {
        @JvmStatic
        fun idle(): TrainingStatus = TrainingStatus(false, null, null, null, 0, false)
    }
}
