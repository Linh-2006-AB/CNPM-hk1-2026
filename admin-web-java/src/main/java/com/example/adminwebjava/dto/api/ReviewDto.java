package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDto {
    private Integer id;
    private Integer poiId;
    private Integer touristId;
    private String touristName = "";
    private Integer rating;
    private String comment = "";
    private LocalDateTime createdAt;
}