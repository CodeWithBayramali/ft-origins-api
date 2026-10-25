package com.codewithbayramali.ftorigins.player.service;

import com.codewithbayramali.ftorigins.exception.BusinessException;
import com.codewithbayramali.ftorigins.geo.model.Region;
import com.codewithbayramali.ftorigins.geo.service.RegionService;
import com.codewithbayramali.ftorigins.model.User;
import com.codewithbayramali.ftorigins.moderation.ProfanityFilter;
import com.codewithbayramali.ftorigins.player.dto.UpdatePlayerRequest;
import com.codewithbayramali.ftorigins.player.model.Player;
import com.codewithbayramali.ftorigins.player.repository.PlayerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** The display name is shown to other players, so both places that set it screen it. */
class PlayerServiceTest {

    private PlayerRepository players;
    private RegionService regions;
    private ProfanityFilter profanity;
    private PlayerService service;

    @BeforeEach
    void setUp() {
        players = mock(PlayerRepository.class);
        regions = mock(RegionService.class);
        profanity = mock(ProfanityFilter.class);
        service = new PlayerService(players, regions, mock(PlayerUpkeepService.class), profanity);
        when(players.save(any(Player.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void aProfaneDisplayNameStopsRegistrationBeforeTheTeamIsResolved() {
        when(profanity.isProfane("Orospu")).thenReturn(true);

        assertThatThrownBy(() -> register(" Orospu "))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCode()).isEqualTo("DISPLAY_NAME_REJECTED");
                });
        verifyNoInteractions(regions);
    }

    @Test
    void registrationKeepsACleanDisplayNameTrimmedAndFallsBackToTheUsername() {
        when(regions.getOrCreateForLocation(anyDouble(), anyDouble(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(new Region());

        assertThat(register(" Ali Kaya ").getDisplayName()).isEqualTo("Ali Kaya");
        assertThat(register(null).getDisplayName()).isEqualTo("ali_kaya");
    }

    @Test
    void aProfaneDisplayNameIsNotSavedOnAProfileEdit() {
        UUID userId = UUID.randomUUID();
        Player player = new Player();
        player.setDisplayName("Ali");
        when(players.findByUserId(userId)).thenReturn(Optional.of(player));
        when(profanity.isProfane("Pezevenk")).thenReturn(true);

        assertThatThrownBy(() -> service.updateProfile(userId, new UpdatePlayerRequest("Pezevenk", null, null)))
                .isInstanceOfSatisfying(BusinessException.class,
                        e -> assertThat(e.getCode()).isEqualTo("DISPLAY_NAME_REJECTED"));
        assertThat(player.getDisplayName()).isEqualTo("Ali");
        verify(players, never()).save(any(Player.class));
    }

    private Player register(String displayName) {
        User user = new User();
        user.setUsername("ali_kaya");
        return service.createForRegistration(user, 36.9, 30.7, null, displayName, null, null,
                null, null, null, null, false);
    }
}
