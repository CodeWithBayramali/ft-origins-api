package com.codewithbayramali.ftorigins.service;

import com.codewithbayramali.ftorigins.auth.dto.*;
import com.codewithbayramali.ftorigins.auth.security.JwtService;
import com.codewithbayramali.ftorigins.enums.UserRole;
import com.codewithbayramali.ftorigins.exception.BusinessException;
import com.codewithbayramali.ftorigins.exception.ConflictException;
import com.codewithbayramali.ftorigins.exception.UnauthorizedException;
import com.codewithbayramali.ftorigins.model.User;
import com.codewithbayramali.ftorigins.player.service.PlayerService;
import com.codewithbayramali.ftorigins.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final StringRedisTemplate redisTemplate;
    private final PlayerService playerService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       StringRedisTemplate redisTemplate,
                       PlayerService playerService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.redisTemplate = redisTemplate;
        this.playerService = playerService;
    }

    /**
     * Pre-registration availability probe (see {@link AvailabilityRequest}). Mirrors exactly the
     * checks {@link #register} enforces, so the wizard can reject a taken email/username on the form
     * where it was typed instead of at the end. Purely advisory — register stays authoritative
     * (a value can still be taken in the window between this check and the final submit).
     */
    @Transactional(readOnly = true)
    public AvailabilityResponse checkAvailability(AvailabilityRequest request) {
        boolean emailTaken = request.getEmail() != null && !request.getEmail().isBlank()
                && userRepository.existsByEmailIgnoreCase(request.getEmail().trim());
        boolean usernameTaken = request.getUsername() != null && !request.getUsername().isBlank()
                && userRepository.existsByUsernameIgnoreCase(request.getUsername().trim());
        return new AvailabilityResponse(emailTaken, usernameTaken);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new ConflictException("USERNAME_TAKEN", "Username is already taken");
        }
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new ConflictException("EMAIL_TAKEN", "Email is already registered");
        }

        LocalDate birthDate = request.getBirthDate();
        if (birthDate == null || Period.between(birthDate, LocalDate.now()).getYears() < 13) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "UNDERAGE",
                    "You must be at least 13 years old to register");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER);
        user.setPremium(false);
        user.setCoins(0);
        user.setGems(0);
        user.setBirthDate(birthDate);

        User saved;
        try {
            // Flush now so a unique-constraint violation surfaces here (not at commit). The existsBy
            // pre-checks above are not atomic: a concurrent registration with the same username/email
            // can slip in between the check and this insert, and the DB unique constraints
            // (uq_users_username / uq_users_email) reject it. Map that race to the same clean 409 the
            // pre-checks return, instead of letting it bubble up as a generic 500.
            saved = userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException dup) {
            throw new ConflictException("USERNAME_OR_EMAIL_TAKEN", "Username or email is already registered");
        }

        playerService.createForRegistration(
                saved,
                request.getLatitude(),
                request.getLongitude(),
                request.getPreferredFoot(),
                request.getDisplayName(),
                request.getAppearance(),
                request.getTeamName(),
                request.getKitHomeId(),
                request.getKitAwayId(),
                request.getPatternId(),
                request.getCrest(),
                Boolean.TRUE.equals(request.getJoinExisting()));

        return issueTokens(saved);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                request.getUsernameOrEmail(), request.getPassword());
        try {
            authenticationManager.authenticate(authToken);
        } catch (AuthenticationException ex) {
            // Funnel every failure mode (no user / wrong password / etc.) into
            // one indistinguishable error. No enumeration, no timing hints
            // beyond what BCrypt already gives us.
            throw new BadCredentialsException("Invalid credentials");
        }
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        return issueTokens(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        JwtService.ParsedToken parsed = jwtService.parseRefreshToken(request.getRefreshToken());
        String storedUserId = redisTemplate.opsForValue().get(refreshKey(parsed.jti()));
        if (storedUserId == null) {
            throw new UnauthorizedException("REFRESH_TOKEN_REVOKED", "Refresh token is no longer valid");
        }
        if (!storedUserId.equals(parsed.subject().toString())) {
            // jti exists but points elsewhere — treat as compromise and burn it
            redisTemplate.delete(refreshKey(parsed.jti()));
            throw new UnauthorizedException("REFRESH_TOKEN_MISMATCH", "Refresh token is invalid");
        }
        // Rotate: delete old jti before minting a new pair
        redisTemplate.delete(refreshKey(parsed.jti()));

        User user = userRepository.findById(parsed.subject())
                .orElseThrow(() -> new UnauthorizedException("USER_GONE", "User no longer exists"));
        return issueTokens(user);
    }

    public void logout(String refreshToken) {
        // Best-effort revocation; we never error if the token is already
        // invalid, since "logout" should feel idempotent from the client's
        // perspective.
        try {
            JwtService.ParsedToken parsed = jwtService.parseRefreshToken(refreshToken);
            redisTemplate.delete(refreshKey(parsed.jti()));
        } catch (RuntimeException ignored) {
            // swallow
        }
    }

    private AuthResponse issueTokens(User user) {
        String access = jwtService.generateAccessToken(user);
        JwtService.ParsedToken accessParsed = jwtService.parseAccessToken(access);
        JwtService.RefreshToken refresh = jwtService.generateRefreshToken(user);

        Duration ttl = Duration.between(Instant.now(), refresh.expiresAt());
        if (!ttl.isZero() && !ttl.isNegative()) {
            UUID userId = user.getId();
            if (userId == null) {
                throw new IllegalStateException("User must be persisted");
            }
            redisTemplate.opsForValue().set(refreshKey(refresh.jti()), userId.toString(), ttl);
        }

        return new AuthResponse(
                "Bearer",
                access,
                accessParsed.expiresAt(),
                refresh.token(),
                refresh.expiresAt(),
                UserResponse.from(user)
        );
    }

    private String refreshKey(String jti) {
        return "refresh:" + jti;
    }
}
