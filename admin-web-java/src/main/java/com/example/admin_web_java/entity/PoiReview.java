package com.example.adminwebjava.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "PoiReviews")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PoiReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "PoiId", insertable = false, updatable = false)
    private Integer poiId;

    @Column(name = "TouristId", insertable = false, updatable = false)
    private Integer touristId;

    @Min(value = 1, message = "Rating tối thiểu là 1")
    @Max(value = 5, message = "Rating tối đa là 5")
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    // Quan hệ với bảng Poi
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PoiId")
    private Poi poi;

    // Quan hệ với bảng Tourist
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TouristId")
    private Tourist tourist;
}