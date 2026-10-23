package com.codewithbayramali.ftorigins.match.service;

import com.codewithbayramali.ftorigins.economy.model.CoinReason;
import com.codewithbayramali.ftorigins.economy.repository.CoinTransactionRepository;
import com.codewithbayramali.ftorigins.economy.service.CoinWallet;
import com.codewithbayramali.ftorigins.match.model.MatchType;
import com.codewithbayramali.ftorigins.match.repository.MatchParticipantRepository;
import com.codewithbayramali.ftorigins.match.repository.MatchParticipantRepository.HumanParticipant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MatchRewardServiceTest {

    private final UUID matchId = UUID.randomUUID();
    private final UUID home = UUID.randomUUID();
    private final UUID away = UUID.randomUUID();
    // Fixed ids so their sort order is known: a < b < c.
    private final UUID a = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private final UUID b = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private final UUID c = UUID.fromString("00000000-0000-0000-0000-00000000000c");

    private MatchParticipantRepository participants;
    private CoinTransactionRepository transactions;
    private CoinWallet wallet;
    private MatchRewardService service;

    private static HumanParticipant person(UUID regionId, UUID userId) {
        return new HumanParticipant() {
            @Override
            public UUID getRegionId() {
                return regionId;
            }

            @Override
            public UUID getUserId() {
                return userId;
            }
        };
    }

    @BeforeEach
    void setUp() {
        participants = mock(MatchParticipantRepository.class);
        transactions = mock(CoinTransactionRepository.class);
        wallet = mock(CoinWallet.class);
        service = new MatchRewardService(participants, transactions, wallet);
        // c and a played for the home side, b for the away side (listed out of order on purpose).
        when(participants.findHumanParticipants(matchId))
                .thenReturn(List.of(person(home, c), person(away, b), person(home, a)));
    }

    @Test
    void rewardsByResult() {
        assertThat(MatchRewardService.reward(true, 2)).isEqualTo(100);
        assertThat(MatchRewardService.reward(true, 0)).isEqualTo(60);
        assertThat(MatchRewardService.reward(true, -1)).isEqualTo(30);
        assertThat(MatchRewardService.reward(false, 1)).isEqualTo(40);
        assertThat(MatchRewardService.reward(false, 0)).isEqualTo(25);
        assertThat(MatchRewardService.reward(false, -3)).isEqualTo(15);
    }

    @Test
    void aLeagueWinPaysTheWinnersAndTheLosersEachTheirShareInAccountOrder() {
        service.payOut(matchId, MatchType.LEAGUE, home, 2, 1);

        InOrder order = inOrder(wallet);
        order.verify(wallet).credit(a, 100, CoinReason.LEAGUE_MATCH, matchId.toString());
        order.verify(wallet).credit(b, 30, CoinReason.LEAGUE_MATCH, matchId.toString());
        order.verify(wallet).credit(c, 100, CoinReason.LEAGUE_MATCH, matchId.toString());
    }

    @Test
    void aFriendlyDrawPaysTheFriendlyRate() {
        service.payOut(matchId, MatchType.FRIENDLY, home, 1, 1);

        verify(wallet).credit(a, 25, CoinReason.FRIENDLY_MATCH, matchId.toString());
        verify(wallet).credit(b, 25, CoinReason.FRIENDLY_MATCH, matchId.toString());
    }

    @Test
    void friendliesStopPayingAfterTwoADay() {
        when(transactions.countByUserIdAndReasonAndCreatedAtGreaterThanEqual(eq(a), eq(CoinReason.FRIENDLY_MATCH), any()))
                .thenReturn(2L);

        service.payOut(matchId, MatchType.FRIENDLY, home, 3, 0);

        verify(wallet, never()).credit(eq(a), anyInt(), any(), anyString());
        verify(wallet).credit(c, 40, CoinReason.FRIENDLY_MATCH, matchId.toString());
    }

    @Test
    void leagueMatchesHaveNoDailyLimit() {
        service.payOut(matchId, MatchType.LEAGUE, home, 0, 0);

        verify(transactions, never()).countByUserIdAndReasonAndCreatedAtGreaterThanEqual(any(), any(), any());
        verify(wallet).credit(a, 60, CoinReason.LEAGUE_MATCH, matchId.toString());
    }
}
