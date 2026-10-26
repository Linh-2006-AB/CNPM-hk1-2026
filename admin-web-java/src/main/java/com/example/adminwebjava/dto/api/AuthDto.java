package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthDto {
    private String accessToken = "";
    private OffsetDateTime expiresAt;
    private TouristProfileDto profile = new TouristProfileDto();
}