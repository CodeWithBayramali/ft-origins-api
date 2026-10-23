package com.codewithbayramali.ftorigins.player.service;

import com.codewithbayramali.ftorigins.player.dto.AttributeChange;
import com.codewithbayramali.ftorigins.player.model.Player;
import com.codewithbayramali.ftorigins.player.model.TrainableAttribute;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/** Soft inactivity decay (PlayerUpkeepService) and the muscle memory that repays it (TrainingService). */
class SoftDecayTest {

    private final PlayerUpkeepService upkeep = new PlayerUpkeepService();

    /** A player idle for {@code idle} (nothing decayed yet), every attribute at {@code level}, energy full. */
    private static Player idleFor(Duration idle, int level) {
        Player p = new Player();
        for (TrainableAttribute a : TrainableAttribute.values()) {
            AttributeAccess.write(p, a, level);
        }
        Instant lastTrained = Instant.now().minus(idle);
        p.setLastTrainedAt(lastTrained);
        p.setDecayUpdatedAt(lastTrained);
        p.setEnergy(PlayerUpkeepService.MAX_ENERGY);
        return p;
    }

    private static Duration days(double d) {
        return Duration.ofMinutes(Math.round(d * 24 * 60));
    }

    @Test
    void nothingDecaysWithinTheGraceDays() {
        Player p = idleFor(days(3.9), 60);

        assertThat(upkeep.refresh(p)).isFalse();
        assertThat(p.getShooting()).isEqualTo(60);
    }

    @Test
    void decaysAPointEveryTwoIdleDaysAfterTheGrace() {
        Player p = idleFor(days(3 + 4.2), 60);   // 4 days past the grace = 2 points

        assertThat(upkeep.refresh(p)).isTrue();
        for (TrainableAttribute a : TrainableAttribute.values()) {
            assertThat(AttributeAccess.read(p, a)).isEqualTo(58);
            assertThat(AttributeAccess.readDebt(p, a)).isEqualTo(2);
        }
    }

    @Test
    void anOddIdleDayCarriesOverToTheNextPoint() {
        Player p = idleFor(days(3 + 3.2), 60);   // 3 days past the grace: 1 point, 1 day left over

        upkeep.refresh(p);

        assertThat(p.getPace()).isEqualTo(59);
        // The marker moved by the whole period only (grace end + 2 days), not by all 3 days.
        Instant graceEnd = p.getLastTrainedAt().plus(Duration.ofDays(PlayerUpkeepService.DECAY_GRACE_DAYS));
        assertThat(p.getDecayUpdatedAt()).isEqualTo(graceEnd.plus(Duration.ofDays(2)));
    }

    @Test
    void aMonthAwayCostsFivePointsNotTwentySeven() {
        Player p = idleFor(days(30.5), 70);

        upkeep.refresh(p);

        for (TrainableAttribute a : TrainableAttribute.values()) {
            assertThat(AttributeAccess.read(p, a)).isEqualTo(65);
            assertThat(AttributeAccess.readDebt(p, a)).isEqualTo(PlayerUpkeepService.DECAY_MAX_DEBT);
        }
        // At its limit nothing more is taken, however long the break goes on.
        assertThat(upkeep.refresh(p)).isFalse();
        assertThat(p.getPassing()).isEqualTo(65);
    }

    @Test
    void decayOnlyTakesWhatTheDebtLimitStillAllows() {
        Player p = idleFor(days(30.5), 70);
        p.setDecayDebtDribbling(4);   // already 4 points down from an earlier break

        upkeep.refresh(p);

        assertThat(p.getDribbling()).isEqualTo(69);
        assertThat(p.getDecayDebtDribbling()).isEqualTo(5);
        assertThat(p.getShooting()).isEqualTo(65);
    }

    @Test
    void neverBelowTheFloor() {
        Player p = idleFor(days(30.5), 32);

        upkeep.refresh(p);

        assertThat(p.getCurve()).isEqualTo(PlayerUpkeepService.DECAY_FLOOR);
        assertThat(p.getDecayDebtCurve()).isEqualTo(2);
    }

    @Test
    void trainingWithoutDebtGainsTheNormalAmount() {
        Player p = idleFor(Duration.ZERO, 40);

        AttributeChange change = TrainingService.applyGain(p, TrainableAttribute.SHOOTING, 2);

        // round(2 * (95 - 40) / 60) = 2
        assertThat(change.getGain()).isEqualTo(2);
        assertThat(p.getShooting()).isEqualTo(42);
    }

    @Test
    void decayedPointsComeBackTwiceAsFast() {
        Player p = idleFor(Duration.ZERO, 40);
        p.setDecayDebtShooting(5);

        AttributeChange change = TrainingService.applyGain(p, TrainableAttribute.SHOOTING, 2);

        assertThat(change.getGain()).isEqualTo(4);
        assertThat(change.getNewValue()).isEqualTo(44);
        assertThat(p.getDecayDebtShooting()).isEqualTo(1);
    }

    @Test
    void theBonusIsOnlyAsBigAsWhatIsStillOwed() {
        Player p = idleFor(Duration.ZERO, 40);
        p.setDecayDebtShooting(1);

        AttributeChange change = TrainingService.applyGain(p, TrainableAttribute.SHOOTING, 2);

        assertThat(change.getGain()).isEqualTo(3);
        assertThat(p.getDecayDebtShooting()).isZero();
    }

    @Test
    void theCapStillHolds() {
        Player p = idleFor(Duration.ZERO, 94);
        p.setDecayDebtPace(5);

        AttributeChange change = TrainingService.applyGain(p, TrainableAttribute.PACE, 4);

        assertThat(change.getNewValue()).isEqualTo(TrainingService.ATTRIBUTE_CAP);
        assertThat(p.getDecayDebtPace()).isEqualTo(4);
        assertThat(TrainingService.applyGain(p, TrainableAttribute.PACE, 4)).isNull();
    }
}
