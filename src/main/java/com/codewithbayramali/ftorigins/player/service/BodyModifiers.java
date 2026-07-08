package com.codewithbayramali.ftorigins.player.service;

/**
 * Body-shape effect on attributes — the game's only non-cosmetic body inputs.
 *
 * <p>Weight (mass) and height apply a <b>balanced trade-off</b> on top of a player's base
 * attributes: heavier/taller = stronger &amp; more defensive but slower &amp; less agile; lighter/shorter
 * = the reverse. Shooting and passing are body-neutral. Both axes are continuous and measured around
 * a neutral reference (75&nbsp;kg / 178&nbsp;cm = zero modifier).
 *
 * <p>Effective = {@code clamp(base + weightDelta + heightDelta, 1, 99)}. Base attributes (what
 * training raises) are never mutated — these modifiers are applied on read when building responses.
 */
public final class BodyModifiers {

    /** Reference values that yield zero modifier; deviations from them scale the effect. */
    public static final int REFERENCE_HEIGHT_CM = 178;
    public static final int MIN_HEIGHT_CM = 150;
    public static final int MAX_HEIGHT_CM = 210;

    public static final int REFERENCE_WEIGHT_KG = 75;
    public static final int MIN_WEIGHT_KG = 40;
    public static final int MAX_WEIGHT_KG = 150;

    /** Deviation at which each axis reaches its full modifier magnitude. */
    private static final double HEIGHT_SPAN_CM = 20.0;
    private static final double WEIGHT_SPAN_KG = 20.0;

    private BodyModifiers() {
    }

    /** Clamp a value into the valid attribute range [1, 99]. */
    public static int clamp(int value) {
        return Math.max(1, Math.min(99, value));
    }

    public static int clampHeight(int heightCm) {
        return Math.max(MIN_HEIGHT_CM, Math.min(MAX_HEIGHT_CM, heightCm));
    }

    public static int clampWeight(int weightKg) {
        return Math.max(MIN_WEIGHT_KG, Math.min(MAX_WEIGHT_KG, weightKg));
    }

    public static int paceDelta(int weightKg, int heightCm) {
        return round(-5 * weightFactor(weightKg)) + round(-2 * heightFactor(heightCm));
    }

    public static int dribblingDelta(int weightKg, int heightCm) {
        return round(-3 * weightFactor(weightKg)) + round(-2 * heightFactor(heightCm));
    }

    public static int defendingDelta(int weightKg, int heightCm) {
        return round(3 * weightFactor(weightKg)) + round(2 * heightFactor(heightCm));
    }

    public static int physicalDelta(int weightKg, int heightCm) {
        return round(6 * weightFactor(weightKg)) + round(4 * heightFactor(heightCm));
    }

    /** Weight does not affect goalkeeping; only height (taller keepers cover more goal). */
    public static int goalkeepingDelta(int weightKg, int heightCm) {
        return round(3 * heightFactor(heightCm));
    }

    // -- internals -------------------------------------------------------------

    /** -1.0 at the short extreme, 0 at reference, +1.0 at the tall extreme. */
    private static double heightFactor(int heightCm) {
        double f = (heightCm - REFERENCE_HEIGHT_CM) / HEIGHT_SPAN_CM;
        return Math.max(-1.0, Math.min(1.0, f));
    }

    /** -1.0 at the light extreme, 0 at reference, +1.0 at the heavy extreme. */
    private static double weightFactor(int weightKg) {
        double f = (weightKg - REFERENCE_WEIGHT_KG) / WEIGHT_SPAN_KG;
        return Math.max(-1.0, Math.min(1.0, f));
    }

    private static int round(double v) {
        return (int) Math.round(v);
    }
}
