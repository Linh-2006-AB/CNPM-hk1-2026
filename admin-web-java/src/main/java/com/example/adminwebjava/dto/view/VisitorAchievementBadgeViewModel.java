package com.example.adminwebjava.dto.view;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VisitorAchievementBadgeViewModel {

    private String title = "";
    private String description = "";
    private String icon = "";
    private Integer currentValue;
    private Integer requiredValue;
    private Boolean isUnlocked;
}