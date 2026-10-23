package com.codewithbayramali.ftorigins.economy.model

/**
 * Why a coin balance changed. Together with [CoinTransaction.reference] it names one grant or one
 * purchase, which the ledger lets happen only once.
 */
enum class CoinReason {
    /** The once-a-day reward; reference = the UTC day (yyyy-MM-dd). */
    DAILY_REWARD,

    /** Played in a league match; reference = the match id. */
    LEAGUE_MATCH,

    /** Played in a friendly; reference = the match id. Only a few a day pay (see MatchRewardService). */
    FRIENDLY_MATCH,

    /** Bought a boot design; reference = the design id. */
    BOOT_PURCHASE
}
