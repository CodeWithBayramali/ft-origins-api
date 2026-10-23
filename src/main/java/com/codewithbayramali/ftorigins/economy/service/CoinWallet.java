package com.codewithbayramali.ftorigins.economy.service;

import com.codewithbayramali.ftorigins.economy.model.CoinReason;
import com.codewithbayramali.ftorigins.economy.model.CoinTransaction;
import com.codewithbayramali.ftorigins.economy.repository.CoinTransactionRepository;
import com.codewithbayramali.ftorigins.exception.BusinessException;
import com.codewithbayramali.ftorigins.exception.NotFoundException;
import com.codewithbayramali.ftorigins.model.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Every change to a coin balance goes through here and leaves a {@link CoinTransaction}. The user's
 * row is locked and re-read for the change (SELECT ... FOR UPDATE), so two changes at once can't
 * overwrite each other; and a grant or a purchase (reason + reference) happens at most once, so a
 * code path that runs twice — a retried request, two claims racing — pays or charges once.
 *
 * <p>Runs inside the caller's transaction: the change commits or rolls back with what it was for.
 */
@Service
public class CoinWallet {

    private final EntityManager entityManager;
    private final CoinTransactionRepository transactions;

    public CoinWallet(EntityManager entityManager, CoinTransactionRepository transactions) {
        this.entityManager = entityManager;
        this.transactions = transactions;
    }

    /** Pays a grant. Returns false, changing nothing, if this grant was already paid. */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean credit(UUID userId, int amount, CoinReason reason, String reference) {
        if (amount <= 0) {
            throw new IllegalArgumentException("A credit must be positive: " + amount);
        }
        User user = lock(userId);
        if (transactions.existsByUserIdAndReasonAndReference(userId, reason, reference)) {
            return false;
        }
        user.setCoins(user.getCoins() + amount);
        record(user, amount, reason, reference);
        return true;
    }

    /**
     * Charges a purchase. Returns false, changing nothing, if this purchase was already charged;
     * throws INSUFFICIENT_COINS if the balance can't cover it.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean debit(UUID userId, int amount, CoinReason reason, String reference) {
        if (amount <= 0) {
            throw new IllegalArgumentException("A debit must be positive: " + amount);
        }
        User user = lock(userId);
        if (transactions.existsByUserIdAndReasonAndReference(userId, reason, reference)) {
            return false;
        }
        if (user.getCoins() < amount) {
            throw new BusinessException(HttpStatus.CONFLICT, "INSUFFICIENT_COINS", "Not enough coins");
        }
        user.setCoins(user.getCoins() - amount);
        record(user, -amount, reason, reference);
        return true;
    }

    /** The user, row-locked and re-read: a copy loaded earlier in the transaction may be stale. */
    private User lock(UUID userId) {
        User user = entityManager.find(User.class, userId);
        if (user == null) {
            throw new NotFoundException("USER_NOT_FOUND", "No such account");
        }
        entityManager.refresh(user, LockModeType.PESSIMISTIC_WRITE);
        return user;
    }

    private void record(User user, int amount, CoinReason reason, String reference) {
        CoinTransaction row = new CoinTransaction();
        row.setUser(user);
        row.setAmount(amount);
        row.setReason(reason);
        row.setReference(reference);
        transactions.save(row);
    }
}
