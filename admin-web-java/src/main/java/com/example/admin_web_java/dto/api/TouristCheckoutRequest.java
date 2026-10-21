package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TouristCheckoutRequest {
    private Integer planId;
    private String paymentMethod = "Manual";
}