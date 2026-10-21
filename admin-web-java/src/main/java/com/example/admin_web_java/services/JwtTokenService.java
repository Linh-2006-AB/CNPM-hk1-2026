package com.adminweb.services;

import com.adminweb.dto.AuthDto;
import com.adminweb.dto.TouristProfileDto;
import com.adminweb.models.Tourist;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class JwtTokenService {

    @Value("${jwt.issuer:TourGuideSystem.AdminWeb}")
    private String issuer;

    @Value("${jwt.audience:TourGuideSystem.UserMobile}")
    private String audience;

    @Value("${jwt.signing-key:}")
    private String signingKey;

    @Value("${jwt.expiration-days:30}")
    private int expirationDays;

    public AuthDto create(Tourist tourist) {
        if (signingKey == null || signingKey.isBlank()) {
            throw new IllegalStateException("Jwt:SigningKey is not configured.");
        }

        int validExpirationDays = Math.max(1, expirationDays);
        Instant now = Instant.now();
        Instant expiresAt = now.plus(validExpirationDays, ChronoUnit.DAYS);

        // Chuẩn bị thông tin Tên hiển thị (FullName)
        String fullName = tourist.getFullName();
        if (fullName == null || fullName.isBlank()) {
            fullName = tourist.getEmail();
        }
        if (fullName == null || fullName.isBlank()) {
            fullName = "Tourist #" + tourist.getId();
        }

        String email = tourist.getEmail() != null ? tourist.getEmail() : "";

        // Tạo HMAC-SHA Secret Key
        SecretKey key = Keys.hmacShaKeyFor(signingKey.getBytes(StandardCharsets.UTF_8));

        // Build JWT Token
        String token = Jwts.builder()
                .issuer(issuer)
                .audience().add(audience).and()
                .subject(String.valueOf(tourist.getId()))
                .claim("nameidentifier", String.valueOf(tourist.getId()))
                .claim("email", email)
                .claim("name", fullName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(key)
                .compact();

        // Build TouristProfileDto
        TouristProfileDto profile = new TouristProfileDto(
                tourist.getId(),
                email,
                tourist.getFullName() != null ? tourist.getFullName() : "",
                tourist.getCreatedAt()
        );

        return new AuthDto(token, expiresAt, profile);
    }
}
