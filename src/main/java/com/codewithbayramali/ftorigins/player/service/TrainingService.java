package com.codewithbayramali.ftorigins.player.service;

import com.codewithbayramali.ftorigins.exception.BusinessException;
import com.codewithbayramali.ftorigins.exception.NotFoundException;
import com.codewithbayramali.ftorigins.player.dto.AttributeChange;
import com.codewithbayramali.ftorigins.player.dto.DrillCatalogResponse;
import com.codewithbayramali.ftorigins.player.dto.TrainResult;
import com.codewithbayramali.ftorigins.player.dto.TrainingStatus;
import com.codewithbayramali.ftorigins.player.model.DrillEffect;
import com.codewithbayramali.ftorigins.player.model.Player;
import com.codewithbayramali.ftorigins.player.model.TrainableAttribute;
import com.codewithbayramali.ftorigins.player.model.TrainingDrill;
import com.codewithbayramali.ftorigins.player.repository.PlayerRepository;
import com.codewithbayramali.ftorigins.player.repository.TrainingDrillRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Timed training. A drill takes real time:
 * <ul>
 *   <li><b>START</b> spends energy up-front and opens a single active session (drill + finishesAt),</li>
 *   <li><b>CLAIM</b> (after the duration) applies the stat gains,</li>
 *   <li><b>CANCEL</b> abandons the session — no gain, but the spent energy is refunded.</li>
 * </ul>
 * Only one session at a time; energy does not regenerate while one is active (see {@link PlayerUpkeepService}).
 * A high OVR is a long grind: diminishing returns + hard cap ({@link #ATTRIBUTE_CAP}) + hours-long
 * sessions gate the rate; inactivity decay rewards consistency. The active session is also the "busy"
 * state that the match system will check so training and matches never overlap.
 */
@Service
public class TrainingService {

    static final int ATTRIBUTE_CAP = 95;
    static final int DIMINISH_FLOOR = 35;

    private final PlayerRepository playerRepository;
    private final TrainingDrillRepository drillRepository;
    private final PlayerUpkeepService upkeep;

    public TrainingService(PlayerRepository playerRepository,
                           TrainingDrillRepository drillRepository,
                           PlayerUpkeepService upkeep) {
        this.playerRepository = playerRepository;
        this.drillRepository = drillRepository;
        this.upkeep = upkeep;
    }

    /** The training screen's drill catalog (active drills, ordered). */
    @Transactional(readOnly = true)
    public List<DrillCatalogResponse> listDrills() {
        return drillRepository.findActiveWithEffects().stream()
                .map(DrillCatalogResponse::from)
                .toList();
    }

    /** Current session (or idle). Applies lazy energy/decay upkeep first. */
    @Transactional
    public TrainingStatus status(UUID userId) {
        Player player = requirePlayer(userId);
        upkeep.refresh(player);
        return statusOf(player, Instant.now());
    }

    /** Begin a timed training. Spends energy up-front; the gain lands on {@link #claim}. */
    @Transactional
    public TrainingStatus start(UUID userId, UUID drillId) {
        Player player = requirePlayer(userId);
        upkeep.refresh(player);

        if (player.getActiveDrillId() != null) {
            throw new BusinessException(HttpStatus.CONFLICT, "ALREADY_TRAINING",
                    "Claim or cancel your current training first");
        }

        TrainingDrill drill = drillRepository.findById(drillId)
                .filter(TrainingDrill::getActive)
                .orElseThrow(() -> new NotFoundException("DRILL_NOT_FOUND", "No such training drill"));

        if (player.getEnergy() < drill.getEnergyCost()) {
            throw new BusinessException(HttpStatus.CONFLICT, "NO_ENERGY", "Not enough energy to start this drill");
        }
        if (allMaxed(player, drill)) {
            throw new BusinessException(HttpStatus.CONFLICT, "ATTRIBUTE_CAPPED",
                    "Every attribute this drill trains is already maxed");
        }

        Instant now = Instant.now();
        player.setEnergy(player.getEnergy() - drill.getEnergyCost());
        player.setActiveDrillId(drill.getId());
        player.setTrainingStartedAt(now);
        player.setTrainingFinishesAt(now.plus(Duration.ofSeconds(drill.getDurationSeconds())));
        // Training counts as activity; freeze the energy clock (no regen while a session is active).
        player.setEnergyUpdatedAt(now);
        player.setLastTrainedAt(now);
        player.setDecayUpdatedAt(now);

        playerRepository.save(player);
        return statusOf(player, now);
    }

    /** Collect a finished training: applies the stat gains and clears the session. */
    @Transactional
    public TrainResult claim(UUID userId) {
        Player player = requirePlayer(userId);
        if (player.getActiveDrillId() == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "NO_ACTIVE_TRAINING", "You are not training");
        }
        Instant now = Instant.now();
        if (player.getTrainingFinishesAt() != null && player.getTrainingFinishesAt().isAfter(now)) {
            throw new BusinessException(HttpStatus.CONFLICT, "TRAINING_NOT_FINISHED", "This training is not finished yet");
        }

        TrainingDrill drill = drillRepository.findById(player.getActiveDrillId())
                .orElseThrow(() -> new NotFoundException("DRILL_NOT_FOUND", "Training drill no longer exists"));

        List<AttributeChange> changes = new ArrayList<>();
        for (DrillEffect effect : drill.getEffects()) {
            AttributeChange change = applyGain(player, effect.getAttribute(), effect.getBaseGain());
            if (change != null) {
                changes.add(change);
            }
        }

        clearSession(player, now);
        playerRepository.save(player);
        return new TrainResult(drill.getName(), changes, player.getEnergy());
    }

    /** Abandon the active training — no gain; refunds the energy the start spent. Frees the player for a match. */
    @Transactional
    public TrainingStatus cancel(UUID userId) {
        Player player = requirePlayer(userId);
        if (player.getActiveDrillId() == null) {
            throw new BusinessException(HttpStatus.CONFLICT, "NO_ACTIVE_TRAINING", "You are not training");
        }
        Instant now = Instant.now();
        // No gain is awarded, so refund the energy the start spent — only the elapsed time is lost.
        drillRepository.findById(player.getActiveDrillId()).ifPresent(drill ->
                player.setEnergy(Math.min(PlayerUpkeepService.MAX_ENERGY,
                        player.getEnergy() + drill.getEnergyCost())));
        clearSession(player, now);
        playerRepository.save(player);
        return statusOf(player, now);
    }

    // -- helpers ---------------------------------------------------------------

    private Player requirePlayer(UUID userId) {
        return playerRepository.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException("PLAYER_NOT_FOUND", "No player profile for this account"));
    }

    /** Clears the session and resumes the energy/decay clocks from now (no back-regen for the span). */
    private void clearSession(Player player, Instant now) {
        player.setActiveDrillId(null);
        player.setTrainingStartedAt(null);
        player.setTrainingFinishesAt(null);
        player.setEnergyUpdatedAt(now);
        player.setLastTrainedAt(now);
        player.setDecayUpdatedAt(now);
    }

    private TrainingStatus statusOf(Player player, Instant now) {
        UUID drillId = player.getActiveDrillId();
        if (drillId == null) {
            return TrainingStatus.idle();
        }
        String name = drillRepository.findById(drillId).map(TrainingDrill::getName).orElse(null);
        Instant finishesAt = player.getTrainingFinishesAt();
        long remaining = finishesAt == null ? 0 : Math.max(0, Duration.between(now, finishesAt).getSeconds());
        return new TrainingStatus(true, drillId.toString(), name,
                finishesAt == null ? null : finishesAt.toString(), remaining, remaining <= 0);
    }

    private boolean allMaxed(Player player, TrainingDrill drill) {
        for (DrillEffect e : drill.getEffects()) {
            if (AttributeAccess.read(player, e.getAttribute()) < ATTRIBUTE_CAP) {
                return false;
            }
        }
        return true;
    }

    /**
     * Trains one attribute: the diminishing-returns gain, doubled while it repays decay debt — points
     * lost to inactivity come back twice as fast as new ones are earned ("muscle memory"). Null when
     * the attribute is already maxed.
     */
    static AttributeChange applyGain(Player player, TrainableAttribute attribute, int baseGain) {
        int current = AttributeAccess.read(player, attribute);
        if (current >= ATTRIBUTE_CAP) {
            return null;
        }
        int gain = computeGain(baseGain, current, ATTRIBUTE_CAP);
        int debt = AttributeAccess.readDebt(player, attribute);
        int total = Math.min(ATTRIBUTE_CAP - current, gain + Math.min(gain, debt));
        int updated = current + total;
        AttributeAccess.write(player, attribute, updated);
        AttributeAccess.writeDebt(player, attribute, Math.max(0, debt - total));
        return new AttributeChange(attribute.name(), total, updated);
    }

    /** Diminishing returns: gain shrinks as the attribute approaches the cap (always at least +1). */
    static int computeGain(int baseGain, int current, int cap) {
        int span = Math.max(1, cap - DIMINISH_FLOOR);
        int raw = (int) Math.round(baseGain * (double) (cap - current) / span);
        return Math.max(1, Math.min(raw, cap - current));
    }
}
