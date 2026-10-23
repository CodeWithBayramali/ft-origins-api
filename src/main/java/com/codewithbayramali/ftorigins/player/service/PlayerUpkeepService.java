package com.codewithbayramali.ftorigins.player.service;

import com.codewithbayramali.ftorigins.player.model.Player;
import com.codewithbayramali.ftorigins.player.model.TrainableAttribute;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Time-based player upkeep, applied lazily on read/train (no scheduler — cheap and scales to any
 * number of players):
 * <ul>
 *   <li><b>Energy regen</b> — the bar refills toward {@link #MAX_ENERGY} at {@link #ENERGY_REGEN_PER_HOUR}/h.</li>
 *   <li><b>Inactivity decay</b> — after {@link #DECAY_GRACE_DAYS} days without training, every attribute
 *       erodes by a point every {@link #DECAY_DAYS_PER_POINT} days, down to {@link #DECAY_FLOOR} — so
 *       recent activity (not seniority) drives the overall rating and keeps players coming back. It is
 *       soft: an attribute stops eroding once it has lost {@link #DECAY_MAX_DEBT} points it hasn't
 *       trained back (its decay debt), and training wins those back twice as fast (see TrainingService).
 *       A long break costs a little, never everything — the old rule (a point a day, no limit) took 11
 *       points off every attribute in two weeks away.</li>
 * </ul>
 * Only mutates the player when something actually changes, so an idle player's profile GET stays
 * write-free. Energy's regen clock is reset on spend by the caller, so a full bar never needs a write here.
 */
@Component
public class PlayerUpkeepService {

    // Energy
    static final int MAX_ENERGY = 100;
    static final int ENERGY_REGEN_PER_HOUR = 10;

    // Inactivity decay
    static final int DECAY_GRACE_DAYS = 3;
    static final int DECAY_DAYS_PER_POINT = 2;
    static final int DECAY_MAX_DEBT = 5;
    static final int DECAY_FLOOR = 30;

    /** Apply energy regen + inactivity decay. Returns {@code true} if the player was mutated. */
    public boolean refresh(Player player) {
        Instant now = Instant.now();
        boolean changed = regenerateEnergy(player, now);
        changed |= applyDecay(player, now);
        return changed;
    }

    private boolean regenerateEnergy(Player player, Instant now) {
        if (player.getActiveDrillId() != null) {
            return false; // energy does not regenerate while a training session is active
        }
        if (player.getEnergy() >= MAX_ENERGY) {
            // Full: the regen clock is reset whenever energy is spent, so a stale marker here is
            // harmless and we avoid writing on every read.
            return false;
        }
        long minutes = Duration.between(player.getEnergyUpdatedAt(), now).toMinutes();
        if (minutes <= 0) {
            return false;
        }
        int regen = (int) (minutes * ENERGY_REGEN_PER_HOUR / 60);
        if (regen <= 0) {
            return false;
        }
        int newEnergy = Math.min(MAX_ENERGY, player.getEnergy() + regen);
        if (newEnergy >= MAX_ENERGY) {
            player.setEnergyUpdatedAt(now);
        } else {
            long consumedSeconds = Math.round(regen / (double) ENERGY_REGEN_PER_HOUR * 3600);
            player.setEnergyUpdatedAt(player.getEnergyUpdatedAt().plusSeconds(consumedSeconds));
        }
        player.setEnergy(newEnergy);
        return true;
    }

    private boolean applyDecay(Player player, Instant now) {
        Instant graceEnd = player.getLastTrainedAt().plus(Duration.ofDays(DECAY_GRACE_DAYS));
        // Count only whole decay periods past the grace window that have not already been applied.
        Instant start = player.getDecayUpdatedAt().isAfter(graceEnd) ? player.getDecayUpdatedAt() : graceEnd;
        long points = Duration.between(start, now).toDays() / DECAY_DAYS_PER_POINT;
        if (points <= 0) {
            return false;
        }
        boolean changed = decayAttributes(player, (int) Math.min(points, DECAY_MAX_DEBT));
        if (changed) {
            // Only the whole periods are spent: an odd idle day carries over to the next one.
            player.setDecayUpdatedAt(start.plus(Duration.ofDays(points * DECAY_DAYS_PER_POINT)));
        }
        // If nothing changed (every attribute at its debt limit or the floor) we leave the marker
        // untouched — recomputing a big period count next time is cheap and still produces no write.
        return changed;
    }

    /** Takes up to {@code drop} points off each attribute — never past its debt limit or the floor. */
    private boolean decayAttributes(Player p, int drop) {
        boolean changed = false;
        for (TrainableAttribute attr : TrainableAttribute.values()) {
            int current = AttributeAccess.read(p, attr);
            int debt = AttributeAccess.readDebt(p, attr);
            int next = Math.max(DECAY_FLOOR, current - Math.min(drop, DECAY_MAX_DEBT - debt));
            if (next < current) {
                AttributeAccess.write(p, attr, next);
                AttributeAccess.writeDebt(p, attr, debt + (current - next));
                changed = true;
            }
        }
        return changed;
    }
}
