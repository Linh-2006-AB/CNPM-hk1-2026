package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DiscoveryResultDto {
    private Boolean isNewDiscovery;
    private Integer pointsAwarded;
    private String message = "";
    private List newlyUnlockedAchievements = new ArrayList<>();
    private AchievementSummaryDto summary = new AchievementSummaryDto();
}