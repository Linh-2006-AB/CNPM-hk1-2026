package com.example.adminwebjava.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "TouristBookmarks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TouristBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "TouristId", insertable = false, updatable = false)
    private Integer touristId;

    @Column(name = "PoiId", insertable = false, updatable = false)
    private Integer poiId;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TouristId")
    private Tourist tourist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PoiId")
    private Poi poi;
}