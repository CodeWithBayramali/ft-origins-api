package com.codewithbayramali.ftorigins.service;

import com.codewithbayramali.ftorigins.auth.dto.RegisterRequest;
import com.codewithbayramali.ftorigins.auth.security.JwtService;
import com.codewithbayramali.ftorigins.exception.BusinessException;
import com.codewithbayramali.ftorigins.moderation.ProfanityFilter;
import com.codewithbayramali.ftorigins.player.service.PlayerService;
import com.codewithbayramali.ftorigins.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    private UserRepository users;
    private PlayerService players;
    private ProfanityFilter profanity;
    private AuthService service;

    @BeforeEach
    void setUp() {
        users = mock(UserRepository.class);
        players = mock(PlayerService.class);
        profanity = mock(ProfanityFilter.class);
        service = new AuthService(users, mock(PasswordEncoder.class), mock(AuthenticationManager.class),
                mock(JwtService.class), mock(StringRedisTemplate.class), players, profanity);
    }

    @Test
    void aProfaneUsernameIsRejectedBeforeAnythingIsLookedUpOrSaved() {
        when(profanity.isProfane("s1ktir")).thenReturn(true);

        assertThatThrownBy(() -> service.register(request("s1ktir")))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCode()).isEqualTo("USERNAME_REJECTED");
                });
        verifyNoInteractions(users, players);
    }

    private static RegisterRequest request(String username) {
        return new RegisterRequest(username, "player@example.com", "secret123", LocalDate.of(2000, 1, 1),
                36.9, 30.7, null, null, null, null, null, null, null, false, null);
    }
}
