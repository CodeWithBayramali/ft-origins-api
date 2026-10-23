package com.codewithbayramali.ftorigins.economy.service;

import com.codewithbayramali.ftorigins.economy.dto.DailyRewardResponse;
import com.codewithbayramali.ftorigins.economy.model.CoinReason;
import com.codewithbayramali.ftorigins.exception.BusinessException;
import com.codewithbayramali.ftorigins.model.User;
import com.codewithbayramali.ftorigins.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyRewardServiceTest {

    private final LocalDate today = LocalDate.now(ZoneOffset.UTC);
    private final UUID userId = UUID.randomUUID();
    private UserRepository users;
    private CoinWallet wallet;
    private DailyRewardService service;
    private User user;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        wallet = mock(CoinWallet.class);
        service = new DailyRewardService(users, wallet);
        user = new User();
        user.setId(userId);
        when(users.findById(userId)).thenReturn(Optional.of(user));
    }

    @Test
    void theLadderClimbsOnConsecutiveDaysAndGoesRound() {
        assertThat(DailyRewardService.ladderDay(null, 0, today)).isEqualTo(1);
        assertThat(DailyRewardService.ladderDay(today.minusDays(1), 3, today)).isEqualTo(4);
        assertThat(DailyRewardService.ladderDay(today.minusDays(1), 7, today)).isEqualTo(1);
    }

    @Test
    void aMissedDayStartsTheLadderAgain() {
        assertThat(DailyRewardService.ladderDay(today.minusDays(2), 5, today)).isEqualTo(1);
    }

    @Test
    void claimPaysTodaysLadderDayAndStampsTheDay() {
        user.setDailyRewardDay(today.minusDays(1));
        user.setDailyRewardStreak(2);
        when(wallet.credit(userId, 30, CoinReason.DAILY_REWARD, today.toString())).thenReturn(true);

        DailyRewardResponse response = service.claim(userId);

        assertThat(response.getDay()).isEqualTo(3);
        assertThat(response.getAmount()).isEqualTo(30);
        assertThat(response.getClaimable()).isFalse();
        assertThat(user.getDailyRewardDay()).isEqualTo(today);
        assertThat(user.getDailyRewardStreak()).isEqualTo(3);
    }

    @Test
    void aSecondClaimTheSameDayIsRefused() {
        user.setDailyRewardDay(today);
        user.setDailyRewardStreak(4);

        assertThatThrownBy(() -> service.claim(userId))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo("DAILY_REWARD_CLAIMED");
        verify(wallet, never()).credit(eq(userId), anyInt(), eq(CoinReason.DAILY_REWARD), anyString());
    }

    @Test
    void aClaimThatLostTheRaceIsRefusedAndStampsNothing() {
        when(wallet.credit(eq(userId), anyInt(), eq(CoinReason.DAILY_REWARD), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.claim(userId)).isInstanceOf(BusinessException.class);
        assertThat(user.getDailyRewardDay()).isNull();
    }

    @Test
    void statusShowsWhatTodaysClaimWouldPay() {
        user.setDailyRewardDay(today.minusDays(1));
        user.setDailyRewardStreak(6);

        DailyRewardResponse status = service.status(userId);

        assertThat(status.getClaimable()).isTrue();
        assertThat(status.getDay()).isEqualTo(7);
        assertThat(status.getAmount()).isEqualTo(75);
        assertThat(status.getLadder()).containsExactly(20, 25, 30, 35, 40, 50, 75);
    }
}
