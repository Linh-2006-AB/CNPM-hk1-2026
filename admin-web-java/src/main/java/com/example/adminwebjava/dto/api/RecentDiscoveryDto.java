package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecentDiscoveryDto {
    private Integer poiId;
    private String poiName = "";
    private String method = "";
    private Integer points;
    private LocalDateTime discoveredAt;
}