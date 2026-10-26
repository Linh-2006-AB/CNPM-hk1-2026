package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TouristPaymentPlanDto {
    private Integer id;
    private String planCode = "";
    private String planName = "";
    private BigDecimal price;
    private Integer durationDays;
    private String description = "";
}