package com.example.adminwebjava.dto.view;

import com.example.adminwebjava.entity.Tourist;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitorAchievementDetailsViewModel {

    private Tourist tourist = new Tourist();
    private Integer totalPoints;
    private Integer discoveredPoiCount;
    private Integer totalPoiCount;
    private Double completionPercentage;
    private String rankName = "";
    private String nextRankName = "";
    private Integer pointsToNextRank;
    private List achievements = new ArrayList<>();
    private List discoveries = new ArrayList<>();
}