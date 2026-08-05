package com.codewithbayramali.ftorigins.player.service;

import com.codewithbayramali.ftorigins.player.model.Player;

/**
 * A player's EFFECTIVE attributes: base (trained) values + the {@link BodyModifiers} trade-off,
 * clamped to [1, 99]. The single source of truth for every consumer of "how good is this player
 * right now" — profile responses, lineup suggestions, and the match engine's kickoff snapshots —
 * so they can never drift apart.
 */
public record EffectiveAttributes(int pace, int shooting, int passing, int dribbling,
                                  int defending, int physical, int goalkeeping, int curve) {

    public static EffectiveAttributes of(Player player) {
        int weight = player.getWeightKg();
        int height = player.getHeightCm();
        return new EffectiveAttributes(
                BodyModifiers.clamp(player.getPace() + BodyModifiers.paceDelta(weight, height)),
                BodyModifiers.clamp(player.getShooting()),   // body-neutral
                BodyModifiers.clamp(player.getPassing()),    // body-neutral
                BodyModifiers.clamp(player.getDribbling() + BodyModifiers.dribblingDelta(weight, height)),
                BodyModifiers.clamp(player.getDefending() + BodyModifiers.defendingDelta(weight, height)),
                BodyModifiers.clamp(player.getPhysical() + BodyModifiers.physicalDelta(weight, height)),
                BodyModifiers.clamp(player.getGoalkeeping() + BodyModifiers.goalkeepingDelta(weight, height)),
                BodyModifiers.clamp(player.getCurve())       // body-neutral
        );
    }

    /** General overall rating — a simple average across every skill. Position-neutral: every
     *  player can play anywhere, so all-round completeness is what counts. */
    public int overall() {
        return (int) Math.round(
                (pace + shooting + passing + dribbling + defending + physical + goalkeeping + curve) / 8.0);
    }
}
