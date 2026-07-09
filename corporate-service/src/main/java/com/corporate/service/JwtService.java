package com.corporate.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import com.corporate.entity.AppUser;

@Service
public class JwtService {

    /**
     * The dev/test-only default secret. Kept as a constant so the guard below and
     * the @Value fallback share one source of truth. If this ever appears in a
     * real deployment we want to crash loudly rather than sign tokens with it.
     */
    public static final String DEV_DEFAULT_SECRET =
            "change-me-in-prod-this-is-a-32-byte-default-secret-key!!";

    private final SecretKey key;
    private final long ttlSeconds;

    public JwtService(
            @Value("${app.jwt.secret:" + DEV_DEFAULT_SECRET + "}") String secret,
            @Value("${app.jwt.ttl-seconds:86400}") long ttlSeconds,
            Environment env
    ) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("app.jwt.secret must be at least 32 bytes for HS256");
        }
        boolean isProd = Arrays.asList(env.getActiveProfiles()).contains("prod");
        if (isProd && DEV_DEFAULT_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "Refusing to start with the built-in dev JWT secret while the 'prod' profile is active. "
                            + "Set the JWT_SECRET environment variable to at least 32 random bytes."
            );
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.ttlSeconds = ttlSeconds;
    }

    public String issue(AppUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("name", user.getFullName())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(Duration.ofSeconds(ttlSeconds))))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long getTtlSeconds() { return ttlSeconds; }
}
