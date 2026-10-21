package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AchievementDto {
    private String code = "";
    private String title = "";
    private String description = "";
    private String icon = "";
    private Integer requiredValue;
    private Integer currentValue;
    private Boolean isUnlocked;
    private LocalDateTime unlockedAt;
}