package com.codewithbayramali.ftorigins.match.engine;

/** The engine's probability shapes: a logistic curve on stat difference, plus clamping. */
final class DuelMath {

    private DuelMath() {
    }

    /**
     * Probability that the side holding {@code a} beats the side holding {@code b}:
     * 0.5 when equal, ~0.9 when a leads by one full {@code scale}. The classic Elo-style curve —
     * smooth, monotonic, and it rewards every single training point.
     */
    static double logistic(double a, double b, double scale) {
        return 1.0 / (1.0 + Math.pow(10.0, (b - a) / scale));
    }

    static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
