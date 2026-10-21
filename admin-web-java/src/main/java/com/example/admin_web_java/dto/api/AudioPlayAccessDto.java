package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AudioPlayAccessDto {
    private Boolean isPremium;
    private Integer dailyLimit;
    private Integer usedToday;
    private Integer remainingToday;
}