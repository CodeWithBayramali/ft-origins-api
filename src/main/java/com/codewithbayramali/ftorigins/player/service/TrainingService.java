package com.codewithbayramali.ftorigins.player.service;

import com.codewithbayramali.ftorigins.exception.BusinessException;
import com.codewithbayramali.ftorigins.exception.NotFoundException;
import com.codewithbayramali.ftorigins.player.dto.TrainResult;
import com.codewithbayramali.ftorigins.player.model.Player;
import com.codewithbayramali.ftorigins.player.model.TrainableAttribute;
import com.codewithbayramali.ftorigins.player.repository.PlayerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Player development. Fairness levers:
 *  - training costs energy that regenerates over time (caps how much you can grind per day),
 *  - attribute gains diminish as they approach a level-gated cap (max 95),
 *  - lower-level players earn XP faster (catch-up) so the gap to veterans stays bounded.
 * Match-performance XP (later) will feed the same {@link #grantXp} path.
 */
@Service
public class TrainingService {

    static final int MAX_ENERGY = 100;
    static final int ENERGY_REGEN_PER_HOUR = 10;
    static final int TRAINING_ENERGY_COST = 20;
    static final int BASE_TRAINING_GAIN = 3;
    static final int ATTRIBUTE_FLOOR = 40;
    static final int XP_PER_TRAINING = 10;

    private final PlayerRepository playerRepository;

    public TrainingService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public TrainResult train(UUID userId, TrainableAttribute attribute) {
        Player player = playerRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("PLAYER_NOT_FOUND", "No player profile for this account"));

        regenerateEnergy(player);

        if (player.getEnergy() < TRAINING_ENERGY_COST) {
            throw new BusinessException(HttpStatus.CONFLICT, "NO_ENERGY", "Not enough energy to train");
        }

        int cap = attributeCap(player.getLevel());
        int current = read(player, attribute);
        if (current >= cap) {
            throw new BusinessException(HttpStatus.CONFLICT, "ATTRIBUTE_CAPPED",
                    "This attribute is capped for your level — level up to train it further");
        }

        int gain = computeGain(current, cap);
        write(player, attribute, current + gain);
        player.setEnergy(player.getEnergy() - TRAINING_ENERGY_COST);
        grantXp(player, (int) Math.round(XP_PER_TRAINING * catchUpMultiplier(player.getLevel())));

        playerRepository.save(player);

        return new TrainResult(attribute.name(), gain, read(player, attribute),
                player.getEnergy(), player.getLevel(), player.getXp());
    }

    /** Attribute ceiling grows with level, hard-capped so the field stays competitive. */
    static int attributeCap(int level) {
        return Math.min(95, 45 + level);
    }

    /** Diminishing returns: smaller gains as the attribute approaches its cap (always at least +1). */
    static int computeGain(int current, int cap) {
        int span = Math.max(1, cap - ATTRIBUTE_FLOOR);
        int raw = (int) Math.round(BASE_TRAINING_GAIN * (double) (cap - current) / span);
        return Math.max(1, Math.min(raw, cap - current));
    }

    /** Lower-level players gain XP faster so the gap to veterans does not explode. */
    static double catchUpMultiplier(int level) {
        return Math.max(1.0, 2.0 - 0.05 * level);
    }

    static int xpForNextLevel(int level) {
        return 100 * level;
    }

    private void grantXp(Player player, int amount) {
        int xp = player.getXp() + amount;
        int level = player.getLevel();
        while (xp >= xpForNextLevel(level)) {
            xp -= xpForNextLevel(level);
            level++;
        }
        player.setXp(xp);
        player.setLevel(level);
    }

    private void regenerateEnergy(Player player) {
        Instant now = Instant.now();
        if (player.getEnergy() >= MAX_ENERGY) {
            player.setEnergyUpdatedAt(now);
            return;
        }
        long minutes = Duration.between(player.getEnergyUpdatedAt(), now).toMinutes();
        if (minutes <= 0) {
            return;
        }
        int regen = (int) (minutes * ENERGY_REGEN_PER_HOUR / 60);
        if (regen <= 0) {
            return;
        }
        int newEnergy = Math.min(MAX_ENERGY, player.getEnergy() + regen);
        if (newEnergy >= MAX_ENERGY) {
            player.setEnergyUpdatedAt(now);
        } else {
            long consumedSeconds = Math.round(regen / (double) ENERGY_REGEN_PER_HOUR * 3600);
            player.setEnergyUpdatedAt(player.getEnergyUpdatedAt().plusSeconds(consumedSeconds));
        }
        player.setEnergy(newEnergy);
    }

    private int read(Player p, TrainableAttribute attribute) {
        return switch (attribute) {
            case PACE -> p.getPace();
            case SHOOTING -> p.getShooting();
            case PASSING -> p.getPassing();
            case DRIBBLING -> p.getDribbling();
            case DEFENDING -> p.getDefending();
            case PHYSICAL -> p.getPhysical();
            case GOALKEEPING -> p.getGoalkeeping();
            case CURVE -> p.getCurve();
        };
    }

    private void write(Player p, TrainableAttribute attribute, int value) {
        switch (attribute) {
            case PACE -> p.setPace(value);
            case SHOOTING -> p.setShooting(value);
            case PASSING -> p.setPassing(value);
            case DRIBBLING -> p.setDribbling(value);
            case DEFENDING -> p.setDefending(value);
            case PHYSICAL -> p.setPhysical(value);
            case GOALKEEPING -> p.setGoalkeeping(value);
            case CURVE -> p.setCurve(value);
        }
    }
}
