package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TouristCheckoutDto {
    private Integer paymentId;
    private String status;
    private String checkoutUrl;
}
