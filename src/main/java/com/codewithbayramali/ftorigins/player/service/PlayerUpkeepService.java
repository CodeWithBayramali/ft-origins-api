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
 *       erodes by {@link #DECAY_PER_DAY}/day down to {@link #DECAY_FLOOR}, so recent activity (not
 *       seniority) drives the overall rating and keeps players coming back.</li>
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
    static final int DECAY_PER_DAY = 1;
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
        // Count only whole days past the grace window that have not already been decayed.
        Instant start = player.getDecayUpdatedAt().isAfter(graceEnd) ? player.getDecayUpdatedAt() : graceEnd;
        long days = Duration.between(start, now).toDays();
        if (days <= 0) {
            return false;
        }
        int drop = (int) (days * DECAY_PER_DAY);
        boolean changed = decayAttributes(player, drop);
        if (changed) {
            player.setDecayUpdatedAt(start.plus(Duration.ofDays(days)));
        }
        // If nothing changed (already at the floor) we leave the marker untouched — recomputing a big
        // day-count next time is cheap and still produces no write.
        return changed;
    }

    private boolean decayAttributes(Player p, int drop) {
        boolean changed = false;
        for (TrainableAttribute attr : TrainableAttribute.values()) {
            int current = AttributeAccess.read(p, attr);
            if (current <= DECAY_FLOOR) {
                continue;
            }
            int next = Math.max(DECAY_FLOOR, current - drop);
            if (next != current) {
                AttributeAccess.write(p, attr, next);
                changed = true;
            }
        }
        return changed;
    }
}
