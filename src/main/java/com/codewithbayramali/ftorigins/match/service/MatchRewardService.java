package com.codewithbayramali.ftorigins.match.service;

import com.codewithbayramali.ftorigins.economy.model.CoinReason;
import com.codewithbayramali.ftorigins.economy.repository.CoinTransactionRepository;
import com.codewithbayramali.ftorigins.economy.service.CoinWallet;
import com.codewithbayramali.ftorigins.match.model.MatchType;
import com.codewithbayramali.ftorigins.match.repository.MatchParticipantRepository;
import com.codewithbayramali.ftorigins.match.repository.MatchParticipantRepository.HumanParticipant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Coins for playing. Everyone who played in a match — starters and substitutes; the AI fillers earn
 * nothing — is paid when it finishes: a win most, a draw less, a loss still something, since turning
 * up is what keeps a team alive. League matches pay in full. Friendlies pay less, and only the first
 * {@link #FRIENDLY_PAID_PER_DAY} a person plays in a (UTC) day: a founder can arrange as many as he likes.
 */
@Service
public class MatchRewardService {

    static final int LEAGUE_WIN = 100;
    static final int LEAGUE_DRAW = 60;
    static final int LEAGUE_LOSS = 30;
    static final int FRIENDLY_WIN = 40;
    static final int FRIENDLY_DRAW = 25;
    static final int FRIENDLY_LOSS = 15;
    static final int FRIENDLY_PAID_PER_DAY = 2;

    private final MatchParticipantRepository participants;
    private final CoinTransactionRepository transactions;
    private final CoinWallet coinWallet;

    public MatchRewardService(MatchParticipantRepository participants,
                              CoinTransactionRepository transactions,
                              CoinWallet coinWallet) {
        this.participants = participants;
        this.transactions = transactions;
        this.coinWallet = coinWallet;
    }

    /**
     * Pays everyone who played in a finished match. Runs inside the transaction that finishes it, so
     * the match is FINISHED and paid together, or neither. Running it again for the same match pays
     * nobody twice: the ledger takes one grant per person per match.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void payOut(UUID matchId, MatchType type, UUID homeRegionId, int homeScore, int awayScore) {
        boolean league = type == MatchType.LEAGUE;
        CoinReason reason = league ? CoinReason.LEAGUE_MATCH : CoinReason.FRIENDLY_MATCH;
        Instant todayStart = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        // In account order: two payouts running at once lock the same users in the same order, never crosswise.
        List<HumanParticipant> people = participants.findHumanParticipants(matchId).stream()
                .sorted(Comparator.comparing(HumanParticipant::getUserId))
                .toList();
        for (HumanParticipant person : people) {
            if (!league && transactions.countByUserIdAndReasonAndCreatedAtGreaterThanEqual(
                    person.getUserId(), reason, todayStart) >= FRIENDLY_PAID_PER_DAY) {
                continue;
            }
            int goalDifference = person.getRegionId().equals(homeRegionId)
                    ? homeScore - awayScore
                    : awayScore - homeScore;
            coinWallet.credit(person.getUserId(), reward(league, goalDifference), reason, matchId.toString());
        }
    }

    /** Coins for a result, by match type and the goal difference seen from the player's side. */
    static int reward(boolean league, int goalDifference) {
        if (goalDifference > 0) {
            return league ? LEAGUE_WIN : FRIENDLY_WIN;
        }
        if (goalDifference == 0) {
            return league ? LEAGUE_DRAW : FRIENDLY_DRAW;
        }
        return league ? LEAGUE_LOSS : FRIENDLY_LOSS;
    }
}
