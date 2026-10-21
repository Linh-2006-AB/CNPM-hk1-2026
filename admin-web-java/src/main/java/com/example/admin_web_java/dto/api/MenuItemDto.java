package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MenuItemDto {
    private Integer id;
    private Integer poiId;
    private String name = "";
    private String description = "";
    private BigDecimal price;
    private String currency = "VND";
    private String imageUrl;
}