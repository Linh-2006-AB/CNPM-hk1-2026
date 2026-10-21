package com.example.adminwebjava.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Pois")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Poi {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(precision = 10, scale = 7, nullable = false)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7, nullable = false)
    private BigDecimal longitude;

    @Column(nullable = false)
    private Integer radius = 50;

    @Column(nullable = false)
    private String qrCodeToken = "";

    @Column(nullable = false)
    private String status = "Pending";

    @Column(columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "CreatedBy", insertable = false, updatable = false)
    private Integer createdBy;

    @Column(name = "OwnerProfileId", insertable = false, updatable = false)
    private Integer ownerProfileId;

    private String coverImageUrl;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CreatedBy")
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "OwnerProfileId")
    private OwnerProfile ownerProfile;

    @OneToMany(mappedBy = "poi", cascade = CascadeType.ALL)
    private List translations = new ArrayList<>();

    @OneToMany(mappedBy = "poi", cascade = CascadeType.ALL)
    private List mediaTasks = new ArrayList<>();

    @OneToMany(mappedBy = "poi", cascade = CascadeType.ALL)
    private List mediaAssets = new ArrayList<>();

    @OneToMany(mappedBy = "poi", cascade = CascadeType.ALL)
    private List beacons = new ArrayList<>();

    @OneToMany(mappedBy = "poi", cascade = CascadeType.ALL)
    private List poiCategories = new ArrayList<>();
}