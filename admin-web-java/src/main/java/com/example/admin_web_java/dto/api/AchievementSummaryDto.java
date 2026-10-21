package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AchievementSummaryDto {
    private Integer totalPoints;
    private Integer discoveredPoiCount;
    private Integer totalPoiCount;
    private Double completionPercentage;
    private String rankName = "";
    private String nextRankName = "";
    private Integer pointsToNextRank;
    private Double rankProgress;
    private List achievements = new ArrayList<>();
    private List recentDiscoveries = new ArrayList<>();
}