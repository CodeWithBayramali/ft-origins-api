package com.codewithbayramali.ftorigins.player.service;

/**
 * Server-side boot design catalog. The design id is the ARRAY INDEX and must stay in lockstep with
 * the client BootCatalog (Unity): 20 designs, id 0 ("Classico") is the free default. Only ids and
 * prices live here — names, icons and meshes are client concerns.
 *
 * <p>!! Never reorder/remove entries — ids are persisted in players.boot_design_id and
 * player_boots.design_id. Adding new designs = append to the price table (and ship the client
 * assets in the same release). !!
 */
public final class BootCatalog {

    /** Coin price per design id. Index = design id; 0 = free default (implicitly owned). */
    private static final int[] PRICES = {
            0,     // 0  Classico (default)
            1000,  // 1  Shadow       - wave
            750,   // 2  Firebrand    - toe/collar accents
            500,   // 3  Royal        - stripes
            750,   // 4  Volt Strike  - toe/collar accents
            1000,  // 5  White Wave   - wave
            500,   // 6  Midnight     - stripes
            500,   // 7  Pink Punch   - stripes
            1000,  // 8  Aqua Pop     - wave
            500,   // 9  Emerald      - stripes
            1250,  // 10 Bumblebee    - fade
            1250,  // 11 Ember        - fade
            1000,  // 12 Sky Wave     - wave
            750,   // 13 Red Alert    - toe/heel/collar accents
            1000,  // 14 Night Volt   - wave
            1250,  // 15 Amethyst     - fade
            1250,  // 16 Tiger Fade   - fade
            500,   // 17 Bordeaux     - stripes
            500,   // 18 Tricolore    - stripes
            1000,  // 19 Panther      - wave
    };

    public static final int COUNT = PRICES.length;
    public static final int DEFAULT_DESIGN = 0;

    private BootCatalog() {
    }

    public static boolean exists(int designId) {
        return designId >= 0 && designId < COUNT;
    }

    public static int priceOf(int designId) {
        return PRICES[designId];
    }
}
