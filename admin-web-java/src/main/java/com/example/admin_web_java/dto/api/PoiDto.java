package com.example.adminwebjava.dto.api;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoiDto {
    private Integer id;
    private String qrCodeToken = "";
    private String name = "";
    private String shortDescription = "";
    private String fullDescription = "";
    private String coverImageUrl;
    private Double latitude;
    private Double longitude;
    private Double radius;
    private Double averageRating;
    private Integer ratingCount;
    private Boolean isFeatured;
    private String ownerBusinessName;
    private List recentReviews = new ArrayList<>();
    private List categoryIds = new ArrayList<>();
    private List translations = new ArrayList<>();
}