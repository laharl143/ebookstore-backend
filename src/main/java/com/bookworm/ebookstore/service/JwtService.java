package com.bookworm.ebookstore.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final long expirationSeconds;

    public JwtService(
            JwtEncoder jwtEncoder,
            Clock clock,
            @Value("${app.jwt.expiration-seconds:3600}") long expirationSeconds) {
        this.jwtEncoder = jwtEncoder;
        this.clock = clock;
        this.expirationSeconds = expirationSeconds;
    }

    public String generateToken(Long userId) {
        Instant now = clock.instant();
        Instant expiresAt = now.plus(expirationSeconds, ChronoUnit.SECONDS);

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("bookworm")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(String.valueOf(userId))
                .build();

        JwsHeader header = JwsHeader.with(() -> "HS256").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
