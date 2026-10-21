package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TouristPremiumDto {
    private Boolean isPremium;
    private String planName = "";
    private LocalDateTime expiresAt;
    private Boolean vnPayAvailable;
    private Boolean momoAvailable;
    private Boolean momoDemoMode;
}