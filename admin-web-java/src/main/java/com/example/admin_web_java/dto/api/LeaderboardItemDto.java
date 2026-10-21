package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardItemDto {
    private Integer rank;
    private String displayName = "";
    private Integer totalPoints;
    private Integer discoveredPoiCount;
    private String rankName = "";
    private Boolean isCurrentUser;
}