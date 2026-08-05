package com.codewithbayramali.ftorigins.player.dto

/**
 * The player's boot locker for the market/wardrobe UI: which designs are owned (always includes
 * the free default 0), which one is on the feet, and the coin balance for the buy buttons.
 * Prices per design come from [priceList] so the client never hardcodes economy values.
 */
data class BootsResponse(
    val equipped: Int,
    val owned: List<Int>,
    val coins: Int,
    /** Coin price by design id (index = id, 0 = free default). */
    val priceList: List<Int>
)
