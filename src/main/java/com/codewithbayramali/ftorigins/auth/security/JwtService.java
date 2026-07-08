package com.codewithbayramali.ftorigins.auth.security;

import com.codewithbayramali.ftorigins.auth.SecurityProperties;
import com.codewithbayramali.ftorigins.exception.UnauthorizedException;
import com.codewithbayramali.ftorigins.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecurityProperties properties;
    private final SecretKey key;

    public JwtService(SecurityProperties properties) {
        this.properties = properties;
        this.key = Keys.hmacShaKeyFor(properties.getJwt().getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public enum TokenType { ACCESS, REFRESH }

    public String generateAccessToken(User user) {
        UUID userId = user.getId();
        if (userId == null) {
            throw new IllegalStateException("User must be persisted");
        }
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(properties.getJwt().getAccessTokenExpirationMs());
        return Jwts.builder()
                .issuer(properties.getJwt().getIssuer())
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("type", TokenType.ACCESS.name())
                .claim("role", user.getRole().name())
                .claim("username", user.getUsername())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Returns the signed refresh token AND the jti used inside it, so the
     * caller can persist {@code refresh:{jti} -> userId} in Redis and rotate it
     * on use.
     */
    public RefreshToken generateRefreshToken(User user) {
        UUID userId = user.getId();
        if (userId == null) {
            throw new IllegalStateException("User must be persisted");
        }
        String jti = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(properties.getJwt().getRefreshTokenExpirationMs());
        String token = Jwts.builder()
                .issuer(properties.getJwt().getIssuer())
                .subject(userId.toString())
                .id(jti)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim("type", TokenType.REFRESH.name())
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        return new RefreshToken(token, jti, expiry);
    }

    public ParsedToken parse(String token) {
        Claims claims = parseClaims(token);

        String typeClaim = claims.get("type", String.class);
        TokenType type;
        try {
            type = TokenType.valueOf(typeClaim == null ? "" : typeClaim);
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedException("INVALID_TOKEN", "Token is invalid");
        }

        UUID subject;
        try {
            subject = UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new UnauthorizedException("INVALID_TOKEN", "Token is invalid");
        }

        String jti = claims.getId();
        if (jti == null || jti.isBlank()) {
            throw new UnauthorizedException("INVALID_TOKEN", "Token is invalid");
        }

        return new ParsedToken(
                subject,
                jti,
                type,
                claims.get("role", String.class),
                claims.getExpiration().toInstant()
        );
    }

    public ParsedToken parseAccessToken(String token) {
        ParsedToken parsed = parse(token);
        if (parsed.type() != TokenType.ACCESS) {
            throw new UnauthorizedException("INVALID_TOKEN_TYPE", "Expected access token");
        }
        return parsed;
    }

    public ParsedToken parseRefreshToken(String token) {
        ParsedToken parsed = parse(token);
        if (parsed.type() != TokenType.REFRESH) {
            throw new UnauthorizedException("INVALID_TOKEN_TYPE", "Expected refresh token");
        }
        return parsed;
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .requireIssuer(properties.getJwt().getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new UnauthorizedException("TOKEN_EXPIRED", "Token has expired");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("INVALID_TOKEN", "Token is invalid");
        }
    }

    public record ParsedToken(UUID subject, String jti, TokenType type, String role, Instant expiresAt) { }

    public record RefreshToken(String token, String jti, Instant expiresAt) { }
}