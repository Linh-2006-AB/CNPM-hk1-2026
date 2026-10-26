package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummaryDto {
    private Integer poiId;
    private Double averageRating;
    private Integer ratingCount;
    private ReviewDto myReview;
    private List reviews = new ArrayList<>();
}