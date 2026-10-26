package com.example.adminwebjava.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tour_pois")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TourPoi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "TourId", insertable = false, updatable = false)
    private Integer tourId;

    @Column(name = "PoiId", insertable = false, updatable = false)
    private Integer poiId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TourId")
    private Tour tour;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PoiId")
    private Poi poi;
}
