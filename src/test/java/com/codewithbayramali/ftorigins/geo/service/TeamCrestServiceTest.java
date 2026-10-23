package com.codewithbayramali.ftorigins.geo.service;

import com.codewithbayramali.ftorigins.exception.BusinessException;
import com.codewithbayramali.ftorigins.geo.dto.CrestDto;
import com.codewithbayramali.ftorigins.geo.dto.TeamResponse;
import com.codewithbayramali.ftorigins.geo.dto.TeamStatsResponse;
import com.codewithbayramali.ftorigins.geo.model.Region;
import com.codewithbayramali.ftorigins.match.service.FounderGuard;
import com.codewithbayramali.ftorigins.player.model.Player;
import com.codewithbayramali.ftorigins.player.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TeamCrestServiceTest {

    private static final CrestDto OLD = new CrestDto(0, 12, 3, 0);
    private static final CrestDto NEW = new CrestDto(7, 63, 15, 4);

    private final UUID founderUserId = UUID.randomUUID();
    private final UUID memberUserId = UUID.randomUUID();
    private PlayerRepository players;
    private TeamRosterService roster;
    private TeamCrestService service;
    private Region team;

    @BeforeEach
    void setUp() {
        players = mock(PlayerRepository.class);
        roster = mock(TeamRosterService.class);
        service = new TeamCrestService(players, new FounderGuard(), new TeamCrestPolicy(), roster);

        team = new Region();
        team.setId(UUID.randomUUID());
        team.setName("Kırmızı Şeytanlar");
        new TeamCrestPolicy().apply(team, OLD);
        team.setFounderPlayerId(playerOf(founderUserId).getId());
        playerOf(memberUserId);
        // The "My Team" read is built from the team as it is when called — i.e. after the change.
        when(roster.getMyTeam(any())).thenAnswer(call -> TeamResponse.of(team, List.of(), true, 10, 6,
                TeamStatsResponse.empty(), List.of(), List.of()));
    }

    @Test
    void theFounderChangesTheCrestAndGetsTheTeamBackWithIt() {
        TeamResponse response = service.changeMyCrest(founderUserId, NEW);

        assertThat(CrestDto.from(team)).isEqualTo(NEW);
        assertThat(response.getCrest()).isEqualTo(NEW);
    }

    @Test
    void aMemberWhoIsNotTheFounderCannotChangeTheCrest() {
        assertThatThrownBy(() -> service.changeMyCrest(memberUserId, NEW))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(e.getCode()).isEqualTo("NOT_FOUNDER");
                });
        assertThat(CrestDto.from(team)).isEqualTo(OLD);
        verify(roster, never()).getMyTeam(any());
    }

    @Test
    void aCrestWhoseSymbolColourMatchesItsBaseIsRejectedAndTheOldOneStays() {
        assertThatThrownBy(() -> service.changeMyCrest(founderUserId, new CrestDto(7, 63, 4, 4)))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCode()).isEqualTo("CREST_COLOURS_SAME");
                });
        assertThat(CrestDto.from(team)).isEqualTo(OLD);
    }

    private Player playerOf(UUID userId) {
        Player player = new Player();
        player.setId(UUID.randomUUID());
        player.setRegion(team);
        when(players.findDetailByUserId(userId)).thenReturn(Optional.of(player));
        return player;
    }
}
