package com.codewithbayramali.ftorigins.economy.dto

/**
 * The daily reward card: whether today's reward is still there to collect, which day of the 7-day
 * ladder a claim today is (or, once collected, was) and what it pays, the whole ladder for the UI,
 * when the day turns, and the coin balance.
 */
data class DailyRewardResponse(
    val claimable: Boolean,
    /** 1-7: the ladder day today's claim is (or was). */
    val day: Int,
    /** Coins today's claim pays (or paid). */
    val amount: Int,
    /** Coins per ladder day, day 1 first. */
    val ladder: List<Int>,
    /** When the next day starts (UTC midnight, ISO-8601): a new claim opens, and a missed day resets the ladder. */
    val dayEndsAt: String,
    val coins: Int
)
