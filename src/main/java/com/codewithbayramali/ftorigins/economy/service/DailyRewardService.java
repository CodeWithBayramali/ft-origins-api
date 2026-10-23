package com.codewithbayramali.ftorigins.economy.service;

import com.codewithbayramali.ftorigins.economy.dto.DailyRewardResponse;
import com.codewithbayramali.ftorigins.economy.model.CoinReason;
import com.codewithbayramali.ftorigins.exception.ConflictException;
import com.codewithbayramali.ftorigins.exception.NotFoundException;
import com.codewithbayramali.ftorigins.model.User;
import com.codewithbayramali.ftorigins.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * The once-a-day reward. A day is a UTC calendar day (for Turkey it turns at 03:00). Claiming on
 * consecutive days climbs a 7-day ladder; a missed day starts it again from day 1, and after day 7
 * it goes round again.
 */
@Service
public class DailyRewardService {

    /** Coins per ladder day: day 1 first. A full week pays 275. */
    static final int[] LADDER = {20, 25, 30, 35, 40, 50, 75};

    private final UserRepository userRepository;
    private final CoinWallet coinWallet;

    public DailyRewardService(UserRepository userRepository, CoinWallet coinWallet) {
        this.userRepository = userRepository;
        this.coinWallet = coinWallet;
    }

    @Transactional(readOnly = true)
    public DailyRewardResponse status(UUID userId) {
        return toResponse(requireUser(userId), today());
    }

    /** Collects today's reward; DAILY_REWARD_CLAIMED if it already was. */
    @Transactional
    public DailyRewardResponse claim(UUID userId) {
        User user = requireUser(userId);
        LocalDate today = today();
        if (today.equals(user.getDailyRewardDay())) {
            throw alreadyClaimed();
        }
        int day = ladderDay(user.getDailyRewardDay(), user.getDailyRewardStreak(), today);
        // The wallet locks and re-reads the user, so a claim racing this one is seen here: it paid
        // today's grant first and this one pays nothing. The day is stamped after the payment — the
        // re-read would otherwise drop it.
        if (!coinWallet.credit(userId, LADDER[day - 1], CoinReason.DAILY_REWARD, today.toString())) {
            throw alreadyClaimed();
        }
        user.setDailyRewardDay(today);
        user.setDailyRewardStreak(day);
        return toResponse(user, today);
    }

    /** The ladder day a claim on {@code today} is: the next one after yesterday's, otherwise day 1. */
    static int ladderDay(LocalDate lastClaimDay, int lastLadderDay, LocalDate today) {
        if (lastClaimDay != null && lastClaimDay.plusDays(1).equals(today) && lastLadderDay >= 1) {
            return lastLadderDay % LADDER.length + 1;
        }
        return 1;
    }

    private DailyRewardResponse toResponse(User user, LocalDate today) {
        boolean claimable = !today.equals(user.getDailyRewardDay());
        int day = claimable
                ? ladderDay(user.getDailyRewardDay(), user.getDailyRewardStreak(), today)
                : Math.max(1, user.getDailyRewardStreak());
        List<Integer> ladder = Arrays.stream(LADDER).boxed().toList();
        String dayEndsAt = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toString();
        return new DailyRewardResponse(claimable, day, LADDER[day - 1], ladder, dayEndsAt, user.getCoins());
    }

    private User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("USER_NOT_FOUND", "No such account"));
    }

    private static LocalDate today() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    private static ConflictException alreadyClaimed() {
        return new ConflictException("DAILY_REWARD_CLAIMED", "Today's reward is already collected");
    }
}
