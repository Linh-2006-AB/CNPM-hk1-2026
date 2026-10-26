package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuOrderCheckoutDto {
    private Integer paymentId;
    private Integer orderId;
    private String paymentMethod = "";
    private String status = "";
    private String checkoutUrl;
}