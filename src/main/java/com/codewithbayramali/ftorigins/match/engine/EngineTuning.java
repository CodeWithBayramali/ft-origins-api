package com.codewithbayramali.ftorigins.match.engine;

/**
 * Every balance knob of the engine in one place, so tuning is a data change and tests can probe
 * variants. The two clamp pairs are the "professionalism" guards: duels stay inside
 * [duelMin, duelMax] so a single moment can surprise (football) but never absurdly, and possession
 * stays inside [possessionMin, possessionMax] so weaker teams still touch the ball — superiority is
 * expressed by ACCUMULATION over 90 minutes, exactly like the real game.
 */
public record EngineTuning(
        // Logistic scales: stat-difference at which an advantage becomes strong (see DuelMath).
        double possessionScale,
        double duelScale,
        double keeperScale,
        // Clamps.
        double possessionMin, double possessionMax,
        double duelMin, double duelMax,
        // How many attacking sequences a minute produces: none / one / two.
        double quietMinuteProb,
        double doubleSequenceProb,
        // Penetration mix (remainder = long shot attempt).
        double dribblePenetrationProb,
        double passPenetrationProb,
        // Shots.
        double blockProbOpen,
        double blockProbLong,
        double onTargetFloor, double onTargetCeil,
        double goalBaseOpen, double goalBaseThrough, double goalBaseLong,
        double goalBaseHeader, double goalBaseFreeKick, double goalBasePenalty,
        double goalProbMin, double goalProbMax,
        double penaltyProbMin, double penaltyProbMax,
        // Dead balls & rebounds.
        double cornerAfterSaveProb,
        double cornerAfterBlockProb,
        double cornerHeaderProb,
        double freeKickDirectProb,
        // Discipline.
        double foulOnFailedDribbleProb,
        double boxFoulShare,
        double yellowPerFoulProb,
        double directRedPerFoulProb,
        int minOutfieldForRed,
        // Fatigue: linear condition drop over 90' plus a flat penalty for arriving with low energy.
        double fatigueFullDrop,
        double lowEnergyFloorFactor,
        // Game management: once a side leads by this many goals it cruises — conversion drops, so
        // blowouts stay football-shaped without ever dulling a competitive match.
        int mercyGoalDiff,
        double mercyConversionFactor
) {

    // Scales sit at 55-60: flat enough that a 10-OVR team edge wins ~3 of 4 matches (football,
    // not chess), steep enough that it NEVER disappears — the advantage compounds through
    // possession, duels, finishing and keeping, so training still decides seasons.
    public static final EngineTuning DEFAULT = new EngineTuning(
            60.0, 55.0, 60.0,
            0.30, 0.70,
            0.15, 0.85,
            0.35, 0.10,
            0.45, 0.35,
            0.12, 0.20,
            0.35, 0.80,
            0.30, 0.38, 0.12,
            0.22, 0.20, 0.78,
            0.04, 0.90,
            0.60, 0.92,
            0.25, 0.30, 0.60,
            0.50,
            0.22, 0.10, 0.28, 0.03, 2,
            0.12, 0.90,
            5, 0.65
    );
}
