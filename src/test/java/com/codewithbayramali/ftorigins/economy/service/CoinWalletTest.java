package com.codewithbayramali.ftorigins.economy.service;

import com.codewithbayramali.ftorigins.economy.model.CoinReason;
import com.codewithbayramali.ftorigins.economy.model.CoinTransaction;
import com.codewithbayramali.ftorigins.economy.repository.CoinTransactionRepository;
import com.codewithbayramali.ftorigins.exception.BusinessException;
import com.codewithbayramali.ftorigins.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CoinWalletTest {

    private final UUID userId = UUID.randomUUID();
    private EntityManager entityManager;
    private CoinTransactionRepository transactions;
    private CoinWallet wallet;
    private User user;

    @BeforeEach
    void setUp() {
        entityManager = mock(EntityManager.class);
        transactions = mock(CoinTransactionRepository.class);
        wallet = new CoinWallet(entityManager, transactions);
        user = new User();
        user.setId(userId);
        user.setCoins(100);
        when(entityManager.find(User.class, userId)).thenReturn(user);
    }

    @Test
    void creditLocksTheRowAddsTheCoinsAndRecordsTheGrant() {
        assertThat(wallet.credit(userId, 40, CoinReason.DAILY_REWARD, "2026-10-23")).isTrue();

        verify(entityManager).refresh(user, LockModeType.PESSIMISTIC_WRITE);
        assertThat(user.getCoins()).isEqualTo(140);
        ArgumentCaptor<CoinTransaction> row = ArgumentCaptor.forClass(CoinTransaction.class);
        verify(transactions).save(row.capture());
        assertThat(row.getValue().getAmount()).isEqualTo(40);
        assertThat(row.getValue().getReason()).isEqualTo(CoinReason.DAILY_REWARD);
        assertThat(row.getValue().getReference()).isEqualTo("2026-10-23");
    }

    @Test
    void aGrantAlreadyPaidPaysNothing() {
        when(transactions.existsByUserIdAndReasonAndReference(userId, CoinReason.LEAGUE_MATCH, "m1")).thenReturn(true);

        assertThat(wallet.credit(userId, 100, CoinReason.LEAGUE_MATCH, "m1")).isFalse();

        assertThat(user.getCoins()).isEqualTo(100);
        verify(transactions, never()).save(any());
    }

    @Test
    void debitChargesAndRecordsANegativeAmount() {
        assertThat(wallet.debit(userId, 75, CoinReason.BOOT_PURCHASE, "3")).isTrue();

        assertThat(user.getCoins()).isEqualTo(25);
        ArgumentCaptor<CoinTransaction> row = ArgumentCaptor.forClass(CoinTransaction.class);
        verify(transactions).save(row.capture());
        assertThat(row.getValue().getAmount()).isEqualTo(-75);
    }

    @Test
    void debitRefusesWhatTheBalanceCannotCover() {
        assertThatThrownBy(() -> wallet.debit(userId, 500, CoinReason.BOOT_PURCHASE, "1"))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo("INSUFFICIENT_COINS");
        assertThat(user.getCoins()).isEqualTo(100);
        verify(transactions, never()).save(any());
    }

    @Test
    void aPurchaseAlreadyChargedChargesNothing() {
        when(transactions.existsByUserIdAndReasonAndReference(userId, CoinReason.BOOT_PURCHASE, "3")).thenReturn(true);

        assertThat(wallet.debit(userId, 75, CoinReason.BOOT_PURCHASE, "3")).isFalse();
        assertThat(user.getCoins()).isEqualTo(100);
    }
}
